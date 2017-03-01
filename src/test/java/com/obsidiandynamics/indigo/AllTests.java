package com.obsidiandynamics.indigo;

import org.junit.runner.*;
import org.junit.runners.*;
import org.junit.runners.Suite.*;

@RunWith(Suite.class)
@SuiteClasses({
  ParallelConsistencyTest.class,
  RequestResponseTest.class,
  StatelessChainTest.class,
  StatelessPassivationTest.class,
  StatefulPassivationTest.class,
  TimeoutTest.class
})         
public class AllTests {}
