package com.obsidiandynamics.indigo.marketstrategy;

import static org.junit.Assert.*;

import org.junit.*;

public final class StochasticOscillatorTest {
  private static final double DELTA = .001;
  
  @Test
  public void test() {
    final StochasticOscillator fast = new StochasticOscillator(14, 3); // fast stochastic

    assertNull(fast.add(bar(127.0090f, 125.3574f, 127.0090f)));
    assertNull(fast.add(bar(127.6159f, 126.1633f, 127.6159f)));
    assertNull(fast.add(bar(126.5911f, 124.9296f, 126.5911f)));
    assertNull(fast.add(bar(127.3472f, 126.0937f, 127.3472f)));
    assertNull(fast.add(bar(128.1730f, 126.8199f, 128.1730f)));
    assertNull(fast.add(bar(128.4317f, 126.4817f, 128.4317f)));
    assertNull(fast.add(bar(127.3671f, 126.0340f, 127.3671f)));
    assertNull(fast.add(bar(126.4220f, 124.8301f, 126.4220f)));
    assertNull(fast.add(bar(126.8995f, 126.3921f, 126.8995f)));
    assertNull(fast.add(bar(126.8498f, 125.7156f, 126.8498f)));
    assertNull(fast.add(bar(125.6460f, 124.5615f, 125.6460f)));
    assertNull(fast.add(bar(125.7156f, 124.5715f, 125.7156f)));
    assertNull(fast.add(bar(127.1582f, 125.0689f, 127.1582f)));
    assertNull(fast.add(bar(127.7154f, 126.8597f, 127.2876f)));
    assertNull(fast.add(bar(127.6855f, 126.6309f, 127.1781f))); 
    
    final StochasticOutput o1 = fast.add(bar(128.2228f, 126.8001f, 128.0138f));
    assertEquals(89.2021f, o1.getK(), DELTA);
    assertEquals(75.7497f, o1.getD(), DELTA);
    
    final StochasticOutput o2 = fast.add(bar(128.2725f, 126.7105f, 127.1085f));
    assertEquals(65.8106f, o2.getK(), DELTA);
    assertEquals(74.2072f, o2.getD(), DELTA);
  }
  
  private static Bar bar(float high, float low, float close) {
    return new Bar("TEST", 0, high, low, close);
  }
}
