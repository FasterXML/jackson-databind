package com.fasterxml.jackson.databind.node;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.json.JsonMapper;

/**
 * Helper value class only used during JDK serialization: contains JSON as `byte[]`
 *
 * @since 2.10
 */
class NodeSerialization implements java.io.Serializable,
    java.io.Externalizable
{
    private static final long serialVersionUID = 1L;

    private static final JsonMapper JSON_MAPPER = JsonMapper.shared();
    private static final ObjectReader NODE_READER = JSON_MAPPER.readerFor(JsonNode.class);
    
    public byte[] json;

    public NodeSerialization() { }

    public NodeSerialization(byte[] b) { json = b; }

    protected Object readResolve() {
        try {
            return bytesToNode(json);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to JDK deserialize `JsonNode` value: "+e.getMessage(), e);
        }
    }    

    public static NodeSerialization from(Object o) {
        try {
            return new NodeSerialization(valueToBytes(o));
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
        json = new byte[len];
        in.readFully(json, 0, len);
    }

    private static byte[] valueToBytes(Object value) throws IOException {
        return JSON_MAPPER.writeValueAsBytes(value);
    }

    private static JsonNode bytesToNode(byte[] json) throws IOException {
        return NODE_READER.readValue(json);
    }
}
