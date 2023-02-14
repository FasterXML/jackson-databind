package com.fasterxml.jackson.databind.ser.std;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.type.WritableTypeId;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.ObjectIdInfo;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitable;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonObjectFormatVisitor;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.*;
import com.fasterxml.jackson.databind.ser.impl.MapEntrySerializer;
import com.fasterxml.jackson.databind.ser.impl.ObjectIdWriter;
import com.fasterxml.jackson.databind.ser.impl.PropertyBasedObjectIdGenerator;
import com.fasterxml.jackson.databind.ser.impl.WritableObjectId;
import com.fasterxml.jackson.databind.util.ArrayBuilders;
import com.fasterxml.jackson.databind.util.ClassUtil;
import com.fasterxml.jackson.databind.util.Converter;
import com.fasterxml.jackson.databind.util.IgnorePropertiesUtil;
import com.fasterxml.jackson.databind.util.NameTransformer;

/**
 * Base class both for the standard bean serializer, and couple
 * of variants that only differ in small details.
 * Can be used for custom bean serializers as well, although that
 * is not the primary design goal.
 */
@SuppressWarnings("serial")
public abstract class BeanSerializerBase
    extends StdSerializer<Object>
    implements ContextualSerializer, ResolvableSerializer,
        JsonFormatVisitable
{
    protected final static PropertyName NAME_FOR_OBJECT_REF = new PropertyName("#object-ref");

    final protected static BeanPropertyWriter[] NO_PROPS = new BeanPropertyWriter[0];

    /*
    /**********************************************************
    /* Configuration
    /**********************************************************
     */

    /**
     * @since 2.9
     */
    final protected JavaType _beanType;

    /**
     * Writers used for outputting actual property values
     */
    final protected BeanPropertyWriter[] _props;

    /**
     * Optional filters used to suppress output of properties that
     * are only to be included in certain views
     */
    final protected BeanPropertyWriter[] _filteredProps;

    /**
     * Handler for {@link com.fasterxml.jackson.annotation.JsonAnyGetter}
     * annotated properties
     */
    final protected AnyGetterWriter _anyGetterWriter;

    /**
     * Id of the bean property filter to use, if any; null if none.
     */
    final protected Object _propertyFilterId;

    /**
     * If using custom type ids (usually via getter, or field), this is the
     * reference to that member.
     */
    final protected AnnotatedMember _typeId;

    /**
     * If this POJO can be alternatively serialized using just an object id
     * to denote a reference to previously serialized object,
     * this Object will handle details.
     */
    final protected ObjectIdWriter _objectIdWriter;

    /**
     * Requested shape from bean class annotations.
     */
    final protected JsonFormat.Shape _serializationShape;

    /*
    /**********************************************************
    /* Life-cycle: constructors
    /**********************************************************
     */

    /**
     * Constructor used by {@link BeanSerializerBuilder} to create an
     * instance
     *
     * @param type Nominal type of values handled by this serializer
     * @param builder Builder for accessing other collected information
     */
    protected BeanSerializerBase(JavaType type, BeanSerializerBuilder builder,
            BeanPropertyWriter[] properties, BeanPropertyWriter[] filteredProperties)
    {
        super(type);
        _beanType = type;
        _props = properties;
        _filteredProps = filteredProperties;
        if (builder == null) { // mostly for testing
            // 20-Sep-2019, tatu: Actually not just that but also "dummy" serializer for
            //     case of no bean properties, too
            _typeId = null;
            _anyGetterWriter = null;
            _propertyFilterId = null;
            _objectIdWriter = null;
            _serializationShape = null;
        } else {
            _typeId = builder.getTypeId();
            _anyGetterWriter = builder.getAnyGetter();
            _propertyFilterId = builder.getFilterId();
            _objectIdWriter = builder.getObjectIdWriter();
            final JsonFormat.Value format = builder.getBeanDescription().findExpectedFormat(null);
            _serializationShape = format.getShape();
        }
    }

    protected BeanSerializerBase(BeanSerializerBase src,
            BeanPropertyWriter[] properties, BeanPropertyWriter[] filteredProperties)
    {
        super(src._handledType);
        _beanType = src._beanType;
        _props = properties;
        _filteredProps = filteredProperties;

        _typeId = src._typeId;
        _anyGetterWriter = src._anyGetterWriter;
        _objectIdWriter = src._objectIdWriter;
        _propertyFilterId = src._propertyFilterId;
        _serializationShape = src._serializationShape;
    }

    protected BeanSerializerBase(BeanSerializerBase src,
            ObjectIdWriter objectIdWriter)
    {
        this(src, objectIdWriter, src._propertyFilterId);
    }

    /**
     * @since 2.3
     */
    protected BeanSerializerBase(BeanSerializerBase src,
            ObjectIdWriter objectIdWriter, Object filterId)
    {
        super(src._handledType);
        _beanType = src._beanType;
        _props = src._props;
        _filteredProps = src._filteredProps;

        _typeId = src._typeId;
        _anyGetterWriter = src._anyGetterWriter;
        _objectIdWriter = objectIdWriter;
        _propertyFilterId = filterId;
        _serializationShape = src._serializationShape;
    }

    @Deprecated // since 2.8, remove soon
    protected BeanSerializerBase(BeanSerializerBase src, String[] toIgnore)
    {
        this(src, ArrayBuilders.arrayToSet(toIgnore), null);
    }

    @Deprecated // since 2.12
    protected BeanSerializerBase(BeanSerializerBase src, Set<String> toIgnore) {
        this(src, toIgnore, null);
    }

    protected BeanSerializerBase(BeanSerializerBase src, Set<String> toIgnore, Set<String> toInclude)
    {
        super(src._handledType);

        _beanType = src._beanType;
        final BeanPropertyWriter[] propsIn = src._props;
        final BeanPropertyWriter[] fpropsIn = src._filteredProps;
        final int len = propsIn.length;

        ArrayList<BeanPropertyWriter> propsOut = new ArrayList<BeanPropertyWriter>(len);
        ArrayList<BeanPropertyWriter> fpropsOut = (fpropsIn == null) ? null : new ArrayList<BeanPropertyWriter>(len);

        for (int i = 0; i < len; ++i) {
            BeanPropertyWriter bpw = propsIn[i];
            // should be ignored?
            if (IgnorePropertiesUtil.shouldIgnore(bpw.getName(), toIgnore, toInclude)) {
                continue;
            }
            propsOut.add(bpw);
            if (fpropsIn != null) {
                fpropsOut.add(fpropsIn[i]);
            }
        }
        _props = propsOut.toArray(new BeanPropertyWriter[propsOut.size()]);
        _filteredProps = (fpropsOut == null) ? null : fpropsOut.toArray(new BeanPropertyWriter[fpropsOut.size()]);

        _typeId = src._typeId;
        _anyGetterWriter = src._anyGetterWriter;
        _objectIdWriter = src._objectIdWriter;
        _propertyFilterId = src._propertyFilterId;
        _serializationShape = src._serializationShape;
    }

    /**
     * Mutant factory used for creating a new instance with different
     * {@link ObjectIdWriter}.
     *
     * @since 2.0
     */
    public abstract BeanSerializerBase withObjectIdWriter(ObjectIdWriter objectIdWriter);

    /**
     * Mutant factory used for creating a new instance with additional
     * set of properties to ignore (from properties this instance otherwise has)
     *
     * @since 2.8
     * @deprecated Since 2.12
     */
    @Deprecated // since 2.12
    protected BeanSerializerBase withIgnorals(Set<String> toIgnore) {
        return withByNameInclusion(toIgnore, null);
    }

    /**
     * Mutant factory used for creating a new instance with additional
     * set of properties to ignore or include (from properties this instance otherwise has)
     *
     * @since 2.12
     */
    protected abstract BeanSerializerBase withByNameInclusion(Set<String> toIgnore,
            Set<String> toInclude);

    /**
     * Mutant factory used for creating a new instance with additional
     * set of properties to ignore (from properties this instance otherwise has)
     *
     * @deprecated since 2.8
     */
    @Deprecated
    protected BeanSerializerBase withIgnorals(String[] toIgnore) {
        return withIgnorals(ArrayBuilders.arrayToSet(toIgnore));
    }

    /**
     * Mutant factory for creating a variant that output POJO as a
     * JSON Array. Implementations may ignore this request if output
     * as array is not possible (either at all, or reliably).
     *
     * @since 2.1
     */
    protected abstract BeanSerializerBase asArraySerializer();

    /**
     * Mutant factory used for creating a new instance with different
     * filter id (used with <code>JsonFilter</code> annotation)
     *
     * @since 2.3
     */
    @Override
    public abstract BeanSerializerBase withFilterId(Object filterId);

    /**
     * Mutant factory used for creating a new instance with modified set
     * of properties.
     *<p>
     * Note: in 2.11.x, need to keep non-abstract for slightly better compatibility
     * (XML module extends)
     *
     * @since 2.11.1
     */
    protected abstract BeanSerializerBase withProperties(BeanPropertyWriter[] properties,
            BeanPropertyWriter[] filteredProperties);

    /**
     * Copy-constructor that is useful for sub-classes that just want to
     * copy all super-class properties without modifications.
     */
    protected BeanSerializerBase(BeanSerializerBase src) {
        this(src, src._props, src._filteredProps);
    }

    /**
     * Copy-constructor that will also rename properties with given prefix
     * (if it's non-empty)
     */
    protected BeanSerializerBase(BeanSerializerBase src, NameTransformer unwrapper) {
        this(src, rename(src._props, unwrapper), rename(src._filteredProps, unwrapper));
    }

    private final static BeanPropertyWriter[] rename(BeanPropertyWriter[] props,
            NameTransformer transformer)
    {
        if (props == null || props.length == 0 || transformer == null || transformer == NameTransformer.NOP) {
            return props;
        }
        final int len = props.length;
        BeanPropertyWriter[] result = new BeanPropertyWriter[len];
        for (int i = 0; i < len; ++i) {
            BeanPropertyWriter bpw = props[i];
            if (bpw != null) {
                result[i] = bpw.rename(transformer);
            }
        }
        return result;
    }

    /*
    /**********************************************************
    /* Post-construction processing: resolvable, contextual
    /**********************************************************
     */

    /**
     * We need to implement {@link ResolvableSerializer} to be able to
     * properly handle cyclic type references.
     */
    @Override
    public void resolve(SerializerProvider provider)
        throws JsonMappingException
    {
        int filteredCount = (_filteredProps == null) ? 0 : _filteredProps.length;
        for (int i = 0, len = _props.length; i < len; ++i) {
            BeanPropertyWriter prop = _props[i];
            // let's start with null serializer resolution actually
            if (!prop.willSuppressNulls() && !prop.hasNullSerializer()) {
                JsonSerializer<Object> nullSer = provider.findNullValueSerializer(prop);
                if (nullSer != null) {
                    prop.assignNullSerializer(nullSer);
                    // also: remember to replace filtered property too? (see [JACKSON-364])
                    if (i < filteredCount) {
                        BeanPropertyWriter w2 = _filteredProps[i];
                        if (w2 != null) {
                            w2.assignNullSerializer(nullSer);
                        }
                    }
                }
            }

            if (prop.hasSerializer()) {
                continue;
            }
            // [databind#124]: allow use of converters
            JsonSerializer<Object> ser = findConvertingSerializer(provider, prop);
            if (ser == null) {
                // Was the serialization type hard-coded? If so, use it
                JavaType type = prop.getSerializationType();

                // It not, we can use declared return type if and only if declared type is final:
                // if not, we don't really know the actual type until we get the instance.
                if (type == null) {
                    type = prop.getType();
                    if (!type.isFinal()) {
                        if (type.isContainerType() || type.containedTypeCount() > 0) {
                            prop.setNonTrivialBaseType(type);
                        }
                        continue;
                    }
                }
                ser = provider.findValueSerializer(type, prop);
                // 04-Feb-2010, tatu: We may have stashed type serializer for content types
                //   too, earlier; if so, it's time to connect the dots here:
                if (type.isContainerType()) {
                    TypeSerializer typeSer = type.getContentType().getTypeHandler();
                    if (typeSer != null) {
                        // for now, can do this only for standard containers...
                        if (ser instanceof ContainerSerializer<?>) {
                            // ugly casts... but necessary
                            @SuppressWarnings("unchecked")
                            JsonSerializer<Object> ser2 = (JsonSerializer<Object>)((ContainerSerializer<?>) ser).withValueTypeSerializer(typeSer);
                            ser = ser2;
                        }
                    }
                }
            }
            // and maybe replace filtered property too?
            if (i < filteredCount) {
                BeanPropertyWriter w2 = _filteredProps[i];
                if (w2 != null) {
                    w2.assignSerializer(ser);
                    // 17-Mar-2017, tatu: Typically will lead to chained call to original property,
                    //    which would lead to double set. Not a problem itself, except... unwrapping
                    //    may require work to be done, which does lead to an actual issue.
                    continue;
                }
            }
            prop.assignSerializer(ser);
        }

        // also, any-getter may need to be resolved
        if (_anyGetterWriter != null) {
            // 23-Feb-2015, tatu: Misleading, as this actually triggers call to contextualization...
            _anyGetterWriter.resolve(provider);
        }
    }

    /**
     * Helper method that can be used to see if specified property is annotated
     * to indicate use of a converter for property value (in case of container types,
     * it is container type itself, not key or content type).
     *
     * @since 2.2
     */
    protected JsonSerializer<Object> findConvertingSerializer(SerializerProvider provider,
            BeanPropertyWriter prop)
        throws JsonMappingException
    {
        final AnnotationIntrospector intr = provider.getAnnotationIntrospector();
        if (intr != null) {
            AnnotatedMember m = prop.getMember();
            if (m != null) {
                Object convDef = intr.findSerializationConverter(m);
                if (convDef != null) {
                    Converter<Object,Object> conv = provider.converterInstance(prop.getMember(), convDef);
                    JavaType delegateType = conv.getOutputType(provider.getTypeFactory());
                    // [databind#731]: Should skip if nominally java.lang.Object
                    JsonSerializer<?> ser = delegateType.isJavaLangObject() ? null
                            : provider.findValueSerializer(delegateType, prop);
                    return new StdDelegatingSerializer(conv, delegateType, ser);
                }
            }
        }
        return null;
    }

    @SuppressWarnings("incomplete-switch")
    @Override
    public JsonSerializer<?> createContextual(SerializerProvider provider,
            BeanProperty property)
        throws JsonMappingException
    {
        final AnnotationIntrospector intr = provider.getAnnotationIntrospector();
        final AnnotatedMember accessor = (property == null || intr == null)
                ? null : property.getMember();
        final SerializationConfig config = provider.getConfig();

        // Let's start with one big transmutation: Enums that are annotated
        // to serialize as Objects may want to revert
        JsonFormat.Value format = findFormatOverrides(provider, property, _handledType);
        JsonFormat.Shape shape = null;
        if ((format != null) && format.hasShape()) {
            shape = format.getShape();
            // or, alternatively, asked to revert "back to" other representations...
            if ((shape != JsonFormat.Shape.ANY) && (shape != _serializationShape)) {
                if (_beanType.isEnumType()) {
                    switch (shape) {
                    case STRING:
                    case NUMBER:
                    case NUMBER_INT:
                        // 12-Oct-2014, tatu: May need to introspect full annotations... but
                        //   for now, just do class ones
                        BeanDescription desc = config.introspectClassAnnotations(_beanType);
                        JsonSerializer<?> ser = EnumSerializer.construct(_beanType.getRawClass(),
                                provider.getConfig(), desc, format);
                        return provider.handlePrimaryContextualization(ser, property);
                    }
                // 16-Oct-2016, tatu: Ditto for `Map`, `Map.Entry` subtypes
                } else if (shape == JsonFormat.Shape.NATURAL) {
                    if (_beanType.isMapLikeType() && Map.class.isAssignableFrom(_handledType)) {
                        ;
                    } else if (Map.Entry.class.isAssignableFrom(_handledType)) {
                        JavaType mapEntryType = _beanType.findSuperType(Map.Entry.class);

                        JavaType kt = mapEntryType.containedTypeOrUnknown(0);
                        JavaType vt = mapEntryType.containedTypeOrUnknown(1);

                        // 16-Oct-2016, tatu: could have problems with type handling, as we do not
                        //   see if "static" typing is needed, nor look for `TypeSerializer` yet...
                        JsonSerializer<?> ser = new MapEntrySerializer(_beanType, kt, vt,
                                false, null, property);
                        return provider.handlePrimaryContextualization(ser, property);
                    }
                }
            }
        }

        ObjectIdWriter oiw = _objectIdWriter;

        // 16-Jun-2020, tatu: [databind#2759] means we need to handle reordering
        //    at a later point
        int idPropOrigIndex = 0;
        Set<String> ignoredProps = null;
        Set<String> includedProps = null;
        Object newFilterId = null;

        // Then we may have an override for Object Id
        if (accessor != null) {
            ignoredProps = intr.findPropertyIgnoralByName(config, accessor).findIgnoredForSerialization();
            includedProps = intr.findPropertyInclusionByName(config, accessor).getIncluded();
            ObjectIdInfo objectIdInfo = intr.findObjectIdInfo(accessor);
            if (objectIdInfo == null) {
                // no ObjectId override, but maybe ObjectIdRef?
                if (oiw != null) {
                    objectIdInfo = intr.findObjectReferenceInfo(accessor, null);
                    if (objectIdInfo != null) {
                        oiw = _objectIdWriter.withAlwaysAsId(objectIdInfo.getAlwaysAsId());
                    }
                }
            } else {
                // Ugh: mostly copied from BeanDeserializerBase: but can't easily change it
                // to be able to move to SerializerProvider (where it really belongs)

                // 2.1: allow modifications by "id ref" annotations as well:
                objectIdInfo = intr.findObjectReferenceInfo(accessor, objectIdInfo);
                Class<?> implClass = objectIdInfo.getGeneratorType();
                JavaType type = provider.constructType(implClass);
                JavaType idType = provider.getTypeFactory().findTypeParameters(type, ObjectIdGenerator.class)[0];
                // Property-based generator is trickier
                if (implClass == ObjectIdGenerators.PropertyGenerator.class) { // most special one, needs extra work
                    String propName = objectIdInfo.getPropertyName().getSimpleName();
                    BeanPropertyWriter idProp = null;

                    for (int i = 0, len = _props.length; ; ++i) {
                        if (i == len) {
                            provider.reportBadDefinition(_beanType, String.format(
                                    "Invalid Object Id definition for %s: cannot find property with name %s",
                                    ClassUtil.nameOf(handledType()), ClassUtil.name(propName)));
                        }
                        BeanPropertyWriter prop = _props[i];
                        if (propName.equals(prop.getName())) {
                            idProp = prop;
                            // Let's mark id prop to be moved as the first (may still get rearranged)
                            // (although it may still get rearranged etc)
                            idPropOrigIndex = i;
                            break;
                        }
                    }
                    idType = idProp.getType();
                    ObjectIdGenerator<?> gen = new PropertyBasedObjectIdGenerator(objectIdInfo, idProp);
                    oiw = ObjectIdWriter.construct(idType, (PropertyName) null, gen, objectIdInfo.getAlwaysAsId());
                } else { // other types need to be simpler
                    ObjectIdGenerator<?> gen = provider.objectIdGeneratorInstance(accessor, objectIdInfo);
                    oiw = ObjectIdWriter.construct(idType, objectIdInfo.getPropertyName(), gen,
                            objectIdInfo.getAlwaysAsId());
                }
            }
            // Or change Filter Id in use?
            Object filterId = intr.findFilterId(accessor);
            if (filterId != null) {
                // but only consider case of adding a new filter id (no removal via annotation)
                if (_propertyFilterId == null || !filterId.equals(_propertyFilterId)) {
                    newFilterId = filterId;
                }
            }
        }
        // either way, need to resolve serializer:
        BeanSerializerBase contextual = this;

        // 16-Jun-2020, tatu: [databind#2759] must make copies, then reorder
        if (idPropOrigIndex > 0) { // note: must shuffle both regular properties and filtered
            final BeanPropertyWriter[] newProps = Arrays.copyOf(_props, _props.length);
            BeanPropertyWriter bpw = newProps[idPropOrigIndex];
            System.arraycopy(newProps, 0, newProps, 1, idPropOrigIndex);
            newProps[0] = bpw;
            final BeanPropertyWriter[] newFiltered;
            if (_filteredProps == null) {
                newFiltered = null;
            } else {
                newFiltered = Arrays.copyOf(_filteredProps, _filteredProps.length);
                bpw = newFiltered[idPropOrigIndex];
                System.arraycopy(newFiltered, 0, newFiltered, 1, idPropOrigIndex);
                newFiltered[0] = bpw;
            }
            contextual = contextual.withProperties(newProps, newFiltered);
        }

        if (oiw != null) {
            JsonSerializer<?> ser = provider.findValueSerializer(oiw.idType, property);
            oiw = oiw.withSerializer(ser);
            if (oiw != _objectIdWriter) {
                contextual = contextual.withObjectIdWriter(oiw);
            }
        }
        // Possibly change inclusions: for ignored, only non-empty set matters;
        // for inclusion `null` means "not defined" but empty "include nothing":
        if (((ignoredProps != null) && !ignoredProps.isEmpty())
                || (includedProps != null)) {
            contextual = contextual.withByNameInclusion(ignoredProps, includedProps);
        }
        if (newFilterId != null) {
            contextual = contextual.withFilterId(newFilterId);
        }

        if (shape == null) {
            shape = _serializationShape;
        }
        // last but not least; may need to transmute into as-array serialization
        if (shape == JsonFormat.Shape.ARRAY) {
            return contextual.asArraySerializer();
        }
        return contextual;
    }

    /*
    /**********************************************************
    /* Accessors
    /**********************************************************
     */

    @Override
    public Iterator<PropertyWriter> properties() {
        return Arrays.<PropertyWriter>asList(_props).iterator();
    }

    /*
    /**********************************************************
    /* Partial JsonSerializer implementation
    /**********************************************************
     */

    @Override
    public boolean usesObjectId() {
        return (_objectIdWriter != null);
    }

    // Main serialization method left unimplemented
    @Override
    public abstract void serialize(Object bean, JsonGenerator gen, SerializerProvider provider)
        throws IOException;

    // Type-info-augmented case implemented as it does not usually differ between impls
    @Override
    public void serializeWithType(Object bean, JsonGenerator gen,
            SerializerProvider provider, TypeSerializer typeSer)
        throws IOException
    {
        if (_objectIdWriter != null) {
            // 08-Jul-2021, tatu: Should NOT yet set, would override "parent"
            //    context (wrt [databind#3160]
//            gen.setCurrentValue(bean);
            _serializeWithObjectId(bean, gen, provider, typeSer);
            return;
        }

        WritableTypeId typeIdDef = _typeIdDef(typeSer, bean, JsonToken.START_OBJECT);
        typeSer.writeTypePrefix(gen, typeIdDef);
        gen.setCurrentValue(bean); // [databind#878]
        if (_propertyFilterId != null) {
            serializeFieldsFiltered(bean, gen, provider);
        } else {
            serializeFields(bean, gen, provider);
        }
        typeSer.writeTypeSuffix(gen, typeIdDef);
    }

    protected final void _serializeWithObjectId(Object bean, JsonGenerator gen, SerializerProvider provider,
            boolean startEndObject) throws IOException
    {
        final ObjectIdWriter w = _objectIdWriter;
        WritableObjectId objectId = provider.findObjectId(bean, w.generator);
        // If possible, write as id already
        if (objectId.writeAsId(gen, provider, w)) {
            return;
        }
        // If not, need to inject the id:
        Object id = objectId.generateId(bean);
        if (w.alwaysAsId) {
            w.serializer.serialize(id, gen, provider);
            return;
        }
        if (startEndObject) {
            gen.writeStartObject(bean);
        }
        objectId.writeAsField(gen, provider, w);
        if (_propertyFilterId != null) {
            serializeFieldsFiltered(bean, gen, provider);
        } else {
            serializeFields(bean, gen, provider);
        }
        if (startEndObject) {
            gen.writeEndObject();
        }
    }

    protected final void _serializeWithObjectId(Object bean, JsonGenerator gen, SerializerProvider provider,
            TypeSerializer typeSer) throws IOException
    {
        final ObjectIdWriter w = _objectIdWriter;
        WritableObjectId objectId = provider.findObjectId(bean, w.generator);
        // If possible, write as id already
        if (objectId.writeAsId(gen, provider, w)) {
            return;
        }
        // If not, need to inject the id:
        Object id = objectId.generateId(bean);
        if (w.alwaysAsId) {
            w.serializer.serialize(id, gen, provider);
            return;
        }
        _serializeObjectId(bean, gen, provider, typeSer, objectId);
    }

    protected  void _serializeObjectId(Object bean, JsonGenerator g,
            SerializerProvider provider,
            TypeSerializer typeSer, WritableObjectId objectId) throws IOException
    {
        final ObjectIdWriter w = _objectIdWriter;
        WritableTypeId typeIdDef = _typeIdDef(typeSer, bean, JsonToken.START_OBJECT);

        typeSer.writeTypePrefix(g, typeIdDef);
        // 08-Jul-2021, tatu: Moved here from earlier place, wrt [databind#3160]
        g.setCurrentValue(bean); // [databind#631]
        objectId.writeAsField(g, provider, w);
        if (_propertyFilterId != null) {
            serializeFieldsFiltered(bean, g, provider);
        } else {
            serializeFields(bean, g, provider);
        }
        typeSer.writeTypeSuffix(g, typeIdDef);
    }

    /**
     * @since 2.9
     */
    protected final WritableTypeId _typeIdDef(TypeSerializer typeSer,
            Object bean, JsonToken valueShape) {
        if (_typeId == null) {
            return typeSer.typeId(bean, valueShape);
        }
        Object typeId = _typeId.getValue(bean);
        if (typeId == null) {
            // 28-Jun-2017, tatu: Is this really needed? Unchanged from 2.8 but...
            typeId = "";
        }
        return typeSer.typeId(bean, valueShape, typeId);
    }

    @Deprecated // since 2.9
    protected final String _customTypeId(Object bean)
    {
        final Object typeId = _typeId.getValue(bean);
        if (typeId == null) {
            return "";
        }
        return (typeId instanceof String) ? (String) typeId : typeId.toString();
    }

    /*
    /**********************************************************
    /* Field serialization methods
    /**********************************************************
     */

    protected void serializeFields(Object bean, JsonGenerator gen, SerializerProvider provider)
        throws IOException
    {
        final BeanPropertyWriter[] props;
        if (_filteredProps != null && provider.getActiveView() != null) {
            props = _filteredProps;
        } else {
            props = _props;
        }
        int i = 0;
        try {
            for (final int len = props.length; i < len; ++i) {
                BeanPropertyWriter prop = props[i];
                if (prop != null) { // can have nulls in filtered list
                    prop.serializeAsField(bean, gen, provider);
                }
            }
            if (_anyGetterWriter != null) {
                _anyGetterWriter.getAndSerialize(bean, gen, provider);
            }
        } catch (Exception e) {
            String name = (i == props.length) ? "[anySetter]" : props[i].getName();
            wrapAndThrow(provider, e, bean, name);
        } catch (StackOverflowError e) {
            // 04-Sep-2009, tatu: Dealing with this is tricky, since we don't have many
            //   stack frames to spare... just one or two; can't make many calls.

            // 10-Dec-2015, tatu: and due to above, avoid "from" method, call ctor directly:
            //JsonMappingException mapE = JsonMappingException.from(gen, "Infinite recursion (StackOverflowError)", e);
            DatabindException mapE = new JsonMappingException(gen, "Infinite recursion (StackOverflowError)", e);

            String name = (i == props.length) ? "[anySetter]" : props[i].getName();
            mapE.prependPath(bean, name);
            throw mapE;
        }
    }

    /**
     * Alternative serialization method that gets called when there is a
     * {@link PropertyFilter} that needs to be called to determine
     * which properties are to be serialized (and possibly how)
     */
    protected void serializeFieldsFiltered(Object bean, JsonGenerator gen,
            SerializerProvider provider)
        throws IOException
    {
        // note: almost verbatim copy of "serializeFields"; copied (instead of merged)
        // so that old method need not add check for existence of filter.
        final BeanPropertyWriter[] props;
        if (_filteredProps != null && provider.getActiveView() != null) {
            props = _filteredProps;
        } else {
            props = _props;
        }
        final PropertyFilter filter = findPropertyFilter(provider, _propertyFilterId, bean);
        // better also allow missing filter actually..
        if (filter == null) {
            serializeFields(bean, gen, provider);
            return;
        }
        int i = 0;
        try {
            for (final int len = props.length; i < len; ++i) {
                BeanPropertyWriter prop = props[i];
                if (prop != null) { // can have nulls in filtered list
                    filter.serializeAsField(bean, gen, provider, prop);
                }
            }
            if (_anyGetterWriter != null) {
                _anyGetterWriter.getAndFilter(bean, gen, provider, filter);
            }
        } catch (Exception e) {
            String name = (i == props.length) ? "[anySetter]" : props[i].getName();
            wrapAndThrow(provider, e, bean, name);
        } catch (StackOverflowError e) {
            // Minimize call depth since we are close to fail:
            //JsonMappingException mapE = JsonMappingException.from(gen, "Infinite recursion (StackOverflowError)", e);
            DatabindException mapE = new JsonMappingException(gen, "Infinite recursion (StackOverflowError)", e);
            String name = (i == props.length) ? "[anySetter]" : props[i].getName();
            mapE.prependPath(bean, name);
            throw mapE;
        }
    }

    @Deprecated
    @Override
    public JsonNode getSchema(SerializerProvider provider, Type typeHint)
        throws JsonMappingException
    {
        ObjectNode o = createSchemaNode("object", true);
        // [JACKSON-813]: Add optional JSON Schema id attribute, if found
        // NOTE: not optimal, does NOT go through AnnotationIntrospector etc:
        com.fasterxml.jackson.databind.jsonschema.JsonSerializableSchema ann =
            _handledType.getAnnotation(com.fasterxml.jackson.databind.jsonschema.JsonSerializableSchema.class);
        if (ann != null) {
            String id = ann.id();
            if (id != null && !id.isEmpty()) {
                o.put("id", id);
            }
        }

        //todo: should the classname go in the title?
        //o.put("title", _className);
        ObjectNode propertiesNode = o.objectNode();
        final PropertyFilter filter;
        if (_propertyFilterId != null) {
            filter = findPropertyFilter(provider, _propertyFilterId, null);
        } else {
            filter = null;
        }

        for (int i = 0; i < _props.length; i++) {
            BeanPropertyWriter prop = _props[i];
            if (filter == null) {
                prop.depositSchemaProperty(propertiesNode, provider);
            } else {
                filter.depositSchemaProperty(prop, propertiesNode, provider);
            }

        }
        o.set("properties", propertiesNode);
        return o;
    }

    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
        throws JsonMappingException
    {
        //deposit your output format
        if (visitor == null) {
            return;
        }
        JsonObjectFormatVisitor objectVisitor = visitor.expectObjectFormat(typeHint);
        if (objectVisitor == null) {
            return;
        }
        final SerializerProvider provider = visitor.getProvider();
        if (_propertyFilterId != null) {
            PropertyFilter filter = findPropertyFilter(visitor.getProvider(),
                    _propertyFilterId, null);
            for (int i = 0, end = _props.length; i < end; ++i) {
                filter.depositSchemaProperty(_props[i], objectVisitor, provider);
            }
        } else {
            Class<?> view = ((_filteredProps == null) || (provider == null))
                    ? null : provider.getActiveView();
            final BeanPropertyWriter[] props;
            if (view != null) {
                props = _filteredProps;
            } else {
                props = _props;
            }

            for (int i = 0, end = props.length; i < end; ++i) {
                BeanPropertyWriter prop = props[i];
                if (prop != null) { // may be filtered out unconditionally
                    prop.depositSchemaProperty(objectVisitor, provider);
                }
            }
        }
    }
}
