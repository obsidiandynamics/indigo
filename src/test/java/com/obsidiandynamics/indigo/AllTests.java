package com.obsidiandynamics.indigo;

import org.junit.runner.*;
import org.junit.runners.*;
import org.junit.runners.Suite.*;

import com.obsidiandynamics.indigo.benchmark.*;

@RunWith(Suite.class)
@SuiteClasses({
  DrainTest.class,
  EchoBenchmark.class,
  EgressTest.class,
  ExternalAskTest.class,
  FaultTest.class,
  IntegralTest.class,
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
public class AllTests {}
