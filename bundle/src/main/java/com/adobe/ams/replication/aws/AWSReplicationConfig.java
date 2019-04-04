package com.adobe.ams.replication.aws;

import org.apache.sling.caconfig.annotation.Configuration;
import org.apache.sling.caconfig.annotation.Property;

@Configuration(label = "CA config based AWS replication ")
public @interface AWSReplicationConfig {
  @Property(label = "aws profile")
  String profile() default "";

  @Property(label = "Only replicate content in this path")
  String pathToInclude() default "/";

  @Property(label = "Default extension")
  String defaultSuffix() default "html";

  @Property(label = "Bucket Region")
  String regionName() default "us-west-2";

  @Property(label = "Replicate referenced content?")
  boolean alsoReplicateReferencedContent() default false;

  @Property(label = "Enable url shortening?")
  boolean enableURLShortening() default true;

  @Property(label = "Extract the zip and upload individual files?")
  boolean extractAndUpload() default false;
}
