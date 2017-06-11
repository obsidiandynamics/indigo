package com.obsidiandynamics.indigo.iot.rig;

import com.obsidiandynamics.indigo.benchmark.*;
import com.obsidiandynamics.indigo.iot.remote.*;
import com.obsidiandynamics.indigo.iot.rig.RemoteRig.*;
import com.obsidiandynamics.indigo.iot.rig.RigBenchmark.*;
import com.obsidiandynamics.indigo.topic.*;
import com.obsidiandynamics.indigo.util.*;

public final class RemoteRigBenchmark implements TestSupport {
  private static final String HOST = PropertyUtils.get("Rig.host", String::valueOf, "localhost");
  private static final int PORT = PropertyUtils.get("Rig.port", Integer::valueOf, 6667);
  private static final int SYNC_FRAMES = PropertyUtils.get("Rig.sincFrames", Integer::valueOf, 1000);
  private static final boolean INITIATE = PropertyUtils.get("Rig.initiate", Boolean::valueOf, true);
  
  private static Summary run(Config c) throws Exception {
    final RemoteNode remote = RemoteNode.builder()
        .build();
    final RemoteRig remoteRig = new RemoteRig(remote, new RemoteRigConfig() {{
      topicSpec = c.topicSpec;
      syncFrames = c.syncFrames;
      uri = getUri(c.host, c.port);
      initiate = c.initiate;
      log = c.log;
    }});
    
    remoteRig.run();
    remoteRig.await();
    remoteRig.close();
    return remoteRig.getSummary();
  }
  
  public static void main(String[] args) throws Exception {
    BashInteractor.Ulimit.main(null);
    LOG_STREAM.format("_\nRemote benchmark started (URI: %s, initiate: %b)...\n", 
                      RemoteRigConfig.getUri(HOST, PORT), INITIATE);
    new Config() {{
      runner = RemoteRigBenchmark::run;
      host = HOST;
      port = PORT;
      syncFrames = SYNC_FRAMES;
      topicSpec = TopicLibrary.largeLeaves();
      initiate = INITIATE;
      log = new LogConfig() {{
        progress = intermediateSummaries = false;
        stages = true;
        summary = true;
      }};
    }}.test();
  }
}
