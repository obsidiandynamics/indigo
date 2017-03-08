package com.obsidiandynamics.indigo;

import org.junit.runner.*;
import org.junit.runners.*;
import org.junit.runners.Suite.*;

import com.obsidiandynamics.indigo.benchmark.*;

@RunWith(Suite.class)
@SuiteClasses({
  EchoBenchmark.class,
  EgressTest.class,
  ExternalAskTest.class,
  ParallelConsistencyTest.class,
  RequestResponseBenchmark.class,
  RequestResponseTest.class,
  StatelessChainTest.class,
  StatelessPassivationTest.class,
  StatefulPassivationTest.class,
  TimeoutTest.class
})         
public class AllTests {}
