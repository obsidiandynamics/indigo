package com.obsidiandynamics.indigo.util;
import java.io.*;
import java.util.function.*;

public final class BashInteractor {
  public static int execute(String command, boolean waitForResponse, Consumer<String> handler) {
    int shellExitStatus = -1;
    final ProcessBuilder pb = new ProcessBuilder("bash", "-c", command);
    pb.redirectErrorStream(true);
    try {
      final Process shell = pb.start();

      if (waitForResponse) {
        final InputStream shellIn = shell.getInputStream();
        shellExitStatus = shell.waitFor();
        convertStreamToStr(shellIn, handler);
        shellIn.close();
      }
    } catch (IOException e) {
      System.err.println("Error occured while executing command: " + e.getMessage());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    return shellExitStatus;
  }

  private static String convertStreamToStr(InputStream is, Consumer<String> handler) throws IOException {
    if (is != null) {
      final Writer writer = new StringWriter();
      final char[] buffer = new char[1024];
      try {
        final Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        int n;
        while ((n = reader.read(buffer)) != -1) {
          final String output = new String(buffer, 0, n);
          writer.write(buffer, 0, n);

          if (handler != null) {
            handler.accept(output);
          }
        }
      } finally {
        is.close();
      }
      return writer.toString();
    } else {
      return "";
    }
  }

  public static final class Ulimit {
    public static void main(String[] args) {
      System.out.println("$ ulimit -Sa");
      BashInteractor.execute("ulimit -Sa", true, System.out::print);
      System.out.println();
      System.out.println("$ ulimit -Ha");
      BashInteractor.execute("ulimit -Ha", true, System.out::print);
    }
  }
}