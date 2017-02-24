package com.obsidiandynamics.indigo;

public final class HelloWorldSample {
  public static void main(String[] args) {
    new ActorSystem()
    .when("echo").lambda(a -> {
      System.out.println("Received " + a.message());
      a.to(ActorRef.of("exit")).tell("bye");
    })
    .when("exit").lambda(a -> {
      System.out.println("Received " + a.message());
      System.exit(0);
    })
    .ingress(a -> a.to(ActorRef.of("echo")).tell("hello world"))
    .shutdown();
  }
}
