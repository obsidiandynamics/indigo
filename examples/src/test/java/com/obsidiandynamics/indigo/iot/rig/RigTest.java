package com.obsidiandynamics.indigo.iot.rig;

import java.net.*;
import java.util.function.*;

import org.junit.*;

import com.obsidiandynamics.indigo.benchmark.*;
import com.obsidiandynamics.indigo.iot.edge.*;
import com.obsidiandynamics.indigo.iot.remote.*;
import com.obsidiandynamics.indigo.iot.rig.EdgeRig.*;
import com.obsidiandynamics.indigo.iot.rig.RemoteRig.*;
import com.obsidiandynamics.indigo.topic.*;
import com.obsidiandynamics.indigo.util.*;
import com.obsidiandynamics.indigo.ws.*;

public final class RigTest implements TestSupport {
  private static final int PORT = 6667;
  private static final int PULSE_DURATION = 10;
  private static final int PULSES = 10;
  private static final int CYCLES = 1;
  private static final int SYNC_SUBFRAMES = 0;
  private static final Supplier<TopicGen> GEN = RigTest::largeLeaves;
  
  static TopicGen smallLeaves() {
    return TopicGen.builder()
        .add(new TopicSpec(0, 0, 0).nodes(2))
        .add(new TopicSpec(1, 0, 0).nodes(5))
        .build();
  }
  
  static TopicGen mediumLeaves() {
    return TopicGen.builder()
        .add(new TopicSpec(0, 0, 0).nodes(2))
        .add(new TopicSpec(0, 0, 0).nodes(5))
        .add(new TopicSpec(0, 0, 0).nodes(2))
        .add(new TopicSpec(1, 0, 0).nodes(5))
        .build();
  }
  
  static TopicGen largeLeaves() {
    return TopicGen.builder()
        .add(new TopicSpec(0, 0, 0).nodes(2))
        .add(new TopicSpec(0, 0, 0).nodes(5))
        .add(new TopicSpec(0, 0, 0).nodes(2))
        .add(new TopicSpec(0, 0, 0).nodes(5))
        .add(new TopicSpec(0, 0, 0).nodes(2))
        .add(new TopicSpec(1, 0, 0).nodes(5))
        .build();
  }
  
  static TopicGen jumboLeaves() {
    return TopicGen.builder()
        .add(new TopicSpec(0, 0, 0).nodes(2))
        .add(new TopicSpec(0, 0, 0).nodes(5))
        .add(new TopicSpec(0, 0, 0).nodes(2))
        .add(new TopicSpec(0, 0, 0).nodes(5))
        .add(new TopicSpec(0, 0, 0).nodes(2))
        .add(new TopicSpec(0, 0, 0).nodes(5))
        .add(new TopicSpec(0, 0, 0).nodes(2))
        .add(new TopicSpec(1, 0, 0).nodes(5))
        .build();
  }

  @Test
  public void test() throws Exception {
    BashInteractor.Ulimit.main(null);
    for (int i = 0; i < CYCLES; i++) {
      _test();
      if (i % 10 == 9) LOG_STREAM.format("%,d cycles\n", i);
    }
  }

  private void _test() throws Exception {
    final TopicGen _topicGen = GEN.get();
    
    final EdgeNode edge = EdgeNode.builder()
        .withServerConfig(new WSServerConfig() {{ port = PORT; }})
        .build();
    final EdgeRig edgeRig = new EdgeRig(edge, new EdgeRigConfig() {{
      topicGen = _topicGen;
      pulseDurationMillis = PULSE_DURATION;
      pulses = PULSES;
    }});
    
    final RemoteNode remote = RemoteNode.builder()
        .build();
    final RemoteRig remoteRig = new RemoteRig(remote, new RemoteRigConfig() {{
      topicGen = _topicGen;
      syncSubframes = SYNC_SUBFRAMES;
      uri = new URI("ws://localhost:" + PORT + "/");
    }});
    
    remoteRig.run();
    edgeRig.await();
    remoteRig.awaitReceival(edgeRig.getNumSent());

    edgeRig.close();
    remoteRig.close();
    
    final Summary summary = remoteRig.getSummary();
    LOG_STREAM.format("%s\n", summary.toString());
  }
}
