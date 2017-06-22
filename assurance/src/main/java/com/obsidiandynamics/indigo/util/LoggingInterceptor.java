package com.obsidiandynamics.indigo.util;

import java.lang.reflect.*;
import java.util.*;

import com.obsidiandynamics.indigo.util.InterceptingProxy.*;

public final class LoggingInterceptor<T> implements InvocationObserver<T>, TestSupport {
  private final String prefix;
  
  public LoggingInterceptor() {
    this("");
  }
  
  public LoggingInterceptor(String prefix) {
    this.prefix = prefix;
  }

  @Override
  public void onInvoke(T delegate, Method method, Object[] args, Object ret) {
    final boolean isVoid = method.getReturnType() == Void.TYPE;
    final String argsArray = args == null ? "" : arrayToString(args);
    if (isVoid) {
      log("%s%s(%s) => void\n", prefix ,method.getName(), argsArray);
    } else {
      log("%s%s(%s) => %s\n", prefix, method.getName(), argsArray, ret);
    }
  }
  
  private static String arrayToString(Object[] array) {
    final String rawStr = Arrays.toString(array);
    return rawStr.substring(1, rawStr.length() - 1);
  }
}
