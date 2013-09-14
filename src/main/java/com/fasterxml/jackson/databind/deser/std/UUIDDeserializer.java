package com.fasterxml.jackson.databind.deser.std;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;

public class UUIDDeserializer
    extends FromStringDeserializer<UUID>
{
    private static final long serialVersionUID = 1L;

    final static int[] HEX_DIGITS = new int[127];
    static {
        Arrays.fill(HEX_DIGITS, -1);
        for (int i = 0; i < 10; ++i) {
            HEX_DIGITS['0' + i] = i;
        }
        for (int i = 0; i < 6; ++i) {
            HEX_DIGITS['a' + i] = 10 + i;
            HEX_DIGITS['A' + i] = 10 + i;
        }
    }

    public final static UUIDDeserializer instance = new UUIDDeserializer();
    
    public UUIDDeserializer() { super(UUID.class); }

    @Override
    protected UUID _deserialize(String id, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        // Adapted from java-uuid-generator (https://github.com/cowtowncoder/java-uuid-generator)
        // which is 5x faster than UUID.fromString(value), as oper "ManualReadPerfWithUUID"
        if (id.length() != 36) {
            _badFormat(id);
        }

        // verify hyphens first:
        if ((id.charAt(8) != '-') || (id.charAt(13) != '-')
                || (id.charAt(18) != '-') || (id.charAt(23) != '-')) {
            _badFormat(id);
        }
        long l1 = intFromChars(id, 0);
        l1 <<= 32;
        long l2 = ((long) shortFromChars(id, 9)) << 16;
        l2 |= shortFromChars(id, 14);
        long hi = l1 + l2;

        int i1 = (shortFromChars(id, 19) << 16) | shortFromChars(id, 24);
        l1 = i1;
        l1 <<= 32;
        l2 = intFromChars(id, 28);
        l2 = (l2 << 32) >>> 32; // sign removal, Java-style. Ugh.
        long lo = l1 | l2;

        return new UUID(hi, lo);
    }

    static int intFromChars(String str, int index) {
        return (byteFromChars(str, index) << 24)
                +(byteFromChars(str, index+2) << 16)
                +(byteFromChars(str, index+4) << 8)
                + byteFromChars(str, index+6);
    }
    
    static int shortFromChars(String str, int index) {
        return (byteFromChars(str, index) << 8) + byteFromChars(str, index+2);
    }
    
    static int byteFromChars(String str, int index)
    {
        final char c1 = str.charAt(index);
        final char c2 = str.charAt(index+1);

        if (c1 <= 127 && c2 <= 127) {
            int hex = (HEX_DIGITS[c1] << 4) | HEX_DIGITS[c2];
            if (hex >= 0) {
                return hex;
            }
        }
        if (c1 > 127 || HEX_DIGITS[c1] < 0) {
            return _badChar(str, index, c1);
        }
        return _badChar(str, index+1, c2);
    }

    static int _badChar(String uuidStr, int index, char c) {
        throw new NumberFormatException("Non-hex character '"+c+"', not valid character for a UUID String"
                +"' (value 0x"+Integer.toHexString(c)+") for UUID String \""+uuidStr+"\"");
    }

    private void _badFormat(String uuidStr) {
        throw new NumberFormatException("UUID has to be represented by the standard 36-char representation");
    }
    
    @Override
    protected UUID _deserializeEmbedded(Object ob, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        if (ob instanceof byte[]) {
            byte[] bytes = (byte[]) ob;
            if (bytes.length != 16) {
                ctxt.mappingException("Can only construct UUIDs from 16 byte arrays; got "+bytes.length+" bytes");
            }
            // clumsy, but should work for now...
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));
            long l1 = in.readLong();
            long l2 = in.readLong();
            return new UUID(l1, l2);
        }
        super._deserializeEmbedded(ob, ctxt);
        return null; // never gets here
    }
}