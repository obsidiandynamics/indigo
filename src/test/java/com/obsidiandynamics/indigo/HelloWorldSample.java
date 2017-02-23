package com.obsidiandynamics.indigo;

public final class HelloWorldSample {
  public static void main(String[] args) {
    try (ActorSystem system = new ActorSystem()) {
      system
      .when("echo").apply(a -> {
        System.out.println("Received " + a.message());
        a.to(ActorId.of("exit", 0)).tell("bye");
      })
      .when("exit").apply(a -> {
        System.out.println("Received " + a.message());
        System.exit(0);
      })
      .send(new Message(ActorId.of("echo", 0), "hello world"));
      

      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {}
    }
  }
}
