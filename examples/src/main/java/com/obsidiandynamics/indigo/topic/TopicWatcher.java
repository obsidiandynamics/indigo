package com.obsidiandynamics.indigo.topic;

import com.obsidiandynamics.indigo.*;

public interface TopicWatcher {
  static TopicWatcher VOID = new TopicWatcher() {
    @Override public void created(Activation a, Topic topic) {}
    @Override public void deleted(Activation a, Topic topic) {}
    @Override public void subscribed(Activation a, Topic topic, Subscriber subscriber) {}
    @Override public void unsubscribed(Activation a, Topic topic, Subscriber subscriber) {}
  };
  
  void created(Activation a, Topic topic);
  
  void deleted(Activation a, Topic topic);
  
  void subscribed(Activation a, Topic topic, Subscriber subscriber);
  
  void unsubscribed(Activation a, Topic topic, Subscriber subscriber);
}
