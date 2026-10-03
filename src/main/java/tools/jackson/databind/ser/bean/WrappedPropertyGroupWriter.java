package tools.jackson.databind.ser.bean;

import tools.jackson.core.*;
import tools.jackson.databind.*;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.introspect.AnnotatedClass;
import tools.jackson.databind.introspect.BeanPropertyDefinition;
import tools.jackson.databind.ser.BeanPropertyWriter;
import tools.jackson.databind.ser.VirtualBeanPropertyWriter;
import tools.jackson.databind.type.TypeFactory;

/**
 * Synthetic property writer that groups multiple scalar {@link BeanPropertyWriter}s
 * into a single nested JSON object. Created by the serialization factory when
 * {@code @JsonWrapped} properties are detected.
 * <p>
 * MVP limitation: This writer does not support {@code @JsonView}, {@code @JsonFilter},
 * or conditional wrapper emission based on {@code @JsonInclude}. The wrapper object
 * is always emitted with all inner properties.
 */
public class WrappedPropertyGroupWriter extends VirtualBeanPropertyWriter
{
    protected final BeanPropertyWriter[] _innerWriters;
    protected final JavaType _wrapperType;

    // Primary construction from BeanSerializerFactory
    public WrappedPropertyGroupWriter(String wrapperName,
            BeanPropertyWriter[] innerWriters,
            TypeFactory typeFactory)
    {
        this(createBlueprint(wrapperName, typeFactory), innerWriters);
    }

    // Internal constructor that properly sets the name
    private WrappedPropertyGroupWriter(WrappedPropertyGroupWriter blueprint,
            BeanPropertyWriter[] innerWriters)
    {
        super(blueprint);
        _innerWriters = innerWriters;
        _wrapperType = blueprint._wrapperType;
    }

    // Create a blueprint instance with the wrapper name set
    private static WrappedPropertyGroupWriter createBlueprint(String wrapperName, TypeFactory typeFactory) {
        WrappedPropertyGroupWriter blueprint = new WrappedPropertyGroupWriter(typeFactory);
        // Use the PropertyName constructor to set _name properly
        return new WrappedPropertyGroupWriter(blueprint, PropertyName.construct(wrapperName));
    }

    // No-arg blueprint constructor
    private WrappedPropertyGroupWriter(TypeFactory typeFactory) {
        super();
        _innerWriters = null;
        // Create a simple Object type for the wrapper
        _wrapperType = typeFactory.constructType(Object.class);
    }

    // Copy constructor with PropertyName for setting the serialized name
    private WrappedPropertyGroupWriter(WrappedPropertyGroupWriter base, PropertyName name) {
        super(base, name);
        _innerWriters = base._innerWriters;
        _wrapperType = base._wrapperType;
    }

    // Copy constructor for withConfig()
    protected WrappedPropertyGroupWriter(WrappedPropertyGroupWriter base)
    {
        super(base);
        _innerWriters = base._innerWriters;
        _wrapperType = base._wrapperType;
    }

    /**
     * Returns {@code null}. This method is never called in practice because
     * {@code serializeAsProperty()} and {@code serializeAsElement()} are fully
     * overridden.
     */
    @Override
    protected Object value(Object bean, JsonGenerator g,
            SerializationContext prov) throws Exception
    {
        return null;
    }

    /**
     * Return a sentinel view array so that this writer is always included when view-based
     * filtering is active. {@code Object.class} is used because every active view class
     * is assignable to {@code Object}, making {@code FilteredBeanPropertyWriter}'s
     * {@code isAssignableFrom} check always pass.
     * <p>
     * Without this override, {@code processViews()} would leave this writer as {@code null}
     * in the filtered-properties array whenever any property on the bean carries a
     * {@code @JsonView} annotation, causing the entire wrapper to be suppressed.
     */
    @Override
    public Class<?>[] getViews() {
        return new Class<?>[] { Object.class };
    }

    @Override
    public VirtualBeanPropertyWriter withConfig(MapperConfig<?> config,
            AnnotatedClass declaringClass, BeanPropertyDefinition propDef,
            JavaType type)
    {
        return new WrappedPropertyGroupWriter(this);
    }

    /**
     * Serializes the wrapped group as a named property in an object.
     * Writes all inner properties into a nested JSON object.
     */
    @Override
    public void serializeAsProperty(Object bean, JsonGenerator gen,
            SerializationContext ctxt) throws Exception
    {
        if (_innerWriters == null || _innerWriters.length == 0) {
            return;
        }
        gen.writeName(_name);
        gen.writeStartObject(bean, _innerWriters.length);
        for (BeanPropertyWriter inner : _innerWriters) {
            inner.serializeAsProperty(bean, gen, ctxt);
        }
        gen.writeEndObject();
    }

    /**
     * Serializes the wrapped group as an unnamed element in an array.
     * Similar to {@code serializeAsProperty}, but writes the wrapper object
     * without a field name.
     */
    @Override
    public void serializeAsElement(Object bean, JsonGenerator gen,
            SerializationContext ctxt) throws Exception
    {
        if (_innerWriters == null || _innerWriters.length == 0) {
            return;
        }
        gen.writeStartObject(bean, _innerWriters.length);
        for (BeanPropertyWriter inner : _innerWriters) {
            inner.serializeAsProperty(bean, gen, ctxt);
        }
        gen.writeEndObject();
    }

    /**
     * Override fixAccess to handle the fact that this is a synthetic property
     * without a real member. We delegate fixAccess to the inner writers instead.
     */
    @Override
    public void fixAccess(SerializationConfig config) {
        if (_innerWriters != null) {
            for (BeanPropertyWriter inner : _innerWriters) {
                inner.fixAccess(config);
            }
        }
    }

    /**
     * Override getType() to provide a proper JavaType. Since this is a synthetic
     * wrapper object property, we return a simple Object type which prevents
     * serializer lookup issues during resolution.
     */
    @Override
    public JavaType getType() {
        return (_declaredType != null) ? _declaredType : _wrapperType;
    }
}
