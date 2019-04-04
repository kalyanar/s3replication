package com.adobe.ams.replication.aws.s3;

import com.amazonaws.services.s3.AmazonS3;

public interface AmazonS3Factory {
  AmazonS3 get(String regionName);
  AmazonS3 get(String regionName,String profile);

}
