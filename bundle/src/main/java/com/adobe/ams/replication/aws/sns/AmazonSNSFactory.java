package com.adobe.ams.replication.aws.sns;

import com.amazonaws.services.sns.AmazonSNS;

public interface AmazonSNSFactory {
  AmazonSNS get(String regionName);
  AmazonSNS get(String regionName,String profile);
}
