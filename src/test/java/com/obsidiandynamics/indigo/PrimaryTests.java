package com.obsidiandynamics.indigo;

import org.junit.runner.*;
import org.junit.runners.*;
import org.junit.runners.Suite.*;

import com.obsidiandynamics.indigo.benchmark.*;

@RunWith(Suite.class)
@SuiteClasses({
  ActorSystemConfigTest.class,
  ApiTest.class,
  DiagnosticsTest.class,
  DLQTest.class,
  DrainTest.class,
  EchoBenchmark.class,
  EgressTest.class,
  ExceptionHandlerTest.class,
  ExternalAskTest.class,
  FaultTest.class,
  ForwarderTest.class,
  ParallelConsistencyTest.class,
  RequestResponseBenchmark.class,
  RequestResponseTest.class,
  StashTest.class,
  StatelessChainTest.class,
  StatelessLifeCycleTest.class,
  StatefulLifeCycleTest.class,
  ThrottleTest.class,
  ThroughputBenchmark.class,
  TimeoutTest.class
})
public class PrimaryTests {}
