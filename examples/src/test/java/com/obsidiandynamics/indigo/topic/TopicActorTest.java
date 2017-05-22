package com.obsidiandynamics.indigo.topic;

import static junit.framework.TestCase.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

import org.junit.*;
import org.mockito.*;

import com.obsidiandynamics.indigo.*;

public final class TopicActorTest {
  private ActorSystem system;
  
  private TopicWatcher topicWatcher;
  
  @Before
  public void setup() {
    topicWatcher = mock(TopicWatcher.class);
    system = ActorSystem.create()
    .addExecutor(r -> r.run()).named("current_thread")
    .on(TopicActor.ROLE).cue(() -> new TopicActor(new TopicConfig() {{
      executorName = "current_thread";
      topicWatcher = TopicActorTest.this.topicWatcher;
    }}));
  }
  
  @After
  public void teardown() {
    system.shutdownQuietly();
  }
  
  @SuppressWarnings("unchecked")
  private static <T> T notNull() {
    return (T) Mockito.notNull();
  }
  
  private static void verifyInOrder(Object mock, Consumer<InOrder> test) {
    final InOrder inOrder = inOrder(mock);
    test.accept(inOrder);
    inOrder.verifyNoMoreInteractions();
  }
  
  @Test
  public void testNonInterfering() throws InterruptedException, ExecutionException {
    final List<Delivery> aList = new ArrayList<>();
    final List<Delivery> bList = new ArrayList<>();
    subscribe("a", aList::add).get();
    subscribe("b", bList::add).get();
    verifyInOrder(topicWatcher, inOrder -> {
      inOrder.verify(topicWatcher).created(notNull(), eq(Topic.of("a")));
      inOrder.verify(topicWatcher).subscribed(notNull(), eq(Topic.of("a")), notNull());
      inOrder.verify(topicWatcher).created(notNull(), eq(Topic.of("b")));
      inOrder.verify(topicWatcher).subscribed(notNull(), eq(Topic.of("b")), notNull());
    });
    publish("a", "hello").get();
    publish("b", "barev").get();
    assertEquals(1, aList.size());
    assertEquals(1, bList.size());
    assertEquals("hello", aList.get(0).getPayload());
    assertEquals("barev", bList.get(0).getPayload());
  }
  
  @Test
  public void testMultipleSubscribers() throws InterruptedException, ExecutionException {
    final List<Delivery> a1List = new ArrayList<>();
    final List<Delivery> a2List = new ArrayList<>();
    subscribe("a", a1List::add).get();
    subscribe("a", a2List::add).get();
    verifyInOrder(topicWatcher, inOrder -> {
      inOrder.verify(topicWatcher).created(notNull(), eq(Topic.of("a")));
      inOrder.verify(topicWatcher, times(2)).subscribed(notNull(), eq(Topic.of("a")), notNull());
    });
    publish("a", "hello").get();
    assertEquals(1, a1List.size());
    assertEquals(1, a2List.size());
    assertEquals("hello", a1List.get(0).getPayload());
    assertEquals("hello", a2List.get(0).getPayload());
  }
  
  @Test
  public void testDuplicateSubscribers() throws InterruptedException, ExecutionException {
    final List<Delivery> aList = new ArrayList<>();
    final Subscriber sub = aList::add;
    subscribe("a", sub).get();
    subscribe("a", sub).get();
    verifyInOrder(topicWatcher, inOrder -> {
      inOrder.verify(topicWatcher).created(notNull(), eq(Topic.of("a")));
      inOrder.verify(topicWatcher).subscribed(notNull(), eq(Topic.of("a")), notNull());
    });
    publish("a", "hello").get();
    assertEquals(1, aList.size());
    assertEquals("hello", aList.get(0).getPayload());
  }
  
  @Test
  public void testNoSubscribers() {
    publish("a", "hello");
  }
  
  @Test
  public void testHierarchyBasic() throws InterruptedException, ExecutionException {
    final List<Delivery> aList = new ArrayList<>();
    final List<Delivery> abList = new ArrayList<>();
    final List<Delivery> abcList = new ArrayList<>();
    subscribe("a", aList::add).get();
    subscribe("a/b", abList::add).get();
    subscribe("a/b/c", abcList::add).get();
    verifyInOrder(topicWatcher, inOrder -> {
      inOrder.verify(topicWatcher).created(notNull(), eq(Topic.of("a")));
      inOrder.verify(topicWatcher).subscribed(notNull(), eq(Topic.of("a")), notNull());
      inOrder.verify(topicWatcher).created(notNull(), eq(Topic.of("a/b")));
      inOrder.verify(topicWatcher).subscribed(notNull(), eq(Topic.of("a/b")), notNull());
      inOrder.verify(topicWatcher).created(notNull(), eq(Topic.of("a/b/c")));
      inOrder.verify(topicWatcher).subscribed(notNull(), eq(Topic.of("a/b/c")), notNull());
    });
    publish("a", "hello").get();
    publish("a/b", "barev").get();
    publish("a/b/c", "ciao").get();
    assertEquals(1, aList.size());
    assertEquals(1, abList.size());
    assertEquals(1, abcList.size());
    assertEquals("hello", aList.get(0).getPayload());
    assertEquals("barev", abList.get(0).getPayload());
    assertEquals("ciao", abcList.get(0).getPayload());
  }
  
  @Test
  public void testHierarchyMultiLevelWildcard() throws InterruptedException, ExecutionException {
    final List<Delivery> xList = new ArrayList<>();
    final List<Delivery> abList = new ArrayList<>();
    final List<Delivery> axList = new ArrayList<>();
    final List<Delivery> cList = new ArrayList<>();
    final List<Delivery> cxList = new ArrayList<>();
    subscribe("#", xList::add).get();
    subscribe("a/b", abList::add).get();
    subscribe("a/#", axList::add).get();
    subscribe("c", cList::add).get();
    subscribe("c/#", cxList::add).get();
    verifyInOrder(topicWatcher, inOrder -> {
      inOrder.verify(topicWatcher).subscribed(notNull(), eq(Topic.of("#")), notNull());
      inOrder.verify(topicWatcher).created(notNull(), eq(Topic.of("a/b")));
      inOrder.verify(topicWatcher).subscribed(notNull(), eq(Topic.of("a/b")), notNull());
      inOrder.verify(topicWatcher).subscribed(notNull(), eq(Topic.of("a/#")), notNull());
      inOrder.verify(topicWatcher).created(notNull(), eq(Topic.of("c")));
      inOrder.verify(topicWatcher).subscribed(notNull(), eq(Topic.of("c")), notNull());
      inOrder.verify(topicWatcher).subscribed(notNull(), eq(Topic.of("c/#")), notNull());
    });
    publish("a", "hello").get();
    publish("a/b", "barev").get();
    publish("c", "ciao").get();
    publish("c/p", "privet").get();
    publish("c/p/b", "privet vsem").get();
    assertEquals(5, xList.size());
    assertEquals(1, abList.size());
    assertEquals(1, abList.size());
    assertEquals(1, axList.size());
    assertEquals(1, cList.size());
    assertEquals(2, cxList.size());
    assertEquals("hello", xList.get(0).getPayload());
    assertEquals("barev", xList.get(1).getPayload());
    assertEquals("ciao", xList.get(2).getPayload());
    assertEquals("privet", xList.get(3).getPayload());
    assertEquals("privet vsem", xList.get(4).getPayload());
    assertEquals("barev", abList.get(0).getPayload());
    assertEquals("barev", axList.get(0).getPayload());
    assertEquals("ciao", cList.get(0).getPayload());
    assertEquals("privet", cxList.get(0).getPayload());
    assertEquals("privet vsem", cxList.get(1).getPayload());
  }
  
  @Test
  public void testHierarchySingleLevelWildcard() throws InterruptedException, ExecutionException {
    final List<Delivery> xList = new ArrayList<>();
    final List<Delivery> abList = new ArrayList<>();
    final List<Delivery> xbList = new ArrayList<>();
    final List<Delivery> xcList = new ArrayList<>();
    final List<Delivery> axList = new ArrayList<>();
    final List<Delivery> xxList = new ArrayList<>();
    subscribe("+", xList::add).get();
    subscribe("a/b", abList::add).get();
    subscribe("+/b", xbList::add).get();
    subscribe("+/c", xcList::add).get();
    subscribe("a/+", axList::add).get();
    subscribe("+/+", xxList::add).get();
    verifyInOrder(topicWatcher, inOrder -> {
      inOrder.verify(topicWatcher).subscribed(notNull(), eq(Topic.of("+")), notNull());
      inOrder.verify(topicWatcher).created(notNull(), eq(Topic.of("a/b")));
      inOrder.verify(topicWatcher).subscribed(notNull(), eq(Topic.of("a/b")), notNull());
      inOrder.verify(topicWatcher).subscribed(notNull(), eq(Topic.of("+/b")), notNull());
      inOrder.verify(topicWatcher).subscribed(notNull(), eq(Topic.of("+/c")), notNull());
      inOrder.verify(topicWatcher).subscribed(notNull(), eq(Topic.of("a/+")), notNull());
      inOrder.verify(topicWatcher).subscribed(notNull(), eq(Topic.of("+/+")), notNull());
    });
    publish("a", "hello").get();
    publish("a/b", "barev").get();
    publish("a/c", "ciao").get();
    assertEquals(1, xList.size());
    assertEquals(1, abList.size());
    assertEquals(1, xbList.size());
    assertEquals(1, xcList.size());
    assertEquals(2, axList.size());
    assertEquals(2, xxList.size());
    assertEquals("hello", xList.get(0).getPayload());
    assertEquals("barev", abList.get(0).getPayload());
    assertEquals("barev", xbList.get(0).getPayload());
    assertEquals("ciao", xcList.get(0).getPayload());
    assertEquals("barev", axList.get(0).getPayload());
    assertEquals("ciao", axList.get(1).getPayload());
    assertEquals("barev", xxList.get(0).getPayload());
    assertEquals("ciao", xxList.get(1).getPayload());
  }
  
  private CompletableFuture<SubscribeResponse> subscribe(String topic, Subscriber subscriber) {
    return system.ask(ActorRef.of(TopicActor.ROLE), new Subscribe(Topic.of(topic), subscriber));
  }
  
  private CompletableFuture<PublishResponse> publish(String topic, Object payload) {
    return system.ask(ActorRef.of(TopicActor.ROLE), new Publish(Topic.of(topic), payload));
  }
}
