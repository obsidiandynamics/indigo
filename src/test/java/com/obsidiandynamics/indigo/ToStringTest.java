package com.obsidiandynamics.indigo;

import static org.junit.Assert.*;

import java.util.*;

import org.junit.*;

import com.obsidiandynamics.indigo.util.*;

public class ToStringTest {
  @Test
  public void test() {
    assertToString(new Message(null, null, null, null, false));
    assertToString(new Message(null, null, null, new UUID(0, 0), false));
    assertToString(ActorRef.of("role"));
    assertToString(ActorRef.of("role", "key"));
    assertToString(new Activation(0, null, null, null, null) {
      @Override public boolean enqueue(Message m) { return false; }
    });
    assertToString(new TimeoutTask(0, null, null, null));
    assertToString(new Fault(null, null, null));
    assertToString(new Diagnostics.LogEntry("test %d, %d %d", 1, 2, 3));
    assertToString(new Integral64.Sum());
    assertToString(new JvmVersionProvider.JvmVersion(0, 0, 0, 0));
  }

  /**
   *  Verifies that the given object overrides the <code>toString()</code> implementation, and that
   *  the implementation operates without throwing any exceptions.
   *   
   *  @param obj The object to test.
   */
  private static void assertToString(Object obj) {
    final String objectToString = obj.getClass().getName() + "@" + Integer.toHexString(obj.hashCode());
    final String actualToString = obj.toString();
    assertNotEquals(objectToString, actualToString);
  }
}
