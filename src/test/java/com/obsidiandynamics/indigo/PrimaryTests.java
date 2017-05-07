package com.obsidiandynamics.indigo;

import org.junit.runner.*;
import org.junit.runners.*;
import org.junit.runners.Suite.*;

import com.obsidiandynamics.indigo.benchmark.*;

@RunWith(Suite.class)
@SuiteClasses({
  ActivationApiTest.class,
  ActorSystemConfigTest.class,
  DiagnosticsTest.class,
  DLQTest.class,
  DrainTest.class,
  EchoBenchmark.class,
  EgressTest.class,
  ExceptionHandlerTest.class,
  ExternalAskTest.class,
  FaultTest.class,
  ForwarderTest.class,
  FrameworkErrorTest.class,
  ParallelConsistencyTest.class,
  RequestResponseBenchmark.class,
  RequestResponseTest.class,
  RoleRegistrationTest.class,
  SelectTest.class,
  StashTest.class,
  StatelessChainTest.class,
  StatelessLifeCycleTest.class,
  StatefulLifeCycleTest.class,
  ThrottleTest.class,
  ThroughputBenchmark.class,
  TimeoutTest.class
})
public class PrimaryTests {}
