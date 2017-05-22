package com.obsidiandynamics.indigo.topic;

import java.util.*;

public final class Topic {
  private static final String SEPARATOR = "/";
  
  private final String[] parts;
  
  private Topic(String[] parts) {
    this.parts = parts;
  }
  
  String[] getParts() {
    return parts;
  }
  
  int length() {
    return parts.length;
  }
  
  public static Topic of(String topic) {
    final String trimmedTopic = topic.trim();
    return trimmedTopic.isEmpty() ? Topic.root() : new Topic(trimmedTopic.split(SEPARATOR));
  }
  
  public static Topic root() {
    return new Topic(new String[0]);
  }
  
  public Topic subtopic(int startIncl, int endExcl) {
    final String[] newParts = new String[endExcl - startIncl];
    System.arraycopy(parts, startIncl, newParts, 0, newParts.length);
    return new Topic(newParts);
  }
  
  public Topic append(String part) {
    final String[] newParts = new String[parts.length + 1];
    System.arraycopy(parts, 0, newParts, 0, parts.length);
    newParts[parts.length] = part;
    return new Topic(newParts);
  }
  
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + Arrays.hashCode(parts);
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
    Topic other = (Topic) obj;
    if (!Arrays.equals(parts, other.parts))
      return false;
    return true;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    for (int i = 0; i < parts.length; i++) {
      sb.append(parts[i]);
      if (i != parts.length - 1) sb.append(SEPARATOR);
    }
    return sb.toString();
  }
}
