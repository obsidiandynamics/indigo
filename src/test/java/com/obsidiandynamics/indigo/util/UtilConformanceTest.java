package com.obsidiandynamics.indigo.util;

import java.lang.reflect.*;

import org.junit.*;

import com.obsidiandynamics.indigo.*;

public class UtilConformanceTest {
  @Test
  public void test() throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
    assertUtilityClassWellDefined(ActorConfig.Key.class);
    assertUtilityClassWellDefined(ActorSystemConfig.Key.class);
    assertUtilityClassWellDefined(Diagnostics.Key.class);
    assertUtilityClassWellDefined(Crypto.class);
    assertUtilityClassWellDefined(Functions.class);
    assertUtilityClassWellDefined(IndigoVersion.class);
    assertUtilityClassWellDefined(PropertyUtils.class);
    assertUtilityClassWellDefined(Threads.class);
  }

  /**
   *  Verifies that a utility class is well defined.
   * 
   *  Taken from 
   *  https://github.com/trajano/maven-jee6/blob/master/maven-jee6-test/src/test/java/net/trajano/maven_jee6/test/test/UtilityClassTestUtilTest.java
   * 
   *  @param clazz Utility class to verify.
   */
  private static void assertUtilityClassWellDefined(final Class<?> clazz) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
    Assert.assertTrue("Class must be final",
                      Modifier.isFinal(clazz.getModifiers()));
    Assert.assertEquals("There must be only one constructor", 1,
                        clazz.getDeclaredConstructors().length);
    final Constructor<?> constructor = clazz.getDeclaredConstructor();
    if (constructor.isAccessible() || 
        !Modifier.isPrivate(constructor.getModifiers())) {
      Assert.fail("Constructor is not private");
    }
    constructor.setAccessible(true);
    constructor.newInstance();
    constructor.setAccessible(false);
    for (final Method method : clazz.getMethods()) {
      if (! Modifier.isStatic(method.getModifiers())
          && method.getDeclaringClass().equals(clazz)) {
        Assert.fail("There exists a non-static method: " + method);
      }
    }
  }
}
