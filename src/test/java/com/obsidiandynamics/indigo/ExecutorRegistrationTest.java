package com.obsidiandynamics.indigo;

import java.util.concurrent.*;

import org.junit.*;

public final class ExecutorRegistrationTest implements TestSupport {
  private ActorSystem system;
  
  @Before
  public void setup() {
    system = new TestActorSystemConfig() {}.createActorSystem();
  }
  
  @After
  public void teardown() {
    system.shutdownQuietly();
  }
  
  @Test(expected=DuplicateExecutorException.class)
  public void testDuplicateExecutorRegistration() {
    system.useExecutor(ForkJoinPool.commonPool()).named("fjp");
    system.useExecutor(ForkJoinPool.commonPool()).named("fjp");
  }
  
  @Test(expected=NoSuchExecutorException.class)
  public void testWithoutExecutor() {
    system.send(Message.builder().to(ActorRef.of("foo")).build(), "bar");
  }
}
