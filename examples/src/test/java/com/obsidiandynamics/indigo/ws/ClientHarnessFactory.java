package com.obsidiandynamics.indigo.ws;

@FunctionalInterface
interface ClientHarnessFactory<H extends ClientHarness<?>> {
  H create() throws Exception;
}
