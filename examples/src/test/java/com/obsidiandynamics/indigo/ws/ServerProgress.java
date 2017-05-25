package com.obsidiandynamics.indigo.ws;

@FunctionalInterface
interface ServerProgress {
  void update(ServerHarness<?> server, long sent);
}
