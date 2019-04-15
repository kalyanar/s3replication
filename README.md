# AWS SQS/SNS/S3 replication	

### Features

- Exposes the replication events via SQS/SNS
- Content aware configuration allowing to push to different queues for different content paths
- Custom replication agents for S3/SQS/SNS
- Allow us to also persist content with each message to SQS/SNS by persisting the file in a s3 bucket configurable as part of configuration
- Allows reactive architectures 
- Runs only on the leader publish(when there are more than one publish )


**Set  up**
1. build with maven
```
mvn clean install
```
2. Install the `ams-awsreplication-content-0.0.1-SNAPSHOT-min.zip` to author and publish
3. The code has 3 sample agents (one each for sqs,sns,s3) under agents.publish. The idea being, it would make sense to push events to the cloud after the replicated package successfully reaches the publish server.
4.On the publish servers,In System Console modify the config 'Apache Sling Oak-Based Discovery Service Configuration’ and set the value of 'Topology Connector URLs’ to 'http://authorip:4502/libs/sling/topology/connector' (this means the servers will announce themselves too author, which will then notify the rest of the topology). You will see the log entry as below. 
```
handleTopologyEvent: i am leader = 

```
The handle method in the transport handler only returns true if the instance is a leader.

```
    return uri != null && (uri.startsWith(TRANSPORT_SCHEME))&&leaderProvider
            .isLeaderPublish();  
```

**What you will see**

1. A system user named 	`aws-agent-service` under `/home/users/system/ams-commons/`. Check [aws-agent-service](https://github.com/kalyanar/s3replication/tree/master/content/src/main/content/jcr_root/home/users/system/ams-commons/aws-agent-service).
2. A service user mapping under `/apps/ams-commons/config`. The file name is `org.apache.sling.serviceusermapping.impl.ServiceUserMapperImpl.amended-ams-commons.xml`
```
com.adobe.ams.ams-awsreplication-bundle:aws-agent=aws-agent-service
```
3. Under `/conf/global`, there would be a `sling:configs` node of type `sling:Folder`
4. This will have the configs for `com.adobe.ams.replication.aws.AWSReplicationConfig` ,`com.adobe.ams.replication.aws.s3.S3ReplicationConfig` , `com.adobe.ams.replication.aws.sns.SNSReplicationConfig`,`com.adobe.ams.replication.aws.sqs.SQSReplicationConfig`
5. Check the code([AWSReplicationConfig](https://github.com/kalyanar/s3replication/blob/master/bundle/src/main/java/com/adobe/ams/replication/aws/AWSReplicationConfig.java) , [S3ReplicationConfig](https://github.com/kalyanar/s3replication/blob/master/bundle/src/main/java/com/adobe/ams/replication/aws/s3/S3ReplicationConfig.java) , [SNSReplicationConfig ](https://github.com/kalyanar/s3replication/blob/master/bundle/src/main/java/com/adobe/ams/replication/aws/sns/SNSReplicationConfig.java),[SQSReplicationConfig](https://github.com/kalyanar/s3replication/blob/master/bundle/src/main/java/com/adobe/ams/replication/aws/sqs/SQSReplicationConfig.java)) for what all values this can have. 
6. Check [sling:configs](https://github.com/kalyanar/s3replication/tree/master/content/src/main/content/jcr_root/conf/global/_sling_configs) for the default global configs that we have set up.  
7. Check [custom agents](https://github.com/kalyanar/s3replication/tree/master/content/src/main/content/jcr_root/etc/replication/agents.publish) created.
