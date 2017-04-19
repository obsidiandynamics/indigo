package com.obsidiandynamics.indigo;

import org.junit.runner.*;
import org.junit.runners.*;
import org.junit.runners.Suite.*;

@RunWith(Suite.class)
@SuiteClasses({
  CycledPrimaryTests.class,
  SupportingTests.class
})         
public class AllTests {}
