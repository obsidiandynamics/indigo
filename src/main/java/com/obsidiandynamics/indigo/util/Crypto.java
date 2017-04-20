package com.obsidiandynamics.indigo.util;

import java.util.*;

public final class Crypto {
  private Crypto() {}
  
  public static long machineRandom() {
    final UUID uuid = UUID.randomUUID();
    return uuid.getLeastSignificantBits() ^ uuid.getMostSignificantBits();
  }
}
