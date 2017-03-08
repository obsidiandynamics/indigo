package com.obsidiandynamics.indigo;

import static com.obsidiandynamics.indigo.ActorRef.INGRESS;
import static com.obsidiandynamics.indigo.ActorSystemConfig.Executor.FIXED_THREAD_POOL;

import java.util.concurrent.*;

/**
 *  Benchmarks raw message throughput. Nearly identical to 
 *  http://letitcrash.com/post/20397701710/50-million-messages-per-second-on-a-single.
 *  
 *  Run with -server -XX:+TieredCompilation -XX:+UseNUMA -XX:+UseCondCardMark -XX:-UseBiasedLocking -Xms1024M -Xmx2048M -Xss1M -XX:+UseParallelGC
 */
public final class EchoBenchmark3 {
  private static final class State {
    int rx;
    int tx;
  }

  private static final String DRIVER = "driver";
  private static final String ECHO = "echo";
  
  private long test(int actors, int messages, int seedMessages, boolean verbose) {
    if (seedMessages > messages / 2) 
      throw new IllegalArgumentException("Seed messages cannot be greater than half the total number of messages");
    
    final CountDownLatch latch = new CountDownLatch(actors);
    
    final ActorSystem actorSystem = new ActorSystemConfig() {{
      executor = FIXED_THREAD_POOL;
      defaultActorConfig = new ActorConfig() {{
        priority = 10_000;
      }};
    }}
    .define()
    .when(DRIVER).lambda(State::new, (a, s) -> {
      switch (a.message().from().role()) {
        case ECHO:
          s.rx++;
          if (verbose) System.out.format("%s received from %s (rx=%,d, tx=%,d)\n", a.self(), a.message().from(), s.rx, s.tx);
          if (s.rx == messages / 2 && s.tx == messages / 2) {
            if (verbose) System.out.format("Done %s\n", a.self());
            latch.countDown();
          } else if (s.tx != messages / 2) {
            s.tx++;
            a.reply();
          }
          break;
          
        case INGRESS:
          a.to(ActorRef.of(ECHO, a.message().body().toString())).times(seedMessages).tell();
          s.tx += seedMessages;
          break;
          
        default: throw new UnsupportedOperationException(a.message().from().role());
      }
    })
    .when(ECHO).lambda(a -> a.reply());
    
    final long took = TestSupport.took(() -> {
      actorSystem.ingress().times(actors).act((a, i) -> a.to(ActorRef.of(DRIVER, String.valueOf(i))).tell(i));
      try {
        latch.await();
      } catch (InterruptedException e) {}
    });
   
    actorSystem.shutdown();
    return took;
  }
  
  public static void main(String[] args) {
    final int threads = Runtime.getRuntime().availableProcessors();
    final int actors = threads * 16;
    final int messages = 20_000_000;
    final int seedMessages = 1_000;
    System.out.format("Running benchmark...\n");
    System.out.format("%d threads, %,d send actors, %,d messages/actor, %,d seed messages/actor\n", threads, actors, messages, seedMessages);
    final long took = new EchoBenchmark3().test(actors, messages, seedMessages, false);
    System.out.format("Took %,d s, %,d msgs/s\n", took / 1000, (long) messages * actors / took * 1000);
  }
}
