package com.obsidiandynamics.indigo.util;

import java.lang.reflect.*;
import java.util.*;

import com.obsidiandynamics.indigo.util.InterceptingProxy.*;

public final class LoggingInterceptor<T> implements InvocationObserver<T>, TestSupport {
  @Override
  public void onInvoke(T delegate, Method method, Object[] args, Object ret) {
    final boolean isVoid = method.getReturnType() == Void.TYPE;
    final String argsArray = args == null ? "" : arrayToString(args);
    if (isVoid) {
      log("%s(%s) => void\n", method.getName(), argsArray);
    } else {
      log("%s(%s) => %s\n", method.getName(), argsArray, ret);
    }
  }
  
  private static String arrayToString(Object[] array) {
    final String rawStr = Arrays.toString(array);
    return rawStr.substring(1, rawStr.length() - 1);
  }
}
