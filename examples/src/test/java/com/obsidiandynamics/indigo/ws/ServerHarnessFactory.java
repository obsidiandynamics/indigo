package com.obsidiandynamics.indigo.ws;

@FunctionalInterface
interface ServerHarnessFactory<H extends ServerHarness<?>> {
  H create(ServerProgress progress) throws Exception;
}
