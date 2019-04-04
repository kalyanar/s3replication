package com.adobe.ams.replication.aws.impl;

import com.adobe.ams.replication.aws.AWSReplicationConfig;
import com.adobe.ams.replication.provider.EmbeddedReferenceProvider;
import com.adobe.ams.replication.provider.MockHttpRequest;
import com.adobe.ams.replication.provider.MockHttpResponse;
import com.adobe.ams.replication.utils.ReplicationUtils;
import com.day.cq.replication.ContentBuilder;
import com.day.cq.replication.ReplicationContent;
import com.day.cq.replication.ReplicationContentFactory;
import org.apache.commons.io.IOUtils;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.engine.SlingRequestProcessor;

import javax.servlet.ServletException;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.adobe.ams.replication.constants.ReplicationConstants
        .FILE_TYPE;

public abstract class AWSContentBuilder implements ContentBuilder {
  protected ReplicationContent create(String path, ResourceResolver
          resourceResolver, AWSReplicationConfig
          awsReplicationConfig,ReplicationContentFactory replicationContentFactory) throws IOException {
    Path tempDir=Files.createTempDirectory(null);

    if(path.startsWith(awsReplicationConfig.pathToInclude())) {
      Map<String, String> files = buildContent(path, resourceResolver,
              tempDir,awsReplicationConfig);
      if (!files.isEmpty()) {
        try {
          File zip = createZip(files);
//          Files.delete(tempDir);
          return replicationContentFactory.create(getMimeType(), zip, true);
        } catch (IOException e) {
          e.printStackTrace();
        }

      }
    }
    return ReplicationContent.VOID;
  }
  protected Map<String,String> buildContent(String path, ResourceResolver
          resourceResolver, Path tempDir,AWSReplicationConfig awsReplicationConfig){
    FILE_TYPE file_type= ReplicationUtils.guessFileType(path);


    String pathToBeReplicated =ReplicationUtils.getPathTobeReplicated(path,
            awsReplicationConfig.defaultSuffix(),file_type,
            resourceResolver,awsReplicationConfig.enableURLShortening());
    Map<String,String> files=new HashMap<>();
    try {
      File downloadedFile=downloadContent(pathToBeReplicated,
              resourceResolver,
              file_type.getFileType(), tempDir);
      files.put(pathToBeReplicated,downloadedFile.getAbsolutePath());
      if(awsReplicationConfig.alsoReplicateReferencedContent()){
        files.putAll(downloadReferences(pathToBeReplicated,file_type,
                downloadedFile,resourceResolver, tempDir,awsReplicationConfig));
      }
    } catch (IOException e) {
      e.printStackTrace();

    } catch (ServletException e) {
      e.printStackTrace();
    }
    return files;
  }

  private Map<String,String> downloadReferences(String path,FILE_TYPE
          file_type,File file,ResourceResolver resourceResolver,Path tempDir,
                                                AWSReplicationConfig awsReplicationConfig){
    boolean shouldParseEmbeddedContent=shouldParseEmbeddedContent(file_type);
    Map<String,String> files=new HashMap<>();

    if(shouldParseEmbeddedContent){
      EmbeddedReferenceProvider embeddedReferenceProvider=getERP().get
              (FILE_TYPE.valueOf(file_type.name()).toString());
      if(embeddedReferenceProvider!=null){
        try {
          Set<String> references=embeddedReferenceProvider.provideReferences
                  (path, Files.readAllBytes(file.toPath()));
          for(String reference:references){

            FILE_TYPE refFileType=ReplicationUtils.guessFileType(reference);
            files.putAll(buildContent(reference,resourceResolver, tempDir,awsReplicationConfig));

          }
        } catch (IOException e) {

        }
      }
    }
    return files;
  }
  protected File createZip(Map<String,String> files) throws IOException {
    File tmpFile = File.createTempFile("s3temp", ".zip");
    ZipOutputStream out = null;
    boolean successful = false;
    try {
      out = new ZipOutputStream(new FileOutputStream(tmpFile));
      for (Map.Entry<String, String> entry : files.entrySet()) {
        out.putNextEntry(new ZipEntry(entry.getKey()));
        out.write(Files.readAllBytes(Paths.get(entry.getValue())));
      }
      successful = true;
      return tmpFile;
    } finally {
      IOUtils.closeQuietly(out);
      if (!successful) {
        if (!tmpFile.delete()) {
          throw new IOException("tmp zip file could not be deleted");
        }
      }
    }

  }
  private File downloadContent(String path, ResourceResolver
          resourceResolver,String extension,Path tempDir) throws
          IOException,
          ServletException {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    try {
      final MockHttpRequest req = new MockHttpRequest("GET",
              path);
      final MockHttpResponse resp = new MockHttpResponse(bos);
      getRequestProcessor().processRequest(req, resp, resourceResolver);
      Path pathToFile = Paths.get(tempDir.toAbsolutePath()+path);
      Files.createDirectories(pathToFile.getParent());

      File tmpFile = Files.createFile(pathToFile).toFile();
      try(OutputStream outputStream = new FileOutputStream(tmpFile)) {
        bos.writeTo(outputStream);
      }
      return tmpFile;
    }finally {
      bos.close();
    }
  }
  private boolean shouldParseEmbeddedContent(FILE_TYPE
                                                     file_type){
    String key=FILE_TYPE
            .valueOf(file_type.name()).toString();
    return getMapReferences().containsKey(key);
  }

  protected abstract Map<String,String> getMapReferences();
  protected abstract Map<String, EmbeddedReferenceProvider> getERP();
  protected abstract SlingRequestProcessor getRequestProcessor();
  protected abstract String getMimeType();
}
