package com.obsidiandynamics.indigo;

import java.util.concurrent.*;

public final class IntegerState {
  public int value;
  
  @Override public String toString() { return String.valueOf(value); }
  
  public static CompletableFuture<IntegerState> blank() {
    return CompletableFuture.completedFuture(new IntegerState());
  }
}