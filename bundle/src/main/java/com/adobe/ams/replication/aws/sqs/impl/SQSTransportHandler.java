package com.adobe.ams.replication.aws.sqs.impl;


import com.adobe.ams.publishleader.LeaderProvider;
import com.adobe.ams.replication.aws.AWSReplicationConfig;
import com.adobe.ams.replication.aws.impl.AWSTransportHandler;
import com.adobe.ams.replication.aws.s3.AmazonS3Factory;
import com.adobe.ams.replication.aws.sqs.AmazonSQSFactory;
import com.adobe.ams.replication.aws.sqs.SQSReplicationConfig;
import com.adobe.ams.replication.constants.ReplicationConstants;
import com.adobe.ams.replication.utils.ReplicationUtils;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.day.cq.replication.AgentConfig;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationContent;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.ReplicationResult;
import com.day.cq.replication.ReplicationTransaction;
import com.day.cq.replication.TransportContext;
import com.day.cq.replication.TransportHandler;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.caconfig.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component(metatype = false,immediate = true, enabled = true,label =
        "Tansport handler to upload to s3")
@Service(value = TransportHandler.class)
public class SQSTransportHandler extends AWSTransportHandler implements TransportHandler{
  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private static final String TRANSPORT_SCHEME = "sqs";

  private static final String SERVICE_NAME = "aws-agent";
  private static final Map<String, Object> AUTH_INFO;

  static {
    AUTH_INFO = Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, (Object) SERVICE_NAME);
  }


  @Reference
  ResourceResolverFactory resourceResolverFactory;

  @Reference
  private AmazonS3Factory amazonS3Factory;

  @Reference
  LeaderProvider leaderProvider;

  @Override
  public boolean canHandle(AgentConfig agentConfig) {
    String uri = agentConfig == null ? null : agentConfig.getTransportURI();
    return uri != null && (uri.startsWith(TRANSPORT_SCHEME))&&leaderProvider
            .isLeaderPublish();  }


  @Reference
  private AmazonSQSFactory amazonSQSFactory;

  @Override
  public ReplicationResult deliver(TransportContext transportContext, ReplicationTransaction replicationTransaction) throws ReplicationException {
    String path=replicationTransaction.getAction().getPath();
    ReplicationConstants.FILE_TYPE file_type= ReplicationUtils.guessFileType(path);

    ResourceResolver resourceResolver= null;
    try {
      resourceResolver = resourceResolverFactory
              .getServiceResourceResolver(AUTH_INFO);
    } catch (LoginException e) {
      e.printStackTrace();
    }

    Resource contentResource=resourceResolver.getResource(path+"/jcr:content");

    SQSReplicationConfig sqsReplicationConfig=contentResource.adaptTo
            (ConfigurationBuilder.class).as(SQSReplicationConfig.class);
    AWSReplicationConfig awsReplicationConfig=contentResource.adaptTo
            (ConfigurationBuilder.class).as(AWSReplicationConfig.class);
    boolean persist=false;
    String filename="";

    if(sqsReplicationConfig.persistDataWithMessage()){
      ReplicationContent content = replicationTransaction.getContent();
      if(sqsReplicationConfig.randomizeName()) {
        filename = UUID.randomUUID().toString();
      }else {
        try {
          filename=ReplicationUtils.getMD5Checksum(path.getBytes());
        } catch (IOException e) {
          filename=path;
        }
      }
      try {
        persist= uploadToS3(sqsReplicationConfig.bucketName(),content
                        .getInputStream(),
                replicationTransaction.getLog(),awsReplicationConfig,filename);
      } catch (IOException e) {
        logger.error(e.getMessage(),e);
      }
    }


//    if(replicationTransaction.getAction().getType() == ReplicationActionType.ACTIVATE){

    publishToQueue(sqsReplicationConfig,awsReplicationConfig,path
            ,resourceResolver,replicationTransaction,persist,filename);

    return ReplicationResult.OK;
  }

  private void publishToQueue(SQSReplicationConfig sqsReplicationConfig,
                              AWSReplicationConfig awsReplicationConfig,
                              String path,ResourceResolver resourceResolver,
                              ReplicationTransaction replicationTransaction,
                              boolean persist,String filename){
    ReplicationConstants.FILE_TYPE file_type= ReplicationUtils.guessFileType(path);

    String pathToBeReplicated =ReplicationUtils.getPathTobeReplicated(path,
            awsReplicationConfig.defaultSuffix(),file_type,
            resourceResolver,awsReplicationConfig.enableURLShortening());
    final Map<String, MessageAttributeValue> messageAttributes = new HashMap<>();
    messageAttributes.put("action", new MessageAttributeValue()
            .withDataType("String")
            .withStringValue(replicationTransaction.getAction().getType()
                    .toString()));
    messageAttributes.put("path", new MessageAttributeValue()
            .withDataType("String")
            .withStringValue(pathToBeReplicated));
    if(sqsReplicationConfig.persistDataWithMessage()&&replicationTransaction
            .getAction().getType()==ReplicationActionType.ACTIVATE &&persist) {
      messageAttributes.put("persistedinS3", new MessageAttributeValue()
              .withDataType("String")
              .withStringValue("true"));
      messageAttributes.put("pathins3", new MessageAttributeValue()
              .withDataType("String")
              .withStringValue("s3://"+sqsReplicationConfig.bucketName()
                      +"/"+filename));

    }
    SendMessageRequest send_msg_request = new SendMessageRequest()
            .withQueueUrl(sqsReplicationConfig.queueURL())
            .withMessageBody(pathToBeReplicated+" "+replicationTransaction
                    .getAction().getType() .toString())
            .withMessageAttributes(messageAttributes)
            ;
    //send_msg_request.setMessageGroupId(sqsReplicationConfig.messagegroupid());
   // send_msg_request.setMessageDeduplicationId(pathToBeReplicated);
    logger.error("awsReplicationConfig.regionName()"+awsReplicationConfig.regionName());
    logger.error("awsReplicationConfig.profile()"+awsReplicationConfig
            .profile());
    amazonSQSFactory.get(awsReplicationConfig.regionName(),
            awsReplicationConfig.profile()).sendMessage
            (send_msg_request);


  }

  private ResourceResolver getServiceResourceResolver(){
    ResourceResolver resourceResolver= null;
    try {
      resourceResolver = resourceResolverFactory
              .getServiceResourceResolver(AUTH_INFO);
    } catch (LoginException e) {
      logger.error(e.getMessage(),e);
    }
    return resourceResolver;
  }

  @Override
  protected void persist(AWSReplicationConfig awsReplicationConfig,
                         PutObjectRequest request) {
    amazonS3Factory.get(awsReplicationConfig.regionName(),
            awsReplicationConfig.profile()).putObject
            (request);
  }
}
