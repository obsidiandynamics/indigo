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
  public void testHierarchyMultiWildcard() throws InterruptedException, ExecutionException {
    final List<Delivery> aList = new CopyOnWriteArrayList<>(); // should also get b's messages
    final List<Delivery> bList = new CopyOnWriteArrayList<>();
    subscribe("#", aList::add).get();
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
  
  @Test
  public void testHierarchySingleWildcard() throws InterruptedException, ExecutionException {
    final List<Delivery> xList = new CopyOnWriteArrayList<>();
    final List<Delivery> abList = new CopyOnWriteArrayList<>();
    final List<Delivery> xbList = new CopyOnWriteArrayList<>();
    final List<Delivery> xcList = new CopyOnWriteArrayList<>();
    final List<Delivery> axList = new CopyOnWriteArrayList<>();
    subscribe("+", xList::add).get();
    subscribe("a/b", abList::add).get();
    subscribe("+/b", xbList::add).get();
    subscribe("+/c", xcList::add).get();
    subscribe("a/+", axList::add).get();
    publish("a", "hello").get();
    publish("a/b", "barev").get();
    publish("a/c", "ciao").get();
    awaitMinSize(xList, 1);
    awaitMinSize(xbList, 1);
    awaitMinSize(abList, 1);
    awaitMinSize(xcList, 1);
    awaitMinSize(axList, 2);
    assertEquals(1, xList.size());
    assertEquals(1, abList.size());
    assertEquals(1, xbList.size());
    assertEquals(1, xcList.size());
    assertEquals(2, axList.size());
    assertEquals("hello", xList.get(0).getPayload());
    assertEquals("barev", abList.get(0).getPayload());
    assertEquals("barev", xbList.get(0).getPayload());
    assertEquals("ciao", xcList.get(0).getPayload());
    assertEquals("barev", axList.get(0).getPayload());
    assertEquals("ciao", axList.get(1).getPayload());
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
