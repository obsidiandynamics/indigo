package com.obsidiandynamics.indigo.topic;

import static java.util.concurrent.TimeUnit.*;
import static junit.framework.TestCase.*;
import static org.awaitility.Awaitility.*;

import java.util.*;
import java.util.concurrent.*;

import org.junit.*;

import com.obsidiandynamics.indigo.*;

public final class TopicActorTest {
  private ActorSystem system;
  
  @Before
  public void setup() {
    system = ActorSystem.create()
    .on(TopicActor.ROLE).cue(() -> new TopicActor(new TopicConfig()));
  }
  
  @After
  public void teardown() {
    system.shutdownQuietly();
  }
  
  @Test
  public void testBasicRoot() throws InterruptedException, ExecutionException {
    final int n = 10;
    final List<Delivery> received = new CopyOnWriteArrayList<>();
    subscribe("", received::add).get();
    for (int i = 0; i < n; i++) publish("", "hello");
    awaitMinSize(received, n);
    assertEquals(n, received.size());
    assertEquals("hello", received.get(0).getPayload());
  }
  
  @Test
  public void testNonInterfering() throws InterruptedException, ExecutionException {
    final List<Delivery> aList = new CopyOnWriteArrayList<>();
    final List<Delivery> bList = new CopyOnWriteArrayList<>();
    subscribe("a", aList::add).get();
    subscribe("b", bList::add).get();
    publish("a", "hello");
    publish("b", "barev");
    awaitMinSize(aList, 1);
    awaitMinSize(bList, 1);
    assertEquals(1, aList.size());
    assertEquals(1, bList.size());
    assertEquals("hello", aList.get(0).getPayload());
    assertEquals("barev", bList.get(0).getPayload());
  }
  
  @Test
  public void testMultipleSubscribers() throws InterruptedException, ExecutionException {
    final List<Delivery> a1List = new CopyOnWriteArrayList<>();
    final List<Delivery> a2List = new CopyOnWriteArrayList<>();
    subscribe("a", a1List::add).get();
    subscribe("a", a2List::add).get();
    publish("a", "hello");
    awaitMinSize(a1List, 1);
    awaitMinSize(a2List, 1);
    assertEquals(1, a1List.size());
    assertEquals(1, a2List.size());
    assertEquals("hello", a1List.get(0).getPayload());
    assertEquals("hello", a2List.get(0).getPayload());
  }
  
  @Test
  public void testNoSubscribers() {
    publish("a", "hello");
  }
  
  @Test
  public void testHierarchy() throws InterruptedException, ExecutionException {
    final List<Delivery> aList = new CopyOnWriteArrayList<>(); // should also get b's messages
    final List<Delivery> bList = new CopyOnWriteArrayList<>();
    subscribe("a", aList::add).get();
    subscribe("a/b", bList::add).get();
    publish("a", "hello").get();
    publish("a/b", "barev").get();
    awaitMinSize(aList, 2);
    awaitMinSize(bList, 1);
    assertEquals(2, aList.size());
    assertEquals(1, bList.size());
    assertEquals("hello", aList.get(0).getPayload());
    assertEquals("barev", aList.get(1).getPayload());
    assertEquals("barev", bList.get(0).getPayload());
  }
  
  private void awaitMinSize(Collection<?> col, int size) {
    await().atMost(10, SECONDS).until(() -> col.size() >= size);
  }

  private CompletableFuture<SubscribeResponse> subscribe(String topic, Subscriber subscriber) {
    return system.ask(ActorRef.of(TopicActor.ROLE), new Subscribe(Topic.of(topic), subscriber));
  }
  
  private CompletableFuture<PublishResponse> publish(String topic, Object payload) {
    return system.ask(ActorRef.of(TopicActor.ROLE), new Publish(Topic.of(topic), payload));
  }
}
