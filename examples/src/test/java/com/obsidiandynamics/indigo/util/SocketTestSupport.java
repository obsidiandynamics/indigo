package com.obsidiandynamics.indigo.util;

import java.util.*;

public interface SocketTestSupport {
  static byte[] randomBytes(int length) {
    final byte[] bytes = new byte[length];
    new Random().nextBytes(bytes);
    return bytes;
  }
  
  static String randomString(int length) {
    if (length % 2 != 0) throw new IllegalArgumentException("Length must be a multiple of 2");
    final StringBuilder sb = new StringBuilder(length);
    final byte[] bytes = randomBytes(length / 2);
    for (int i = 0; i < bytes.length; i++) {
      sb.append(BinaryUtils.toHex(bytes[i]));
    }
    return sb.toString();
  }
}
