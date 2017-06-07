package com.obsidiandynamics.indigo.iot.rig;

import com.obsidiandynamics.indigo.benchmark.*;
import com.obsidiandynamics.indigo.iot.edge.*;
import com.obsidiandynamics.indigo.iot.rig.EdgeRig.*;
import com.obsidiandynamics.indigo.iot.rig.RigBenchmark.*;
import com.obsidiandynamics.indigo.topic.*;
import com.obsidiandynamics.indigo.util.*;
import com.obsidiandynamics.indigo.ws.*;

public final class EdgeRigBenchmark implements TestSupport {
  private static final int PORT = PropertyUtils.get("Rig.port", Integer::valueOf, 6667);
  
  private static Summary run(Config c) throws Exception {
    final EdgeNode edge = EdgeNode.builder()
        .withServerConfig(new WSServerConfig() {{ port = c.port; }})
        .build();
    final EdgeRig edgeRig = new EdgeRig(edge, new EdgeRigConfig() {{
      topicSpec = c.topicSpec;
      pulseDurationMillis = c.pulseDurationMillis;
      pulses = c.pulses;
      warmupPulses = c.warmupPulses;
      text = c.text;
      bytes = c.bytes;
      log = c.log;
    }});
    
    edgeRig.await();
    edgeRig.close();
    LOG_STREAM.println("Edge benchmark completed");
    
    final Summary summary = new Summary();
    summary.compute(new Elapsed() {
      @Override public long getTotalProcessed() {
        return (long) edgeRig.getSubscribers() * c.pulses;
      }

      @Override public long getTimeTaken() {
        return edgeRig.getTimeTaken();
      }
    });
    return summary;
  }
  
  public static void main(String[] args) throws Exception {
    BashInteractor.Ulimit.main(null);
    LOG_STREAM.println("_\nEdge benchmark started; waiting for remotes...");
    new Config() {{
      runner = EdgeRigBenchmark::run;
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
