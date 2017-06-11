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
    
    @Override public String toString() {
      return length != -1 ? String.format("length=%s, definite=%b", length, definite) : "length=" + length;
    }
  }
  
  private static String[] stripMLWildcard(Topic topic) {
    if (topic.isMultiLevelWildcard()) {
      return topic.subtopic(0, topic.length() - 1).getParts();
    } else {
      return topic.getParts();
    }
  }
  
  /**
   *  Obtains all authenticators applicable to the given topic (which may be an exact topic or a topic 
   *  comprising wildcards).<p>
   *   
   *  The algorithm works by iterating over each installed path filter, aggregating authenticators based
   *  on the concept of a <em>definite</em> and <em>plausible</em> matches. All plausible matches are
   *  aggregated, as well as only the greediest definite matches (of which there may be more than one).<p>
   *      
   *  The base definition of a match involves a path filter (mapped to a single authenticator) and a topic 
   *  (what the user is trying to publish/subscribe to), such that there is a number (zero or more) of 
   *  common leading segments and <em>either</em> the filter or the topic length equals to the length of 
   *  the match. In other words either the filter is completely consumed by the topic, or vice versa. In
   *  fact, the base portion of the algorithm doesn't care which is the filter and which is the topic - 
   *  the two operands may be interchanged without affecting the outcome.<p>
   *  
   *  Some examples of matching filters (on the left) and topics (on the right):<p>
   *  
   *  them them/apples<p>
   *  them them/pears<p>
   *  them them<p>
   *  them/apples them<p>
   *  them/apples them/+<p>
   *  them/apples them/#<p>
   *  them/apples +<p>
   *  
   *  On the other hand, the filter 'them/apples' will no match the topic 'them/pears', as neither 
   *  completely consumes the other.<p>
   *  
   *  Multi-level wildcards are first stripped of the trailing '#' before being subjected to the 
   *  matching process. So 'them/#' is treated as 'them', with a caveat.<p>
   *  
   *  A definite match is one where the filter matches the given topic without traversing any 
   *  wildcards. So 'them' matches 'them/apples' definitively, whereas 'them/apples' matches 
   *  'them/+' or '+/apples' plausibly.<p>
   *  
   *  When dealing with exact topics, or topics containing single-level (but not multi-level) 
   *  wildcards, a match only qualifies when the filter (left) is completely consumed by the 
   *  topic (right), which is stricter than a base match criteria (which allows for either to be 
   *  consumed by the other). So 'them' matches 'them/apples', 'them/pears' and 'them', but 
   *  'them/apples' will not match 'them'. When dealing with a multi-level wildcard, the filter 
   *  'them/apples' matches 'them/#', as well as '#'.<p>
   *  
   *  Upon completion, this method yields <em>all</em> plausible matches, as well as the (equal) 
   *  longest definite matches.
   *  
   *  @param topic The topic under consideration.
   *  @return The matching authenticators.
   */
  public List<Authenticator> get(String topic) {
    final Topic original = Topic.of(topic);
    final String[] stripped = stripMLWildcard(original);
    final boolean exactOrSL = stripped.length == original.length();
    if (LOG.isTraceEnabled()) LOG.trace("topic={} ({}), stripped={} ({}), exactOrSL={}", 
                                        original, original.length(), Arrays.toString(stripped), 
                                        stripped.length, exactOrSL);
    
    final List<Authenticator> definite = new ArrayList<>();
    final List<Authenticator> plausible = new ArrayList<>();
    int longestDefiniteMatch = 0;
    for (Map.Entry<Topic, Authenticator> entry : filters.entrySet()) {
      final Topic filter = entry.getKey();
      final Match match = Match.common(filter.getParts(), stripped);
      if (LOG.isTraceEnabled()) LOG.trace("  filter={}, match: {}", filter, match);
      if (match == Match.INCOMPLETE || exactOrSL && match.length != filter.length()) continue;
      
      if (match.definite && match.length >= longestDefiniteMatch) {
        longestDefiniteMatch = match.length;
        definite.add(entry.getValue());
        if (LOG.isTraceEnabled()) LOG.trace("    adding definite {}", filter);
      } else if (! match.definite) {
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
