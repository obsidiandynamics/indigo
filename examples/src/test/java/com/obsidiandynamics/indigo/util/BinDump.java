package com.obsidiandynamics.indigo.util;

public final class BinDump {
  private BinDump() {}
  
  public static StringBuilder dump(byte[] bytes) {
    final StringBuilder sb = new StringBuilder();
    for (int i = 0; i < bytes.length; i++) {
      sb.append(Integer.toHexString(bytes[i]));
      sb.append(' ');
      if (i % 16 == 0) {
        sb.append('\n');
      } else if (i % 8 == 0) {
        sb.append("   ");
      }
    }
    return sb;
  }
}
