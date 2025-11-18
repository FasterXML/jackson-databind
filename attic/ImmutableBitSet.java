package com.fasterxml.jackson.databind.util;

import java.util.BitSet;

public class ImmutableBitSet extends BitSet
{
    private static final long serialVersionUID = 1L;

    private ImmutableBitSet(BitSet bits) {
        super();
        _parentOr(bits);
    }

    public static ImmutableBitSet of(BitSet bits) {
        return new ImmutableBitSet(bits);
    }

    private void _parentOr(BitSet set) {
        super.or(set);
    }
    
    @Override
    public void and(BitSet set) {
        _failMutableOperation();
    }

    @Override
    public void andNot(BitSet set) {
        _failMutableOperation();
    }

    @Override
    public void or(BitSet set) {
        _failMutableOperation();
    }

    @Override
    public void xor(BitSet set) {
        _failMutableOperation();
    }

    @Override
    public void clear() {
        _failMutableOperation();
    }

    @Override
    public void clear(int ix) {
        _failMutableOperation();
    }

    @Override
    public void clear(int from, int to) {
        _failMutableOperation();
    }

    @Override
    public void flip(int bitIndex) {
        _failMutableOperation();
    }

    @Override
    public void flip(int from, int to) {
        _failMutableOperation();
    }

    @Override
    public void set(int bitIndex) {
        _failMutableOperation();
    }

    @Override
    public void set(int bitIndex, boolean state) {
        _failMutableOperation();
    }

    @Override
    public void set(int from, int to) {
        _failMutableOperation();
    }

    private void _failMutableOperation() {
        throw new UnsupportedOperationException("ImmutableBitSet does not support modification");
    }
}
