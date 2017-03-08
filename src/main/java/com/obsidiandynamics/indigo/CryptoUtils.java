package com.obsidiandynamics.indigo;

import java.util.*;

final class CryptoUtils {
  private CryptoUtils() {};
  
  static long machineRandom() {
    final UUID uuid = UUID.randomUUID();
    return uuid.getLeastSignificantBits() ^ uuid.getMostSignificantBits();
  }
}
