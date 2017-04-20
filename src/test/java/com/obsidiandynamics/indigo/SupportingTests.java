package com.obsidiandynamics.indigo;

import org.junit.runner.*;
import org.junit.runners.*;
import org.junit.runners.Suite.*;

import com.obsidiandynamics.indigo.util.*;

@RunWith(Suite.class)
@SuiteClasses({
  Integral64Test.class,
  UtilConformanceTest.class
})         
public class SupportingTests {}
