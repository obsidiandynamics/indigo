package com.obsidiandynamics.indigo.util;

import static org.mockito.Mockito.*;

import java.util.function.*;

import org.mockito.*;

public final class Mocks {
  private Mocks() {}
  
  @SuppressWarnings("unchecked")
  public static <T> T anyNotNull() {
    return (T) Mockito.notNull();
  }
  
  public static void ordered(Object mock, Consumer<InOrder> test) {
    final InOrder inOrder = inOrder(mock);
    test.accept(inOrder);
    inOrder.verifyNoMoreInteractions();
  }
  
  public static <T> T logger(T delegate) {
    return InterceptingProxy.of(delegate, new LoggingInterceptor<>());
  }
  
  public static <T> T logger(Class<T> cls, T delegate) {
    return logger(cls, delegate, new LoggingInterceptor<>());
  }
  
  public static <T> T logger(Class<T> cls, T delegate, LoggingInterceptor<T> interceptor) {
    return InterceptingProxy.of(cls, delegate, interceptor);
  }
}
