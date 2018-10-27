package com.obsidiandynamics.indigo.linear;

import java.util.concurrent.*;
import java.util.function.*;

/**
 *  Options for configuring a new {@link LinearExecutor} instance.
 */
public final class ExecutorOptions {
  private static final int PROCESSOR_COUNT = Runtime.getRuntime().availableProcessors();
  
  private int parallelism = PROCESSOR_COUNT;
  
  private Function<Integer, ExecutorService> executorFactory = Executors::newWorkStealingPool;
  
  private int actorBias = 1;
  
  private AgentListener agentListener = NopAgentListener.getInstance();
  
  private boolean allowNonLinearTasks = true;

  public int getParallelism() {
    return parallelism;
  }

  public void setParallelism(int parallelism) {
    this.parallelism = parallelism;
  }

  public ExecutorOptions withParallelism(int parallelism) {
    setParallelism(parallelism);
    return this;
  }

  public Function<Integer, ExecutorService> getExecutorFactory() {
    return executorFactory;
  }

  public void setExecutorFactory(Function<Integer, ExecutorService> executorFactory) {
    this.executorFactory = executorFactory;
  }
  
  public ExecutorOptions withExecutorFactory(Function<Integer, ExecutorService> executorFactory) {
    setExecutorFactory(executorFactory);
    return this;
  }

  public int getActorBias() {
    return actorBias;
  }

  public void setActorBias(int actorBias) {
    this.actorBias = actorBias;
  }
  
  public ExecutorOptions withActorBias(int actorBias) {
    setActorBias(actorBias);
    return this;
  }
  
  public AgentListener getAgentListener() {
    return agentListener;
  }

  public void setAgentListener(AgentListener agentListener) {
    this.agentListener = agentListener;
  }

  public ExecutorOptions withAgentListener(AgentListener agentListener) {
    setAgentListener(agentListener);
    return this;
  }
  
  public boolean isAllowNonLinearTasks() {
    return allowNonLinearTasks;
  }

  public void setAllowNonLinearTasks(boolean allowNonLinearTasks) {
    this.allowNonLinearTasks = allowNonLinearTasks;
  }
  
  public ExecutorOptions withAllowNonLinearTasks(boolean allowNonLinearTasks) {
    setAllowNonLinearTasks(allowNonLinearTasks);
    return this;
  }

  @Override
  public String toString() {
    return ExecutorOptions.class.getSimpleName() + " [parallelism=" + parallelism + ", executorFactory=" + executorFactory + 
        ", actorBias="  + actorBias + ", agentListener=" + agentListener + ", allowNonLinearTasks=" + allowNonLinearTasks + "]";
  }
}
