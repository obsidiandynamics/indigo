package com.obsidiandynamics.indigo.util;

import java.io.*;
import java.net.*;
import java.util.*;

public interface SocketTestSupport {
  static int MIN_PORT = 1024;
  static int MAX_PORT = 49151;
  
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
  
  final class NoAvailablePortsException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    
    NoAvailablePortsException(String m) { super(m); }
  }
  
  static int getAvailablePort(int preferredPort) {
    int port = preferredPort;
    while (port <= MAX_PORT) {
      if (isPortAvailable(port)) {
        return port;
      } else {
        port++;
      }
    }
    throw new NoAvailablePortsException("No available ports in the range " + preferredPort + " - " + MAX_PORT);
  }
  
  static boolean isPortAvailable(int port) {
    ServerSocket ss = null;
    DatagramSocket ds = null;
    try {
      ss = new ServerSocket(port);
      ss.setReuseAddress(true);
      ds = new DatagramSocket(port);
      ds.setReuseAddress(true);
      return true;
    } catch (IOException e) {
    } finally {
      if (ds != null) {
        ds.close();
      }

      if (ss != null) {
        try {
          ss.close();
        } catch (IOException e) {}
      }
    }

    return false;
  }
}
