package com.obsidiandynamics.indigo.util;

import static org.junit.Assert.*;

import java.util.*;

import org.junit.*;

public final class CombinationsTest {
  @Test
  public void test() {
    final Character[][] matrix = {
      { 'a', 'b', 'c'},
      { 'd', 'e' },
      { 'f', 'g', 'h', 'i'},
      { 'j' },
      { 'k' }
    };
    
    final Combinations<Character> combinator = new Combinations<>(matrix);
    assertEquals(24, combinator.size());
    
    final char[][] combinations = new char[combinator.size()][];
    int i = 0;
    for (List<Character> combination : combinator) {
      final char[] chars = new char[combination.size()];
      for (int j = 0; j < chars.length; j++) {
        chars[j] = combination.get(j);
      }
      combinations[i++] = chars;
    }
    i = 0;
    assertArrayEquals(combinations[i++], new char[]{'a', 'd', 'f', 'j', 'k'});
    assertArrayEquals(combinations[i++], new char[]{'a', 'd', 'g', 'j', 'k'});
    assertArrayEquals(combinations[i++], new char[]{'a', 'd', 'h', 'j', 'k'});
    assertArrayEquals(combinations[i++], new char[]{'a', 'd', 'i', 'j', 'k'});
    assertArrayEquals(combinations[i++], new char[]{'a', 'e', 'f', 'j', 'k'});
    assertArrayEquals(combinations[i++], new char[]{'a', 'e', 'g', 'j', 'k'});
    assertArrayEquals(combinations[i++], new char[]{'a', 'e', 'h', 'j', 'k'});
    assertArrayEquals(combinations[i++], new char[]{'a', 'e', 'i', 'j', 'k'});
    assertArrayEquals(combinations[i++], new char[]{'b', 'd', 'f', 'j', 'k'});
    assertArrayEquals(combinations[i++], new char[]{'b', 'd', 'g', 'j', 'k'});
    assertArrayEquals(combinations[i++], new char[]{'b', 'd', 'h', 'j', 'k'});
    assertArrayEquals(combinations[i++], new char[]{'b', 'd', 'i', 'j', 'k'});
    assertArrayEquals(combinations[i++], new char[]{'b', 'e', 'f', 'j', 'k'});
    assertArrayEquals(combinations[i++], new char[]{'b', 'e', 'g', 'j', 'k'});
    assertArrayEquals(combinations[i++], new char[]{'b', 'e', 'h', 'j', 'k'});
    assertArrayEquals(combinations[i++], new char[]{'b', 'e', 'i', 'j', 'k'});
    assertArrayEquals(combinations[i++], new char[]{'c', 'd', 'f', 'j', 'k'});
    assertArrayEquals(combinations[i++], new char[]{'c', 'd', 'g', 'j', 'k'});
    assertArrayEquals(combinations[i++], new char[]{'c', 'd', 'h', 'j', 'k'});
    assertArrayEquals(combinations[i++], new char[]{'c', 'd', 'i', 'j', 'k'});
    assertArrayEquals(combinations[i++], new char[]{'c', 'e', 'f', 'j', 'k'});
    assertArrayEquals(combinations[i++], new char[]{'c', 'e', 'g', 'j', 'k'});
    assertArrayEquals(combinations[i++], new char[]{'c', 'e', 'h', 'j', 'k'});
    assertArrayEquals(combinations[i++], new char[]{'c', 'e', 'i', 'j', 'k'});
  }
}
