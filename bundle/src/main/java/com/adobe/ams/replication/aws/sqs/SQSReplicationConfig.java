package com.adobe.ams.replication.aws.sqs;

import org.apache.sling.caconfig.annotation.Configuration;
import org.apache.sling.caconfig.annotation.Property;

@Configuration(label = "CA config based SQS replication ")
public @interface SQSReplicationConfig {
  @Property(label = "SQS queue url")
  String queueURL() default "https://sqs.us-west-1.amazonaws.com/172888048558/amsvcs-64-dev-sqs";
//https://sqs.us-west-1.amazonaws.com/172888048558/amsvcs-64-dev-sqs
  //https://sqs.us-west-2.amazonaws" +
//          ".com/086466013689/AEMPublishEventQueue
  @Property(label = "Persist data with message?")
  boolean persistDataWithMessage() default false;


  @Property(label = "Bucket Name")
  String bucketName() default "";

  @Property(label = "Randomize Name")
  boolean randomizeName() default false;

  @Property(label = "Message group ID")
  String messagegroupid() default "aws-sda";

}
