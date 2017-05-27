package com.obsidiandynamics.indigo.util;

import java.lang.reflect.*;

public final class InterceptingProxy<T> implements InvocationHandler, TestSupport {
  public interface InvocationObserver<T> {
    void onInvoke(T delegate, Method method, Object[] args, Object ret);
  }
  
  private final T delegate;
  private final InvocationObserver<? super T> observer;
  
  private InterceptingProxy(T delegate, InvocationObserver<? super T> observer) {
    this.delegate = delegate;
    this.observer = observer;
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    @SuppressWarnings("unchecked")
    final T ret = (T) method.invoke(delegate, args);
    observer.onInvoke(delegate, method, args, ret);
    return ret;
  }
  
  @SuppressWarnings("unchecked")
  public static <T> T of(T delegate, InvocationObserver<? super T> observer) {
    final Class<?> cls = delegate.getClass();
    final Class<?>[] interfaces;
    if (cls.isInterface()) {
      interfaces = new Class[] {cls};
    } else {
      interfaces = cls.getInterfaces();
    }
    final InterceptingProxy<T> proxy = new InterceptingProxy<>(delegate, observer);
    return (T) Proxy.newProxyInstance(cls.getClassLoader(), interfaces, proxy);
  }
  
  @SuppressWarnings("unchecked")
  public static <T> T of(Class<T> cls, T delegate, InvocationObserver<? super T> observer) {
    final InterceptingProxy<T> proxy = new InterceptingProxy<>(delegate, observer);
    return (T) Proxy.newProxyInstance(cls.getClassLoader(), new Class[] {cls}, proxy);
  }
}
