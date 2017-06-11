package com.obsidiandynamics.indigo.iot.rig;

import java.net.*;

import org.awaitility.*;
import org.junit.*;

import com.obsidiandynamics.indigo.benchmark.*;
import com.obsidiandynamics.indigo.iot.edge.*;
import com.obsidiandynamics.indigo.iot.remote.*;
import com.obsidiandynamics.indigo.iot.rig.EdgeRig.*;
import com.obsidiandynamics.indigo.iot.rig.RemoteRig.*;
import com.obsidiandynamics.indigo.topic.*;
import com.obsidiandynamics.indigo.util.*;
import com.obsidiandynamics.indigo.ws.*;

public final class RigBenchmark implements TestSupport {
  private static final String HOST = "localhost";
  private static final int PREFERRED_PORT = 6667;
  
  abstract static class Config implements Spec {
    static {
      Awaitility.doNotCatchUncaughtExceptionsByDefault();
    }
    
    ThrowingFunction<Config, Summary> runner = RigBenchmark::test;
    String host;
    int port;
    int pulses;
    int pulseDurationMillis;
    int syncFrames;
    TopicSpec topicSpec;
    boolean initiate;
    boolean text;
    int bytes;
    float warmupFrac;
    LogConfig log;
    
    /* Derived fields. */
    int warmupPulses;
    
    private boolean initialised;
    
    @Override
    public void init() {
      if (initialised) return;
      
      warmupPulses = (int) (pulses * warmupFrac);
      initialised = true;
    }

    @Override
    public LogConfig getLog() {
      return log;
    }

    @Override
    public String describe() {
      return String.format("%,d pulses, %,d ms duration, %.0f%% warmup fraction",
                           pulses, pulseDurationMillis, warmupFrac * 100);
    }
    
    public SpecMultiplier applyDefaults() {
      host = HOST;
      port = SocketTestSupport.getAvailablePort(PREFERRED_PORT);
      warmupFrac = 0.05f;
      initiate = true;
      log = new LogConfig() {{
        summary = stages = LOG;
        verbose = false;
      }};
      return times(2);
    }

    @Override
    public Summary run() throws Exception {
      return runner.apply(this);
    }
  }

  @Test
  public void testText() throws Exception {
    new Config() {{
      pulses = 10;
      pulseDurationMillis = 1;
      syncFrames = 10;
      topicSpec = TopicLibrary.smallLeaves();
      text = true;
      bytes = 16;
    }}.applyDefaults().test();
  }

  @Test
  public void testBinary() throws Exception {
    new Config() {{
      pulses = 10;
      pulseDurationMillis = 1;
      syncFrames = 10;
      topicSpec = TopicLibrary.smallLeaves();
      text = false;
      bytes = 16;
    }}.applyDefaults().test();
  }

  private static Summary test(Config c) throws Exception {
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
    
    final RemoteNode remote = RemoteNode.builder()
        .build();
    final RemoteRig remoteRig = new RemoteRig(remote, new RemoteRigConfig() {{
      topicSpec = c.topicSpec;
      syncFrames = c.syncFrames;
      uri = new URI(String.format("ws://%s:%d/", c.host, c.port));
      initiate = c.initiate;
      log = c.log;
    }});

    try {
      remoteRig.run();
      remoteRig.await();
    } finally {
      edgeRig.close();
      remoteRig.close();
    }
    
    return remoteRig.getSummary();
  }
  
  /**
   *  Run with -XX:-MaxFDLimit -Xms2G -Xmx4G -XX:+UseConcMarkSweepGC
   *  
   *  @param args
   *  @throws Exception 
   */
  public static void main(String[] args) throws Exception {
    BashInteractor.Ulimit.main(null);
    new Config() {{
      host = HOST;
      port = SocketTestSupport.getAvailablePort(PREFERRED_PORT);
      pulses = 300;
      pulseDurationMillis = 100;
      syncFrames = 0;
      topicSpec = TopicLibrary.largeLeaves();
      warmupFrac = 0.10f;
      initiate = true;
      text = false;
      bytes = 128;
      log = new LogConfig() {{
        progress = intermediateSummaries = true;
        stages = false;
        summary = true;
      }};
    }}.testPercentile(1, 5, 50, Summary::byLatency);
  }
}
