package com.adobe.ams.replication.aws.sns.impl;

import com.adobe.ams.publishleader.LeaderProvider;
import com.adobe.ams.replication.aws.AWSReplicationConfig;
import com.adobe.ams.replication.aws.impl.AWSTransportHandler;
import com.adobe.ams.replication.aws.s3.AmazonS3Factory;
import com.adobe.ams.replication.aws.sns.AmazonSNSFactory;
import com.adobe.ams.replication.aws.sns.SNSReplicationConfig;
import com.adobe.ams.replication.constants.ReplicationConstants;
import com.adobe.ams.replication.utils.ReplicationUtils;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.sns.model.MessageAttributeValue;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import com.day.cq.replication.AgentConfig;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationContent;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.ReplicationResult;
import com.day.cq.replication.ReplicationTransaction;
import com.day.cq.replication.TransportContext;
import com.day.cq.replication.TransportHandler;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.caconfig.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


@Component(metatype = false,immediate = true, enabled = true,label =
        "Tansport handler to upload to s3")
@Service(value = TransportHandler.class)
public class SNSTransportHandler extends AWSTransportHandler implements TransportHandler {
  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private static final String TRANSPORT_SCHEME = "sns";

  private static final String SERVICE_NAME = "aws-agent";
  private static final Map<String, Object> AUTH_INFO;

  static {
    AUTH_INFO = Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, (Object) SERVICE_NAME);
  }


  @Reference
  ResourceResolverFactory resourceResolverFactory;

  private String  tmpDirStr;
  @Reference
  LeaderProvider leaderProvider;

  @Override
  public boolean canHandle(AgentConfig agentConfig) {
    String uri = agentConfig == null ? null : agentConfig.getTransportURI();
    return uri != null && (uri.startsWith(TRANSPORT_SCHEME))&&leaderProvider
            .isLeader();  }

  @Reference
  private AmazonSNSFactory amazonSNSFactory;

  @Reference
  private AmazonS3Factory amazonS3Factory;

  @Override
  public ReplicationResult deliver(TransportContext transportContext, ReplicationTransaction replicationTransaction) throws ReplicationException {
    String path=replicationTransaction.getAction().getPath();
    ResourceResolver resourceResolver=getServiceResourceResolver();

    Resource contentResource=resourceResolver.getResource(path);

    SNSReplicationConfig snsReplicationConfig=contentResource.adaptTo
            (ConfigurationBuilder.class).as(SNSReplicationConfig.class);
    AWSReplicationConfig awsReplicationConfig=contentResource.adaptTo
            (ConfigurationBuilder.class).as(AWSReplicationConfig.class);
    boolean persist=false;
    String filename="";

    if(snsReplicationConfig.persistDataWithMessage()){
      ReplicationContent content = replicationTransaction.getContent();
      if(snsReplicationConfig.randomizeName()) {
        filename = UUID.randomUUID().toString();
      }else {
        try {
          filename=ReplicationUtils.getMD5Checksum(path.getBytes());
        } catch (IOException e) {
          filename=path;
        }
      }
      try {
       persist= uploadToS3(snsReplicationConfig.bucketName(),content
                       .getInputStream(),
                replicationTransaction.getLog(),awsReplicationConfig,filename);
      } catch (IOException e) {
        logger.error(e.getMessage(),e);
      }
    }
    publishToTopic(snsReplicationConfig,awsReplicationConfig,path
    ,resourceResolver,replicationTransaction,persist,filename);
    return ReplicationResult.OK;
  }
  private void publishToTopic(SNSReplicationConfig snsReplicationConfig,
                              AWSReplicationConfig awsReplicationConfig,
                              String path,ResourceResolver resourceResolver,
                              ReplicationTransaction replicationTransaction,
                              boolean persist,String filename){
    ReplicationConstants.FILE_TYPE file_type= ReplicationUtils.guessFileType(path);

    String pathToBeReplicated =ReplicationUtils.getPathTobeReplicated(path,
            awsReplicationConfig.defaultSuffix(),file_type,
            resourceResolver,awsReplicationConfig.enableURLShortening());
    final String msg = pathToBeReplicated+" "+replicationTransaction
            .getAction().getType() .toString();
    final Map<String, MessageAttributeValue> messageAttributes = new HashMap<>();
    messageAttributes.put("action", new MessageAttributeValue()
            .withDataType("String")
            .withStringValue(replicationTransaction.getAction().getType()
                    .toString()));
    messageAttributes.put("path", new MessageAttributeValue()
            .withDataType("String")
            .withStringValue(pathToBeReplicated));
    if(snsReplicationConfig.persistDataWithMessage()&&replicationTransaction
            .getAction().getType()==ReplicationActionType.ACTIVATE &&persist) {
      messageAttributes.put("persistedinS3", new MessageAttributeValue()
              .withDataType("String")
              .withStringValue("true"));
      messageAttributes.put("pathins3", new MessageAttributeValue()
              .withDataType("String")
              .withStringValue("s3://"+snsReplicationConfig.bucketName()
                      +"/"+filename));

    }
    final PublishRequest publishRequest = new PublishRequest
            (snsReplicationConfig.topic(), msg);
    publishRequest.setMessageAttributes(messageAttributes);
    final PublishResult publishResponse =  amazonSNSFactory.get(awsReplicationConfig.regionName(),
            awsReplicationConfig.profile()).publish(publishRequest);

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

  @Override
  protected void persist(AWSReplicationConfig awsReplicationConfig,
                         PutObjectRequest request) {
      amazonS3Factory.get(awsReplicationConfig.regionName(),
              awsReplicationConfig.profile()).putObject
              (request);
  }
}
