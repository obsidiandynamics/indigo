package com.obsidiandynamics.indigo.util;

public final class Assertions {
  private Assertions() {}
  
  public static boolean areEnabled() {
    try {
      assert false;
      return false;
    } catch (AssertionError e) {
      return true;
    }
  }
}
