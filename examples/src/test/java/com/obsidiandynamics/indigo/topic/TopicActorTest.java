package com.obsidiandynamics.indigo.topic;

import static com.obsidiandynamics.indigo.util.Mocks.*;
import static junit.framework.TestCase.*;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.*;

import org.junit.*;

import com.obsidiandynamics.indigo.*;

public final class TopicActorTest implements TestSupport {
  private static final class ProxyWatcher implements TopicWatcher, TestSupport {
    private final TopicWatcher delegate;
    
    ProxyWatcher(TopicWatcher delegate) {
      this.delegate = delegate;
    }

    @Override public void created(Activation a, Topic topic) {
      log("created(%s, %s)\n", a, topic);
      delegate.created(a, topic);
    }

    @Override public void deleted(Activation a, Topic topic) {
      log("deleted(%s, %s)\n", a, topic);
      delegate.deleted(a, topic);
    }

    @Override public void subscribed(Activation a, Topic topic, Subscriber subscriber) {
      log("subscribed(%s, %s, %s)\n", a, topic, subscriber);
      delegate.subscribed(a, topic, subscriber);
    }

    @Override public void unsubscribed(Activation a, Topic topic, Subscriber subscriber) {
      log("unsubscribed(%s, %s, %s)\n", a, topic, subscriber);
      delegate.unsubscribed(a, topic, subscriber);
    }
  }
  
  private ActorSystem system;
  
  private TopicWatcher topicWatcher;
  
  @Before
  public void setup() {
    topicWatcher = mock(TopicWatcher.class);
    system = ActorSystem.create()
    .on(TopicActor.ROLE).cue(() -> new TopicActor(new TopicConfig() {{
      topicWatcher = new ProxyWatcher(TopicActorTest.this.topicWatcher);
    }}));
  }
  
  @After
  public void teardown() {
    system.shutdownQuietly();
  }
  
  @Test
  public void testNonInterfering() throws InterruptedException, ExecutionException {
    final List<Delivery> aList = new ArrayList<>();
    final List<Delivery> bList = new ArrayList<>();
    subscribe("a", aList::add);
    subscribe("b", bList::add);
    ordered(topicWatcher, inOrder -> {
      inOrder.verify(topicWatcher).created(anyNotNull(), eq(Topic.of("a")));
      inOrder.verify(topicWatcher).subscribed(anyNotNull(), eq(Topic.of("a")), anyNotNull());
      inOrder.verify(topicWatcher).created(anyNotNull(), eq(Topic.of("b")));
      inOrder.verify(topicWatcher).subscribed(anyNotNull(), eq(Topic.of("b")), anyNotNull());
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
    subscribe("a", a1List::add);
    subscribe("a", a2List::add);
    ordered(topicWatcher, inOrder -> {
      inOrder.verify(topicWatcher).created(anyNotNull(), eq(Topic.of("a")));
      inOrder.verify(topicWatcher, times(2)).subscribed(anyNotNull(), eq(Topic.of("a")), anyNotNull());
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
    subscribe("a", sub);
    subscribe("a", sub);
    ordered(topicWatcher, inOrder -> {
      inOrder.verify(topicWatcher).created(anyNotNull(), eq(Topic.of("a")));
      inOrder.verify(topicWatcher).subscribed(anyNotNull(), eq(Topic.of("a")), anyNotNull());
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
    subscribe("a", aList::add);
    subscribe("a/b", abList::add);
    subscribe("a/b/c", abcList::add);
    ordered(topicWatcher, inOrder -> {
      inOrder.verify(topicWatcher).created(anyNotNull(), eq(Topic.of("a")));
      inOrder.verify(topicWatcher).subscribed(anyNotNull(), eq(Topic.of("a")), anyNotNull());
      inOrder.verify(topicWatcher).created(anyNotNull(), eq(Topic.of("a/b")));
      inOrder.verify(topicWatcher).subscribed(anyNotNull(), eq(Topic.of("a/b")), anyNotNull());
      inOrder.verify(topicWatcher).created(anyNotNull(), eq(Topic.of("a/b/c")));
      inOrder.verify(topicWatcher).subscribed(anyNotNull(), eq(Topic.of("a/b/c")), anyNotNull());
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
    subscribe("#", xList::add);
    subscribe("a/b", abList::add);
    subscribe("a/#", axList::add);
    subscribe("c", cList::add);
    subscribe("c/#", cxList::add);
    ordered(topicWatcher, inOrder -> {
      inOrder.verify(topicWatcher).subscribed(anyNotNull(), eq(Topic.of("#")), anyNotNull());
      inOrder.verify(topicWatcher).created(anyNotNull(), eq(Topic.of("a/b")));
      inOrder.verify(topicWatcher).subscribed(anyNotNull(), eq(Topic.of("a/b")), anyNotNull());
      inOrder.verify(topicWatcher).subscribed(anyNotNull(), eq(Topic.of("a/#")), anyNotNull());
      inOrder.verify(topicWatcher).created(anyNotNull(), eq(Topic.of("c")));
      inOrder.verify(topicWatcher).subscribed(anyNotNull(), eq(Topic.of("c")), anyNotNull());
      inOrder.verify(topicWatcher).subscribed(anyNotNull(), eq(Topic.of("c/#")), anyNotNull());
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
    subscribe("+", xList::add);
    subscribe("a/b", abList::add);
    subscribe("+/b", xbList::add);
    subscribe("+/c", xcList::add);
    subscribe("a/+", axList::add);
    subscribe("+/+", xxList::add);
    ordered(topicWatcher, inOrder -> {
      inOrder.verify(topicWatcher).subscribed(anyNotNull(), eq(Topic.of("+")), anyNotNull());
      inOrder.verify(topicWatcher).created(anyNotNull(), eq(Topic.of("a/b")));
      inOrder.verify(topicWatcher).subscribed(anyNotNull(), eq(Topic.of("a/b")), anyNotNull());
      inOrder.verify(topicWatcher).subscribed(anyNotNull(), eq(Topic.of("+/b")), anyNotNull());
      inOrder.verify(topicWatcher).subscribed(anyNotNull(), eq(Topic.of("+/c")), anyNotNull());
      inOrder.verify(topicWatcher).subscribed(anyNotNull(), eq(Topic.of("a/+")), anyNotNull());
      inOrder.verify(topicWatcher).subscribed(anyNotNull(), eq(Topic.of("+/+")), anyNotNull());
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
  
  @Test
  public void testHierarchyUnsubscribeBottomUp() throws InterruptedException, ExecutionException {
    final List<Delivery> list = new ArrayList<>();
    final Subscriber sub = list::add;
    subscribe("a0", sub);
    subscribe("a1", sub);
    subscribe("a0/b0", sub);
    subscribe("a0/b1", sub);
    subscribe("a0/b0/c0", sub);
    
    publishSelf("a0").get();
    publishSelf("a1").get();
    publishSelf("a0/b0").get();
    publishSelf("a0/b1").get();
    publishSelf("a0/b0/c0").get();
    assertEquals(5, list.size());
    assertEquals(5, new HashSet<>(list).size());
    
    unsubscribe("a0/b0/c0", sub);
    unsubscribe("a0/b1", sub);
    unsubscribe("a0/b0", sub);
    unsubscribe("a1", sub);
    unsubscribe("a0", sub);
    ordered(topicWatcher, inOrder -> {
      inOrder.verify(topicWatcher).created(anyNotNull(), eq(Topic.of("a0")));
      inOrder.verify(topicWatcher).subscribed(anyNotNull(), eq(Topic.of("a0")), anyNotNull());
      inOrder.verify(topicWatcher).created(anyNotNull(), eq(Topic.of("a1")));
      inOrder.verify(topicWatcher).subscribed(anyNotNull(), eq(Topic.of("a1")), anyNotNull());
      inOrder.verify(topicWatcher).created(anyNotNull(), eq(Topic.of("a0/b0")));
      inOrder.verify(topicWatcher).subscribed(anyNotNull(), eq(Topic.of("a0/b0")), anyNotNull());
      inOrder.verify(topicWatcher).created(anyNotNull(), eq(Topic.of("a0/b1")));
      inOrder.verify(topicWatcher).subscribed(anyNotNull(), eq(Topic.of("a0/b1")), anyNotNull());
      inOrder.verify(topicWatcher).created(anyNotNull(), eq(Topic.of("a0/b0/c0")));
      inOrder.verify(topicWatcher).subscribed(anyNotNull(), eq(Topic.of("a0/b0/c0")), anyNotNull());
      inOrder.verify(topicWatcher).unsubscribed(anyNotNull(), eq(Topic.of("a0/b0/c0")), anyNotNull());
      inOrder.verify(topicWatcher).deleted(anyNotNull(), eq(Topic.of("a0/b0/c0")));
      inOrder.verify(topicWatcher).unsubscribed(anyNotNull(), eq(Topic.of("a0/b1")), anyNotNull());
      inOrder.verify(topicWatcher).deleted(anyNotNull(), eq(Topic.of("a0/b1")));
      inOrder.verify(topicWatcher).unsubscribed(anyNotNull(), eq(Topic.of("a0/b0")), anyNotNull());
      inOrder.verify(topicWatcher).deleted(anyNotNull(), eq(Topic.of("a0/b0")));
      inOrder.verify(topicWatcher).unsubscribed(anyNotNull(), eq(Topic.of("a1")), anyNotNull());
      inOrder.verify(topicWatcher).deleted(anyNotNull(), eq(Topic.of("a1")));
      inOrder.verify(topicWatcher).unsubscribed(anyNotNull(), eq(Topic.of("a0")), anyNotNull());
      inOrder.verify(topicWatcher).deleted(anyNotNull(), eq(Topic.of("a0")));
    });
    
    list.clear();
    publishSelf("a0").get();
    publishSelf("a1").get();
    publishSelf("a0/b0").get();
    publishSelf("a0/b1").get();
    publishSelf("a0/b0/c0").get();
    assertEquals(0, list.size());
  }
  
  @Test
  public void testHierarchyUnsubscribeTopDown() throws InterruptedException, ExecutionException {
    final List<Delivery> list = new ArrayList<>();
    final Subscriber sub = list::add;
    subscribe("a0", sub);
    subscribe("a1", sub);
    subscribe("a0/b0", sub);
    subscribe("a0/b1", sub);
    subscribe("a0/b0/c0", sub);
    
    publishSelf("a0").get();
    publishSelf("a1").get();
    publishSelf("a0/b0").get();
    publishSelf("a0/b1").get();
    publishSelf("a0/b0/c0").get();
    assertEquals(5, list.size());
    assertEquals(5, new HashSet<>(list).size());
    
    unsubscribe("a0", sub);
    unsubscribe("a1", sub);
    unsubscribe("a0/b0", sub);
    unsubscribe("a0/b1", sub);
    unsubscribe("a0/b0/c0", sub);
    ordered(topicWatcher, inOrder -> {
      inOrder.verify(topicWatcher).created(anyNotNull(), eq(Topic.of("a0")));
      inOrder.verify(topicWatcher).subscribed(anyNotNull(), eq(Topic.of("a0")), anyNotNull());
      inOrder.verify(topicWatcher).created(anyNotNull(), eq(Topic.of("a1")));
      inOrder.verify(topicWatcher).subscribed(anyNotNull(), eq(Topic.of("a1")), anyNotNull());
      inOrder.verify(topicWatcher).created(anyNotNull(), eq(Topic.of("a0/b0")));
      inOrder.verify(topicWatcher).subscribed(anyNotNull(), eq(Topic.of("a0/b0")), anyNotNull());
      inOrder.verify(topicWatcher).created(anyNotNull(), eq(Topic.of("a0/b1")));
      inOrder.verify(topicWatcher).subscribed(anyNotNull(), eq(Topic.of("a0/b1")), anyNotNull());
      inOrder.verify(topicWatcher).created(anyNotNull(), eq(Topic.of("a0/b0/c0")));
      inOrder.verify(topicWatcher).subscribed(anyNotNull(), eq(Topic.of("a0/b0/c0")), anyNotNull());
      inOrder.verify(topicWatcher).unsubscribed(anyNotNull(), eq(Topic.of("a0")), anyNotNull());
      inOrder.verify(topicWatcher).unsubscribed(anyNotNull(), eq(Topic.of("a1")), anyNotNull());
      inOrder.verify(topicWatcher).deleted(anyNotNull(), eq(Topic.of("a1")));
      inOrder.verify(topicWatcher).unsubscribed(anyNotNull(), eq(Topic.of("a0/b0")), anyNotNull());
      inOrder.verify(topicWatcher).unsubscribed(anyNotNull(), eq(Topic.of("a0/b1")), anyNotNull());
      inOrder.verify(topicWatcher).deleted(anyNotNull(), eq(Topic.of("a0/b1")));
      inOrder.verify(topicWatcher).unsubscribed(anyNotNull(), eq(Topic.of("a0/b0/c0")), anyNotNull());
      inOrder.verify(topicWatcher).deleted(anyNotNull(), eq(Topic.of("a0/b0/c0")));
      inOrder.verify(topicWatcher).deleted(anyNotNull(), eq(Topic.of("a0/b0")));
      inOrder.verify(topicWatcher).deleted(anyNotNull(), eq(Topic.of("a0")));
    });
    
    list.clear();
    publishSelf("a0").get();
    publishSelf("a1").get();
    publishSelf("a0/b0").get();
    publishSelf("a0/b1").get();
    publishSelf("a0/b0/c0").get();
    assertEquals(0, list.size());
  }
  
  @Test
  public void testHierarchyUnsubscribeMultiLevelWildcard() throws InterruptedException, ExecutionException {
    final List<Delivery> list = new ArrayList<>();
    final Subscriber sub = list::add;
    subscribe("#", sub);
    subscribe("a/b", sub);
    subscribe("a/#", sub);
    subscribe("c", sub);
    subscribe("c/#", sub);
    
    publishSelf("a").get();
    assertEquals(1, list.size());
    publishSelf("a/b").get();
    assertEquals(4, list.size());
    publishSelf("c").get();
    assertEquals(6, list.size());
    publishSelf("c/d").get();
    assertEquals(8, list.size());
    
    unsubscribe("#", sub);
    unsubscribe("a/b", sub);
    unsubscribe("a/#", sub);
    unsubscribe("c", sub);
    unsubscribe("c/#", sub);
    
    ordered(topicWatcher, inOrder -> {
      inOrder.verify(topicWatcher).subscribed(anyNotNull(), eq(Topic.of("#")), anyNotNull());
      inOrder.verify(topicWatcher).created(anyNotNull(), eq(Topic.of("a")));
      inOrder.verify(topicWatcher).created(anyNotNull(), eq(Topic.of("a/b")));
      inOrder.verify(topicWatcher).subscribed(anyNotNull(), eq(Topic.of("a/b")), anyNotNull());
      inOrder.verify(topicWatcher).subscribed(anyNotNull(), eq(Topic.of("a/#")), anyNotNull());
      inOrder.verify(topicWatcher).created(anyNotNull(), eq(Topic.of("c")));
      inOrder.verify(topicWatcher).subscribed(anyNotNull(), eq(Topic.of("c")), anyNotNull());
      inOrder.verify(topicWatcher).subscribed(anyNotNull(), eq(Topic.of("c/#")), anyNotNull());
      inOrder.verify(topicWatcher).unsubscribed(anyNotNull(), eq(Topic.of("#")), anyNotNull());
      inOrder.verify(topicWatcher).unsubscribed(anyNotNull(), eq(Topic.of("a/b")), anyNotNull());
      inOrder.verify(topicWatcher).deleted(anyNotNull(), eq(Topic.of("a/b")));
      inOrder.verify(topicWatcher).unsubscribed(anyNotNull(), eq(Topic.of("a/#")), anyNotNull());
      inOrder.verify(topicWatcher).deleted(anyNotNull(), eq(Topic.of("a")));
      inOrder.verify(topicWatcher).unsubscribed(anyNotNull(), eq(Topic.of("c")), anyNotNull());
      inOrder.verify(topicWatcher).unsubscribed(anyNotNull(), eq(Topic.of("c/#")), anyNotNull());
      inOrder.verify(topicWatcher).deleted(anyNotNull(), eq(Topic.of("c")));
    });
    
    list.clear();
    publishSelf("a").get();
    publishSelf("a/b").get();
    publishSelf("c").get();
    publishSelf("c/d").get();
    assertEquals(0, list.size());
  }
  
  @Test
  public void testHierarchyUnsubscribeSingleLevelWildcard() throws InterruptedException, ExecutionException {
    final List<Delivery> list = new ArrayList<>();
    final Subscriber sub = list::add;
    subscribe("+", sub);
    subscribe("a/b", sub);
    subscribe("+/b", sub);
    subscribe("+/c", sub);
    subscribe("a/+", sub);
    subscribe("+/+", sub);

    unsubscribe("+", sub);
    unsubscribe("a/b", sub);
    unsubscribe("+/b", sub);
    unsubscribe("+/c", sub);
    unsubscribe("a/+", sub);
    unsubscribe("+/+", sub);
    
    ordered(topicWatcher, inOrder -> {
      inOrder.verify(topicWatcher).subscribed(anyNotNull(), eq(Topic.of("+")), anyNotNull());
      inOrder.verify(topicWatcher).created(anyNotNull(), eq(Topic.of("a")));
      inOrder.verify(topicWatcher).created(anyNotNull(), eq(Topic.of("a/b")));
      inOrder.verify(topicWatcher).subscribed(anyNotNull(), eq(Topic.of("a/b")), anyNotNull());
      inOrder.verify(topicWatcher).subscribed(anyNotNull(), eq(Topic.of("+/b")), anyNotNull());
      inOrder.verify(topicWatcher).subscribed(anyNotNull(), eq(Topic.of("+/c")), anyNotNull());
      inOrder.verify(topicWatcher).subscribed(anyNotNull(), eq(Topic.of("a/+")), anyNotNull());
      inOrder.verify(topicWatcher).subscribed(anyNotNull(), eq(Topic.of("+/+")), anyNotNull());
      inOrder.verify(topicWatcher).unsubscribed(anyNotNull(), eq(Topic.of("+")), anyNotNull());
      inOrder.verify(topicWatcher).unsubscribed(anyNotNull(), eq(Topic.of("a/b")), anyNotNull());
      inOrder.verify(topicWatcher).deleted(anyNotNull(), eq(Topic.of("a/b")));
      inOrder.verify(topicWatcher).unsubscribed(anyNotNull(), eq(Topic.of("+/b")), anyNotNull());
      inOrder.verify(topicWatcher).unsubscribed(anyNotNull(), eq(Topic.of("+/c")), anyNotNull());
      inOrder.verify(topicWatcher).unsubscribed(anyNotNull(), eq(Topic.of("a/+")), anyNotNull());
      inOrder.verify(topicWatcher).deleted(anyNotNull(), eq(Topic.of("a")));
      inOrder.verify(topicWatcher).unsubscribed(anyNotNull(), eq(Topic.of("+/+")), anyNotNull());
    });
    
    publishSelf("a").get();
    publishSelf("a/b").get();
    publishSelf("a/c").get();
    assertEquals(0, list.size());
  }
  
  private void subscribe(String topic, Subscriber subscriber) throws InterruptedException, ExecutionException {
    final CompletableFuture<?> f = system.ask(ActorRef.of(TopicActor.ROLE), new Subscribe(Topic.of(topic), subscriber));
    f.get();
    system.drain(0);
  }
  
  private void unsubscribe(String topic, Subscriber subscriber) throws InterruptedException, ExecutionException {
    final CompletableFuture<?> f =  system.ask(ActorRef.of(TopicActor.ROLE), new Unsubscribe(Topic.of(topic), subscriber));
    f.get();
    system.drain(0);
  }
  
  private CompletableFuture<PublishResponse> publishSelf(String topic) {
    return publish(topic, topic);
  }
  
  private CompletableFuture<PublishResponse> publish(String topic, Object payload) {
    return system.ask(ActorRef.of(TopicActor.ROLE), new Publish(Topic.of(topic), payload));
  }
}
