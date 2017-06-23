package com.obsidiandynamics.indigo.socketx;

@FunctionalInterface
interface ServerHarnessFactory {
  ServerHarness create(int port, ServerProgress progress, int idleTimeout) throws Exception;
}
