/*
Copyright 2012-2017 Viktor Klang
   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at
       http://www.apache.org/licenses/LICENSE-2.0
   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package com.obsidiandynamics.indigo.benchmark;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 *  Taken from https://github.com/plokhotnyuk/actors (initial work by Viktor Klang, performance
 *  enhancements by Andriy Plokhotnyuk) for comparative benchmarking.
 */
public class APActor { // Visibility is achieved by volatile-piggybacking of reads+writes to "on"
  public static interface Func<I, O> {
    O apply(I in);
  }
  
  public static interface Effect extends Func<Behavior, Behavior> { }; // An Effect returns a Behavior given a Behavior
  public static interface Behavior extends Func<Object, Effect> { }; // A Behavior is a message (Object) which returns the behavior for the next message

  public static interface Address { 
    Address tell(Object msg);
  }; // An Address is somewhere you can send messages

  public final static Effect become(final Behavior behavior) { 
    return new Effect() { 
      @Override public Behavior apply(Behavior old) { 
        return behavior; 
      } 
    }; 
  } // Become is an Effect that returns a captured Behavior no matter what the old Behavior is

  public final static Effect stay = new Effect() { 
    @Override public Behavior apply(Behavior old) { 
      return old; 
    } 
  }; // Stay is an Effect that returns the old Behavior when applied.

  public final static Effect die = become(new Behavior() { 
    @Override public Effect apply(Object msg) { 
      return stay; 
    } 
  }); // Die is an Effect which replaces the old Behavior with a new one which does nothing, forever.
  
  private static class Node extends AtomicReference<Node> {
    private static final long serialVersionUID = 1L;
    
    final Object m;
    
    Node(Object m) { this.m = m; }
    
    @Override public String toString() {
      return String.format("(%s)->%s", m, get());
    }
  }
  
  private static final Node ANCHOR = new Node(null);

  static abstract class AtomicAddress extends AtomicReference<Node> implements Address {
    private static final long serialVersionUID = 1L;
  }
  
  public static Address create(final Func<Address, Behavior> initial, final ForkJoinPool e, int batch) {
    final Address a = new AtomicAddress() {
      private static final long serialVersionUID = 1L;

      {
        set(ANCHOR);
      }
      
      private final Behavior behavior = initial.apply(null);

      @Override public final Address tell(Object m) {
        final Node t = new Node(m);
        final Node t1 = getAndSet(t);
        
        if (t1 == ANCHOR) {
          async(t, true);
        } else {
          t1.lazySet(t);
        }
        return this; 
      }
      
      private void async(Node n, boolean x) {
        //final AtomicAddress addr = this;
        e.execute(() -> {
          if (x) {
            act(n, false);
          } else if (/*addr.get() != n ||*/ ! cas(n)) {
            act(n, true);
          }
        });
      }
      
      private boolean cas(Node n) {
        return compareAndSet(n, ANCHOR);
      }

      private void act(Node h, boolean skipCurrent) {
        int remaining = batch;
        if (! skipCurrent) behavior.apply(h.m);
        
        int spins = 0;
        while (true) {
          final Node h1 = h.get();
          if (h1 != null) {
            if (remaining > 0) {
              h = h1;
              behavior.apply(h.m);
              spins = 0;
              remaining--;
            } else {
              //h.lazySet(null);
              async(h1, true);
              return;
            }
          } else if (spins != 9) {
            spins++;
          } else { // no more elements observed
            Thread.yield();
            //async(h, false); //<
            if (!cas(h)) {
              async(h, false);
            }
            return; //<
          }
        }
      }
    };
    //return a.tell(a); //< Make self-aware
    return a;
  }
}
