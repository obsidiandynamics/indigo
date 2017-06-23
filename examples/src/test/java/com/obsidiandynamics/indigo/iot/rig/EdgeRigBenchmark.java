package com.obsidiandynamics.indigo.iot.rig;

import static com.obsidiandynamics.indigo.util.PropertyUtils.get;

import com.obsidiandynamics.indigo.benchmark.*;
import com.obsidiandynamics.indigo.iot.edge.*;
import com.obsidiandynamics.indigo.iot.rig.EdgeRig.*;
import com.obsidiandynamics.indigo.iot.rig.RigBenchmark.*;
import com.obsidiandynamics.indigo.socketx.*;
import com.obsidiandynamics.indigo.topic.*;
import com.obsidiandynamics.indigo.util.*;

public final class EdgeRigBenchmark implements TestSupport {
  private static final int PORT = get("flywheel.rig.port", Integer::valueOf, 6667);
  private static final int PULSES = get("flywheel.rig.pulses", Integer::valueOf, 300);
  private static final int PULSE_DURATION = get("flywheel.rig.pulseDuration", Integer::valueOf, 100);
  private static final float WARMUP_FRAC = get("flywheel.rig.warmupFrac", Float::valueOf, 0.10f);
  private static final boolean TEXT = get("flywheel.rig.text", Boolean::valueOf, false);
  private static final int BYTES = get("flywheel.rig.bytes", Integer::valueOf, 128);
  private static final boolean CYCLE = get("flywheel.rig.cycle", Boolean::valueOf, false);
  
  private static Summary run(Config c) throws Exception {
    final EdgeNode edge = EdgeNode.builder()
        .withServerConfig(new XServerConfig() {{ port = c.port; }})
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
