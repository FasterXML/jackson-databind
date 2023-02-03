package com.fasterxml.jackson.databind.util;

import java.lang.reflect.Array;
import java.util.List;

/**
 * Helper class to use for constructing Object arrays by appending entries
 * to create arrays of various lengths (length that is not known a priori).
 */
public final class ObjectBuffer
{
    // // // Config constants

    /**
     * Also: let's expand by doubling up until 64k chunks (which is 16k entries for
     * 32-bit machines)
     */
    private final static int SMALL_CHUNK = (1 << 14);

    /**
     * Let's limit maximum size of chunks we use; helps avoid excessive allocation
     * overhead for huge data sets.
     * For now, let's limit to quarter million entries, 1 meg chunks for 32-bit
     * machines.
     */
    private final static int MAX_CHUNK = (1 << 18);

    // // // Data storage

    private LinkedNode<Object[]> _head;

    private LinkedNode<Object[]> _tail;

    /**
     * Number of total buffered entries in this buffer, counting all instances
     * within linked list formed by following {@link #_head}.
     */
    private int _size;

    // // // Simple reuse

    /**
     * Reusable Object array, stored here after buffer has been released having
     * been used previously.
     */
    private Object[] _freeBuffer;

    /*
    /**********************************************************
    /* Construction
    /**********************************************************
     */

    public ObjectBuffer() { }

    /*
    /**********************************************************
    /* Public API
    /**********************************************************
     */

    /**
     * Method called to start buffering process. Will ensure that the buffer
     * is empty, and then return an object array to start chunking content on
     */
    public Object[] resetAndStart()
    {
        _reset();
        if (_freeBuffer == null) {
            return (_freeBuffer = new Object[12]);
        }
        return _freeBuffer;
    }

    /**
     * @since 2.9
     */
    public Object[] resetAndStart(Object[] base, int count)
    {
        _reset();
        if ((_freeBuffer == null) || (_freeBuffer.length < count)) {
            _freeBuffer = new Object[Math.max(12, count)];
        }
        System.arraycopy(base, 0, _freeBuffer, 0, count);
        return _freeBuffer;
    }

    /**
     * Method called to add a full Object array as a chunk buffered within
     * this buffer, and to obtain a new array to fill. Caller is not to use
     * the array it gives; but to use the returned array for continued
     * buffering.
     *
     * @param fullChunk Completed chunk that the caller is requesting
     *   to append to this buffer. It is generally chunk that was
     *   returned by an earlier call to {@link #resetAndStart} or
     *   {@link #appendCompletedChunk} (although this is not required or
     *   enforced)
     *
     * @return New chunk buffer for caller to fill
     */
    public Object[] appendCompletedChunk(Object[] fullChunk)
    {
        LinkedNode<Object[]> next = new LinkedNode<Object[]>(fullChunk, null);
        if (_head == null) { // first chunk
            _head = _tail = next;
        } else { // have something already
            _tail.linkNext(next);
            _tail = next;
        }
        int len = fullChunk.length;
        _size += len;
        // double the size for small chunks
        if (len < SMALL_CHUNK) {
            len += len;
        } else if (len < MAX_CHUNK) { // but by +25% for larger (to limit overhead)
            len += (len >> 2);
        }
        return new Object[len];
    }

    /**
     * Method called to indicate that the buffering process is now
     * complete; and to construct a combined exactly-sized result
     * array. Additionally the buffer itself will be reset to
     * reduce memory retention.
     *<p>
     * Resulting array will be of generic <code>Object[]</code> type:
     * if a typed array is needed, use the method with additional
     * type argument.
     */
    public Object[] completeAndClearBuffer(Object[] lastChunk, int lastChunkEntries)
    {
        int totalSize = lastChunkEntries + _size;
        Object[] result = new Object[totalSize];
        _copyTo(result, totalSize, lastChunk, lastChunkEntries);
        _reset();
        return result;
    }

    /**
     * Type-safe alternative to
     * {@link #completeAndClearBuffer(Object[], int)}, to allow
     * for constructing explicitly typed result array.
     *
     * @param componentType Type of elements included in the buffer. Will be
     *   used for constructing the result array.
     */
    public <T> T[] completeAndClearBuffer(Object[] lastChunk, int lastChunkEntries, Class<T> componentType)
    {
       int totalSize = lastChunkEntries + _size;
 	   @SuppressWarnings("unchecked")
        T[] result = (T[]) Array.newInstance(componentType, totalSize);
        _copyTo(result, totalSize, lastChunk, lastChunkEntries);
        _reset();
        return result;
    }

    public void completeAndClearBuffer(Object[] lastChunk, int lastChunkEntries, List<Object> resultList)
    {
        for (LinkedNode<Object[]> n = _head; n != null; n = n.next()) {
            Object[] curr = n.value();
            for (int i = 0, len = curr.length; i < len; ++i) {
                resultList.add(curr[i]);
            }
        }
        // and then the last one
        for (int i = 0; i < lastChunkEntries; ++i) {
            resultList.add(lastChunk[i]);
        }
        _reset();
    }

    /**
     * Helper method that can be used to check how much free capacity
     * will this instance start with. Can be used to choose the best
     * instance to reuse, based on size of reusable object chunk
     * buffer holds reference to.
     */
    public int initialCapacity() {
        return (_freeBuffer == null) ? 0 : _freeBuffer.length;
    }

    /**
     * Method that can be used to check how many Objects have been buffered
     * within this buffer.
     */
    public int bufferedSize() { return _size; }

    /*
    /**********************************************************
    /* Internal methods
    /**********************************************************
     */

    protected void _reset()
    {
        // can we reuse the last (and thereby biggest) array for next time?
        if (_tail != null) {
            _freeBuffer = _tail.value();
        }
        // either way, must discard current contents
        _head = _tail = null;
        _size = 0;
    }

    protected final void _copyTo(Object resultArray, int totalSize,
            Object[] lastChunk, int lastChunkEntries)
    {
        int ptr = 0;

        for (LinkedNode<Object[]> n = _head; n != null; n = n.next()) {
            Object[] curr = n.value();
            int len = curr.length;
            System.arraycopy(curr, 0, resultArray, ptr, len);
            ptr += len;
        }
        System.arraycopy(lastChunk, 0, resultArray, ptr, lastChunkEntries);
        ptr += lastChunkEntries;

        // sanity check (could have failed earlier due to out-of-bounds, too)
        if (ptr != totalSize) {
            throw new IllegalStateException("Should have gotten "+totalSize+" entries, got "+ptr);
        }
    }
}
