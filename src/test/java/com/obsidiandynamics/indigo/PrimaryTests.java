package com.obsidiandynamics.indigo;

import org.junit.runner.*;
import org.junit.runners.Suite.*;

import com.obsidiandynamics.indigo.CycleSuite.*;
import com.obsidiandynamics.indigo.benchmark.*;

@RunWith(CycleSuite.class)
@SuiteClasses({
  DLQTest.class,
  DrainTest.class,
    EchoBenchmark.class,
    EgressTest.class,
    ExceptionHandlerTest.class,
    ExternalAskTest.class,
    FaultTest.class,
    ParallelConsistencyTest.class,
    RequestResponseBenchmark.class,
    RequestResponseTest.class,
    StashTest.class,
    StatelessChainTest.class,
    StatelessLifeCycleTest.class,
    StatefulLifeCycleTest.class,
    ThrottleTest.class,
    ThroughputBenchmark.class,
    TimeoutTest.class
})     
@ParameterMatrix(
  keys={ActorSystemConfig.Key.EXECUTOR,ActorConfig.Key.ACTIVATION_FACTORY},
  values={@ParameterValues({"FIXED_THREAD_POOL","SYNC_QUEUE"}),
          @ParameterValues({"FIXED_THREAD_POOL","NODE_QUEUE"}),
          @ParameterValues({"FORK_JOIN_POOL","SYNC_QUEUE"}),
          @ParameterValues({"FORK_JOIN_POOL","NODE_QUEUE"})
})
public class PrimaryTests {}
