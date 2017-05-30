package com.obsidiandynamics.indigo.xbus;

import java.util.function.*;

public final class AsyncSubscriber extends Thread implements SafeCloseable {
  private final Supplier<XSubscriber> factory;
  
  private Consumer<Object> receiver;
  
  private volatile XSubscriber subscriber;
  
  private AsyncSubscriber(Supplier<XSubscriber> factory) {
    super("AsyncSubsriber");
    this.factory = factory;
  }
  
  public static AsyncSubscriber using(Supplier<XSubscriber> factory) {
    return new AsyncSubscriber(factory);
  }
  
  public synchronized AsyncSubscriber onReceive(Consumer<Object> receiver) {
    if (this.receiver != null) {
      throw new IllegalStateException("Subscriber already running");
    }
    this.receiver = receiver;
    start();
    return this;
  }
  
  @Override
  public void run() {
    subscriber = factory.get();
    for (;;) {
      final Object r = subscriber.receive();
      if (r != null) {
        receiver.accept(r);
      } else {
        break;
      }
    }
  }

  @Override
  public void close() {
    subscriber.close();
    try {
      join();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
