package com.adobe.ams.replication.aws.sqs;

import com.amazonaws.services.sqs.AmazonSQS;

public interface AmazonSQSFactory {
  AmazonSQS get(String regionName);
  AmazonSQS get(String regionName,String profile);
}
