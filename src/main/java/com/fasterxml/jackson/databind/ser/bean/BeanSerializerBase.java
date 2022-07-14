package com.fasterxml.jackson.databind.ser.bean;

import java.util.*;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.core.*;
import tools.jackson.core.type.WritableTypeId;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.ObjectIdInfo;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonObjectFormatVisitor;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.*;
import com.fasterxml.jackson.databind.ser.impl.ObjectIdWriter;
import com.fasterxml.jackson.databind.ser.impl.PropertyBasedObjectIdGenerator;
import com.fasterxml.jackson.databind.ser.jdk.EnumSerializer;
import com.fasterxml.jackson.databind.ser.jdk.MapEntrySerializer;
import com.fasterxml.jackson.databind.ser.std.StdContainerSerializer;
import com.fasterxml.jackson.databind.ser.std.StdDelegatingSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
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
public abstract class BeanSerializerBase
    extends StdSerializer<Object>
{
    protected final static PropertyName NAME_FOR_OBJECT_REF = new PropertyName("#object-ref");

    final protected static BeanPropertyWriter[] NO_PROPS = new BeanPropertyWriter[0];

    /*
    /**********************************************************************
    /* Configuration
    /**********************************************************************
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
    /**********************************************************************
    /* Life-cycle: constructors
    /**********************************************************************
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
            JsonFormat.Value format = builder.getBeanDescription().findExpectedFormat(type.getRawClass());
            _serializationShape = format.getShape();
        }
    }

    /**
     * Copy-constructor that is useful for sub-classes that just want to
     * copy all super-class properties without modifications.
     */
    protected BeanSerializerBase(BeanSerializerBase src) {
        this(src, src._props, src._filteredProps);
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
        _props = propsOut.toArray(NO_PROPS);
        _filteredProps = (fpropsOut == null) ? null : fpropsOut.toArray(NO_PROPS);
        
        _typeId = src._typeId;
        _anyGetterWriter = src._anyGetterWriter;
        _objectIdWriter = src._objectIdWriter;
        _propertyFilterId = src._propertyFilterId;
        _serializationShape = src._serializationShape;
    }
    
    /**
     * Mutant factory used for creating a new instance with different
     * {@link ObjectIdWriter}.
     */
    public abstract BeanSerializerBase withObjectIdWriter(ObjectIdWriter objectIdWriter);

    /**
     * Mutant factory used for creating a new instance with additional
     * set of properties to ignore or include (from properties this instance otherwise has)
     */
    protected abstract BeanSerializerBase withByNameInclusion(Set<String> toIgnore,
            Set<String> toInclude);

    /**
     * Mutant factory for creating a variant that output POJO as a
     * JSON Array. Implementations may ignore this request if output
     * as array is not possible (either at all, or reliably).
     */
    protected abstract BeanSerializerBase asArraySerializer();

    /**
     * Mutant factory used for creating a new instance with different
     * filter id (used with <code>JsonFilter</code> annotation)
     */
    @Override
    public abstract BeanSerializerBase withFilterId(Object filterId);

    /**
     * Mutant factory used for creating a new instance with modified set
     * of properties
     */
    protected abstract BeanSerializerBase withProperties(BeanPropertyWriter[] properties,
            BeanPropertyWriter[] filteredProperties);

    /**
     * Lets force sub-classes to implement this, to avoid accidental missing
     * of handling...
     */
    @Override
    public abstract ValueSerializer<Object> unwrappingSerializer(NameTransformer unwrapper);

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
    /**********************************************************************
    /* Post-construction processing: resolvable, contextual
    /**********************************************************************
     */

    /**
     * We need to resolve dependant serializers here
     * to be able to properly handle cyclic type references.
     */
    @Override
    public void resolve(SerializerProvider provider)
    {
        int filteredCount = (_filteredProps == null) ? 0 : _filteredProps.length;
        for (int i = 0, len = _props.length; i < len; ++i) {
            BeanPropertyWriter prop = _props[i];
            // let's start with null serializer resolution actually
            if (!prop.willSuppressNulls() && !prop.hasNullSerializer()) {
                ValueSerializer<Object> nullSer = provider.findNullValueSerializer(prop);
                if (nullSer != null) {
                    prop.assignNullSerializer(nullSer);
                    // also: remember to replace filtered property too?
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
            ValueSerializer<Object> ser = findConvertingSerializer(provider, prop);
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
                ser = provider.findPrimaryPropertySerializer(type, prop);
                // 04-Feb-2010, tatu: We may have stashed type serializer for content types
                //   too, earlier; if so, it's time to connect the dots here:
                if (type.isContainerType()) {
                    TypeSerializer typeSer = (TypeSerializer) type.getContentType().getTypeHandler();
                    if (typeSer != null) {
                        // for now, can do this only for standard containers...
                        if (ser instanceof StdContainerSerializer<?>) {
                            // ugly casts... but necessary
                            @SuppressWarnings("unchecked")
                            ValueSerializer<Object> ser2 = (ValueSerializer<Object>)((StdContainerSerializer<?>) ser).withValueTypeSerializer(typeSer);
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
     */
    protected ValueSerializer<Object> findConvertingSerializer(SerializerProvider provider,
            BeanPropertyWriter prop)
    {
        final AnnotationIntrospector intr = provider.getAnnotationIntrospector();
        if (intr != null) {
            AnnotatedMember m = prop.getMember();
            if (m != null) {
                Object convDef = intr.findSerializationConverter(provider.getConfig(), m);
                if (convDef != null) {
                    Converter<Object,Object> conv = provider.converterInstance(prop.getMember(), convDef);
                    JavaType delegateType = conv.getOutputType(provider.getTypeFactory());
                    // [databind#731]: Should skip if nominally java.lang.Object
                    ValueSerializer<?> ser = delegateType.isJavaLangObject() ? null
                            : provider.findPrimaryPropertySerializer(delegateType, prop);
                    return new StdDelegatingSerializer(conv, delegateType, ser, prop);
                }
            }
        }
        return null;
    }

    @SuppressWarnings("incomplete-switch")
    @Override
    public ValueSerializer<?> createContextual(SerializerProvider ctxt, BeanProperty property)
    {
        final AnnotationIntrospector intr = ctxt.getAnnotationIntrospector();
        final AnnotatedMember accessor = _neitherNull(property, intr)
                ? property.getMember() : null;
        final SerializationConfig config = ctxt.getConfig();

        // Let's start with one big transmutation: Enums that are annotated
        // to serialize as Objects may want to revert
        JsonFormat.Value format = findFormatOverrides(ctxt, property, _handledType);
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
                        BeanDescription desc = ctxt.introspectBeanDescription(_beanType);
                        ValueSerializer<?> ser = EnumSerializer.construct(_beanType.getRawClass(),
                                config, desc, format);
                        return ctxt.handlePrimaryContextualization(ser, property);
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
                        ValueSerializer<?> ser = new MapEntrySerializer(_beanType, kt, vt,
                                false, null, property);
                        return ctxt.handlePrimaryContextualization(ser, property);
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
            ObjectIdInfo objectIdInfo = intr.findObjectIdInfo(config, accessor);
            if (objectIdInfo == null) {
                // no ObjectId override, but maybe ObjectIdRef?
                if (oiw != null) {
                    objectIdInfo = intr.findObjectReferenceInfo(config, accessor, null);
                    if (objectIdInfo != null) {
                        oiw = _objectIdWriter.withAlwaysAsId(objectIdInfo.getAlwaysAsId());
                    }
                }
            } else {
                // Ugh: mostly copied from BeanDeserializerBase: but can't easily change it
                // to be able to move to SerializerProvider (where it really belongs)
                
                // 2.1: allow modifications by "id ref" annotations as well:
                objectIdInfo = intr.findObjectReferenceInfo(config, accessor, objectIdInfo);

                Class<?> implClass = objectIdInfo.getGeneratorType();
                JavaType type = ctxt.constructType(implClass);
                JavaType idType = ctxt.getTypeFactory().findTypeParameters(type, ObjectIdGenerator.class)[0];
                // Property-based generator is trickier
                if (implClass == ObjectIdGenerators.PropertyGenerator.class) { // most special one, needs extra work
                    String propName = objectIdInfo.getPropertyName().getSimpleName();
                    BeanPropertyWriter idProp = null;
                    
                    for (int i = 0, len = _props.length; ; ++i) {
                        if (i == len) {
                            ctxt.reportBadDefinition(_beanType, String.format(
                                    "Invalid Object Id definition for %s: cannot find property with name %s",
                                    ClassUtil.getTypeDescription(_beanType), ClassUtil.name(propName)));
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
                    ObjectIdGenerator<?>gen = ctxt.objectIdGeneratorInstance(accessor, objectIdInfo);
                    oiw = ObjectIdWriter.construct(idType, objectIdInfo.getPropertyName(), gen,
                            objectIdInfo.getAlwaysAsId());
                }
            }
            // Or change Filter Id in use?
            Object filterId = intr.findFilterId(config, accessor);
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
            // not really associated with the property so let's not pass it?
            ValueSerializer<?> ser = ctxt.findRootValueSerializer(oiw.idType);
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
    /**********************************************************************
    /* Public accessors
    /**********************************************************************
     */

    @Override
    public Iterator<PropertyWriter> properties() {
        return Arrays.<PropertyWriter>asList(_props).iterator();
    }

    /**
     * @since 3.0
     */
    public int propertyCount() {
        return _props.length;
    }

    /**
     * Accessor for checking if view-processing is enabled for this bean,
     * that is, if it has separate set of properties with view-checking
     * added.
     * 
     * @since 3.0
     */
    public boolean hasViewProperties() {
        return (_filteredProps != null);
    }
    /**
     * @since 3.0
     */
    public Object getFilterId() {
        return _propertyFilterId;
    }

    /*
    /**********************************************************************
    /* Helper methods for implementation classes
    /**********************************************************************
     */

    /**
     * Helper method for sub-classes to check if it should be possible to
     * construct an "as-array" serializer. Returns if all of following
     * hold true:
     *<ul>
     * <li>have Object Id (may be allowed in future)</li>
     * <li>have "any getter"</li>
     * </ul>
     *
     * @since 3.0
     */
    public boolean canCreateArraySerializer() {
        return (_objectIdWriter == null)
                && (_anyGetterWriter == null);
    }

    /*
    /**********************************************************************
    /* Partial ValueSerializer implementation
    /**********************************************************************
     */

    @Override
    public boolean usesObjectId() {
        return (_objectIdWriter != null);
    }
    
    // Main serialization method left unimplemented
    @Override
    public abstract void serialize(Object bean, JsonGenerator gen, SerializerProvider provider)
        throws JacksonException;

    // Type-info-augmented case implemented as it does not usually differ between impls
    @Override
    public void serializeWithType(Object bean,
            JsonGenerator gen, SerializerProvider ctxt, TypeSerializer typeSer)
        throws JacksonException
    {
        if (_objectIdWriter != null) {
            _serializeWithObjectId(bean, gen, ctxt, typeSer);
            return;
        }
        WritableTypeId typeIdDef = _typeIdDef(typeSer, bean, JsonToken.START_OBJECT);
        typeSer.writeTypePrefix(gen, ctxt, typeIdDef);
        gen.assignCurrentValue(bean); // [databind#631]

        if (_propertyFilterId != null) {
            _serializePropertiesFiltered(bean, gen, ctxt, _propertyFilterId);
        } else {
            _serializeProperties(bean, gen, ctxt);
        }
        typeSer.writeTypeSuffix(gen, ctxt, typeIdDef);
    }

    protected final void _serializeWithObjectId(Object bean, JsonGenerator g,
            SerializerProvider provider, boolean startEndObject)
        throws JacksonException
    {
        final ObjectIdWriter w = _objectIdWriter;
        WritableObjectId objectId = provider.findObjectId(bean, w.generator);
        // If possible, write as id already
        if (objectId.writeAsReference(g, provider, w)) {
            return;
        }
        // If not, need to inject the id:
        Object id = objectId.generateId(bean);
        if (w.alwaysAsId) {
            w.serializer.serialize(id, g, provider);
            return;
        }
        if (startEndObject) {
            g.writeStartObject(bean);
        }
        objectId.writeAsDeclaration(g, provider, w);
        if (_propertyFilterId != null) {
            _serializePropertiesFiltered(bean, g, provider, _propertyFilterId);
        } else {
            _serializeProperties(bean, g, provider);
        }
        if (startEndObject) {
            g.writeEndObject();
        }
    }
    
    protected final void _serializeWithObjectId(Object bean, JsonGenerator g, SerializerProvider provider,
            TypeSerializer typeSer)
        throws JacksonException
    {
        g.assignCurrentValue(bean);
        final ObjectIdWriter w = _objectIdWriter;
        WritableObjectId objectId = provider.findObjectId(bean, w.generator);
        // If possible, write as id already
        if (objectId.writeAsReference(g, provider, w)) {
            return;
        }
        // If not, need to inject the id:
        Object id = objectId.generateId(bean);
        if (w.alwaysAsId) {
            w.serializer.serialize(id, g, provider);
            return;
        }
        _serializeObjectId(bean, g, provider, typeSer, objectId);
    }

    protected  void _serializeObjectId(Object bean,
            JsonGenerator g, SerializerProvider ctxt,
            TypeSerializer typeSer, WritableObjectId objectId)
        throws JacksonException
    {
        final ObjectIdWriter w = _objectIdWriter;
        WritableTypeId typeIdDef = _typeIdDef(typeSer, bean, JsonToken.START_OBJECT);

        typeSer.writeTypePrefix(g, ctxt, typeIdDef);
        objectId.writeAsDeclaration(g, ctxt, w);
        if (_propertyFilterId != null) {
            _serializePropertiesFiltered(bean, g, ctxt, _propertyFilterId);
        } else {
            _serializeProperties(bean, g, ctxt);
        }
        typeSer.writeTypeSuffix(g, ctxt, typeIdDef);
    }

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

    /*
    /**********************************************************************
    /* Field serialization methods, 3.0
    /**********************************************************************
     */

    /**
     * Method called when neither JSON Filter is to be applied, nor
     * view-filtering. This means that all property writers are non null
     * and can be called directly.
     *
     * @since 3.0
     */
    protected void _serializePropertiesNoView(Object bean, JsonGenerator gen,
            SerializerProvider provider, BeanPropertyWriter[] props)
        throws JacksonException
    {
        int i = 0;
        int left = props.length;
        BeanPropertyWriter prop = null;

        try {
            if (left > 3) {
                do {
                    prop = props[i];
                    prop.serializeAsProperty(bean, gen, provider);
                    prop = props[i+1];
                    prop.serializeAsProperty(bean, gen, provider);
                    prop = props[i+2];
                    prop.serializeAsProperty(bean, gen, provider);
                    prop = props[i+3];
                    prop.serializeAsProperty(bean, gen, provider);
                    left -= 4;
                    i += 4;
                } while (left > 3);
            }
            switch (left) {
            case 3:
                prop = props[i++];
                prop.serializeAsProperty(bean, gen, provider);
            case 2:
                prop = props[i++];
                prop.serializeAsProperty(bean, gen, provider);
            case 1:
                prop = props[i++];
                prop.serializeAsProperty(bean, gen, provider);
            }
            if (_anyGetterWriter != null) {
                prop = null;
                _anyGetterWriter.getAndSerialize(bean, gen, provider);
            }
        } catch (Exception e) {
            String name = (prop == null) ? "[anySetter]" : prop.getName();
            wrapAndThrow(provider, e, bean, name);
        } catch (StackOverflowError e) {
            final String name = (prop == null) ? "[anySetter]" : prop.getName();
            throw DatabindException.from(gen, "Infinite recursion (StackOverflowError)", e)
                .prependPath(bean, name);
        }
    }

    /**
     * Method called when no JSON Filter is to be applied, but
     * View filtering is in effect and so some of properties may be
     * nulls to check.
     *
     * @since 3.0
     */
    protected void _serializePropertiesMaybeView(Object bean, JsonGenerator g,
            SerializerProvider provider, BeanPropertyWriter[] props)
        throws JacksonException
    {
        int i = 0;
        int left = props.length;
        BeanPropertyWriter prop = null;

        try {
            if (left > 3) {
                do {
                    prop = props[i];
                    if (prop != null) {
                        prop.serializeAsProperty(bean, g, provider);
                    }
                    prop = props[i+1];
                    if (prop != null) {
                        prop.serializeAsProperty(bean, g, provider);
                    }
                    prop = props[i+2];
                    if (prop != null) {
                        prop.serializeAsProperty(bean, g, provider);
                    }
                    prop = props[i+3];
                    if (prop != null) {
                        prop.serializeAsProperty(bean, g, provider);
                    }
                    left -= 4;
                    i += 4;
                } while (left > 3);
            }
            switch (left) {
            case 3:
                prop = props[i++];
                if (prop != null) {
                    prop.serializeAsProperty(bean, g, provider);
                }
            case 2:
                prop = props[i++];
                if (prop != null) {
                    prop.serializeAsProperty(bean, g, provider);
                }
            case 1:
                prop = props[i++];
                if (prop != null) {
                    prop.serializeAsProperty(bean, g, provider);
                }
            }
            if (_anyGetterWriter != null) {
                prop = null;
                _anyGetterWriter.getAndSerialize(bean, g, provider);
            }
        } catch (Exception e) {
            String name = (prop == null) ? "[anySetter]" : prop.getName();
            wrapAndThrow(provider, e, bean, name);
        } catch (StackOverflowError e) {
            final String name = (prop == null) ? "[anySetter]" : prop.getName();
            throw DatabindException.from(g, "Infinite recursion (StackOverflowError)", e)
                .prependPath(bean, name);
        }
    }

    /*
    /**********************************************************************
    /* Field serialization methods, 2.x
    /**********************************************************************
     */

    // 28-Oct-2017, tatu: Not yet optimized. Could be, if it seems
    //    commonly useful wrt JsonView filtering
    /**
     * Alternative serialization method that gets called when there is a
     * {@link PropertyFilter} that needs to be called to determine
     * which properties are to be serialized (and possibly how)
     */
    protected void _serializePropertiesFiltered(Object bean, JsonGenerator g,
            SerializerProvider provider, Object filterId)
        throws JacksonException
    {
        final BeanPropertyWriter[] props;
        final PropertyFilter filter = findPropertyFilter(provider, filterId, bean);
        if (_filteredProps != null && provider.getActiveView() != null) {
            props = _filteredProps;
            // better also allow missing filter actually.. Falls down
            if (filter == null) {
                _serializePropertiesMaybeView(bean, g, provider, props);
                return;
            }
        } else {
            props = _props;
            if (filter == null) {
                _serializePropertiesNoView(bean, g, provider, props);
                return;
            }
        }

        int i = 0;
        try {
            for (final int len = props.length; i < len; ++i) {
                BeanPropertyWriter prop = props[i];
                if (prop != null) { // can have nulls in filtered list
                    filter.serializeAsProperty(bean, g, provider, prop);
                }
            }
            if (_anyGetterWriter != null) {
                _anyGetterWriter.getAndFilter(bean, g, provider, filter);
            }
        } catch (Exception e) {
            String name = (i == props.length) ? "[anySetter]" : props[i].getName();
            wrapAndThrow(provider, e, bean, name);
        } catch (StackOverflowError e) {
            // Minimize call depth since we are close to fail:
            final String name = (i == props.length) ? "[anySetter]" : props[i].getName();
            throw DatabindException.from(g, "Infinite recursion (StackOverflowError)", e)
                .prependPath(bean, name);
        }
    }

    protected void _serializeProperties(Object bean, JsonGenerator g, SerializerProvider provider)
        throws JacksonException
    {
        // NOTE: only called from places where FilterId (JsonView) already checked.
        if (_filteredProps != null && provider.getActiveView() != null) {
            _serializePropertiesMaybeView(bean, g, provider, _filteredProps);
        } else {
            _serializePropertiesNoView(bean, g, provider, _props);
        }
    }

    /*
    /**********************************************************************
    /* Introspection (for schema generation etc)
    /**********************************************************************
     */
    
    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
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

    @Override
    public String toString() {
        return getClass().getSimpleName()+" for "+handledType().getName();
    }
}
