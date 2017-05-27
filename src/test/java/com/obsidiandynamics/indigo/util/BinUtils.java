package com.obsidiandynamics.indigo.util;

public final class BinUtils {
  private BinUtils() {}
  
  public static StringBuilder dump(byte[] bytes) {
    final StringBuilder sb = new StringBuilder();
    for (int i = 0; i < bytes.length; i++) {
      sb.append(toHex(bytes[i]));
      sb.append(' ');
      if (i % 16 == 0) {
        sb.append('\n');
      } else if (i % 8 == 0) {
        sb.append("   ");
      }
    }
    return sb;
  }
  
  public static String toHex(byte b) {
    final String str = Integer.toHexString(Byte.toUnsignedInt(b));
    return str.length() < 2 ? "0" + str : str;
  }
}
