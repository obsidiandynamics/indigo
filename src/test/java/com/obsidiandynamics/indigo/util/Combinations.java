package com.obsidiandynamics.indigo.util;

import java.util.*;

/**
 *  Generates all column combinations of a given jagged 2D array; that is, the complete set of vectors
 *  resulting when each element in every vector is taken in combination with elements of all
 *  other vectors.<p>
 *  
 *  Example: the 2D array <br>
 *  [<br>
 *  &nbsp;&nbsp;[a,b,c]<br>
 *  &nbsp;&nbsp;[d,e]<br>
 *  ]<br>
 *  will yield combination vectors<br><br>
 *  [a,d],<br>
 *  [a,e],<br>
 *  [b,d],<br>
 *  [b,e],<br>
 *  [c,d],<br>
 *  [c,e]<br>
 */
public final class Combinations<E> implements Iterable<List<E>> {
  private final E[][] vectors;
  private final Indexer indexer;
  
  public Combinations(List<List<E>> listOfLists) {
    this(toMatrix(listOfLists));
  }
  
  public Combinations(E[][] vectors) {
    this.vectors = vectors;
    indexer = new Indexer(getDimensions());
  }
  
  public int size() {
    return indexer.size();
  }  

  public int[] getDimensions() {
    final int[] dimensions = new int[vectors.length];
    for (int i = 0; i < vectors.length; i++) {
      dimensions[i] = vectors[i].length;
    }
    return dimensions;
  }

  public List<E> get(int[] indices) {
    @SuppressWarnings("unchecked")
    final E[] combination = (E[]) new Object[vectors.length];
    for (int i = 0; i < indices.length; i++) {
      combination[i] = vectors[i][indices[i]];
    }
    return Arrays.asList(combination);
  }

  @SuppressWarnings("unchecked")
  private static <E> E[][] toMatrix(List<List<E>> listOfLists) {
    final E[][] vectors = (E[][]) new Object[listOfLists.size()][];
    for (int i = 0; i < vectors.length; i++) {
      vectors[i] = (E[]) listOfLists.get(i).toArray();
    }
    return vectors;
  }

  @Override
  public Iterator<List<E>> iterator() {
    return new Iterator<List<E>>() {
      private final Iterator<int[]> indexIterator = indexer.iterator();
      
      @Override public boolean hasNext() {
        return indexIterator.hasNext();
      }

      @Override public List<E> next() {
        return get(indexIterator.next());
      }
    };
  }
}
