package tools.jackson.databind.ser.jdk;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;

import tools.jackson.core.*;

import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JacksonStdImpl;
import tools.jackson.databind.introspect.AnnotatedClass;
import tools.jackson.databind.introspect.EnumNamingStrategyFactory;
import tools.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import tools.jackson.databind.jsonFormatVisitors.JsonStringFormatVisitor;
import tools.jackson.databind.ser.std.StdScalarSerializer;
import tools.jackson.databind.util.EnumValues;

/**
 * Standard serializer used for {@link java.lang.Enum} types.
 *<p>
 * Based on {@link StdScalarSerializer} since the JSON value is
 * scalar (String).
 */
@JacksonStdImpl
public class EnumSerializer
    extends StdScalarSerializer<Enum<?>>
{
    /**
     * This map contains pre-resolved values (since there are ways to customize
     * actual String constants to use) to use as serializations.
     */
    protected final EnumValues _values;

    /**
     * Flag that is set if we statically know serialization choice between
     * index and textual format (null if it needs to be dynamically checked).
     */
    protected final Boolean _serializeAsIndex;

    /**
     * Map with key as converted property class defined implementation of {@link EnumNamingStrategy}
     * and with value as Enum names collected using <code>Enum.name()</code>.
     */
    protected final EnumValues _valuesByEnumNaming;

    /**
     * Map that contains pre-resolved values for {@link Enum#toString} to use for serialization,
     * while respecting {@link com.fasterxml.jackson.annotation.JsonProperty}
     * and {@link tools.jackson.databind.cfg.EnumFeature#WRITE_ENUMS_TO_LOWERCASE}.
     */
    protected final EnumValues _valuesByToString;

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    public EnumSerializer(EnumValues v, Boolean serializeAsIndex, EnumValues valuesByEnumNaming,
            EnumValues valuesByToString)
    {
        super(v.getEnumClass(), false);
        _values = v;
        _serializeAsIndex = serializeAsIndex;
        _valuesByEnumNaming = valuesByEnumNaming;
        _valuesByToString = valuesByToString;
    }

    /**
     * Factory method used by {@link tools.jackson.databind.ser.BasicSerializerFactory}
     * for constructing serializer instance of Enum types.
     */
    @SuppressWarnings("unchecked")
    public static EnumSerializer construct(Class<?> enumClass, SerializationConfig config,
            BeanDescription beanDesc, JsonFormat.Value format)
    {
        // 08-Apr-2015, tatu: As per [databind#749], we cannot statically determine
        //   between name() and toString(), need to construct `EnumValues` with names,
        //   handle toString() case dynamically (for example)
        EnumValues v = EnumValues.constructFromName(config, beanDesc.getClassInfo());
        EnumValues valuesByEnumNaming = constructEnumNamingStrategyValues(config, (Class<Enum<?>>) enumClass, beanDesc.getClassInfo());
        EnumValues valuesByToString = EnumValues.constructFromToString(config, beanDesc.getClassInfo());
        Boolean serializeAsIndex = _isShapeWrittenUsingIndex(enumClass, format, true, null);
        return new EnumSerializer(v, serializeAsIndex, valuesByEnumNaming, valuesByToString);
    }

    /**
     * To support some level of per-property configuration, we will need
     * to make things contextual. We are limited to "textual vs index"
     * choice here, however.
     */
    @Override
    public ValueSerializer<?> createContextual(SerializerProvider ctxt,
            BeanProperty property)
    {
        JsonFormat.Value format = findFormatOverrides(ctxt,
                property, handledType());
        if (format != null) {
            Class<?> type = handledType();
            Boolean serializeAsIndex = _isShapeWrittenUsingIndex(type,
                    format, false, _serializeAsIndex);
            if (!Objects.equals(serializeAsIndex, _serializeAsIndex)) {
                return new EnumSerializer(_values, serializeAsIndex,
                        _valuesByEnumNaming, _valuesByToString);
            }
        }
        return this;
    }

    /*
    /**********************************************************************
    /* Extended API for Jackson databind core
    /**********************************************************************
     */

    public EnumValues getEnumValues() { return _values; }

    /*
    /**********************************************************************
    /* Actual serialization
    /**********************************************************************
     */

    @Override
    public final void serialize(Enum<?> en, JsonGenerator g, SerializerProvider ctxt)
        throws JacksonException
    {
        if (_valuesByEnumNaming != null) {
            g.writeString(_valuesByEnumNaming.serializedValueFor(en));
            return;
        }
        // Serialize as index?
        if (_serializeAsIndex(ctxt)) {
            g.writeNumber(en.ordinal());
            return;
        }
        // [databind#749]: or via toString()?
        if (ctxt.isEnabled(SerializationFeature.WRITE_ENUMS_USING_TO_STRING)) {
            g.writeString(_valuesByToString.serializedValueFor(en));
            return;
        }
        g.writeString(_values.serializedValueFor(en));
    }

    /*
    /**********************************************************************
    /* Schema support
    /**********************************************************************
     */

    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
    {
        SerializerProvider serializers = visitor.getProvider();
        if (_serializeAsIndex(serializers)) {
            visitIntFormat(visitor, typeHint, JsonParser.NumberType.INT);
            return;
        }
        JsonStringFormatVisitor stringVisitor = visitor.expectStringFormat(typeHint);
        if (stringVisitor != null) {
            Set<String> enums = new LinkedHashSet<String>();

            // Use toString()?
            if ((serializers != null) &&
                    serializers.isEnabled(SerializationFeature.WRITE_ENUMS_USING_TO_STRING)) {
                for (SerializableString value : _valuesByToString.values()) {
                    enums.add(value.getValue());
                }
            } else {
                // No, serialize using name() or explicit overrides
                for (SerializableString value : _values.values()) {
                    enums.add(value.getValue());
                }
            }
            stringVisitor.enumTypes(enums);
        }
    }

    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */

    protected final boolean _serializeAsIndex(SerializerProvider ctxt)
    {
        if (_serializeAsIndex != null) {
            return _serializeAsIndex;
        }
        return ctxt.isEnabled(SerializationFeature.WRITE_ENUMS_USING_INDEX);
    }

    /**
     * Helper method called to check whether serialization should be done using
     * index (number) or not.
     */
    protected static Boolean _isShapeWrittenUsingIndex(Class<?> enumClass,
            JsonFormat.Value format, boolean fromClass,
            Boolean defaultValue)
    {
        JsonFormat.Shape shape = (format == null) ? null : format.getShape();
        if (shape == null) {
            return defaultValue;
        }
        // i.e. "default", check dynamically
        if (shape == Shape.ANY || shape == Shape.SCALAR) {
            return defaultValue;
        }
        // 19-May-2016, tatu: also consider "natural" shape
        if (shape == Shape.STRING || shape == Shape.NATURAL) {
            return Boolean.FALSE;
        }
        // 01-Oct-2014, tatu: For convenience, consider "as-array" to also mean 'yes, use index')
        if (shape.isNumeric() || (shape == Shape.ARRAY)) {
            return Boolean.TRUE;
        }
        // 07-Mar-2017, tatu: Also means `OBJECT` not available as property annotation...
        throw new IllegalArgumentException(String.format(
                "Unsupported serialization shape (%s) for Enum %s, not supported as %s annotation",
                    shape, enumClass.getName(), (fromClass? "class" : "property")));
    }

    /**
     * Factory method used to resolve an instance of {@link EnumValues}
     * with {@link EnumNamingStrategy} applied for the target class.
     */
    protected static EnumValues constructEnumNamingStrategyValues(SerializationConfig config, Class<Enum<?>> enumClass,
            AnnotatedClass annotatedClass) {
        Object namingDef = config.getAnnotationIntrospector().findEnumNamingStrategy(config, annotatedClass);
        EnumNamingStrategy enumNamingStrategy = EnumNamingStrategyFactory.createEnumNamingStrategyInstance(
            namingDef, config.canOverrideAccessModifiers());
        return enumNamingStrategy == null ? null : EnumValues.constructUsingEnumNamingStrategy(
            config, annotatedClass, enumNamingStrategy);
    }
}
