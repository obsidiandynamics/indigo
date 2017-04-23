package com.obsidiandynamics.indigo;

import org.junit.runner.*;
import org.junit.runners.Suite.*;

import com.obsidiandynamics.indigo.CycleSuite.*;

@RunWith(CycleSuite.class)
@SuiteClasses(PrimaryTests.class)     
@ParameterMatrix(
  keys={ActorSystemConfig.Key.EXECUTOR,ActorConfig.Key.ACTIVATION_FACTORY},
  values={@ParameterValues({"FIXED_THREAD_POOL","SYNC_QUEUE"}),
          @ParameterValues({"FIXED_THREAD_POOL","NODE_QUEUE"}),
          @ParameterValues({"AUTO","SYNC_QUEUE"}),
          @ParameterValues({"FORK_JOIN_POOL","NODE_QUEUE"})
})
public class CycledPrimaryTests {}
