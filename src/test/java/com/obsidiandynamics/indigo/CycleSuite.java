package com.obsidiandynamics.indigo;

import java.util.*;

import org.junit.runner.*;
import org.junit.runner.notification.*;
import org.junit.runners.*;
import org.junit.runners.model.*;

public final class CycleSuite extends Suite {
  public CycleSuite(Class<?> klass, RunnerBuilder builder) throws InitializationError {
    super(klass, builder);
  }

  @Override
  protected List<Runner> getChildren() {
    final List<Runner> runners = new ArrayList<>();
    runners.addAll(super.getChildren());
    runners.addAll(super.getChildren());
    for (Runner runner : runners) {
      System.out.println(runner.getDescription());
    }
    return runners;
  }

  @Override
  protected void runChild(Runner runner, final RunNotifier notifier) {
    runner.run(notifier);
//    runner.run(notifier);
  }
}
