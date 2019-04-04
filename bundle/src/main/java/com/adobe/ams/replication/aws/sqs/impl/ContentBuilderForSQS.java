package com.adobe.ams.replication.aws.sqs.impl;

import com.adobe.ams.replication.aws.AWSReplicationConfig;
import com.adobe.ams.replication.aws.impl.AWSContentBuilder;
import com.adobe.ams.replication.aws.sqs.SQSReplicationConfig;
import com.adobe.ams.replication.provider.EmbeddedReferenceProvider;
import com.day.cq.replication.ContentBuilder;
import com.day.cq.replication.ReplicationAction;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationContent;
import com.day.cq.replication.ReplicationContentFactory;
import com.day.cq.replication.ReplicationException;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyUnbounded;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.caconfig.ConfigurationBuilder;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.engine.SlingRequestProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component(
        name = "com.adobe.ams.replication.impl.SQSContentBuilder",
        metatype = true,
        label = "SQS Content Builder",
        description = "SQS Content Builder"
)
@Service(value=ContentBuilder.class)
public class ContentBuilderForSQS extends AWSContentBuilder implements ContentBuilder {
  private static final Logger logger = LoggerFactory.getLogger(ContentBuilderForSQS.class);

  @Property(name = ContentBuilder.PROPERTY_NAME, propertyPrivate = true)
  private static final String NAME = "sqs";

  /** Descriptive title of this content builder */
  private static final String TITLE = "Sqs Content Builder";

  /** Mime type: application/zip */
  private static final String MT_SQS = "application/sqs";

  private static final String SERVICE_NAME = "aws-agent";
  private static final Map<String, Object> AUTH_INFO;
  @Property(name="s3.map.filetype.to.erp",label = "Map " +
          "file types to embedded reference providers",
          description =
                  "Map " +
                          "file types to embedded reference providers. ex.," +
                          "html-html. html before hyphen(-) denotes " +
                          "S3EnumConstances.FILE_TYPE and html after hyphen" +
                          "(-) denotes HTMLEmbeddedReferenceProvider which " +
                          "has providername as html",
          value={"html-html" ,
                  "js-js","css-css"},unbounded =
          PropertyUnbounded.ARRAY)
  private static final String PROP_MAP_FILETYPE_ERP = "s3.map.filetype.to.erp";

  @Reference
  private ResourceResolverFactory resourceResolverFactory;

  @Reference
  private SlingRequestProcessor requestProcessor;

  @Reference(name = "erp", referenceInterface = EmbeddedReferenceProvider.class,
          cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy =
          ReferencePolicy.DYNAMIC)
  private final Map<String, EmbeddedReferenceProvider> erp = new
          ConcurrentHashMap<>();

  protected void bindErp(final EmbeddedReferenceProvider erp,
                         final
                         Map<String, Object> props) {
    synchronized (this.erp) {
      this.erp.put(erp.getProviderName(), erp);
    }
  }

  protected void unbindErp(final EmbeddedReferenceProvider erp, final
  Map<String, Object> props) {
    synchronized (this.erp) {
      this.erp.remove(erp.getProviderName());
    }
  }

  private Map<String,String> mapReferences;

  private String  tmpDirStr;

  static {
    AUTH_INFO = Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, (Object) SERVICE_NAME);
  }

  private String name;

  @Override
  public ReplicationContent create(Session session, ReplicationAction replicationAction, ReplicationContentFactory replicationContentFactory) throws ReplicationException {
    return create(session,replicationAction,replicationContentFactory,null);
  }

  @Override
  public ReplicationContent create(Session session, ReplicationAction
          replicationAction, ReplicationContentFactory replicationContentFactory, Map<String, Object> map) throws ReplicationException {
    String path=replicationAction.getPath();
    ResourceResolver resourceResolver = getServiceResourceResolver();
    if (replicationAction.getType() != ReplicationActionType.ACTIVATE ||
            resourceResolver==null
            ) {
      return ReplicationContent.VOID;
    }
    SQSReplicationConfig sqsReplicationConfig=getCAConfig(resourceResolver,
            path);
    if(!sqsReplicationConfig.persistDataWithMessage()){
      return ReplicationContent.VOID;
    }
    AWSReplicationConfig awsReplicationConfig=getAWSCAConfig(resourceResolver,
            path);
    try {
      return create(path,resourceResolver,
              awsReplicationConfig,replicationContentFactory);

    } catch (IOException e) {

    }
    return ReplicationContent.VOID;
  }


  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getTitle() {
    return TITLE;
  }


  @Activate
  protected void activate(Map <String, Object> properties) {
    this.name = PropertiesUtil.toString(properties.get(ContentBuilder.PROPERTY_NAME), NAME);

  }

  @Override
  protected Map<String, String> getMapReferences() {
    return mapReferences;
  }

  @Override
  protected Map<String, EmbeddedReferenceProvider> getERP() {
    return erp;
  }

  @Override
  protected SlingRequestProcessor getRequestProcessor() {
    return requestProcessor;
  }
  private ResourceResolver getServiceResourceResolver(){

    ResourceResolver resourceResolver = null;
    try {
      resourceResolver = resourceResolverFactory
              .getServiceResourceResolver(AUTH_INFO);
    } catch (LoginException e) {

    }
    return resourceResolver;
  }
  private SQSReplicationConfig getCAConfig(ResourceResolver resourceResolver,
                                           String path){
    Resource contentResource=resourceResolver.getResource(path);
    SQSReplicationConfig sqsReplicationConfig=contentResource.adaptTo
            (ConfigurationBuilder.class).as(SQSReplicationConfig.class);
    return sqsReplicationConfig;
  }
  private AWSReplicationConfig getAWSCAConfig(ResourceResolver resourceResolver,
                                              String path){
    Resource contentResource=resourceResolver.getResource(path);
    AWSReplicationConfig awsReplicationConfig=contentResource.adaptTo
            (ConfigurationBuilder.class).as(AWSReplicationConfig.class);
    return awsReplicationConfig;
  }
  protected String getMimeType(){
    return MT_SQS;
  }
}
