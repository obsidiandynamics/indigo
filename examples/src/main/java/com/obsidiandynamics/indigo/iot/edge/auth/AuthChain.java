package com.obsidiandynamics.indigo.iot.edge.auth;

import static com.obsidiandynamics.indigo.topic.Topic.*;

import java.util.*;

import org.slf4j.*;

import com.obsidiandynamics.indigo.iot.*;
import com.obsidiandynamics.indigo.topic.*;

public final class AuthChain {
  private static final Logger LOG = LoggerFactory.getLogger(AuthChain.class);
  
  public static final class NoAuthenticatorException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    
    NoAuthenticatorException(String m) { super(m); }
  }
  
  private final Map<Topic, Authenticator> filters = new TreeMap<>(AuthChain::byLengthDescending);
  
  private static int byLengthDescending(Topic t1, Topic t2) {
    final int lengthComparison = Integer.compare(t2.length(), t1.length());
    if (lengthComparison != 0) {
      return lengthComparison;
    } else {
      return t2.toString().compareTo(t1.toString());
    }
  }
  
  private AuthChain() {}
  
  public static AuthChain createDefault() {
    return new AuthChain().registerDefaults();
  }
  
  public AuthChain clear() {
    filters.clear();
    return this;
  }
  
  private AuthChain registerDefaults() {
    set("", Authenticator::allowAll);
    set(Flywheel.REMOTE_PREFIX, new RemoteTopicAuthenticator());
    return this;
  }
  
  private Topic create(String topic) {
    return topic.isEmpty() ? Topic.root() : Topic.of(topic);
  }
  
  public AuthChain set(String topicPrefix, Authenticator authenticator) {
    filters.put(create(topicPrefix), authenticator);
    return this;
  }
  
  private static final class Match {
    static final Match INCOMPLETE = new Match(-1, true);
    
    final int length;
    final boolean definite;
    
    private Match(int length, boolean definite) {
      this.length = length;
      this.definite = definite;
    }
    
    static Match common(String[] t1, String[] t2) {
      final int extent = Math.min(t1.length, t2.length);
      
      boolean definite = true;
      int i;
      for (i = 0; i < extent; i++) {
        if (t1[i].equals(t2[i])) {
        } else if (t1[i].equals(SL_WILDCARD) || t2[i].equals(SL_WILDCARD)) {
          definite = false;
        } else {
          break;
        }
      }
      
      if (i != t1.length && i != t2.length) return INCOMPLETE;
      
      return new Match(i, definite);
    }
  }
  
  private static String[] stripMLWildcard(Topic topic) {
    if (topic.isMultiLevelWildcard()) {
      return topic.subtopic(0, topic.length() - 1).getParts();
    } else {
      return topic.getParts();
    }
  }
  
  public List<Authenticator> get(String topic) {
    final Topic original = Topic.of(topic);
    final String[] stripped = stripMLWildcard(original);
    final boolean exactOrSL = stripped.length == original.length();
    if (LOG.isTraceEnabled()) LOG.trace("topic={} ({}), stripped={} ({}), exactOrSL={}", 
                                        original, original.length(), Arrays.toString(stripped), stripped.length, exactOrSL);
    
    final List<Authenticator> definite = new ArrayList<>();
    final List<Authenticator> plausible = new ArrayList<>();
    int longestDefiniteMatch = 0, longestPlausibleMatch = 0;
    for (Map.Entry<Topic, Authenticator> entry : filters.entrySet()) {
      final Topic filter = entry.getKey();
      final Match match = Match.common(filter.getParts(), stripped);
      if (LOG.isTraceEnabled()) LOG.trace("  filter={}, matchLength={} (definite={})", 
                                          filter, match.length, match.definite);
      if (match == Match.INCOMPLETE || exactOrSL && match.length != filter.length()) continue;
      
      if (match.definite && match.length >= longestDefiniteMatch) {
        longestDefiniteMatch = match.length;
        definite.add(entry.getValue());
        if (LOG.isTraceEnabled()) LOG.trace("    adding definite {}", filter);
      } else if (! match.definite && match.length >= longestPlausibleMatch) {
        longestPlausibleMatch = match.length;
        plausible.add(entry.getValue());
        if (LOG.isTraceEnabled()) LOG.trace("    adding plausible {}", filter);
      }
    }
    
    if (definite.isEmpty() && plausible.isEmpty()) {
      throw new NoAuthenticatorException("No match for topic " + topic + ", filters=" + filters.keySet());
    }
    
    definite.addAll(plausible);
    return definite;
  }
  
  public void validate() {
    get("#");
  }
}
