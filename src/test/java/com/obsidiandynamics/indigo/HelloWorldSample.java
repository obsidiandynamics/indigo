package com.obsidiandynamics.indigo;

public final class HelloWorldSample {
  public static void main(String[] args) {
    new ActorSystemConfig() {}
    .define()
    .when("echo").lambda((a, m) -> System.out.println(a.self() + " received " + m.body()))
    .ingress(a -> a.to(ActorRef.of("echo")).tell("hello world"))
    .shutdown();
  }
}
