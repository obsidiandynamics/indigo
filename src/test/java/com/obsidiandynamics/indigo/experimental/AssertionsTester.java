package com.obsidiandynamics.indigo.experimental;

public class AssertionsTester {
  public static void main(String[] args) {
    System.out.println("before macro");
    assert macro(() -> {
      throw new RuntimeException("in macro");
    });
    System.out.println("after macro");
  }
  
  private static boolean macro(Runnable r) {
    System.out.println("Macro created");
    r.run();
    return true;
  }
}
