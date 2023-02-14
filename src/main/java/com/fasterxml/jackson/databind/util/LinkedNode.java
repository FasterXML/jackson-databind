package com.fasterxml.jackson.databind.util;

/**
 * Node of a forward-only linked list.
 *
 * @author tatu
 */
public final class LinkedNode<T>
{
    private final T value;

    private LinkedNode<T> next;

    public LinkedNode(T value, LinkedNode<T> next)
    {
        this.value = value;
        this.next = next;
    }

    public void linkNext(LinkedNode<T> n)
    {
        if (next != null) { // sanity check
            throw new IllegalStateException();
        }
        next = n;
    }

    public LinkedNode<T> next() { return next; }

    public T value() { return value; }

    /**
     * Convenience method that can be used to check if a linked list
     * with given head node (which may be null to indicate empty list)
     * contains given value
     *
     * @param <ST> Type argument that defines contents of the linked list parameter
     * @param node Head node of the linked list
     * @param value Value to look for
     * @return True if linked list contains the value, false otherwise
     */
    public static <ST> boolean contains(LinkedNode<ST> node, ST value)
    {
        while (node != null) {
            if (node.value() == value) {
                return true;
            }
            node = node.next();
        }
        return false;
    }
}
