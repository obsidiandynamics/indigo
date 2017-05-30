package com.obsidiandynamics.indigo.iot.rig;

import static org.junit.Assert.*;

import java.net.*;

import org.junit.*;

import com.obsidiandynamics.indigo.iot.edge.*;
import com.obsidiandynamics.indigo.iot.remote.*;
import com.obsidiandynamics.indigo.iot.rig.EdgeRig.*;
import com.obsidiandynamics.indigo.iot.rig.RemoteRig.*;
import com.obsidiandynamics.indigo.topic.*;
import com.obsidiandynamics.indigo.ws.*;

public final class RigTest {
  private static final int PORT = 6667;
  private static final int PULSE_INTERVAL = 100;
  
  private EdgeRig edgeRig;
  
  private RemoteRig remoteRig;
  
  @Before
  public void setup() throws Exception {
    final TopicGen _topicGen = mediumLeaves();
    
    final EdgeNode edge = EdgeNode.builder()
        .withServerConfig(new WSServerConfig() {{ port = PORT; }})
        .build();
    edgeRig = new EdgeRig(edge, new EdgeRigConfig() {{
      topicGen = _topicGen;
      pulseIntervalMillis = PULSE_INTERVAL;
    }});
    
    final RemoteNode remote = RemoteNode.builder()
        .build();
    remoteRig = new RemoteRig(remote, new RemoteRigConfig() {{
      topicGen = _topicGen;
      syncSubframes = 10000;
      uri = new URI("ws://localhost:" + PORT + "/");
    }});
  }
  
  @After
  public void teardown() throws Exception {
    remoteRig.close();
    edgeRig.close();
  }
  
  static TopicGen mediumLeaves() {
    return TopicGen.builder()
        .add(new TopicSpec(0, 0, 0).nodes(2))
        .add(new TopicSpec(0, 0, 0).nodes(5))
        .add(new TopicSpec(0, 0, 0).nodes(2))
        .add(new TopicSpec(1, 0, 0).nodes(5))
        .build();
  }

  @Test
  public void test() throws Exception {
    remoteRig.run();
    fail("Not yet implemented");
  }

}
