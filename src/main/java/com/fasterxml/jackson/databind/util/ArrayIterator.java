package com.fasterxml.jackson.databind.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Iterator implementation used to efficiently expose contents of an
 * Array as read-only iterator.
 */
@SuppressWarnings("IterableAndIterator") // only used internally, not exposed via API
public class ArrayIterator<T> implements Iterator<T>, Iterable<T> // lgtm [java/iterator-implements-iterable]
{
    private final T[] _a;

    private int _index;

    public ArrayIterator(T[] a) {
        _a = a;
        _index = 0;
    }

    @Override
    public boolean hasNext() { return _index < _a.length; }

    @Override
    public T next() {
        if (_index >= _a.length) {
            throw new NoSuchElementException();
        }
        return _a[_index++];
    }

    @Override public void remove() { throw new UnsupportedOperationException(); }
    @Override public Iterator<T> iterator() { return this; }
}