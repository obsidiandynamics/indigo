package com.obsidiandynamics.indigo.util;

import static com.obsidiandynamics.assertion.Assertions.*;

import org.junit.*;

import com.obsidiandynamics.assertion.*;
import com.obsidiandynamics.indigo.*;

public class UtilConformanceTest {
  @Test
  public void test() throws Exception {
    assertUtilityClassWellDefined(ActorConfig.Key.class);
    assertUtilityClassWellDefined(ActorSystemConfig.Key.class);
    assertUtilityClassWellDefined(Assertions.class);
    assertUtilityClassWellDefined(Crypto.class);
    assertUtilityClassWellDefined(Diagnostics.Key.class);
    assertUtilityClassWellDefined(Functions.class);
    assertUtilityClassWellDefined(IndigoVersion.class);
    assertUtilityClassWellDefined(PropertyUtils.class);
    assertUtilityClassWellDefined(Threads.class);
  }
}
