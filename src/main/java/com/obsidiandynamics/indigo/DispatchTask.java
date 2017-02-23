package com.obsidiandynamics.indigo;

final class DispatchTask implements Runnable {
  private final Activation activation;
  
  DispatchTask(Activation activation) {
    this.activation = activation;
  }

  @Override
  public void run() {
    activation.run();
  }

}
