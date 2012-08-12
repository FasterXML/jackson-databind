package com.fasterxml.jackson.databind.ser.std;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.ObjectIdGenerator;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.io.SerializedString;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.ObjectIdInfo;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorAware;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonObjectFormatVisitor;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.AnyGetterWriter;
import com.fasterxml.jackson.databind.ser.BeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerBuilder;
import com.fasterxml.jackson.databind.ser.ContainerSerializer;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.ResolvableSerializer;
import com.fasterxml.jackson.databind.ser.impl.ObjectIdWriter;
import com.fasterxml.jackson.databind.ser.impl.PropertyBasedObjectIdGenerator;
import com.fasterxml.jackson.databind.ser.impl.WritableObjectId;
import com.fasterxml.jackson.databind.util.ArrayBuilders;
import com.fasterxml.jackson.databind.util.NameTransformer;

/**
 * Base class both for the standard bean serializer, and couple
 * of variants that only differ in small details.
 * Can be used for custom bean serializers as well, although that
 * is not the primary design goal.
 */
public abstract class BeanSerializerBase
    extends StdSerializer<Object>
    implements ContextualSerializer, ResolvableSerializer,
        JsonFormatVisitorAware
{
    final protected static BeanPropertyWriter[] NO_PROPS = new BeanPropertyWriter[0];

    /*
    /**********************************************************
    /* Configuration
    /**********************************************************
     */

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
     *<p>
     * Note: not final since we need to get contextual instance during
     * resolution.
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
        _props = properties;
        _filteredProps = filteredProperties;
        if (builder == null) { // mostly for testing
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
            JsonFormat.Value format = builder.getBeanDescription().findExpectedFormat(null);
            _serializationShape = (format == null) ? null : format.getShape();
        }
    }

    public BeanSerializerBase(BeanSerializerBase src,
            BeanPropertyWriter[] properties, BeanPropertyWriter[] filteredProperties)
    {
        super(src._handledType);
        _props = properties;
        _filteredProps = filteredProperties;

        _typeId = src._typeId;
        _anyGetterWriter = src._anyGetterWriter;
        _objectIdWriter = src._objectIdWriter;
        _propertyFilterId = src._propertyFilterId;
        _serializationShape = src._serializationShape;
    }

    protected BeanSerializerBase(BeanSerializerBase src, ObjectIdWriter objectIdWriter)
    {
        super(src._handledType);
        _props = src._props;
        _filteredProps = src._filteredProps;
        
        _typeId = src._typeId;
        _anyGetterWriter = src._anyGetterWriter;
        _objectIdWriter = objectIdWriter;
        _propertyFilterId = src._propertyFilterId;
        _serializationShape = src._serializationShape;
    }

    protected BeanSerializerBase(BeanSerializerBase src, String[] toIgnore)
    {
        super(src._handledType);

        // Bit clumsy, but has to do:
        HashSet<String> ignoredSet = ArrayBuilders.arrayToSet(toIgnore);
        final BeanPropertyWriter[] propsIn = src._props;
        final BeanPropertyWriter[] fpropsIn = src._filteredProps;
        final int len = propsIn.length;

        ArrayList<BeanPropertyWriter> propsOut = new ArrayList<BeanPropertyWriter>(len);
        ArrayList<BeanPropertyWriter> fpropsOut = (fpropsIn == null) ? null : new ArrayList<BeanPropertyWriter>(len);

        for (int i = 0; i < len; ++i) {
            BeanPropertyWriter bpw = propsIn[i];
            // should be ignored?
            if (ignoredSet.contains(bpw.getName())) {
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
     * Fluent factory used for creating a new instance with different
     * {@link ObjectIdWriter}.
     * 
     * @since 2.0
     */
    public abstract BeanSerializerBase withObjectIdWriter(ObjectIdWriter objectIdWriter);

    /**
     * Fluent factory used for creating a new instance with additional
     * set of properties to ignore (from properties this instance otherwise has)
     * 
     * @since 2.0
     */
    protected abstract BeanSerializerBase withIgnorals(String[] toIgnore);

    /**
     * Fluent factory for creating a variant that output POJO as a
     * JSON Array. Implementations may ignore this request if output
     * as array is not possible (either at all, or reliably).
     * 
     * @since 2.1
     */
    protected abstract BeanSerializerBase asArraySerializer();
    
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
    /* Post-constriction processing: resolvable, contextual
    /**********************************************************
     */

    /**
     * We need to implement {@link ResolvableSerializer} to be able to
     * properly handle cyclic type references.
     */
//  @Override
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
            // Was the serialization type hard-coded? If so, use it
            JavaType type = prop.getSerializationType();
            
            /* It not, we can use declared return type if and only if
             * declared type is final -- if not, we don't really know
             * the actual type until we get the instance.
             */
            if (type == null) {
                type = provider.constructType(prop.getGenericPropertyType());
                if (!type.isFinal()) {
                    /* 18-Feb-2010, tatus: But even if it is non-final, we may
                     *   need to retain some of type information so that we can
                     *   accurately handle contained types
                     */
                    if (type.isContainerType() || type.containedTypeCount() > 0) {
                        prop.setNonTrivialBaseType(type);
                    }
                    continue;
                }
            }
            
            JsonSerializer<Object> ser = provider.findValueSerializer(type, prop);
            /* 04-Feb-2010, tatu: We may have stashed type serializer for content types
             *   too, earlier; if so, it's time to connect the dots here:
             */
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
            prop.assignSerializer(ser);
            // and maybe replace filtered property too? (see [JACKSON-364])
            if (i < filteredCount) {
                BeanPropertyWriter w2 = _filteredProps[i];
                if (w2 != null) {
                    w2.assignSerializer(ser);
                }
            }
        }

        // also, any-getter may need to be resolved
        if (_anyGetterWriter != null) {
            _anyGetterWriter.resolve(provider);
        }
    }

//  @Override
    public JsonSerializer<?> createContextual(SerializerProvider provider,
            BeanProperty property)
        throws JsonMappingException
    {
        ObjectIdWriter oiw = _objectIdWriter;
        String[] ignorals = null;
        final AnnotationIntrospector intr = provider.getAnnotationIntrospector();
        final AnnotatedMember accessor = (property == null || intr == null)
                ? null : property.getMember();
        
        // First: may have an override for Object Id:
        if (property != null && intr != null) {
            ignorals = intr.findPropertiesToIgnore(accessor);
            final ObjectIdInfo objectIdInfo = intr.findObjectIdInfo(accessor);
            if (objectIdInfo != null) {
                /* Ugh: mostly copied from BeanSerializerBase: but can't easily
                 * change it to be able to move to SerializerProvider (where it
                 * really belongs)
                 */
                ObjectIdGenerator<?> gen;
                Class<?> implClass = objectIdInfo.getGeneratorType();
                JavaType type = provider.constructType(implClass);
                JavaType idType = provider.getTypeFactory().findTypeParameters(type, ObjectIdGenerator.class)[0];
                // Property-based generator is trickier
                if (implClass == ObjectIdGenerators.PropertyGenerator.class) { // most special one, needs extra work
                    String propName = objectIdInfo.getPropertyName();
                    BeanPropertyWriter idProp = null;

                    for (int i = 0, len = _props.length ;; ++i) {
                        if (i == len) {
                            throw new IllegalArgumentException("Invalid Object Id definition for "+_handledType.getName()
                                    +": can not find property with name '"+propName+"'");
                        }
                        BeanPropertyWriter prop = _props[i];
                        if (propName.equals(prop.getName())) {
                            idProp = prop;
                            /* Let's force it to be the first property to output
                             * (although it may still get rearranged etc)
                             */
                            if (i > 0) { // note: must shuffle both regular properties and filtered
                                System.arraycopy(_props, 0, _props, 1, i);
                                _props[0] = idProp;
                                if (_filteredProps != null) {
                                    BeanPropertyWriter fp = _filteredProps[i];
                                    System.arraycopy(_filteredProps, 0, _filteredProps, 1, i);
                                    _filteredProps[0] = fp;
                                }
                            }
                            break;
                        }
                    }
                    idType = idProp.getType();
                    gen = new PropertyBasedObjectIdGenerator(objectIdInfo, idProp);
                    oiw = ObjectIdWriter.construct(idType, null, gen, objectIdInfo.getFirstAsId());
                } else { // other types need to be simpler
                    gen = provider.objectIdGeneratorInstance(accessor, objectIdInfo);
                    oiw = ObjectIdWriter.construct(idType, objectIdInfo.getPropertyName(), gen,
                            objectIdInfo.getFirstAsId());
                }
            }
        }
        // either way, need to resolve serializer:
        BeanSerializerBase contextual = this;
        if (oiw != null) {
            JsonSerializer<?> ser = provider.findValueSerializer(oiw.idType, property);
            oiw = oiw.withSerializer(ser);
            if (oiw != _objectIdWriter) {
                contextual = contextual.withObjectIdWriter(oiw);
            }
        }
        // And possibly add more properties to ignore
        if (ignorals != null && ignorals.length != 0) {
            contextual = contextual.withIgnorals(ignorals);
        }
        // One more thing: are we asked to serialize POJO as array?
        JsonFormat.Shape shape = null;
        if (accessor != null) {
            JsonFormat.Value format = intr.findFormat((Annotated) accessor);

            if (format != null) {
                shape = format.getShape();
            }
        }
        if (shape == null) {
            shape = _serializationShape;
        }
        if (shape == JsonFormat.Shape.ARRAY) {
            contextual = contextual.asArraySerializer();
        }
        return contextual;
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
    public abstract void serialize(Object bean, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonGenerationException;

    // Type-info-augmented case implemented as it does not usually differ between impls
    @Override
    public void serializeWithType(Object bean, JsonGenerator jgen,
            SerializerProvider provider, TypeSerializer typeSer)
        throws IOException, JsonGenerationException
    {
        if (_objectIdWriter != null) {
            _serializeWithObjectId(bean, jgen, provider, typeSer);
            return;
        }

        String typeStr = (_typeId == null) ? null :_customTypeId(bean);
        if (typeStr == null) {
            typeSer.writeTypePrefixForObject(bean, jgen);
        } else {
            typeSer.writeCustomTypePrefixForObject(bean, jgen, typeStr);
        }
        if (_propertyFilterId != null) {
            serializeFieldsFiltered(bean, jgen, provider);
        } else {
            serializeFields(bean, jgen, provider);
        }
        if (typeStr == null) {
            typeSer.writeTypeSuffixForObject(bean, jgen);
        } else {
            typeSer.writeCustomTypeSuffixForObject(bean, jgen, typeStr);
        }
    }

    private final void _serializeWithObjectId(Object bean,
            JsonGenerator jgen, SerializerProvider provider,
            TypeSerializer typeSer)
        throws IOException, JsonGenerationException
    {
        final ObjectIdWriter w = _objectIdWriter;
        WritableObjectId oid = provider.findObjectId(bean, w.generator);
        Object id = oid.id;
        
        if (id != null) { // have seen before; serialize just id
            oid.serializer.serialize(id, jgen, provider);
            return;
        }
        // if not, bit more work:
        oid.serializer = w.serializer;
        oid.id = id = oid.generator.generateId(bean);
        
        String typeStr = (_typeId == null) ? null :_customTypeId(bean);
        if (typeStr == null) {
            typeSer.writeTypePrefixForObject(bean, jgen);
        } else {
            typeSer.writeCustomTypePrefixForObject(bean, jgen, typeStr);
        }

        // Very first thing: inject the id property
        SerializedString name = w.propertyName;
        if (name != null) {
            jgen.writeFieldName(name);
            w.serializer.serialize(id, jgen, provider);
        }

        if (_propertyFilterId != null) {
            serializeFieldsFiltered(bean, jgen, provider);
        } else {
            serializeFields(bean, jgen, provider);
        }
        if (typeStr == null) {
            typeSer.writeTypeSuffixForObject(bean, jgen);
        } else {
            typeSer.writeCustomTypeSuffixForObject(bean, jgen, typeStr);
        }
    }
    
    private final String _customTypeId(Object bean)
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

    protected void serializeFields(Object bean, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonGenerationException
    {
        final BeanPropertyWriter[] props;
        if (_filteredProps != null && provider.getSerializationView() != null) {
            props = _filteredProps;
        } else {
            props = _props;
        }
        int i = 0;
        try {
            for (final int len = props.length; i < len; ++i) {
                BeanPropertyWriter prop = props[i];
                if (prop != null) { // can have nulls in filtered list
                    prop.serializeAsField(bean, jgen, provider);
                }
            }
            if (_anyGetterWriter != null) {
                _anyGetterWriter.getAndSerialize(bean, jgen, provider);
            }
        } catch (Exception e) {
            String name = (i == props.length) ? "[anySetter]" : props[i].getName();
            wrapAndThrow(provider, e, bean, name);
        } catch (StackOverflowError e) {
            /* 04-Sep-2009, tatu: Dealing with this is tricky, since we do not
             *   have many stack frames to spare... just one or two; can't
             *   make many calls.
             */
            JsonMappingException mapE = new JsonMappingException("Infinite recursion (StackOverflowError)", e);
            String name = (i == props.length) ? "[anySetter]" : props[i].getName();
            mapE.prependPath(new JsonMappingException.Reference(bean, name));
            throw mapE;
        }
    }

    /**
     * Alternative serialization method that gets called when there is a
     * {@link BeanPropertyFilter} that needs to be called to determine
     * which properties are to be serialized (and possibly how)
     */
    protected void serializeFieldsFiltered(Object bean, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonGenerationException
    {
        /* note: almost verbatim copy of "serializeFields"; copied (instead of merged)
         * so that old method need not add check for existence of filter.
         */
        
        final BeanPropertyWriter[] props;
        if (_filteredProps != null && provider.getSerializationView() != null) {
            props = _filteredProps;
        } else {
            props = _props;
        }
        final BeanPropertyFilter filter = findFilter(provider);
        // better also allow missing filter actually..
        if (filter == null) {
            serializeFields(bean, jgen, provider);
            return;
        }
        
        int i = 0;
        try {
            for (final int len = props.length; i < len; ++i) {
                BeanPropertyWriter prop = props[i];
                if (prop != null) { // can have nulls in filtered list
                    filter.serializeAsField(bean, jgen, provider, prop);
                }
            }
            if (_anyGetterWriter != null) {
                _anyGetterWriter.getAndSerialize(bean, jgen, provider);
            }
        } catch (Exception e) {
            String name = (i == props.length) ? "[anySetter]" : props[i].getName();
            wrapAndThrow(provider, e, bean, name);
        } catch (StackOverflowError e) {
            JsonMappingException mapE = new JsonMappingException("Infinite recursion (StackOverflowError)", e);
            String name = (i == props.length) ? "[anySetter]" : props[i].getName();
            mapE.prependPath(new JsonMappingException.Reference(bean, name));
            throw mapE;
        }
    }

    /**
     * Helper method used to locate filter that is needed, based on filter id
     * this serializer was constructed with.
     */
    protected BeanPropertyFilter findFilter(SerializerProvider provider)
        throws JsonMappingException
    {
        final Object filterId = _propertyFilterId;
        FilterProvider filters = provider.getFilterProvider();
        // Not ok to miss the provider, if a filter is declared to be needed.
        if (filters == null) {
            throw new JsonMappingException("Can not resolve BeanPropertyFilter with id '"+filterId+"'; no FilterProvider configured");
        }
        BeanPropertyFilter filter = filters.findFilter(filterId);
        // But whether unknown ids are ok just depends on filter provider; if we get null that's fine
        return filter;
    }
    
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
    {
    	//deposit your output format 
    	JsonObjectFormatVisitor objectVisitor = visitor.expectObjectFormat(typeHint);
 
        if (_propertyFilterId != null) {
        	try {
        		BeanPropertyFilter filter = findFilter(visitor.getProvider());
				for (int i = 0; i < _props.length; i++) {
		            BeanPropertyWriter prop = _props[i];
		            filter.depositSchemaProperty(prop, objectVisitor, visitor.getProvider());
		        }
				return;
			} catch (JsonMappingException e) {
				// TODO Auto-generated catch block

			}
        } 
        		
        for (int i = 0; i < _props.length; i++) {
            BeanPropertyWriter prop = _props[i];

            JavaType propType = prop.getSerializationType();
            BeanSerializerBase.depositSchemaProperty(prop, objectVisitor);
        }
    }

    /**
	 * 	Attempt to add the output of the given {@link BeanPropertyWriter} in the given {@link ObjectNode}.
	 * 	Otherwise, add the default schema {@link JsonNode} in place of the writer's output
	 * 
	 * @param writer Bean property serializer to use to create schema value
     * @param propertiesNode Node which the given property would exist within
	 */
	public static void depositSchemaProperty(BeanPropertyWriter writer, JsonObjectFormatVisitor objectVisitor) {
		if (isPropertyRequired(writer, objectVisitor.getProvider())) {
			objectVisitor.property(writer); 
		} else {
			objectVisitor.optionalProperty(writer);
		}
	}

	/**
     * Determines if a bean property is required, as determined by
     * {@link com.fasterxml.jackson.databind.AnnotationIntrospector#hasRequiredMarker}.
     *<p>
     * 
     * 
     * @param prop the bean property.
     * @return true if the property is optional, false otherwise.
     */
    public static boolean isPropertyRequired(final BeanPropertyWriter prop, final SerializerProvider provider) {
        Boolean value = provider.getAnnotationIntrospector().hasRequiredMarker(prop.getMember());
        return (value == null) ? false : value.booleanValue();
    }
    

}
