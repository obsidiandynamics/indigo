package com.obsidiandynamics.indigo;

import static com.obsidiandynamics.indigo.ActorSystemConfig.ExceptionHandlerChoice.*;
import static junit.framework.TestCase.*;

import org.junit.*;

public final class RoleRegistrationTest implements TestSupport {
  private static final String SINK = "sink";
  
  private ActorSystem system;
  
  @Before
  public void setup() {
    system = new TestActorSystemConfig() {}.createActorSystem();
  }
  
  @After
  public void teardown() {
    system.shutdownQuietly();
  }
  
  @Test(expected=DuplicateRoleException.class)
  public void testDuplicateRoleRegistration() {
    system.on(SINK).cue((a, m) -> {});
    system.on(SINK).cue((a, m) -> {});
  }
  
  @Test
  public void testTellWithoutRole() throws InterruptedException {
    system.getConfig().exceptionHandler = DRAIN;
    system.ingress(a -> {
      try {
        a.to(ActorRef.of(SINK)).tell();
        fail("Failed to catch NoSuchRoleException");
      } catch (NoSuchRoleException e) {}
    })
    .drain(0);
  }
  
  @Test
  public void testUnboundedAskWithoutRole() throws InterruptedException {
    system.getConfig().exceptionHandler = DRAIN;
    system.ingress(a -> {
      try {
        a.to(ActorRef.of(SINK)).ask().onResponse(r -> {});
        fail("Failed to catch NoSuchRoleException");
      } catch (NoSuchRoleException e) {}
    })
    .drain(0);
  }
  
  @Test
  public void testBoundedAskWithoutRole() throws InterruptedException {
    system.getConfig().exceptionHandler = DRAIN;
    system.ingress(a -> {
      try {
        a.to(ActorRef.of(SINK)).ask().await(1000).onTimeout(() -> {}).onResponse(r -> {});
        fail("Failed to catch NoSuchRoleException");
      } catch (NoSuchRoleException e) {}
    })
    .drain(0);
  }
}
