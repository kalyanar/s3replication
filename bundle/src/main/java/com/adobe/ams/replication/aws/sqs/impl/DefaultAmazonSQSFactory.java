package com.adobe.ams.replication.aws.sqs.impl;

import com.adobe.ams.replication.aws.sqs.AmazonSQSFactory;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component(
        name = "com.adobe.ams.replication.impl.DefaultAmazonSQSFactory",
        metatype = true,
        label = "SQS DefaultAmazonSQSFactory per region",
        description = "SQS DefaultAmazonSQSFactory per region"
)
@Service(value=AmazonSQSFactory.class)
public class DefaultAmazonSQSFactory implements AmazonSQSFactory {
  private static final String DEFAULT_REGION="us-east-1";
  private Map<String, AmazonSQS> amazonSQSClientByRegion;

  @Override
  public synchronized AmazonSQS get(String regionName) {
    Region region = RegionUtils.getRegion(regionName);
    if(amazonSQSClientByRegion.containsKey(regionName)){
      return amazonSQSClientByRegion.get(regionName);
    }else {
      AmazonSQS client= AmazonSQSClientBuilder.standard()
              //.withCredentials(new ProfileCredentialsProvider("bpbu13"))
              .withRegion(regionName) // The first region to try your request
              // against
              // again in the correct region
              .build();

      amazonSQSClientByRegion.put(regionName,client);
      return client;
    }
  }

  @Override
  public AmazonSQS get(String regionName, String profile) {
    if(StringUtils.isEmpty(profile)){
      return get(regionName);
    }
    Region region = RegionUtils.getRegion(regionName);
    if(amazonSQSClientByRegion.containsKey(regionName+profile)){
      return amazonSQSClientByRegion.get(regionName+profile);
    }else {
      AmazonSQS client= AmazonSQSClientBuilder.standard()
              .withCredentials(new ProfileCredentialsProvider(profile))
              .withRegion(regionName) // The first region to try your request
              // against
              // again in the correct region
              .build();

      amazonSQSClientByRegion.put(regionName+profile,client);
      return client;
    }
  }

  @Activate
  protected void activate(Map <String, Object> properties) {
    amazonSQSClientByRegion=new ConcurrentHashMap<>();
    AmazonSQS amazonSQS= AmazonSQSClientBuilder
            .standard()
            .withRegion(DEFAULT_REGION) // The first region to try your request
            // against
            // again in the correct region
            .build();
    amazonSQSClientByRegion.put(DEFAULT_REGION,amazonSQS );
  }
}
