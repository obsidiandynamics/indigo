package com.obsidiandynamics.indigo.hello;

import com.obsidiandynamics.indigo.*;

public final class HelloWorldSample {
  public static void main(String[] args) throws InterruptedException {
    ActorSystem.create()
    .on("echo").cue((a, m) -> System.out.println(a.self() + " received " + m.body()))
    .ingress(a -> a.to(ActorRef.of("echo")).tell("hello world"))
    .shutdown();
  }
}