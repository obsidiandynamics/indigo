package com.obsidiandynamics.indigo.iot.rig;

import java.net.*;

import com.obsidiandynamics.indigo.benchmark.*;
import com.obsidiandynamics.indigo.iot.remote.*;
import com.obsidiandynamics.indigo.iot.rig.RemoteRig.*;
import com.obsidiandynamics.indigo.iot.rig.RigBenchmark.*;
import com.obsidiandynamics.indigo.topic.*;
import com.obsidiandynamics.indigo.util.*;

public final class RemoteRigBenchmark implements TestSupport {
  private static final String HOST = "localhost";
  private static final int PORT = 6667;
  
  private static Summary run(Config c) throws Exception {
    final RemoteNode remote = RemoteNode.builder()
        .build();
    final RemoteRig remoteRig = new RemoteRig(remote, new RemoteRigConfig() {{
      topicSpec = c.topicSpec;
      syncSubframes = c.syncSubframes;
      uri = new URI(String.format("ws://%s:%d/", HOST, PORT));
      log = c.log;
    }});
    
    remoteRig.run();
    remoteRig.await();
    remoteRig.close();
    return remoteRig.getSummary();
  }
  
  public static void main(String[] args) throws Exception {
    BashInteractor.Ulimit.main(null);
    LOG_STREAM.println("\nRemote benchmark started...");
    new Config() {{
      runner = RemoteRigBenchmark::run;
      port = PORT;
      pulses = 300;
      pulseDurationMillis = 100;
      syncSubframes = 0;
      topicSpec = TopicLibrary.largeLeaves();
      warmupFrac = 0.10f;
      text = false;
      bytes = 128;
      log = new LogConfig() {{
        progress = intermediateSummaries = false;
        stages = true;
        summary = true;
      }};
    }}.test();
  }
}
