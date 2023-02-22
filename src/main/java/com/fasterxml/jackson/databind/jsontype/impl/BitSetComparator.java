package com.fasterxml.jackson.databind.jsontype.impl;

import java.util.BitSet;
import java.util.Comparator;

/**
 * Terse but inefficient (for large arrays) ordering of {@link BitSet} as though it
 * were an unsigned integer.
 */
public class BitSetComparator implements Comparator<BitSet> {

  // This could be done much more efficiently with access to the long[]
  @Override
  public int compare(BitSet left, BitSet right) {
    if (left.equals(right)) return 0;
    BitSet diff = (BitSet)left.clone();
    diff.xor(right); // diff = left ^ right
    int firstDifference = diff.length() - 1;

    if (firstDifference == -1) return 0;
    // due to xor, this high-bit is 1 in either left OR right
    return right.get(firstDifference) ? 1 : -1;
  }

}
