package com.obsidiandynamics.indigo.iot.rig;

import static com.obsidiandynamics.indigo.util.PropertyUtils.get;

import com.obsidiandynamics.indigo.benchmark.*;
import com.obsidiandynamics.indigo.iot.edge.*;
import com.obsidiandynamics.indigo.iot.rig.EdgeRig.*;
import com.obsidiandynamics.indigo.iot.rig.RigBenchmark.*;
import com.obsidiandynamics.indigo.topic.*;
import com.obsidiandynamics.indigo.util.*;
import com.obsidiandynamics.indigo.ws.*;

public final class EdgeRigBenchmark implements TestSupport {
  private static final int PORT = get("Rig.port", Integer::valueOf, 6667);
  private static final int PULSES = get("Rig.pulses", Integer::valueOf, 300);
  private static final int PULSE_DURATION = get("Rig.pulseDuration", Integer::valueOf, 100);
  private static final float WARMUP_FRAC = get("Rig.warmupFrac", Float::valueOf, 0.10f);
  private static final boolean TEXT = get("Rig.text", Boolean::valueOf, false);
  private static final int BYTES = get("Rig.bytes", Integer::valueOf, 128);
  private static final boolean CYCLE = get("Rig.cycle", Boolean::valueOf, false);
  
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
        return (long) edgeRig.getTotalSubscribers() * c.pulses;
      }

      @Override public long getTimeTaken() {
        return edgeRig.getTimeTaken();
      }
    });
    return summary;
  }
  
  public static void main(String[] args) throws Exception {
    BashInteractor.Ulimit.main(null);
    do {
      LOG_STREAM.println("_\nEdge benchmark started; waiting for remote connections...");
      new Config() {{
        runner = EdgeRigBenchmark::run;
        port = PORT;
        pulses = PULSES;
        pulseDurationMillis = PULSE_DURATION;
        topicSpec = TopicLibrary.largeLeaves();
        warmupFrac = WARMUP_FRAC;
        text = TEXT;
        bytes = BYTES;
        log = new LogConfig() {{
          progress = intermediateSummaries = false;
          stages = true;
          summary = true;
        }};
      }}.test();
    } while (CYCLE);
  }
}
