package tools.jackson.databind.util;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple identity value class that may be used as Serializable key
 * for entries that need to retain identity of some kind, but where
 * actual appearance of id itself does not matter.
 * Instances NEVER equal each other, only themselves, even if generated
 * ids might be same (although they should not be).
 *
 * @since 3.0
 */
public class UniqueId
    implements java.io.Serializable, Comparable<UniqueId>
{
    private static final long serialVersionUID = 3L;

    // Start with 0x1000 for funsies
    private static final AtomicInteger ID_SEQ = new AtomicInteger(4096);

    protected final String _id;

    protected UniqueId(String prefix) {
        String id = Integer.toHexString(ID_SEQ.getAndIncrement());
        _id = (prefix == null) ? id : (prefix + id);
    }

    public static UniqueId create() {
        return new UniqueId(null);
    }

    public static UniqueId create(String prefix) {
        return new UniqueId(prefix);
    }

    @Override
    public String toString() {
        return _id;
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
