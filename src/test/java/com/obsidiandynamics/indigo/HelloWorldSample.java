package com.obsidiandynamics.indigo;

public final class HelloWorldSample {
  public static void main(String[] args) {
    new ActorSystem()
    .when("echo").lambda(a -> System.out.println("Received " + a.message()))
    .ingress(a -> a.to(ActorRef.of("echo")).tell("hello world"))
    .shutdown();
  }
}
