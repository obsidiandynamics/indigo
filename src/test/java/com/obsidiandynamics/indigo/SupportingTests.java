package com.obsidiandynamics.indigo;

import org.junit.runner.*;
import org.junit.runners.*;
import org.junit.runners.Suite.*;

import com.obsidiandynamics.indigo.util.*;

@RunWith(Suite.class)
@SuiteClasses({
  ActorRefTest.class,
  CappedForkJoinPoolTest.class,
  IndigoVersionTest.class,
  Integral64Test.class,
  JvmVersionProviderTest.class,
  MessageBuilderTest.class,
  StatefulLambdaActorBuilderTest.class,
  StatelessLambdaActorBuilderTest.class,
  ThreadsTest.class,
  TimeoutWatchdogTest.class,
  ToStringTest.class,
  UtilConformanceTest.class
})         
public class SupportingTests {}
