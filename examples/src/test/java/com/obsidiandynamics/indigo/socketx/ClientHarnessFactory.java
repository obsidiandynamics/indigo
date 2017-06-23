package com.obsidiandynamics.indigo.ws;

@FunctionalInterface
interface ClientHarnessFactory {
  ClientHarness create(int port, boolean echo) throws Exception;
}
