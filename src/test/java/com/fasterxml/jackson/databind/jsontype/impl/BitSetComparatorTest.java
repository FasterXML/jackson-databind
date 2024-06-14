package com.fasterxml.jackson.databind.jsontype.impl;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class BitSetComparatorTest {

  private final TreeSet<BitSet> tree = new TreeSet<>(new BitSetComparator());

  private Map<BitSet, String> dictionary = new HashMap<>();

  @Before
  public void setUp() {
    dictionary.put(bitset("11111111_11111111"), "a");
    dictionary.put(bitset("00111111_11111111"), "b");
    dictionary.put(bitset("00111111_00111111"), "c");
    dictionary.put(bitset("00000001_00111111"), "d");
    dictionary.put(bitset("00000000_11111111"), "e");
    dictionary.put(bitset("11111110"), "f");
    dictionary.put(bitset("00011110"), "g");
    dictionary.put(bitset("1"), "h");
    dictionary.put(bitset(""), "i");
  }

  @Test
  public void sortsInputs() {
    // Given:
    List<BitSet> inputs = new ArrayList<>(dictionary.keySet());
    Collections.shuffle(inputs, new Random(1966));
    // When:
    tree.addAll(inputs);
    // Then:
    String output = tree.stream().map(dictionary::get).collect(Collectors.joining());
    assertEquals("abcdefghi", output);
  }

  private static BitSet bitset(String content) {
    BitSet bitset = new BitSet(content.length());
    int bit = 0;
    char[] chars = content.toCharArray();

    for (int i = chars.length-1; i >= 0; i--) {
      if (chars[i] >= '0') {
        bitset.set(bit++, chars[i] != '0');
      }
    }
    return bitset;
  }

}
