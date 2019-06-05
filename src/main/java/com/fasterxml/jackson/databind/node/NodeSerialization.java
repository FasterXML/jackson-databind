package com.fasterxml.jackson.databind.node;

import java.io.IOException;

/**
 * Helper value class only used during JDK serialization: contains JSON as `byte[]`
 *
 * @since 2.10
 */
class NodeSerialization implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    public byte[] json;

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
}
