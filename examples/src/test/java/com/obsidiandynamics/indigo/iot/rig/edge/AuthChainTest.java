package com.obsidiandynamics.indigo.iot.rig.edge;

import static org.junit.Assert.*;

import java.util.*;

import org.junit.*;

import com.obsidiandynamics.indigo.iot.edge.*;
import com.obsidiandynamics.indigo.iot.edge.auth.*;
import com.obsidiandynamics.indigo.iot.edge.auth.AuthChain.*;
import com.obsidiandynamics.indigo.iot.edge.auth.Authenticator.*;
import com.obsidiandynamics.indigo.iot.frame.*;

public final class AuthChainTest {
  private AuthChain chain;
  
  @Before
  public void setup() {
    chain = AuthChain.createDefault().clear();
  }
  
  private void alw(String topicPrefix) {
    chain.set(topicPrefix, Authenticator::allowAll);
  }
  
  private void dny(String topicPrefix) {
    chain.set(topicPrefix, new Authenticator() {
      @Override public void verify(EdgeNexus nexus, Auth auth, String topic, AuthenticationOutcome outcome) {
        outcome.deny(new TopicAccessError(topicPrefix, topic));
      }
    });
  }
  
  @Test(expected=NoAuthenticatorException.class)
  public void testBlank() {
    assertOutcome("#");
  }
  
  @Test
  public void testSimulatedDefault() {
    alw("");
    dny("$remote");
    
    assertOutcome("foo");
    assertOutcome("$remote", "$remote");
    assertOutcome("$remote/a", "$remote");
    assertOutcome("$remote/a/+/b", "$remote");
    assertOutcome("$remote/+", "$remote");
    assertOutcome("$remote/+/a", "$remote");
    assertOutcome("$remote/+/a/+", "$remote");
    assertOutcome("$remote/#", "$remote");
    assertOutcome("$remote/a/#", "$remote");
    assertOutcome("$remote/+/#", "$remote");
    assertOutcome("$remote/+/a/#", "$remote");
    assertOutcome("+", "$remote");
    assertOutcome("+/+", "$remote");
    assertOutcome("+/#", "$remote");
    assertOutcome("+/$remote", "$remote");
    assertOutcome("+/foo", "$remote");
    assertOutcome("#", "$remote");
    
    assertOutcome("foo");
    assertOutcome("$Remote");
    assertOutcome("remote");
    assertOutcome("$rem");
    assertOutcome("$remoteX");
    assertOutcome("$rem/ote");
    assertOutcome("foo/$remote");
  }

  @Test
  public void testCustomChainDefaultAllow() {
    alw("");
    dny("remote");
    alw("remote/1234");
    alw("remote/1231");
    dny("custom");
    alw("custom/a/b/c/d");
    alw("custom/public");
    dny("custom/public/privatised");
    alw("them");
    dny("them/apples/private");

    assertOutcome("#", "remote", "custom", "custom/public/privatised", "them/apples/private");
    assertOutcome("+", "remote", "custom");
    assertOutcome("+/foo", "remote", "custom");
    assertOutcome("+/foo/#", "custom", "remote");
    assertOutcome("+/+/#", "custom", "remote", "custom/public/privatised", "them/apples/private");
    assertOutcome("+/+", "custom", "remote");
    assertOutcome("+/+/foo", "custom", "remote");
    assertOutcome("+/#", "custom", "remote", "custom/public/privatised", "them/apples/private");
    assertOutcome("+/+/+", "custom", "remote", "custom/public/privatised", "them/apples/private");
    assertOutcome("+/+/#", "custom", "remote", "custom/public/privatised", "them/apples/private");
    assertOutcome("+/+/foo/#", "custom", "remote");

    assertOutcome("remote", "remote");
    assertOutcome("remote/a", "remote");
    assertOutcome("remote/1231");
    assertOutcome("remote/1231/+");
    assertOutcome("remote/1231/#");
    assertOutcome("remote/1234");
    assertOutcome("remote/1234/+");
    assertOutcome("remote/1234/#");
    assertOutcome("remote/+/#", "remote");
    assertOutcome("remote/#", "remote");
    assertOutcome("remote/+", "remote");
    assertOutcome("remote/+/#", "remote");
    assertOutcome("remote/+/rx", "remote");
    
    assertOutcome("custom", "custom");
    assertOutcome("custom/a", "custom");
    assertOutcome("custom/public");
    assertOutcome("custom/public/b");
    assertOutcome("custom/public/b/#");
    assertOutcome("custom/public/+", "custom/public/privatised");
    assertOutcome("custom/public/+/+", "custom/public/privatised");
    assertOutcome("custom/public/+/foo", "custom/public/privatised");
    assertOutcome("custom/public/+/#", "custom/public/privatised");
    assertOutcome("custom/public/b/+");
    assertOutcome("custom/public/privatised", "custom/public/privatised");
    assertOutcome("custom/public/privatised/b", "custom/public/privatised");
    assertOutcome("custom/public/privatised/+", "custom/public/privatised");
    assertOutcome("custom/+/privatised/+", "custom", "custom/public/privatised");
    assertOutcome("custom/+/+", "custom", "custom/public/privatised");
    assertOutcome("custom/+/#", "custom", "custom/public/privatised");
    assertOutcome("custom/+/+/foo/+", "custom", "custom/public/privatised");
    assertOutcome("custom/#", "custom", "custom/public/privatised");
    assertOutcome("custom/+/b", "custom");
    assertOutcome("custom/+", "custom");
    assertOutcome("+/public", "custom", "remote");
    assertOutcome("+/+/privatised", "custom", "remote", "custom/public/privatised");
    assertOutcome("+/public/+", "custom", "remote", "custom/public/privatised");
    
    assertOutcome("them");
    assertOutcome("them/pears");
    assertOutcome("them/apples");
    assertOutcome("them/apples/private", "them/apples/private");
    assertOutcome("them/apples/private/b", "them/apples/private");
    assertOutcome("them/apples/private/+", "them/apples/private");
    assertOutcome("them/+/private/+", "them/apples/private");
    assertOutcome("them/#", "them/apples/private");
    assertOutcome("them/+/#", "them/apples/private");
    assertOutcome("them/+/public/+");
    assertOutcome("+/apples", "remote", "custom");
    assertOutcome("+/apples/private", "remote", "custom", "them/apples/private");
    assertOutcome("+", "remote", "custom");
  }

  @Test
  public void testCustomChainDefaultDeny() {
    dny("");
    dny("remote");
    alw("remote/1234");
    alw("remote/1231");
    dny("custom");
    alw("custom/a/b/c/d");
    alw("custom/public");
    dny("custom/public/privatised");
    alw("them");
    dny("them/apples/private");

    assertOutcome("#", "", "remote", "custom", "custom/public/privatised", "them/apples/private");
    assertOutcome("+", "", "remote", "custom");
    assertOutcome("+/foo", "", "remote", "custom");
    assertOutcome("+/foo/#", "", "custom", "remote");
    assertOutcome("+/+/#", "", "custom", "remote", "custom/public/privatised", "them/apples/private");
    assertOutcome("+/+", "", "custom", "remote");
    assertOutcome("+/+/foo", "", "custom", "remote");
    assertOutcome("+/#", "", "custom", "remote", "custom/public/privatised", "them/apples/private");
    assertOutcome("+/+/+", "", "custom", "remote", "custom/public/privatised", "them/apples/private");
    assertOutcome("+/+/#", "", "custom", "remote", "custom/public/privatised", "them/apples/private");
    assertOutcome("+/+/foo/#", "", "custom", "remote");

    assertOutcome("remote", "remote");
    assertOutcome("remote/a", "remote");
    assertOutcome("remote/1231");
    assertOutcome("remote/1231/+");
    assertOutcome("remote/1231/#");
    assertOutcome("remote/1234");
    assertOutcome("remote/1234/+");
    assertOutcome("remote/1234/#");
    assertOutcome("remote/+/#", "remote");
    assertOutcome("remote/#", "remote");
    assertOutcome("remote/+", "remote");
    assertOutcome("remote/+/#", "remote");
    assertOutcome("remote/+/rx", "remote");
    
    assertOutcome("custom", "custom");
    assertOutcome("custom/a", "custom");
    assertOutcome("custom/public");
    assertOutcome("custom/public/b");
    assertOutcome("custom/public/b/#");
    assertOutcome("custom/public/+", "custom/public/privatised");
    assertOutcome("custom/public/+/+", "custom/public/privatised");
    assertOutcome("custom/public/+/foo", "custom/public/privatised");
    assertOutcome("custom/public/+/#", "custom/public/privatised");
    assertOutcome("custom/public/b/+");
    assertOutcome("custom/public/privatised", "custom/public/privatised");
    assertOutcome("custom/public/privatised/b", "custom/public/privatised");
    assertOutcome("custom/public/privatised/+", "custom/public/privatised");
    assertOutcome("custom/+/privatised/+", "custom", "custom/public/privatised");
    assertOutcome("custom/+/+", "custom", "custom/public/privatised");
    assertOutcome("custom/+/#", "custom", "custom/public/privatised");
    assertOutcome("custom/+/+/foo/+", "custom", "custom/public/privatised");
    assertOutcome("custom/#", "custom", "custom/public/privatised");
    assertOutcome("custom/+/b", "custom");
    assertOutcome("custom/+", "custom");
    assertOutcome("+/public", "", "custom", "remote");
    assertOutcome("+/+/privatised", "", "custom", "remote", "custom/public/privatised");
    assertOutcome("+/public/+", "", "custom", "remote", "custom/public/privatised");
    
    assertOutcome("them");
    assertOutcome("them/pears");
    assertOutcome("them/apples");
    assertOutcome("them/apples/private", "them/apples/private");
    assertOutcome("them/apples/private/b", "them/apples/private");
    assertOutcome("them/apples/private/+", "them/apples/private");
    assertOutcome("them/+/private/+", "them/apples/private");
    assertOutcome("them/#", "them/apples/private");
    assertOutcome("them/+/#", "them/apples/private");
    assertOutcome("them/+/public/+");
    assertOutcome("+/apples", "", "remote", "custom");
    assertOutcome("+/apples/private", "", "remote", "custom", "them/apples/private");
    assertOutcome("+", "remote", "", "custom");
  }
  
  private void assertOutcome(String topic, String ... errorDescriptions) {
    final List<Authenticator> matchingAuthenticators = chain.get(topic);
    final Set<String> actualErrors = collectErrors(matchingAuthenticators, topic);
    assertEquals(new HashSet<>(Arrays.asList(errorDescriptions)), actualErrors);
  }

  private static Set<String> collectErrors(List<Authenticator> authenticators, String topic) {
    final Set<String> errors = new HashSet<>();
    for (Authenticator auth : authenticators) {
      auth.verify(null, null, topic, new AuthenticationOutcome() {
        @Override public void deny(TopicAccessError error) {
          errors.add(error.getDescription());
        }
        @Override public void allow() {}
      });
    }
    return errors;
  }
}
