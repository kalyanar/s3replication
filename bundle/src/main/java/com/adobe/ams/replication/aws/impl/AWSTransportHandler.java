package com.adobe.ams.replication.aws.impl;

import com.adobe.ams.replication.aws.AWSReplicationConfig;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.day.cq.replication.ReplicationLog;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;

public abstract class AWSTransportHandler  {

  protected   File newFile(File destinationDir, ZipEntry zipEntry) throws
          IOException {
    File destFile = new File(destinationDir, zipEntry.getName());

    String destDirPath = destinationDir.getCanonicalPath();
    String destFilePath = destFile.getCanonicalPath();

    if (!destFilePath.startsWith(destDirPath + File.separator)) {
      throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
    }

    return destFile;
  }
  protected boolean uploadToS3(String bucketName,InputStream in,
                          ReplicationLog
                                  log,AWSReplicationConfig
                                  awsReplicationConfig,String fileName){
    try{
      String mimeType = "application/zip";
      ObjectMetadata meta = new ObjectMetadata();
      meta.setContentType(mimeType);
      PutObjectRequest request = new PutObjectRequest(bucketName, fileName,
              in,meta);
      //request.setMetadata(meta);
      log.debug("uploading entry %s to %s",fileName,bucketName);

      persist(awsReplicationConfig,request);
      log.info("uploaded entry %s to %s",fileName,bucketName);
    }catch (Exception e){
      log.error(e.getMessage(),e);
      return false;
    }
    return true;
  }
  protected abstract void persist(AWSReplicationConfig awsReplicationConfig,
                                  PutObjectRequest request);
}
