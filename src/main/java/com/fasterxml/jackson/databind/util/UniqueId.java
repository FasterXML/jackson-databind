package com.fasterxml.jackson.databind.util;

/**
 * Simple identity value class that may be used as Serializable key
 * for entries that need to retain identity of some kind, but where
 * actual appearance of id itself does not matter.
 *
 * @since 3.0
 */
public class UniqueId
    implements java.io.Serializable, Comparable<UniqueId>
{
    private static final long serialVersionUID = 3L;

    protected final String _id;

    public UniqueId() {
        _id = Long.toHexString(System.identityHashCode(this));
    }

    @Override
    public boolean equals(Object other) {
        return this == other;
    }

    @Override
    public int hashCode() {
        return _id.hashCode();
    }

    @Override
    public int compareTo(UniqueId o) {
        return _id.compareTo(o._id);
    }
}
