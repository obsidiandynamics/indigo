package com.obsidiandynamics.indigo;

import java.util.concurrent.*;

import org.junit.*;

import com.obsidiandynamics.indigo.util.*;

public final class ExecutorRegistrationTest implements TestSupport {
  private ActorSystem system;
  
  @Before
  public void setup() {
    system = new TestActorSystemConfig() {}.createActorSystem();
  }
  
  @After
  public void teardown() {
    system.shutdownSilently();
  }
  
  @Test(expected=DuplicateExecutorException.class)
  public void testDuplicateExecutorRegistration() {
    system.addExecutor(ForkJoinPool.commonPool()).named("fjp");
    system.addExecutor(ForkJoinPool.commonPool()).named("fjp");
  }
  
  @Test(expected=NoSuchExecutorException.class)
  public void testWithoutExecutor() {
    system.send(Message.builder().to(ActorRef.of("foo")).build(), "bar");
  }
}
