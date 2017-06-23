package com.obsidiandynamics.indigo.socketx;

@FunctionalInterface
interface ServerProgress {
  void update(ServerHarness server, long sent);
}
