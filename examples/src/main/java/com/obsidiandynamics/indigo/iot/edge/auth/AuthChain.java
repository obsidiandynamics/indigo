package com.obsidiandynamics.indigo.iot.edge.auth;

import static com.obsidiandynamics.indigo.topic.Topic.*;

import java.util.*;

import com.obsidiandynamics.indigo.iot.*;
import com.obsidiandynamics.indigo.topic.*;

public final class AuthChain {
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
    final int length;
    final boolean definite;
    
    private Match(int length, boolean definite) {
      this.length = length;
      this.definite = definite;
    }
    
    static Match common(Topic t1, Topic t2) {
      final String[] parts1 = t1.getParts();
      final String[] parts2 = t2.getParts();
      final int extent = Math.min(parts1.length, parts2.length);
      
      boolean definite = true;
      int i;
      for (i = 0; i < extent; i++) {
        if (parts1[i].equals(parts2[i])) {
        } else if (parts1[i].equals(SL_WILDCARD) || parts2[i].equals(SL_WILDCARD)) {
          definite = false;
        } else {
          break;
        }
      }
      return new Match(i, definite);
    }
  }
  
  private static Topic strip(Topic topic) {
    final String[] parts = topic.getParts();
    final List<String> newParts = new ArrayList<>(parts.length);
    for (int i = 0; i < parts.length; i++) {
      final String part = parts[i];
      if (! part.equals(ML_WILDCARD)) {
        newParts.add(part);
      } else {
        break;
      }
    }
    return new Topic(newParts.toArray(new String[newParts.size()]));
  }
  
  public List<Authenticator> get(String topic) {
    final Topic original = Topic.of(topic);
    final Topic stripped = strip(original);
    final boolean exact = stripped.length() == original.length();
    System.out.format("original=%s (%d), stripped=%s (%d), exact=%b\n", 
                      original, original.length(), stripped, stripped.length(), exact);
    
    final List<Authenticator> definite = new ArrayList<>();
    final List<Authenticator> plausible = new ArrayList<>();
    int longestDefiniteMatch = 0, longestPlausibleMatch = 0;
    for (Map.Entry<Topic, Authenticator> entry : filters.entrySet()) {
      final Topic filter = entry.getKey();
      final Match match = Match.common(filter, stripped);
      System.out.format("filter=%s, matchLength=%d (%b)\n", filter, match.length, match.definite);
      if (match.length != filter.length() && match.length != stripped.length()) continue;
      if (exact && match.length != filter.length()) continue;
      
      if (match.definite && match.length >= longestDefiniteMatch) {
        if (match.length > longestDefiniteMatch) {
          definite.clear();
        }
        
        longestDefiniteMatch = match.length;
        definite.add(entry.getValue());
        System.out.format("  adding definite %s\n", filter);
      } else if (! match.definite && match.length >= longestPlausibleMatch) {
        if (match.length > longestPlausibleMatch) {
          plausible.clear();
        }
        
        longestPlausibleMatch = match.length;
        plausible.add(entry.getValue());
        System.out.format("  adding plausible %s\n", filter);
      }
    }
    
    if (definite.isEmpty() && plausible.isEmpty()) throw new NoAuthenticatorException("No match for topic " + topic + ", filters=" + filters.keySet());
    
    definite.addAll(plausible);
    return definite;
  }
  
//  public Authenticator get(String topic) {
//    final Topic t = Topic.of(topic);
//    for (Map.Entry<Topic, Authenticator> entry : filters.entrySet()) {
//      if (entry.getKey().accepts(t)) {
//        return entry.getValue();
//      }
//    }
//    throw new NoAuthenticatorException("No authenticator for topic " + topic);
//  }
  
  public void validate() {
    get("#");
  }
}
