package com.obsidiandynamics.indigo.iot;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertEquals;

import java.util.*;

import org.junit.*;

import com.obsidiandynamics.indigo.iot.edge.auth.*;
import com.obsidiandynamics.indigo.iot.frame.*;
import com.obsidiandynamics.indigo.iot.remote.*;

public final class BindValidationTest extends AbstractAuthTest {
  @Test
  public void test() throws Exception {
    setupEdgeNode(AuthChain.createPubDefault(), AuthChain.createSubDefault());
    
    final RemoteNexus remoteNexus = openNexus();

    final BindFrame bind1 = new BindFrame(UUID.randomUUID(),
                                          null,
                                          null,
                                          null,
                                          new String[]{},
                                          null);
    final BindResponseFrame bind1Res = remoteNexus.bind(bind1).get();
    assertFalse(bind1Res.isSuccess());
    assertEquals(1, bind1Res.getErrors().length);
    assertEquals(new GeneralError("Missing attribute 'subscribe"), bind1Res.getErrors()[0]);

    final BindFrame bind2 = new BindFrame(UUID.randomUUID(),
                                          null,
                                          null,
                                          new String[]{},
                                          null,
                                          null);
    final BindResponseFrame bind2Res = remoteNexus.bind(bind2).get();
    assertFalse(bind2Res.isSuccess());
    assertEquals(1, bind2Res.getErrors().length);
    assertEquals(new GeneralError("Missing attribute 'unsubscribe'"), bind2Res.getErrors()[0]);
  }
}