package com.obsidiandynamics.indigo.benchmark;

/**
 *  Run with -server -XX:+TieredCompilation -XX:+UseNUMA -XX:+UseCondCardMark -XX:-UseBiasedLocking -Xms1024M -Xmx2048M -Xss1M -XX:+UseParallelGC
 */
public final class BenchmarkSuite {
  public static void main(String[] args) {
    final LogConfig LOG = new LogConfig() {{
      summary = true;
    }};
    
    System.out.println("_\nExternal messages - high throughput");
    new ThroughputBenchmark.Config() {{
      threads = Runtime.getRuntime().availableProcessors() * 1;
      actors = threads * 1;
      n = 10_000_000;
      warmupFrac = .05f;
      bias = 10_000;
      log = LOG;
    }}.testPercentile(9, 11, 50, Summary::byThroughput);

    System.out.println("_\nMessage echo - low latency (one hop measured)");
    new EchoBenchmark.Config() {{
      threads = 1;
      actors = 1;
      bias = 1;
      messages = 1_000_000;
      seedMessages = 1;
      warmupFrac = .25f;
      log = LOG;
      stats = true;
      statsSync = true;
      statsSamples = 1_000;
    }}.testPercentile(1, 3, 50, Summary::byLatency);
    
    System.out.println("_\nMessage echo - high throughput");
    new EchoBenchmark.Config() {{
      threads = Runtime.getRuntime().availableProcessors();
      actors = threads * 4;
      bias = 2_000;
      messages = 20_000_000;
      seedMessages = 2_000;
      warmupFrac = .25f;
      log = LOG;
      stats = false;
      statsSync = true;
      statsSamples = 1_000;
    }}.testPercentile(1, 3, 50, Summary::byThroughput);
    
    System.out.println("_\nMessage pairs - low latency (round-trip measured)");
    new RequestResponseBenchmark.Config() {{
      threads = 1;
      actors = 1;
      bias = 1;
      pairs = 1_000_000;
      seedPairs = 1;
      warmupFrac = .25f;
      timeout = 0;
      log = LOG;
      stats = true;
      statsSync = true;
      statsSamples = 1_000;
    }}.testPercentile(1, 3, 50, Summary::byLatency);
    
    System.out.println("_\nMessage pairs - high throughput");
    new RequestResponseBenchmark.Config() {{
      threads = Runtime.getRuntime().availableProcessors();
      actors = threads * 2;
      bias = 1_000;
      pairs = 10_000_000;
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
