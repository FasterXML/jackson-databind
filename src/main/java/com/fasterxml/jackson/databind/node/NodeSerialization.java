package com.fasterxml.jackson.databind.node;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import com.fasterxml.jackson.core.util.ByteArrayBuilder;

/**
 * Helper value class only used during JDK serialization: contains JSON as `byte[]`
 *
 * @since 2.10
 */
class NodeSerialization implements java.io.Serializable,
    java.io.Externalizable
{
    // To avoid malicious input only allocate up to 100k
    protected final static int LONGEST_EAGER_ALLOC = 100_000;

    private static final long serialVersionUID = 1L;

    public byte[] json;

    public NodeSerialization() { }

    public NodeSerialization(byte[] b) { json = b; }

    protected Object readResolve() {
        try {
            return InternalNodeMapper.bytesToNode(json);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to JDK deserialize `JsonNode` value: "+e.getMessage(), e);
        }
    }

    public static NodeSerialization from(Object o) {
        try {
            return new NodeSerialization(InternalNodeMapper.valueToBytes(o));
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to JDK serialize `"+o.getClass().getSimpleName()+"` value: "+e.getMessage(), e);
        }
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(json.length);
        out.write(json);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException {
        final int len = in.readInt();
        json = _read(in, len);
    }

    private byte[] _read(ObjectInput in, int expLen) throws IOException {
        // Common case, just read directly
        if (expLen <= LONGEST_EAGER_ALLOC) {
            byte[] result = new byte[expLen];
            in.readFully(result, 0, expLen);
            return result;
        }
        // but longer content needs more care to avoid DoS by maliciously crafted data
        // (this wrt [databind#3328]
        try (final ByteArrayBuilder bb = new ByteArrayBuilder(LONGEST_EAGER_ALLOC)) {
            byte[] buffer = bb.resetAndGetFirstSegment();
            int outOffset = 0;
            while (true) {
                int toRead = Math.min(buffer.length - outOffset, expLen);
                in.readFully(buffer, 0, toRead);
                expLen -= toRead;
                outOffset += toRead;
                // Did we get everything we needed? If so, we are done
                if (expLen == 0) {
                    return bb.completeAndCoalesce(outOffset);
                }
                // Or perhaps we filled the current segment? If so, finish, get next
                if (outOffset == buffer.length) {
                    buffer = bb.finishCurrentSegment();
                    outOffset = 0;
                }
            }
        }
    }
}
