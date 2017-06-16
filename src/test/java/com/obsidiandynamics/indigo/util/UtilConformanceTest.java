package com.obsidiandynamics.indigo.util;

import static com.obsidiandynamics.indigo.util.TestSupport.*;

import java.lang.reflect.*;

import org.junit.*;

import com.obsidiandynamics.indigo.*;

public class UtilConformanceTest {
  @Test
  public void test() throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
    assertUtilityClassWellDefined(ActorConfig.Key.class);
    assertUtilityClassWellDefined(ActorSystemConfig.Key.class);
    assertUtilityClassWellDefined(Assertions.class);
    assertUtilityClassWellDefined(BashInteractor.class);
    assertUtilityClassWellDefined(Crypto.class);
    assertUtilityClassWellDefined(Diagnostics.Key.class);
    assertUtilityClassWellDefined(Functions.class);
    assertUtilityClassWellDefined(IndigoVersion.class);
    assertUtilityClassWellDefined(Mocks.class);
    assertUtilityClassWellDefined(PropertyUtils.class);
    assertUtilityClassWellDefined(Threads.class);
  }
}
