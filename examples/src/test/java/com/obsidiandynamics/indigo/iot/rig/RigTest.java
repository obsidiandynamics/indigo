package com.obsidiandynamics.indigo.iot.rig;

import java.net.*;

import org.junit.*;

import com.obsidiandynamics.indigo.iot.edge.*;
import com.obsidiandynamics.indigo.iot.remote.*;
import com.obsidiandynamics.indigo.iot.rig.EdgeRig.*;
import com.obsidiandynamics.indigo.iot.rig.RemoteRig.*;
import com.obsidiandynamics.indigo.topic.*;
import com.obsidiandynamics.indigo.util.*;
import com.obsidiandynamics.indigo.ws.*;

public final class RigTest implements TestSupport {
  private static final int PORT = 6667;
  private static final int PULSE_INTERVAL = 100;
  private static final int CYCLES = 10000;
  
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
    for (int i = 0; i < CYCLES; i++) {
      _test();
      if (i % 10 == 9) LOG_STREAM.format("%,d cycles\n", i);
    }
  }

  private void _test() throws Exception {
    final TopicGen _topicGen = mediumLeaves();
    
    final EdgeNode edge = EdgeNode.builder()
        .withServerConfig(new WSServerConfig() {{ port = PORT; }})
        .build();
    final EdgeRig edgeRig = new EdgeRig(edge, new EdgeRigConfig() {{
      topicGen = _topicGen;
      pulseIntervalMillis = PULSE_INTERVAL;
    }});
    
    final RemoteNode remote = RemoteNode.builder()
        .build();
    final RemoteRig remoteRig = new RemoteRig(remote, new RemoteRigConfig() {{
      topicGen = _topicGen;
      syncSubframes = 10;
      uri = new URI("ws://localhost:" + PORT + "/");
    }});
    
    remoteRig.run();
//    Thread.sleep(100);
    remoteRig.close();
    edgeRig.close();
  }

}
