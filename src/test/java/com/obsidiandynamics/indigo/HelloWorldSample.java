package com.obsidiandynamics.indigo;

public final class HelloWorldSample {
  public static void main(String[] args) {
    try (ActorSystem s = new ActorSystem()) {
      s
      .with("echo", System.out::println)
      .send(new Message(new ActorId("echo", 0), "hello world"));
    }

    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {}
  }
}
