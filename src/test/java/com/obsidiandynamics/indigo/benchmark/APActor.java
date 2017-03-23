package com.obsidiandynamics.indigo.benchmark;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class APActor { // Visibility is achieved by volatile-piggybacking of reads+writes to "on"
  public static interface Func<I, O> {
    O apply(I in);
  }
  
  public static interface Effect extends Func<Behavior, Behavior> { }; // An Effect returns a Behavior given a Behavior
  public static interface Behavior extends Func<Object, Effect> { }; // A Behavior is a message (Object) which returns the behavior for the next message

  public static interface Address { 
    Address tell(Object msg);
    
    long getBacklog();
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
      
//      private Behavior behavior = new Behavior() { 
//        @Override public Effect apply(Object m) { 
//          return (m instanceof Address) ? become(initial.apply((Address) m)) : stay; 
//        } 
//      };
      
      private final Behavior behavior = initial.apply(null);

      @Override public final Address tell(Object m) {
        //backlog.increment();
        final Node t = new Node(m);
        final Node t1 = getAndSet(t);
        
        synchronized (this) {
          if (t1 == ANCHOR) {
            async(t, true);
          } else {
            t1.lazySet(t);
          }
        }
        return this; 
      }
      
      private void async(Node n, boolean x) {
        //final AtomicAddress addr = this;
        e.execute(new ForkJoinTask<Void>() {
          private static final long serialVersionUID = 1L;
          @Override public Void getRawResult() { return null; }
          @Override protected void setRawResult(Void value) {}
          @Override protected boolean exec() {
            if (x) {
              act(n, false);
            } else if (/*addr.get() != n ||*/ ! cas(n)) {
              //actOrAsync(n); //<
              act(n, true);
            }
            return false;
          }
        });
      }
      
      private boolean cas(Node n) {
        synchronized (this) {
          return compareAndSet(n, ANCHOR);
        }
      }
      
      /*private void actOrAsync(Node h) {
        int attempts = 0;
        
        while (true) {
          final Node h1 = h.get();
          if (h1 != null) {
            act(h1, false);
            return;
          } else if (attempts != 9) {
            attempts++;
          } else {
            Thread.yield(); //<
            //async(h, false); //<
            if (!compareAndSet(h, ANCHOR)) {
              async(h, false);
            }
            return; //<
          }
        }
      }*/

      private void act(Node h, boolean skipCurrent) {
        //int cycles = 0;
        int remaining = batch;
        if (! skipCurrent) behavior.apply(h.m);
        
        int spins = 0;
        while (true) {
          //behavior = behavior.apply(h.m).apply(behavior);
          //behavior.apply(h.m); //<
          //cycles++;
          
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
        //backlog.add(-cycles);
      }
      
      private final LongAdder backlog = new LongAdder();

      @Override
      public long getBacklog() {
        return backlog.sum();
      }
    };
    //return a.tell(a); // Make self-aware
    return a;
  }
}
