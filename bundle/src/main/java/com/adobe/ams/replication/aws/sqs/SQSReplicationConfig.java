package com.adobe.ams.replication.aws.sqs;

import org.apache.sling.caconfig.annotation.Configuration;
import org.apache.sling.caconfig.annotation.Property;

@Configuration(label = "CA config based SQS replication ")
public @interface SQSReplicationConfig {
  @Property(label = "SQS queue url")
  String queueURL() default "";

  @Property(label = "Persist data with message?")
  boolean persistDataWithMessage() default false;


  @Property(label = "Bucket Name")
  String bucketName() default "";

  @Property(label = "Randomize Name")
  boolean randomizeName() default false;

  @Property(label = "Message group ID")
  String messagegroupid() default "aws-sda";

}
