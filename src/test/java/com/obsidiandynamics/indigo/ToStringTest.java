package com.obsidiandynamics.indigo;

import static com.obsidiandynamics.indigo.util.TestSupport.*;

import java.util.*;

import org.junit.*;

import com.obsidiandynamics.indigo.task.*;
import com.obsidiandynamics.indigo.util.*;

public class ToStringTest {
  @Test
  public void test() {
    assertToString(new Message(null, null, null, null, false));
    assertToString(new Message(null, null, null, new UUID(0, 0), false));
    assertToString(ActorRef.of("role"));
    assertToString(ActorRef.of("role", "key"));
    assertToString(new Activation(0, null, null, new ActorConfig(), null, null) {
      @Override public boolean enqueue(Message m) { return false; }
    });
    assertToString(new TimeoutTask(0, null, null, null, null));
    assertToString(new Task<Integer>(0, null) {
      @Override protected void execute() {}
    });
    assertToString(new Fault(null, null, null));
    assertToString(new Diagnostics.LogEntry("test %d, %d %d", 1, 2, 3));
    assertToString(new Integral64.Sum());
    assertToString(new Integral64.Sum().certain(42));
    assertToString(new Integral64.Sum().uncertain(42));
    assertToString(new JvmVersionProvider.JvmVersion(0, 0, 0, 0));
  }
}
