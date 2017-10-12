package com.obsidiandynamics.indigo;

import static com.obsidiandynamics.assertion.Assertions.*;

import java.util.*;

import org.junit.*;

import com.obsidiandynamics.indigo.task.*;
import com.obsidiandynamics.indigo.util.*;

public class ToStringTest {
  @Test
  public void test() {
    assertToStringOverride(new Message(null, null, null, null, false));
    assertToStringOverride(new Message(null, null, null, new UUID(0, 0), false));
    assertToStringOverride(ActorRef.of("role"));
    assertToStringOverride(ActorRef.of("role", "key"));
    assertToStringOverride(new Activation(0, null, null, new ActorConfig(), null, null) {
      @Override public boolean enqueue(Message m) { return false; }
    });
    assertToStringOverride(new TimeoutTask(0, null, null, null, null));
    assertToStringOverride(new Task<Integer>(0, null) {
      @Override protected void execute() {}
    });
    assertToStringOverride(new Fault(null, null, null));
    assertToStringOverride(new Diagnostics.LogEntry("test %d, %d %d", 1, 2, 3));
    assertToStringOverride(new Integral64.Sum());
    assertToStringOverride(new Integral64.Sum().certain(42));
    assertToStringOverride(new Integral64.Sum().uncertain(42));
    assertToStringOverride(new JvmVersionProvider.JvmVersion(0, 0, 0, 0));
  }
}
