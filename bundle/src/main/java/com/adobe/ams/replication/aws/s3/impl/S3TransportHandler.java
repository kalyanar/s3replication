package com.adobe.ams.replication.aws.s3.impl;

import com.adobe.ams.publishleader.LeaderProvider;
import com.adobe.ams.replication.aws.AWSReplicationConfig;
import com.adobe.ams.replication.aws.impl.AWSTransportHandler;
import com.adobe.ams.replication.aws.s3.AmazonS3Factory;
import com.adobe.ams.replication.aws.s3.S3ReplicationConfig;
import com.adobe.ams.replication.constants.ReplicationConstants;
import com.adobe.ams.replication.provider.EmbeddedReferenceProvider;
import com.adobe.ams.replication.utils.ReplicationUtils;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.day.cq.replication.AgentConfig;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationContent;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.ReplicationLog;
import com.day.cq.replication.ReplicationResult;
import com.day.cq.replication.ReplicationTransaction;
import com.day.cq.replication.TransportContext;
import com.day.cq.replication.TransportHandler;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.caconfig.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


@Component(metatype = false,immediate = true, enabled = true,label =
        "Tansport handler to upload to s3")
@Service(value = TransportHandler.class)
public class S3TransportHandler extends AWSTransportHandler implements TransportHandler {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private static final String TRANSPORT_SCHEME = "s3";

  private static final String SERVICE_NAME = "aws-agent";
  private static final Map<String, Object> AUTH_INFO;

  static {
    AUTH_INFO = Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, (Object) SERVICE_NAME);
  }


  @Reference
  private AmazonS3Factory amazonS3Factory;

  @Reference(name = "erp", referenceInterface = EmbeddedReferenceProvider.class,
          cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy =
          ReferencePolicy.DYNAMIC)
  private final Map<String, EmbeddedReferenceProvider> erp = new
          ConcurrentHashMap<>();
  @Reference
  ResourceResolverFactory resourceResolverFactory;

  private String  tmpDirStr;

  protected void bindErp(final EmbeddedReferenceProvider erp,
                         final
                         Map<String, Object> props) {
    synchronized (this.erp) {
      this.erp.put(erp.getProviderName(), erp);
    }
  }

  protected void unbindErp(final EmbeddedReferenceProvider erp, final
  Map<String, Object> props) {
    synchronized (this.erp) {
      this.erp.remove(erp.getProviderName());
    }
  }
  @Reference
  LeaderProvider leaderProvider;

  @Override
  public boolean canHandle(AgentConfig agentConfig) {
    String uri = agentConfig == null ? null : agentConfig.getTransportURI();
    return uri != null && (uri.startsWith(TRANSPORT_SCHEME))&&leaderProvider
            .isLeader();  }

  @Override
  public ReplicationResult deliver(TransportContext transportContext, ReplicationTransaction replicationTransaction) throws ReplicationException {
    String path=replicationTransaction.getAction().getPath();
    ResourceResolver resourceResolver=getServiceResourceResolver();

    Resource contentResource=resourceResolver.getResource(path);

    S3ReplicationConfig s3ReplicationConfig=contentResource.adaptTo
            (ConfigurationBuilder.class).as(S3ReplicationConfig.class);
    AWSReplicationConfig awsReplicationConfig=contentResource.adaptTo
            (ConfigurationBuilder.class).as(AWSReplicationConfig.class);
    String bucketName=s3ReplicationConfig.bucketName();

    if(replicationTransaction.getAction().getType() == ReplicationActionType.ACTIVATE){
      ReplicationContent content = replicationTransaction.getContent();
      ZipInputStream in =null;
      try{

        InputStream input = content.getInputStream();
        if (input != null) {
          in=new ZipInputStream(input);
          if(s3ReplicationConfig.extractAndUpload()){
            extractZipAndUpload(bucketName, in, replicationTransaction.getLog()
                    ,s3ReplicationConfig,awsReplicationConfig);
          }else {
            String filename="";
            if(s3ReplicationConfig.randomizeName()) {
              filename = UUID.randomUUID().toString();
            }else {
              try {
                filename=ReplicationUtils.getMD5Checksum(path.getBytes());
              } catch (IOException e) {
                filename=path;
              }
            }
            uploadToS3(bucketName,input,replicationTransaction.getLog(),
                    awsReplicationConfig,filename);
          }

          return ReplicationResult.OK;
        }
      }catch (IOException e) {
        String msg = String.format(
                "Unable to deserialize replication content: %s",
                e.getMessage());
        throw new ReplicationException(msg, e);
      } finally {
        IOUtils.closeQuietly(in);
      }
      return ReplicationResult.OK;
    }else if(replicationTransaction.getAction().getType() ==
            ReplicationActionType.DEACTIVATE){
      ReplicationConstants.FILE_TYPE file_type= ReplicationUtils
              .guessFileType(path);
      String pathToBeReplicated =ReplicationUtils.getPathTobeReplicated
              (path,awsReplicationConfig.defaultSuffix(),file_type,
              resourceResolver,awsReplicationConfig.enableURLShortening());
      DeleteObjectRequest request = new DeleteObjectRequest(bucketName,
             path );
      AmazonS3 amazonS3=amazonS3Factory.get(awsReplicationConfig.regionName(),
                awsReplicationConfig.profile());


      amazonS3.deleteObject
              (request);
      return ReplicationResult.OK;
    }
    return ReplicationResult.OK;
  }

  private void extractZipAndUpload(String bucketName,ZipInputStream in,
                              ReplicationLog
          log,S3ReplicationConfig s3ReplicationConfig,AWSReplicationConfig awsReplicationConfig)
          throws IOException {
    byte[] buffer = new byte[1024];
    ZipEntry entry;
    File destDir = new File(tmpDirStr);
    while ((entry = in.getNextEntry()) != null) {

      String name = entry.getName();
      if (name.startsWith("/")) {
        name = name.substring(1);
      }
//      File newFile = newFile(destDir, entry);
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      int len;
      while ((len = in.read(buffer)) > 0) {
        outputStream.write(buffer, 0, len);
      }
      log.debug("Extracting entry %s to %s", entry.getName(),entry.getName());

      String mimeType = ReplicationConstants.FileMimeType.fromExtension(FilenameUtils.getExtension
              (entry.getName())).mimeType();
      ObjectMetadata meta = new ObjectMetadata();
      meta.setContentLength(outputStream.toByteArray().length);
      meta.setContentType(mimeType);
      byte[] bytes=outputStream.toByteArray();
      InputStream is = new ByteArrayInputStream(bytes);
      //use the newFile to upload

      try{
        PutObjectRequest request = new PutObjectRequest(bucketName, name, is,
                meta);
//        request.setMetadata(meta);
        log.debug("uploading entry %s to %s",entry.getName(),bucketName);
        amazonS3Factory.get(awsReplicationConfig.regionName(),
                awsReplicationConfig.profile()).putObject
                (request);
        log.info("uploaded entry %s to %s",entry.getName(),bucketName);
      }catch (Exception e){
        e.printStackTrace();
      }
      is.close();
      outputStream.close();

    }

  }
  @Activate
  protected void activate(Map <String, Object> properties) {
    tmpDirStr = System.getProperty("java.io.tmpdir");

  }


  @Override
  protected void persist(AWSReplicationConfig awsReplicationConfig,
                         PutObjectRequest request) {

    amazonS3Factory.get(awsReplicationConfig.regionName(),
            awsReplicationConfig.profile()).putObject
            (request);
  }

  private ResourceResolver getServiceResourceResolver(){
    ResourceResolver resourceResolver= null;
    try {
      resourceResolver = resourceResolverFactory
              .getServiceResourceResolver(AUTH_INFO);
    } catch (LoginException e) {
      logger.error(e.getMessage(),e);
    }
    return resourceResolver;
  }
}
