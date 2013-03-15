package com.fasterxml.jackson.databind.type;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonSerializable;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;

public abstract class TypeBase
    extends JavaType
    implements JsonSerializable
{
    private static final long serialVersionUID = -3581199092426900829L;

    /**
     * Lazily initialized external representation of the type
     */
    volatile transient String _canonicalName;

    /**
     * @deprecated Since 2.2 use method that takes 'asStatic' argument
     */
    @Deprecated
    protected TypeBase(Class<?> raw, int hash,
            Object valueHandler, Object typeHandler)
    {
        this(raw, hash, valueHandler, typeHandler, false);
    }

    /**
     * Main constructor to use by extending classes.
     */
    protected TypeBase(Class<?> raw, int hash,
            Object valueHandler, Object typeHandler, boolean asStatic)
    {
        super(raw, hash, valueHandler, typeHandler, asStatic);
    }

    @Override
    public String toCanonical()
    {
        String str = _canonicalName;
        if (str == null) {
            str = buildCanonicalName();
        }
        return str;
    }
    
    protected abstract String buildCanonicalName();

    @Override
    public abstract StringBuilder getGenericSignature(StringBuilder sb);

    @Override
    public abstract StringBuilder getErasedSignature(StringBuilder sb);

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getValueHandler() { return (T) _valueHandler; }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getTypeHandler() { return (T) _typeHandler; }
    
    /*
    /**********************************************************
    /* JsonSerializableWithType base implementation
    /**********************************************************
     */

    @Override
    public void serializeWithType(JsonGenerator jgen, SerializerProvider provider,
            TypeSerializer typeSer)
        throws IOException, JsonProcessingException
    {
        typeSer.writeTypePrefixForScalar(this, jgen);
        this.serialize(jgen, provider);
        typeSer.writeTypeSuffixForScalar(this, jgen);
    }

    @Override
    public void serialize(JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonProcessingException
    {
        jgen.writeString(toCanonical());
    } 
    
    /*
    /**********************************************************
    /* Methods for sub-classes to use
    /**********************************************************
     */

    /**
     * @param trailingSemicolon Whether to add trailing semicolon for non-primitive
     *   (reference) types or not
     */
    protected static StringBuilder _classSignature(Class<?> cls, StringBuilder sb,
           boolean trailingSemicolon)
    {
        if (cls.isPrimitive()) {
            if (cls == Boolean.TYPE) {                
                sb.append('Z');
            } else if (cls == Byte.TYPE) {
                sb.append('B');
            }
            else if (cls == Short.TYPE) {
                sb.append('S');
            }
            else if (cls == Character.TYPE) {
                sb.append('C');
            }
            else if (cls == Integer.TYPE) {
                sb.append('I');
            }
            else if (cls == Long.TYPE) {
                sb.append('J');
            }
            else if (cls == Float.TYPE) {
                sb.append('F');
            }
            else if (cls == Double.TYPE) {
                sb.append('D');
            }
            else if (cls == Void.TYPE) {
                sb.append('V');
            } else {
                throw new IllegalStateException("Unrecognized primitive type: "+cls.getName());
            }
        } else {
            sb.append('L');
            String name = cls.getName();
            for (int i = 0, len = name.length(); i < len; ++i) {
                char c = name.charAt(i);
                if (c == '.') c = '/';
                sb.append(c);
            }
            if (trailingSemicolon) {
                sb.append(';');
            }
        }
        return sb;
    }
}
