package com.fasterxml.jackson.databind.ser.std;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.type.WritableTypeId;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;

/**
 * "Combo" serializer used for JDK types that work almost like {@link ToStringSerializer}.
 *
 * @since 3.0
 */
@JacksonStdImpl
public class StringLikeSerializer
    extends StdSerializer<Object>
{
    private static final long serialVersionUID = 1L;

    protected final static int TYPE_URL = 1;
    protected final static int TYPE_URI = 2;
    protected final static int TYPE_FILE = 3;
    protected final static int TYPE_PATH = 4;

    protected final static int TYPE_CLASS = 5;
    
    protected final static int TYPE_CURRENCY = 6;
    protected final static int TYPE_LOCALE = 7;
    protected final static int TYPE_PATTERN = 8;

    private final static Map<Class<?>,Integer> _types = new HashMap<>();
    static {
        _types.put(URL.class, TYPE_URL);
        _types.put(URI.class, TYPE_URI);
        _types.put(File.class, TYPE_FILE);
        _types.put(Path.class, TYPE_PATH);

        _types.put(Class.class, TYPE_CLASS);

        _types.put(Currency.class, TYPE_CURRENCY);
        _types.put(Locale.class, TYPE_LOCALE);
        _types.put(Pattern.class, TYPE_PATTERN);
    }

    private final int _type;

    public StringLikeSerializer(Class<?> handledType, int type) {
        super(handledType, false);
        _type = type;
    }

    public static final JsonSerializer<?> find(Class<?> raw)
    {
        Integer I = _types.get(raw);
        if (I == null) {
            return null;
        }
        return new StringLikeSerializer(raw, I.intValue());
    }
    
    @Override
    public boolean isEmpty(SerializerProvider prov, Object value) {
        return value.toString().isEmpty();
    }

    @Override
    public void serialize(Object value, JsonGenerator g, SerializerProvider provider)
        throws IOException
    {
        String str;

        switch (_type) {
        case TYPE_FILE:
            str = ((File) value).getAbsolutePath();
            break;
        case TYPE_PATH:
            str = ((Path)value).toUri().toString();
            break;
        case TYPE_CLASS:
            str = ((Class<?>)value).getName();
            break;
        case TYPE_LOCALE: // [databind#1600]
            {
                Locale loc = (Locale) value;
                if (loc == Locale.ROOT) {
                    str = "";
                } else {
                    str = loc.toLanguageTag();
                }
            }
            break;
        default:
            str = value.toString();
            break;
        }
        g.writeString(str);
    }

    /**
     * Default implementation will write type prefix, call regular serialization
     * method (since assumption is that value itself does not need JSON
     * Array or Object start/end markers), and then write type suffix.
     * This should work for most cases; some sub-classes may want to
     * change this behavior.
     */
    @Override
    public void serializeWithType(Object value, JsonGenerator g, SerializerProvider provider,
            TypeSerializer typeSer)
        throws IOException
    {
        // 15-Feb-2018, tatu: Note! In some cases `handledType` is base type, and not necessarily
        //    actual specific value type (f.ex. nio.Path)
        WritableTypeId typeIdDef = typeSer.writeTypePrefix(g,
                typeSer.typeId(value, handledType(), JsonToken.VALUE_STRING));
        serialize(value, g, provider);
        typeSer.writeTypeSuffix(g, typeIdDef);
    }

    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint) throws JsonMappingException
    {
        visitStringFormat(visitor, typeHint);
    }
}
