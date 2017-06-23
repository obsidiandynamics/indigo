package com.obsidiandynamics.indigo.socketx;

@FunctionalInterface
interface ClientHarnessFactory {
  ClientHarness create(int port, boolean echo) throws Exception;
}
