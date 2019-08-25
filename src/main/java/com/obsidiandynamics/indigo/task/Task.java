package com.obsidiandynamics.indigo.task;

import java.util.*;

import com.obsidiandynamics.func.*;

public abstract class Task<I extends Comparable<I>> implements Comparable<Task<I>> {
  /** The scheduled execution time, in absolute nanoseconds. See {@link System#nanoTime()}. */
  private final long time;
  
  private final I id;
  
  public Task(long time, I id) {
    this.time = time;
    this.id = id;
  }
  
  protected final long getTime() {
    return time;
  }

  protected final I getId() {
    return id;
  }
  
  protected abstract void execute();

  @Override
  public final int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + Long.hashCode(time);
    result = prime * result + Objects.hashCode(id);
    return result;
  }

  @Override
  public final boolean equals(Object obj) {
    if (this == obj) {
      return true;
    } else if (obj instanceof Task) {
      final Task<?> that = Classes.cast(obj);
      return time == that.time && Objects.equals(id, that.id);
    } else {
      return false;
    }
  }

  @Override
  public String toString() {
    return Task.class.getSimpleName() + " [time=" + time + ", id=" + id + "]";
  }

  @Override
  public final int compareTo(Task<I> o) {
    return byTimeThenId(this, o);
  }

  private static <I extends Comparable<I>> int byTimeThenId(Task<I> t1, Task<I> t2) {
    final int timeComp = Long.compare(t1.time, t2.time);
    return timeComp != 0 ? timeComp : t1.id.compareTo(t2.id);
  }
}