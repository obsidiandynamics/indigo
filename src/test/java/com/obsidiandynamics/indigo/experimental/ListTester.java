package com.obsidiandynamics.indigo.experimental;

import java.util.*;

import com.obsidiandynamics.indigo.*;

public final class ListTester {
  public static void main(String[] args) {
    final int steps = 1_000_000;
    final int stepSize = 1_000;
    
    final Deque<Object> list = new ArrayDeque<>();
    
    System.out.format("Running benchmark...\n");
    System.out.format("steps=%,d, stepSize=%,d\n", steps, stepSize);
    
    final long took = TestSupport.took(() -> {
      for (int i = 0; i < steps; i++) {
        for (int j = 0; j < stepSize; j++) {
          final Object obj = new Object();
          list.addLast(obj);
        }
        
        for (int j = 0; j < stepSize; j++) {
          final Object first = list.removeFirst();
          first.hashCode();
        }
      }
    });

    System.out.format("Took %,d s, %,d objs/s\n", took / 1000, (long) steps * stepSize * 2 / took * 1000);
  }
}
