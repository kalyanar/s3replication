package com.adobe.ams.replication.aws.s3;

import org.apache.sling.caconfig.annotation.Configuration;
import org.apache.sling.caconfig.annotation.Property;

@Configuration(label = "CA config based S3 replication ")
public @interface S3ReplicationConfig {

  @Property(label = "Bucket Name")
  String bucketName() default "amazonsdapoc";

  @Property(label = "Extract the zip and upload individual files?")
  boolean extractAndUpload() default true;


  @Property(label = "Randomize Name")
  boolean randomizeName() default false;
}
