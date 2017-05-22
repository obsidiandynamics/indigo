package com.obsidiandynamics.indigo.iot;

import com.obsidiandynamics.indigo.topic.*;

public interface SubscriptionVerifier {
  public interface VerificationOutcome {
    void allow();
    
    void deny(Object error);
  }
  
  void verify(Subscribe subscribe, VerificationOutcome outcome);
  
  static void allowAll(Subscribe subscribe, VerificationOutcome outcome) {
    outcome.allow();
  }
}
