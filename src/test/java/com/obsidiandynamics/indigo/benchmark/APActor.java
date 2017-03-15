package com.obsidiandynamics.indigo.benchmark;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

public class APActor { // Visibility is achieved by volatile-piggybacking of reads+writes to "on"
  public static interface Effect extends Function<Behavior, Behavior> { }; // An Effect returns a Behavior given a Behavior
  public static interface Behavior extends Function<Object, Effect> { }; // A Behavior is a message (Object) which returns the behavior for the next message

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

  //static abstract class AtomicAddress extends AtomicReference<Node> implements Address {}
  
  public static Address create(final Function<Address, Behavior> initial, final Executor e, int batch) {
    final Address a = new Address() {
      private final AtomicReference<Node> tail = new AtomicReference<>(ANCHOR);
      
      private Behavior behavior = new Behavior() { 
        @Override public Effect apply(Object m) { 
          return (m instanceof Address) ? become(initial.apply((Address)m)) : stay; 
        } 
      };

      @Override public final Address tell(Object m) {
        //System.out.println("Sent " + m);
        final Node t = new Node(m);
        final Node t1 = tail.getAndSet(t);
        
        if (t1 == ANCHOR) {
          async(t);
        } else {
          t1.lazySet(t);
        }
        return this; 
      }
      
      //volatile boolean acting;

      private void act(Node h) {
        int remaining = batch;
        batch: while (true) {
//          if (acting) throw new IllegalStateException();
//          acting = true;
          
          behavior = behavior.apply(h.m).apply(behavior);

//          if (! acting) throw new IllegalStateException();
//          acting = false;
          
          parking: while (true) {
            final Node h1 = h.get();
            
            if (h1 != null) {
              if (remaining-- > 0) {
                h = h1;
                continue batch;
              } else {
                async(h1);
                break batch;
              }
            } else {
              if (tail.compareAndSet(h, ANCHOR)) {
                //System.out.format("%x parked\n", System.identityHashCode(this));
                break batch;
              } else {
//                System.out.println("parking..");
                Thread.yield();
//                b++;
                continue parking;
              }
            }
          }
        }
      }

      private void async(Node n) {
        e.execute(() -> {
          act(n);
        });
      }
    };
    return a.tell(a); // Make self-aware
  }
}
