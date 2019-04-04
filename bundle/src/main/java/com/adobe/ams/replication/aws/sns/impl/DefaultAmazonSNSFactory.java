package com.adobe.ams.replication.aws.sns.impl;

import com.adobe.ams.replication.aws.sns.AmazonSNSFactory;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component(
        name = "com.adobe.ams.replication.impl.DefaultAmazonSNSFactory",
        metatype = true,
        label = "DefaultAmazonSNSFactory per region",
        description = "DefaultAmazonSNSFactory per region"
)
@Service(value=AmazonSNSFactory.class)
public class DefaultAmazonSNSFactory implements AmazonSNSFactory{
  private static final String DEFAULT_REGION="us-east-1";
  private Map<String, AmazonSNS> amazonSNSClientByRegion;
  @Override
  public AmazonSNS get(String regionName) {
    Region region = RegionUtils.getRegion(regionName);
    if(amazonSNSClientByRegion.containsKey(regionName)){
      return amazonSNSClientByRegion.get(regionName);
    }else {
      AmazonSNS client= AmazonSNSClientBuilder.standard()
//              .withCredentials(new ProfileCredentialsProvider("bpbu13"))
              .withRegion(regionName) // The first region to try your request
              // against
              // again in the correct region
              .build();

      amazonSNSClientByRegion.put(regionName,client);
      return client;
    }
  }

  @Override
  public AmazonSNS get(String regionName, String profile) {
    if(StringUtils.isEmpty(profile)){
      return get(regionName);
    }

    Region region = RegionUtils.getRegion(regionName);
    String key=regionName+profile;


    if(amazonSNSClientByRegion.containsKey(key)){
      return amazonSNSClientByRegion.get(key);
    }else {
      AmazonSNS client= AmazonSNSClientBuilder.standard()
              .withCredentials(new ProfileCredentialsProvider(profile))
              .withRegion(regionName) // The first region to try your request
              // against

              // again in the correct region
              .build();

      amazonSNSClientByRegion.put(key,client);
      return client;
    }
  }
  @Activate
  protected void activate(Map <String, Object> properties) {
    amazonSNSClientByRegion=new ConcurrentHashMap<>();
    amazonSNSClientByRegion.put(DEFAULT_REGION,  AmazonSNSClientBuilder
            .standard()
            .withRegion(DEFAULT_REGION) // The first region to try your request
            // against
            // again in the correct region
            .build());
  }
}
