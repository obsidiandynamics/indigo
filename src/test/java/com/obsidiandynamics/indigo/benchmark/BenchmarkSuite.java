package com.obsidiandynamics.indigo.benchmark;

/**
 *  Run with -server -XX:+TieredCompilation -XX:+UseNUMA -XX:+UseCondCardMark -XX:-UseBiasedLocking -Xms1024M -Xmx2048M -Xss1M -XX:+UseParallelGC
 */
public final class BenchmarkSuite {
  public static void main(String[] args) {
    new EchoBenchmark.Config() {{
      threads = Runtime.getRuntime().availableProcessors();
      actors = threads * 4;
      bias = 2_000;
      messages = 50_000_000;
      seedMessages = 2_000;
      warmupFrac = .25f;
      log = true;
      verbose = false;
      stats = false;
      statsSync = true;
      statsSamples = 1_000;
    }}.test();
  }
}
