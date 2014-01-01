package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

import com.fasterxml.jackson.core.Base64Variants;

import com.fasterxml.jackson.databind.DeserializationContext;

public class UUIDDeserializer extends FromStringDeserializer<UUID>
{
    private static final long serialVersionUID = 1L;

    final static int[] HEX_DIGITS = new int[127];
    static {
        Arrays.fill(HEX_DIGITS, -1);
        for (int i = 0; i < 10; ++i) { HEX_DIGITS['0' + i] = i; }
        for (int i = 0; i < 6; ++i) {
            HEX_DIGITS['a' + i] = 10 + i;
            HEX_DIGITS['A' + i] = 10 + i;
        }
    }

    public UUIDDeserializer() { super(UUID.class); }

    @Override
    protected UUID _deserialize(String id, DeserializationContext ctxt) throws IOException
    {
        // Adapted from java-uuid-generator (https://github.com/cowtowncoder/java-uuid-generator)
        // which is 5x faster than UUID.fromString(value), as oper "ManualReadPerfWithUUID"
        if (id.length() != 36) {
            /* 14-Sep-2013, tatu: One trick we do allow, Base64-encoding, since we know
             *   length it must have...
             */
            if (id.length() == 24) {
                byte[] stuff = Base64Variants.getDefaultVariant().decode(id);
                return _fromBytes(stuff, ctxt);
            }
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
    
    @Override
    protected UUID _deserializeEmbedded(Object ob, DeserializationContext ctxt) throws IOException
    {
        if (ob instanceof byte[]) {
            return _fromBytes((byte[]) ob, ctxt);
        }
        super._deserializeEmbedded(ob, ctxt);
        return null; // never gets here
    }

    private void _badFormat(String uuidStr) {
        throw new NumberFormatException("UUID has to be represented by the standard 36-char representation");
    }
    
    static int intFromChars(String str, int index) {
        return (byteFromChars(str, index) << 24) + (byteFromChars(str, index+2) << 16) + (byteFromChars(str, index+4) << 8) + byteFromChars(str, index+6);
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
    
    private UUID _fromBytes(byte[] bytes, DeserializationContext ctxt) throws IOException {
        if (bytes.length != 16) {
            ctxt.mappingException("Can only construct UUIDs from byte[16]; got "+bytes.length+" bytes");
        }
        return new UUID(_long(bytes, 0), _long(bytes, 8));
    }

    private static long _long(byte[] b, int offset) {
        long l1 = ((long) _int(b, offset)) << 32;
        long l2 = _int(b, offset+4);
        // faster to just do it than check if it has sign
        l2 = (l2 << 32) >>> 32; // to get rid of sign
        return l1 | l2;
    }

    private static int _int(byte[] b, int offset) {
        return (b[offset] << 24) | ((b[offset+1] & 0xFF) << 16) | ((b[offset+2] & 0xFF) << 8) | (b[offset+3] & 0xFF);
    }
}