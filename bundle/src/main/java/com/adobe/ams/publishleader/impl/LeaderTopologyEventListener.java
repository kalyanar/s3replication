package com.adobe.ams.publishleader.impl;

import com.adobe.ams.publishleader.LeaderProvider;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.discovery.InstanceDescription;
import org.apache.sling.discovery.TopologyEvent;
import org.apache.sling.discovery.TopologyEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

@Component
@Service(value = { TopologyEventListener.class, LeaderProvider.class })
public class LeaderTopologyEventListener implements TopologyEventListener,
        LeaderProvider {

  private static Logger LOG = LoggerFactory.getLogger(LeaderTopologyEventListener.class);

  private boolean leader;

  @Override
  public boolean isLeader() {
    return leader;
  }

  @Override
  public void handleTopologyEvent(TopologyEvent topologyEvent) {
    String leaderSlingId = null;


    Set<InstanceDescription> instances = topologyEvent.getNewView().getInstances();
    for(InstanceDescription desc : instances) {

//      if("publish".equals(desc.getProperty(RunmodePropertyProvider.INSTANCE_TYPE))){

        if(leaderSlingId==null ){
          leaderSlingId = desc.getSlingId();
        } else if (leaderSlingId.compareTo(desc.getSlingId())>0){
          leaderSlingId = desc.getSlingId();
        }

//      }
    }

    LOG.info("handleTopologyEvent: leader sling id "+leaderSlingId);

    leader = topologyEvent.getNewView().getLocalInstance().getSlingId().equals
            (leaderSlingId);
    LOG.info("handleTopologyEvent: i am leader = "+leader);
  }
}
