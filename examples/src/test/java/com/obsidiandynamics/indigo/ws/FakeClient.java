package com.obsidiandynamics.indigo.ws;

import java.io.*;
import java.net.*;

import javax.net.*;

public final class FakeClient extends Thread {
  private final InputStream in;
  
  private final byte[] buffer;
  
  public FakeClient(String path, int port) throws UnknownHostException, IOException {
    super("FakeClient");
    final Socket socket = SocketFactory.getDefault().createSocket("localhost", port);
    
    final PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
    writer.println("GET " + path + " HTTP/1.1\r");
    writer.println("Accept-Encoding: gzip\r");
    writer.println("User-Agent: fake\r");
    writer.println("Upgrade: websocket\r");
    writer.println("Connection: Upgrade\r");
    writer.println("Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==\r");
    writer.println("Origin: http://example.com\r");
    writer.println("Sec-WebSocket-Protocol: chat, superchat\r");
    writer.println("Sec-WebSocket-Version: 13\r");
    writer.println("Pragma: no-cache\r");
    writer.println("Cache-Control: no-cache\r");
    writer.println("Host: localhost:" + port + "\r");
    writer.println("\r");
    writer.flush();
    
    in = socket.getInputStream();
    final BufferedReader reader = new BufferedReader(new InputStreamReader(in));
    String line;
    while ((line = reader.readLine()) != null) {
      line = reader.readLine();
      System.out.println("read " + line);
      if (line.isEmpty()) break;
    }
    
    buffer = new byte[1024];
    start();
  }
  
  @Override
  public void run() {
    for (;;) {
      try {
        final int read = in.read(buffer);
        System.out.println("read " + read);
        if (read == -1) break;
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
}
