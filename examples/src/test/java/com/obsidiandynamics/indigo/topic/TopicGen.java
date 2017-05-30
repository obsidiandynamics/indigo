package com.obsidiandynamics.indigo.topic;

import java.util.*;

import com.obsidiandynamics.indigo.util.*;

public final class TopicGen {
  public static final class Interest {
    final Topic topic;
    final int count;
    
    Interest(Topic topic, int count) {
      this.topic = topic;
      this.count = count;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + count;
      result = prime * result + ((topic == null) ? 0 : topic.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      Interest other = (Interest) obj;
      if (count != other.count)
        return false;
      if (topic == null) {
        if (other.topic != null)
          return false;
      } else if (!topic.equals(other.topic))
        return false;
      return true;
    }
    
    public Topic getTopic() {
      return topic;
    }
    
    public int getCount() {
      return count;
    }
    
    public String toString() {
      return count + " in " + topic;
    }
  }
  
  private final Combinations<TopicGenNode> combs;
  
  public TopicGen(List<List<TopicGenNode>> topicGenMatrix) {
    combs = new Combinations<>(topicGenMatrix);
  }
  
  public final List<Topic> getLeafTopics() {
    final List<Topic> topics = new ArrayList<>(combs.size());
    for (List<TopicGenNode> nodes : combs) {
      final String[] frags = new String[nodes.size()];
      for (int i = 0; i < frags.length; i++) {
        frags[i] = nodes.get(i).fragment;
      }
      topics.add(new Topic(frags));
    }
    return topics;
  }
  
  public final List<Interest> getExactInterests() {
    final Set<Interest> interests = new LinkedHashSet<>();
    for (List<TopicGenNode> nodes : combs) {
      for (int i = 0; i < nodes.size(); i++) {
        if (nodes.get(i).spec.exacts > 0) {
          final String[] frags = new String[i + 1];
          for (int j = 0; j < frags.length; j++) {
            frags[j] = nodes.get(j).fragment;
          }
          interests.add(new Interest(new Topic(frags), nodes.get(i).spec.exacts));
        }
      }
    }
    return new ArrayList<>(interests);
  }
  
  public final List<Interest> getSingleLevelWildcardInterests() {
    final Set<Interest> interests = new LinkedHashSet<>();
    for (List<TopicGenNode> nodes : combs) {
      for (int i = 0; i < nodes.size(); i++) {
        if (nodes.get(i).spec.slWildcards > 0) {
          final String[] frags = new String[i + 1];
          for (int j = 0; j < frags.length; j++) {
            frags[j] = j < i ? nodes.get(j).fragment : Topic.SL_WILDCARD;
          }
          interests.add(new Interest(new Topic(frags), nodes.get(i).spec.slWildcards));
        }
      }
    }
    return new ArrayList<>(interests);
  }
  
  public final List<Interest> getMultiLevelWildcardInterests() {
    final Set<Interest> interests = new LinkedHashSet<>();
    for (List<TopicGenNode> nodes : combs) {
      for (int i = 0; i < nodes.size(); i++) {
        if (nodes.get(i).spec.mlWildcards > 0) {
          final String[] frags = new String[i + 1];
          for (int j = 0; j < frags.length; j++) {
            frags[j] = j < i ? nodes.get(j).fragment : Topic.ML_WILDCARD;
          }
          interests.add(new Interest(new Topic(frags), nodes.get(i).spec.mlWildcards));
        }
      }
    }
    return new ArrayList<>(interests);
  }
  
  public List<Interest> getAllInterests() {
    final List<Interest> interests = new ArrayList<>();
    interests.addAll(getExactInterests());
    interests.addAll(getSingleLevelWildcardInterests());
    interests.addAll(getMultiLevelWildcardInterests());
    return interests;
  }
  
  public static final class TopicGenBuilder {
    private final List<List<TopicGenNode>> matrix = new ArrayList<>();
    
    public TopicGenBuilder add(List<TopicGenNode> nodes) {
      matrix.add(nodes);
      return this;
    }
    
    public TopicGen build() {
      return new TopicGen(matrix);
    }
  }
  
  public static TopicGenBuilder builder() {
    return new TopicGenBuilder();
  }
}
