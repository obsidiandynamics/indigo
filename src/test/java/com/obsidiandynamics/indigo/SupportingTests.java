package com.obsidiandynamics.indigo;

import org.junit.runner.*;
import org.junit.runners.*;
import org.junit.runners.Suite.*;

import com.obsidiandynamics.indigo.util.*;

@RunWith(Suite.class)
@SuiteClasses({
  CappedForkJoinPoolTest.class,
  Integral64Test.class,
  JvmVersionProviderTest.class,
  StatefulLambdaActorApiTest.class,
  StatelessLambdaActorApiTest.class,
  TimeoutWatchdogTest.class,
  ToStringTest.class,
  UtilConformanceTest.class
})         
public class SupportingTests {}
