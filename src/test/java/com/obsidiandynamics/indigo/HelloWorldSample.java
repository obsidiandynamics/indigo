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
      .ingress(a -> a.to(ActorId.of("echo", 0)).tell("hello world"));
    }
  }
}
