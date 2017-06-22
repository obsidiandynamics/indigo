package com.obsidiandynamics.indigo.util;

import java.util.*;

final class Indexer implements Iterable<int[]> {
  /** The dimensions of the hypercube. */
  private final int[] dimensions;
  
  Indexer(int[] dimensions) {
    this.dimensions = dimensions;
  }

  /**
   *  Computes the hypercube indices for the given hypervector offset, based
   *  on the dimensions of this hypervector.<br>
   *  This is a more efficient variation, allowing the caller to specify the array
   *  to write the indices to.
   *  
   *  @param offset The hypervector offset.
   *  @param indices The indices array to mutate.
   *  @return The corresponding hypercube indices.
   */
  private int[] computeIndices(int offset, int[] indices) {
    int left = offset;
    for (int caret = indices.length; --caret >= 0; ) {
      final int quotient = left / dimensions[caret];
      final int remainder = left % dimensions[caret];
      indices[caret] = remainder;
      left = quotient;
    }
    return indices;
  }
  
  int size() {
    int product = 1;
    for (int d : dimensions) {
      product *= d;
    }
    return product;
  }

  @Override
  public Iterator<int[]> iterator() {
    return new Iterator<int[]>() {
      private final int length = size();
      private final int[] indices = new int[dimensions.length];
      private int offset = 0;
      
      @Override public boolean hasNext() {
        return offset < length;
      }

      @Override public int[] next() {
        return computeIndices(offset++, indices);
      }
    };
  }
}
