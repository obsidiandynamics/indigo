package com.obsidiandynamics.indigo;

import static org.junit.Assert.*;

import java.util.concurrent.atomic.*;
import java.util.function.*;

import org.junit.*;

import com.obsidiandynamics.indigo.util.*;

public final class SelectTest implements TestSupport {
  private static final String SINK = "sink";
  
  private final class Foo {}
  
  private final class Bar {}
  
  private ActorSystem system;
  
  @Before
  public void setup() {
    system = new TestActorSystemConfig() {}.createActorSystem();
  }
  
  @After
  public void teardown() {
    system.shutdownQuietly();
  }
  
  private static <B> Consumer<B> got(AtomicBoolean flag) {
    return b -> flag.set(true);
  }
  
  @Test
  public void testWhenClassCaseFirst() throws InterruptedException {
    final AtomicBoolean foo = new AtomicBoolean();
    final AtomicBoolean bar = new AtomicBoolean();
    final AtomicBoolean aNull = new AtomicBoolean();
    final AtomicBoolean otherwise = new AtomicBoolean();
    
    system
    .on(SINK).cue((a, m) ->
      m.select()
      .when(Foo.class).then(got(foo))
      .when(Bar.class).then(got(bar))
      .whenNull(got(aNull))
      .otherwise(got(otherwise))
    )
    .ingress(a -> a.to(ActorRef.of(SINK)).tell(new Foo()))
    .drain(0);
    
    assertTrue(foo.get());
    assertFalse(bar.get());
    assertFalse(aNull.get());
    assertFalse(otherwise.get());
  }
  
  @Test
  public void testWhenClassCaseSecond() throws InterruptedException {
    final AtomicBoolean foo = new AtomicBoolean();
    final AtomicBoolean bar = new AtomicBoolean();
    final AtomicBoolean aNull = new AtomicBoolean();
    final AtomicBoolean otherwise = new AtomicBoolean();
    
    system
    .on(SINK).cue((a, m) ->
      m.select()
      .when(Bar.class).then(got(bar))
      .when(Foo.class).then(got(foo))
      .whenNull(got(aNull))
      .otherwise(got(otherwise))
    )
    .ingress(a -> a.to(ActorRef.of(SINK)).tell(new Foo()))
    .drain(0);
    
    assertTrue(foo.get());
    assertFalse(bar.get());
    assertFalse(aNull.get());
    assertFalse(otherwise.get());
  }
  
  @Test
  public void testWhenNull() throws InterruptedException {
    final AtomicBoolean foo = new AtomicBoolean();
    final AtomicBoolean bar = new AtomicBoolean();
    final AtomicBoolean aNull = new AtomicBoolean();
    final AtomicBoolean otherwise = new AtomicBoolean();
    
    system
    .on(SINK).cue((a, m) ->
      m.select()
      .when(Bar.class).then(got(bar))
      .when(Foo.class).then(got(foo))
      .whenNull(got(aNull))
      .otherwise(got(otherwise))
    )
    .ingress(a -> a.to(ActorRef.of(SINK)).tell())
    .drain(0);
    
    assertFalse(foo.get());
    assertFalse(bar.get());
    assertTrue(aNull.get());
    assertFalse(otherwise.get());
  }
  
  @Test
  public void testOtherwise() throws InterruptedException {
    final AtomicBoolean foo = new AtomicBoolean();
    final AtomicBoolean aNull = new AtomicBoolean();
    final AtomicBoolean otherwise = new AtomicBoolean();
    
    system
    .on(SINK).cue((a, m) ->
      m.select()
      .when(Foo.class).then(got(foo))
      .whenNull(got(aNull))
      .otherwise(got(otherwise))
    )
    .ingress(a -> a.to(ActorRef.of(SINK)).tell(new Bar()))
    .drain(0);
    
    assertFalse(foo.get());
    assertFalse(aNull.get());
    assertTrue(otherwise.get());
  }
  
  @Test
  public void testUnhandledNotNull() throws InterruptedException {
    final AtomicBoolean foo = new AtomicBoolean();
    final AtomicBoolean aNull = new AtomicBoolean();
    final AtomicReference<Fault> fault = new AtomicReference<>();
    final AtomicBoolean response = new AtomicBoolean();
    
    system
    .on(SINK).cue((a, m) ->
      m.select()
      .when(Foo.class).then(got(foo))
      .whenNull(got(aNull))
      .otherwise(a::messageFault)
    )
    .ingress(a -> a.to(ActorRef.of(SINK)).ask(new Bar()).onFault(fault::set).onResponse(got(response)))
    .drain(0);
    
    assertFalse(foo.get());
    assertFalse(aNull.get());
    assertEquals("Cannot handle message body of type " + Bar.class.getName(), fault.get().getReason());
    assertFalse(response.get());
  }
  
  @Test
  public void testUnhandledNull() throws InterruptedException {
    final AtomicBoolean foo = new AtomicBoolean();
    final AtomicReference<Fault> fault = new AtomicReference<>();
    final AtomicBoolean response = new AtomicBoolean();
    
    system
    .on(SINK).cue((a, m) ->
      m.select()
      .when(Foo.class).then(got(foo))
      .whenNull(a::messageFault)
    )
    .ingress(a -> a.to(ActorRef.of(SINK)).ask().onFault(fault::set).onResponse(got(response)))
    .drain(0);
    
    assertFalse(foo.get());
    assertEquals("Cannot handle null message body", fault.get().getReason());
    assertFalse(response.get());
  }
}
