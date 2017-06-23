package com.obsidiandynamics.indigo.ws;

@FunctionalInterface
interface ServerHarnessFactory {
  ServerHarness create(int port, ServerProgress progress, int idleTimeout) throws Exception;
}
