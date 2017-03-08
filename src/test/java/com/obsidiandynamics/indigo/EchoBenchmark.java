package com.obsidiandynamics.indigo;

import static com.obsidiandynamics.indigo.ActorRef.*;
import static com.obsidiandynamics.indigo.ActorSystemConfig.Executor.FIXED_THREAD_POOL;
import static junit.framework.TestCase.*;

import java.util.*;

import org.junit.*;

/**
 *  Benchmarks raw message throughput. Nearly identical to 
 *  http://letitcrash.com/post/20397701710/50-million-messages-per-second-on-a-single.
 *  
 *  Run with -server -XX:+TieredCompilation -XX:+UseNUMA -XX:+UseCondCardMark -XX:-UseBiasedLocking -Xms1024M -Xmx2048M 
 *  -Xss1M -XX:+UseParallelGC
 */
public final class EchoBenchmark implements TestSupport {
  private static final String ECHO = "echo";
  private static final String DRIVER = "driver";
  private static final String DONE = "done";
  
  @Test
  public void test() {
    test(4, 1_000, 100);
  }

  private void test(int actors, int messages, int seedMessages) {
    logTestName();
    
    final Set<ActorRef> done = new HashSet<>();

    new ActorSystemConfig() {{
      executor = FIXED_THREAD_POOL;
      defaultActorConfig = new ActorConfig() {{
        priority = 10_000;
      }};
    }}
    .define()
    .when(DRIVER).lambda(IntegerState::new, (a, s) -> {
      switch (a.message().from().role()) {
        case ECHO:
          if (s.value == messages / 2) {
            a.to(ActorRef.of(DONE)).tell();
          } else {
            a.reply();
            s.value++;
          }
          break;
          
        case INGRESS:
          a.to(ActorRef.of(ECHO, a.message().body().toString())).times(seedMessages).tell();
          s.value += seedMessages;
          break;
          
        default: throw new UnsupportedOperationException(a.message().from().role());
      }
    })
    .when(ECHO).lambda(a -> a.reply())
    .when(DONE).lambda(refCollector(done))
    .ingress().times(actors).act((a, i) -> a.to(ActorRef.of(DRIVER, String.valueOf(i))).tell(i))
    .shutdown();

    assertEquals(actors, done.size());
  }
  
  public static void main(String[] args) {    
    final int threads = Runtime.getRuntime().availableProcessors();
    final int actors = threads * 16;
    final int messages = 20_000_000;
    final int seedMessages = 1_000;
    System.out.format("Running benchmark...\n");
    System.out.format("%d threads, %,d send actors, %,d messages/actor, %,d seed messages/actor\n", threads, actors, messages, seedMessages);
    final long took = TestSupport.took(() -> new EchoBenchmark().test(actors, messages, seedMessages));
    System.out.format("Took %,d s, %,d msgs/s\n", took / 1000, (long) messages * actors / took * 1000);
  }
}
