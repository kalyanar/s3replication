package com.adobe.ams.replication.aws.s3.impl;

import com.adobe.ams.replication.aws.s3.AmazonS3Factory;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Component(
        name = "com.adobe.ams.replication.impl.DefaultAmazonS3Factory",
        metatype = true,
        label = "S3 DefaultAmazonS3Factory per region",
        description = "S3 DefaultAmazonS3Factory per region"
)
@Service(value=AmazonS3Factory.class)
public class DefaultAmazonS3Factory implements AmazonS3Factory {
  private static final String DEFAULT_REGION="us-east-1";
  private Map<String, AmazonS3> amazonS3ClientByRegion;

  @Override
  public synchronized AmazonS3 get(String regionName) {
    Region region = RegionUtils.getRegion(regionName);
    if(amazonS3ClientByRegion.containsKey(regionName)){
      return amazonS3ClientByRegion.get(regionName);
    }else {
      AmazonS3 client= AmazonS3ClientBuilder.standard()
//              .withCredentials(new ProfileCredentialsProvider("bpbu13"))
              .withRegion(regionName) // The first region to try your request
              // against
              .withForceGlobalBucketAccessEnabled(true) // If a bucket is in a different region, try
              // again in the correct region
              .build();

      amazonS3ClientByRegion.put(regionName,client);
      return client;
    }
  }

  @Override
  public AmazonS3 get(String regionName, String profile) {
    if(StringUtils.isEmpty(profile)){
return get(regionName);
    }

    Region region = RegionUtils.getRegion(regionName);
    String key=regionName+profile;


    if(amazonS3ClientByRegion.containsKey(key)){
      return amazonS3ClientByRegion.get(key);
    }else {
      AmazonS3 client= AmazonS3ClientBuilder.standard()
              .withCredentials(new ProfileCredentialsProvider(profile))
              .withRegion(regionName) // The first region to try your request
              // against
              .withForceGlobalBucketAccessEnabled(true) // If a bucket is in a different region, try
              // again in the correct region
              .build();

      amazonS3ClientByRegion.put(key,client);
      return client;
    }
  }

  @Activate
  protected void activate(Map <String, Object> properties) {
    amazonS3ClientByRegion=new ConcurrentHashMap<>();
    amazonS3ClientByRegion.put(DEFAULT_REGION,  AmazonS3ClientBuilder.standard()
                    .withRegion(DEFAULT_REGION) // The first region to try your request
            // against
            .withForceGlobalBucketAccessEnabled(true) // If a bucket is in a different region, try
            // again in the correct region
            .build());
  }
}
