package com.obsidiandynamics.indigo.util;

public final class BinaryUtils {
  private BinaryUtils() {}
  
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
  
  public static byte[] toByteArray(int ... ints) {
    final byte[] bytes = new byte[ints.length];
    for (int i = 0; i < ints.length; i++) {
      bytes[i] = (byte) ints[i];
    }
    return bytes;
  }
}
