package com.obsidiandynamics.indigo.util;

import java.lang.reflect.*;
import java.util.*;

import com.obsidiandynamics.indigo.util.InterceptingProxy.*;

public final class LoggingInterceptor<T> implements InvocationObserver<T>, TestSupport {
  @Override
  public void onInvoke(T delegate, Method method, Object[] args, Object ret) {
    final boolean isVoid = method.getReturnType() == Void.TYPE;
    if (isVoid) {
      log("%s(%s) => void\n", method.getName(), Arrays.asList(args));
    } else {
      log("%s(%s) => %s\n", method.getName(), Arrays.asList(args), ret);
    }
  }
}
