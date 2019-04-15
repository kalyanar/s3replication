package com.adobe.ams.publishleader.impl;

import org.apache.felix.scr.annotations.*;
import org.apache.sling.settings.SlingSettingsService;
import org.apache.sling.discovery.PropertyProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Service(value = { PropertyProvider.class })
@Properties({
        @Property(name = PropertyProvider.PROPERTY_PROPERTIES, value = {RunmodePropertyProvider.INSTANCE_TYPE})
})
public class RunmodePropertyProvider implements PropertyProvider {
  private static Logger LOG = LoggerFactory.getLogger(RunmodePropertyProvider.class);

  public static final String INSTANCE_TYPE = "instance.type";

  @Reference
  SlingSettingsService settingService;

  public String getProperty(final String name) {

    String answer = null;

    if(INSTANCE_TYPE.equals(name)){
      if(settingService.getRunModes().contains("publish")){
        answer = "publish";
      } else if(settingService.getRunModes().contains("author")){
        answer = "author";
      } else {
        answer = null;
      }
      LOG.info("getProperty: sending instance.type of "+answer);
    }


    return answer;


  }

}
