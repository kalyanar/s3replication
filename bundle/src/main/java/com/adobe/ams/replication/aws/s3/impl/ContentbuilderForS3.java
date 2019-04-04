package com.adobe.ams.replication.aws.s3.impl;

import com.adobe.ams.replication.aws.AWSReplicationConfig;
import com.adobe.ams.replication.aws.impl.AWSContentBuilder;
import com.adobe.ams.replication.aws.s3.S3ReplicationConfig;
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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component(
        name = "com.adobe.ams.replication.impl.S3ContentBuilder",
        metatype = true,
        label = "S3 Content Builder",
        description = "S3 Content Builder"
)
@Service(value=ContentBuilder.class)
public class ContentbuilderForS3 extends AWSContentBuilder implements
        ContentBuilder {
  private static final Logger logger = LoggerFactory.getLogger(ContentbuilderForS3.class);

  @Property(name = ContentBuilder.PROPERTY_NAME, propertyPrivate = true)
  private static final String NAME = "s3";

  /** Descriptive title of this content builder */
  private static final String TITLE = "S3 Content Builder";

  /** Mime type: application/zip */
  private static final String MT_S3 = "application/s3";

  private static final String SERVICE_NAME = "aws-agent";
  private static final Map<String, Object> AUTH_INFO;

  static {
    AUTH_INFO = Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, (Object) SERVICE_NAME);
  }


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

  private String name;

  private Map<String,String> mapReferences;
  private String  tmpDirStr;
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
  @Override
  public ReplicationContent create(Session session, ReplicationAction replicationAction, ReplicationContentFactory replicationContentFactory) throws ReplicationException {
    return create(session,replicationAction,replicationContentFactory,null);
  }

  @Override
  public ReplicationContent create(Session session, ReplicationAction
          replicationAction, ReplicationContentFactory replicationContentFactory, Map<String, Object> map) throws ReplicationException {
    if (replicationAction.getType() != ReplicationActionType.ACTIVATE) {
      return ReplicationContent.VOID;
    }
    String path=replicationAction.getPath();
    ResourceResolver resourceResolver = getServiceResourceResolver();
    S3ReplicationConfig s3ReplicationConfig=getCAConfig(resourceResolver,
            path);

    AWSReplicationConfig awsReplicationConfig=getAWSCAConfig(resourceResolver,
            path);

    try {
      return create(path,resourceResolver,
              awsReplicationConfig,replicationContentFactory);

    } catch (IOException e) {

    }
    return  ReplicationContent.VOID;
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
    String[] astr=PropertiesUtil.toStringArray(properties.get
            (PROP_MAP_FILETYPE_ERP),new String[]{});
     tmpDirStr = System.getProperty("java.io.tmpdir");
    mapReferences=new HashMap<>();
    for(String str:astr){
      String[] vals=str.split("-");
      if(vals.length>1){
        mapReferences.put(vals[0],vals[1]);
      }
    }
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
  private S3ReplicationConfig getCAConfig(ResourceResolver resourceResolver,
                                           String path){
    Resource contentResource=resourceResolver.getResource(path);
    S3ReplicationConfig s3ReplicationConfig=contentResource.adaptTo
            (ConfigurationBuilder.class).as(S3ReplicationConfig.class);
    return s3ReplicationConfig;
  }
  private AWSReplicationConfig getAWSCAConfig(ResourceResolver resourceResolver,
                                              String path){
    Resource contentResource=resourceResolver.getResource(path);
    AWSReplicationConfig awsReplicationConfig=contentResource.adaptTo
            (ConfigurationBuilder.class).as(AWSReplicationConfig.class);
    return awsReplicationConfig;
  }
  protected String getMimeType(){
    return MT_S3;
  }
}
