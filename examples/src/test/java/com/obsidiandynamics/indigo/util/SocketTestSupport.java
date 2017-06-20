package com.obsidiandynamics.indigo.util;

import static com.obsidiandynamics.indigo.util.TestSupport.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.*;

public interface SocketTestSupport {
  static int MIN_PORT = 1024;
  static int MAX_PORT = 49151;
  static int PORT_DRAIN_INTERVAL_MILLIS = 100;
  static boolean LOG_PORT_SCAVENGE = true;
  static boolean LOG_PORT_DRAIN = true;
  
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
      if (isLocalPortAvailable(port)) {
        return port;
      } else {
        port++;
        if (LOG_PORT_SCAVENGE) LOG_STREAM.format("Port %d unavailable for binding; trying %d\n", preferredPort, port);
      }
    }
    throw new NoAvailablePortsException("No available ports in the range " + preferredPort + " - " + MAX_PORT);
  }

  static boolean isRemotePortAvailable(String host, int port) {
    try (Socket s = new Socket(host, port)) {
      return false;
    } catch (IOException e) {}
    return true;
  }
  
  static boolean isLocalPortAvailable(int port) {
    try (ServerSocket ss = new ServerSocket(port, 1, Inet4Address.getByAddress(new byte[4]))) {
      ss.setReuseAddress(true);
      return true;
    } catch (IOException e) {}
    return false;
  }
  
  static int getPortUseCount(int port) {
    final String cmd = String.format("netstat -an | grep %d | wc -l", port);
    final AtomicReference<String> outputHolder = new AtomicReference<>();
    final int exitCode = BashInteractor.execute(cmd, true, outputHolder::set);
    if (exitCode != 0) {
      throw new RuntimeException(String.format("Command '%s' exited with code %d", cmd, exitCode));
    }
    final String output = outputHolder.get();
    return Integer.parseInt(output.trim());
  }
  
  static void drainPort(int port, int maxUseCount) throws InterruptedException {
    final AtomicBoolean logged = new AtomicBoolean();
    Await.bounded(Integer.MAX_VALUE, PORT_DRAIN_INTERVAL_MILLIS, () -> {
      final int useCount = getPortUseCount(port);
      if (LOG_PORT_DRAIN && useCount > maxUseCount && ! logged.get()) {
        logged.set(true);
        LOG_STREAM.format("Port %d at %,d connections; draining to %,d\n", port, useCount, maxUseCount);
      }
      return useCount <= maxUseCount;
    });
  }
}
