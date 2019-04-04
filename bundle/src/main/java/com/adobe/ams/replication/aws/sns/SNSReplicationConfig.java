package com.adobe.ams.replication.aws.sns;

import org.apache.sling.caconfig.annotation.Configuration;
import org.apache.sling.caconfig.annotation.Property;

@Configuration(label = "CA config based SNS replication ")
public @interface SNSReplicationConfig {
  @Property(label = "SNS Topic")
  String topic() default "arn:aws:sns:us-east-1:887895398625:AmazonSDA";

  @Property(label = "Bucket Name")
  String bucketName() default "amazonsdapoc";

  @Property(label = "Randomize Name")
  boolean randomizeName() default false;

  @Property(label = "Persist data with message?")
  boolean persistDataWithMessage() default true;

}
