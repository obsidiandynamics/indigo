package com.obsidiandynamics.indigo.iot.rig.edge;

import static org.junit.Assert.*;

import java.util.*;

import org.junit.*;

import com.obsidiandynamics.indigo.iot.edge.*;
import com.obsidiandynamics.indigo.iot.edge.auth.*;
import com.obsidiandynamics.indigo.iot.edge.auth.Authenticator.*;
import com.obsidiandynamics.indigo.iot.frame.*;

public final class AuthChainTest {
  private static Authenticator deny(String reason) {
    return new Authenticator() {
      @Override public void verify(EdgeNexus nexus, Auth auth, String topic, AuthenticationOutcome outcome) {
        outcome.deny(new TopicAccessError(reason, topic));
      }
    };
  }
  
//  '~' -> allow
//  '$remote' -> deny
//  'custom' -> deny
//  'custom/public' -> allow
//  'custom/public/privatised' -> deny
//
//  # = deny, because $remote and custom
//  $remote = deny because $remote
//  $remote/# = deny because $remote
//  $remote/a = deny because $remote
//  $remote/+/rx = deny because $remote
//  custom = deny because custom
//  custom/a = deny because custom
//  custom/public = allow
//  custom/public/b = allow
//  custom/public/+ = deny
//  custom/public/b/+ = allow
//  custom/public/privatised = deny
//  custom/public/privatised/b = deny
//  custom/public/privatised/? = deny
//  custom/# = deny
//  custom/+/b = deny
//  custom/# = deny because custom
//  custom/+ = deny because custom
  
  private AuthChain chain;

//  @Test
//  public void test() {
//    chain = AuthChain.createDefault()
//        .clear()
//        .set("", Authenticator::allowAll)
//        .set("remote", deny("remote"))
//        .set("custom", deny("custom"))
//        .set("custom/public", Authenticator::allowAll)
//        .set("custom/public/privatised", deny("custom/public/privatised"));
//    
//    assertOutcome("#", "custom/public/privatised", "remote", "custom");
//  }
//  
//  private void assertOutcome(String topic, String ... errorDescriptions) {
//    final List<Authenticator> matchingAuthenticators = chain.get_(topic);
//    final Set<String> actualErrors = collectErrors(matchingAuthenticators, topic);
//    assertArrayEquals(errorDescriptions, actualErrors);
//  }
//
//  private static Set<String> collectErrors(List<Authenticator> authenticators, String topic) {
//    final List<String> errors = new ArrayList<>(authenticators.size());
//    for (Authenticator auth : authenticators) {
//      auth.verify(null, null, topic, new AuthenticationOutcome() {
//        @Override public void deny(TopicAccessError error) {
//          errors.add(error.getDescription());
//        }
//        @Override public void allow() {}
//      });
//    }
//    return errors.toArray(new String[errors.size()]);
//  }
}
