package tools.jackson.databind.ser.jdk;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;

import tools.jackson.core.*;
import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JacksonStdImpl;
import tools.jackson.databind.cfg.EnumFeature;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import tools.jackson.databind.jsonFormatVisitors.JsonStringFormatVisitor;
import tools.jackson.databind.ser.std.StdScalarSerializer;
import tools.jackson.databind.util.EnumDefinition;
import tools.jackson.databind.util.EnumValuesToWrite;

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
     * Container for dynamically resolved serializations for the type.
     */
    protected final EnumValuesToWrite _enumValuesToWrite;

    /**
     * If statically known, whether to serialize as numeric index ({@code TRUE})
     * or as textual name ({@code FALSE}). {@code null} means not statically known:
     * falls back to global {@code EnumFeature.WRITE_ENUMS_USING_INDEX} at runtime.
     *<p>
     * Note: when {@code TRUE} (explicit {@code Shape.NUMBER}), numeric
     * {@code @JsonProperty} values are used as indexes; non-numeric values
     * are written as-is (as Strings).
     * When {@code null} and the global feature is enabled, ordinal is always used.
     *
     * @since 3.2 (renamed from earlier {@code _serializeAsIndex})
     */
    protected final Boolean _serializeAsNumber;

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    public EnumSerializer(EnumValuesToWrite enumValuesToWrite, Boolean serializeAsNumber)
    {
        super(enumValuesToWrite.enumClass(), false);
        _enumValuesToWrite = enumValuesToWrite;
        _serializeAsNumber = serializeAsNumber;
    }

    /**
     * Factory method used by {@link tools.jackson.databind.ser.BasicSerializerFactory}
     * for constructing serializer instance of Enum types.
     */
    public static EnumSerializer construct(Class<?> enumClass, SerializationConfig config,
            BeanDescription beanDesc, JsonFormat.Value format)
    {
        // 08-Apr-2015, tatu: As per [databind#749], we cannot statically determine
        //   between name() and toString(), need to construct `EnumValues` with names,
        //   handle toString() case dynamically (for example)
        // 26-Nov-2025, tatu: Further refactoring post-[databind#5432] to deprecate
        //   `EnumValues`, replaced with `EnumValuesToWrite`
        EnumValuesToWrite writer = EnumDefinition.construct(config, beanDesc.getClassInfo())
                .valuesToWrite(config);
        return new EnumSerializer(writer,
                _isShapeWrittenUsingNumber(enumClass, format, true, null));
    }

    /**
     * To support some level of per-property configuration, we will need
     * to make things contextual. We are limited to "textual vs numeric"
     * choice here, however.
     */
    @Override
    public ValueSerializer<?> createContextual(SerializationContext ctxt,
            BeanProperty property)
    {
        JsonFormat.Value format = findFormatOverrides(ctxt,
                property, handledType());
        if (format != null) {
            Class<?> type = handledType();
            Boolean serializeAsNumber = _isShapeWrittenUsingNumber(type,
                    format, false, _serializeAsNumber);
            if (!Objects.equals(serializeAsNumber, _serializeAsNumber)) {
                return new EnumSerializer(_enumValuesToWrite, serializeAsNumber);
            }
        }
        return this;
    }

    /*
    /**********************************************************************
    /* Extended API for Jackson databind core
    /**********************************************************************
     */

    @Deprecated // @since 3.1
    public tools.jackson.databind.util.EnumValues getEnumValues() {
        // 26-Nov-2025, tatu: Unfortunate, but can't really support getting
        //    such value, so better fail flamboyantly instead of quietly 
        throw new UnsupportedOperationException();
    }

    /*
    /**********************************************************************
    /* Actual serialization
    /**********************************************************************
     */

    @Override
    public void serialize(Enum<?> en, JsonGenerator g, SerializationContext ctxt)
        throws JacksonException
    {
        if (_serializeAsNumber != null) {
            if (_serializeAsNumber) {
                // Explicit Shape.NUMBER/ARRAY: use @JsonProperty value as number if numeric,
                // ordinal if no @JsonProperty, or @JsonProperty value as String if non-numeric
                final int nr = _enumValuesToWrite.resolvedIndexFor(en);
                if (nr >= 0) {
                    g.writeNumber(nr);
                } else {
                    final SerializableString explicitName = _enumValuesToWrite.explicitNameFor(en);
                    if (explicitName != null) {
                        // Non-numeric @JsonProperty: use as-is as String
                        g.writeString(explicitName);
                    } else {
                        // No @JsonProperty: use ordinal
                        g.writeNumber(en.ordinal());
                    }
                }
                return;
            }
            // Explicit Shape.STRING/NATURAL: fall through to textual serialization
        } else if (ctxt.isEnabled(EnumFeature.WRITE_ENUMS_USING_INDEX)) {
            // No explicit shape, global feature: use ordinal
            g.writeNumber(en.ordinal());
            return;
        }
        // Textual serialization
        final MapperConfig<?> config = ctxt.getConfig();
        if (ctxt.isEnabled(EnumFeature.WRITE_ENUMS_USING_TO_STRING)) {
            g.writeString(_enumValuesToWrite.enumValueFromToString(config, en));
        } else {
            g.writeString(_enumValuesToWrite.enumValueFromName(config, en));
        }
    }

    /*
    /**********************************************************************
    /* Schema support
    /**********************************************************************
     */

    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
    {
        SerializationContext ctxt = visitor.getContext();
        if (_serializeAsNumber(ctxt)) {
            visitIntFormat(visitor, typeHint, JsonParser.NumberType.INT);
            return;
        }
        JsonStringFormatVisitor stringVisitor = visitor.expectStringFormat(typeHint);
        if (stringVisitor != null) {
            final MapperConfig<?> config = ctxt.getConfig();
            SerializableString[] values = ctxt.isEnabled(EnumFeature.WRITE_ENUMS_USING_TO_STRING)
                    ? _enumValuesToWrite.allEnumValuesFromToString(config)
                    : _enumValuesToWrite.allEnumValuesFromName(config);
            Set<String> enumStrings = new LinkedHashSet<>();
            for (SerializableString sstr : values) {
                enumStrings.add(sstr.getValue());
            }
            stringVisitor.enumTypes(enumStrings);
        }
    }

    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */

    protected final boolean _serializeAsNumber(SerializationContext ctxt)
    {
        if (_serializeAsNumber != null) {
            return _serializeAsNumber;
        }
        return ctxt.isEnabled(EnumFeature.WRITE_ENUMS_USING_INDEX);
    }

    /**
     * Helper method called to check whether serialization should be done using
     * numeric representation or not.
     */
    protected static Boolean _isShapeWrittenUsingNumber(Class<?> enumClass,
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
        // 01-Oct-2014, tatu: For convenience, consider "as-array" to also mean 'yes, use number')
        if (shape.isNumeric() || (shape == Shape.ARRAY)) {
            return Boolean.TRUE;
        }
        // 07-Mar-2017, tatu: Also means `OBJECT` not available as property annotation...
        throw new IllegalArgumentException(String.format(
                "Unsupported serialization shape (%s) for Enum %s, not supported as %s annotation",
                    shape, enumClass.getName(), (fromClass? "class" : "property")));
    }
}
