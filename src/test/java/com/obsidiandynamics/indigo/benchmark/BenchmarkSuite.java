package com.obsidiandynamics.indigo.benchmark;

import com.obsidiandynamics.indigo.util.*;

/**
 *  Run with -server -XX:+TieredCompilation -XX:+UseNUMA -XX:+UseCondCardMark -XX:-UseBiasedLocking -Xms1024M -Xmx2048M -Xss1M -XX:+UseParallelGC
 */
public final class BenchmarkSuite {
  public static void main(String[] args) {
    final LogConfig LOG = new LogConfig() {{
      summary = true;
    }};
    
    Threads.sleep(10000);
    
    System.out.println("**\nExternal messages - high throughput");
    new ThroughputBenchmark.Config() {{
      threads = Runtime.getRuntime().availableProcessors() * 1;
      actors = threads * 1;
      n = 10_000_000;
      warmupFrac = .05f;
      bias = 10_000;
      log = LOG;
    }}.testPercentile(5, 11, 50, Summary::byThroughput);
    
    System.out.println("**\nMessage echo - high throughput");
    new EchoBenchmark.Config() {{
      threads = Runtime.getRuntime().availableProcessors();
      actors = threads * 4;
      bias = 2_000;
      messages = 50_000_000;
      seedMessages = 2_000;
      warmupFrac = .25f;
      log = LOG;
      stats = false;
      statsSync = true;
      statsSamples = 1_000;
    }}.testPercentile(1, 3, 50, Summary::byThroughput);
    
    System.out.println("**\nMessage pairs - high throughput");
    new RequestResponseBenchmark.Config() {{
      threads = Runtime.getRuntime().availableProcessors();
      actors = threads * 2;
      bias = 1_000;
      pairs = 20_000_000;
      seedPairs = 1_000;
      warmupFrac = .25f;
      timeout = 0;
      log = LOG;
      stats = false;
      statsSync = true;
      statsSamples = 1_000;
    }}.testPercentile(1, 3, 50, Summary::byThroughput);
  }
}
