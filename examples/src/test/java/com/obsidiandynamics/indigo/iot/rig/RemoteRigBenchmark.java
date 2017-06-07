package com.obsidiandynamics.indigo.iot.rig;

import java.net.*;

import com.obsidiandynamics.indigo.benchmark.*;
import com.obsidiandynamics.indigo.iot.remote.*;
import com.obsidiandynamics.indigo.iot.rig.RemoteRig.*;
import com.obsidiandynamics.indigo.iot.rig.RigBenchmark.*;
import com.obsidiandynamics.indigo.topic.*;
import com.obsidiandynamics.indigo.util.*;

public final class RemoteRigBenchmark implements TestSupport {
  private static final String HOST = PropertyUtils.get("Rig.host", String::valueOf, "localhost");
  private static final int PORT = PropertyUtils.get("Rig.port", Integer::valueOf, 6667);
  
  private static Summary run(Config c) throws Exception {
    final RemoteNode remote = RemoteNode.builder()
        .build();
    final RemoteRig remoteRig = new RemoteRig(remote, new RemoteRigConfig() {{
      topicSpec = c.topicSpec;
      syncSubframes = c.syncSubframes;
      uri = getUri(c.host, c.port);
      log = c.log;
    }});
    
    remoteRig.run();
    remoteRig.await();
    remoteRig.close();
    return remoteRig.getSummary();
  }
  
  public static void main(String[] args) throws Exception {
    BashInteractor.Ulimit.main(null);
    LOG_STREAM.format("_\nRemote benchmark started (URI: %s)...\n", RemoteRigConfig.getUri(HOST, PORT));
    new Config() {{
      runner = RemoteRigBenchmark::run;
      host = HOST;
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
