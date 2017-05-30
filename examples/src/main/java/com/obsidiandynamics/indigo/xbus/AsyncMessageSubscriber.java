package com.obsidiandynamics.indigo.xbus;

import java.util.function.*;

public final class AsyncMessageSubscriber extends Thread implements SafeCloseable{
  private final Supplier<MessageSubscriber> factory;
  
  private Consumer<Object> receiver;
  
  private volatile MessageSubscriber subscriber;
  
  private AsyncMessageSubscriber(Supplier<MessageSubscriber> factory) {
    super("AsyncMessageSubsriber");
    this.factory = factory;
  }
  
  public static AsyncMessageSubscriber using(Supplier<MessageSubscriber> factory) {
    return new AsyncMessageSubscriber(factory);
  }
  
  public synchronized AsyncMessageSubscriber onReceive(Consumer<Object> receiver) {
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
