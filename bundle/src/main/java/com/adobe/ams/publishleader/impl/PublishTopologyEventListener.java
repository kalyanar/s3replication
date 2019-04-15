package com.adobe.ams.publishleader.impl;

import com.adobe.ams.publishleader.LeaderProvider;
import org.apache.sling.discovery.InstanceDescription;
import org.apache.sling.discovery.TopologyEvent;
import org.apache.sling.discovery.TopologyEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class PublishTopologyEventListener implements TopologyEventListener,
        LeaderProvider {

  private static Logger LOG = LoggerFactory.getLogger(PublishTopologyEventListener.class);

  private boolean leaderPublish;

  @Override
  public boolean isLeaderPublish() {
    return false;
  }

  @Override
  public void handleTopologyEvent(TopologyEvent topologyEvent) {
    String leaderSlingId = null;


    Set<InstanceDescription> instances = topologyEvent.getNewView().getInstances();
    for(InstanceDescription desc : instances) {

      if("publish".equals(desc.getProperty(RunmodePropertyProvider.INSTANCE_TYPE))){

        if(leaderSlingId==null ){
          leaderSlingId = desc.getSlingId();
        } else if (leaderSlingId.compareTo(desc.getSlingId())>0){
          leaderSlingId = desc.getSlingId();
        }

      }
    }

    LOG.info("handleTopologyEvent: leader sling id "+leaderSlingId);

    leaderPublish = topologyEvent.getNewView().getLocalInstance().getSlingId().equals
            (leaderSlingId);
    LOG.info("handleTopologyEvent: i am leader = "+leaderPublish);
  }
}
