package com.obsidiandynamics.indigo.adder;

/**
 *  The public message contract for the adder actor.
 */
public final class AdderContract {
  public static final String ROLE = "adder";
  
  /** This class is just a container for others; it doesn't require instantiation. */
  private AdderContract() {}
  
  /** Sent to the actor when adding a value. */
  public static final class Add {
    private int value;

    public Add(int value) {
      this.value = value;
    }

    int getValue() {
      return value;
    }
  }
  
  /** Sent to the actor when requesting the current sum. */
  public static final class Get {}
  
  /** Response sent by the actor containing the current sum. */
  public static final class GetResponse {
    private int sum;

    GetResponse(int sum) {
      this.sum = sum;
    }

    public int getSum() {
      return sum;
    }
  }
}
