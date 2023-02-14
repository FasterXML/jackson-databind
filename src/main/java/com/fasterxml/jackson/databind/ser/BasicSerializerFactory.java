package com.fasterxml.jackson.databind.ser;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.cfg.SerializerFactoryConfig;
import com.fasterxml.jackson.databind.ext.OptionalHandlerFactory;
import com.fasterxml.jackson.databind.introspect.*;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.impl.*;
import com.fasterxml.jackson.databind.ser.std.*;
import com.fasterxml.jackson.databind.type.*;
import com.fasterxml.jackson.databind.util.*;

/**
 * Factory class that can provide serializers for standard JDK classes,
 * as well as custom classes that extend standard classes or implement
 * one of "well-known" interfaces (such as {@link java.util.Collection}).
 *<p>
 * Since all the serializers are eagerly instantiated, and there is
 * no additional introspection or customizability of these types,
 * this factory is essentially stateless.
 */
@SuppressWarnings("serial")
public abstract class BasicSerializerFactory
    extends SerializerFactory
    implements java.io.Serializable
{
    /*
    /**********************************************************
    /* Configuration, lookup tables/maps
    /**********************************************************
     */

    /**
     * Since these are all JDK classes, we shouldn't have to worry
     * about ClassLoader used to load them. Rather, we can just
     * use the class name, and keep things simple and efficient.
     */
    protected final static HashMap<String, JsonSerializer<?>> _concrete;

    /**
     * Actually it may not make much sense to eagerly instantiate all
     * kinds of serializers: so this Map actually contains class references,
     * not instances
     */
    protected final static HashMap<String, Class<? extends JsonSerializer<?>>> _concreteLazy;

    static {
        HashMap<String, Class<? extends JsonSerializer<?>>> concLazy
            = new HashMap<String, Class<? extends JsonSerializer<?>>>();
        HashMap<String, JsonSerializer<?>> concrete
            = new HashMap<String, JsonSerializer<?>>();


        /* String and string-like types (note: date types explicitly
         * not included -- can use either textual or numeric serialization)
         */
        concrete.put(String.class.getName(), new StringSerializer());
        final ToStringSerializer sls = ToStringSerializer.instance;
        concrete.put(StringBuffer.class.getName(), sls);
        concrete.put(StringBuilder.class.getName(), sls);
        concrete.put(Character.class.getName(), sls);
        concrete.put(Character.TYPE.getName(), sls);

        // Primitives/wrappers for primitives (primitives needed for Beans)
        NumberSerializers.addAll(concrete);
        concrete.put(Boolean.TYPE.getName(), new BooleanSerializer(true));
        concrete.put(Boolean.class.getName(), new BooleanSerializer(false));

        // Other numbers, more complicated
        concrete.put(BigInteger.class.getName(), new NumberSerializer(BigInteger.class));
        concrete.put(BigDecimal.class.getName(),new NumberSerializer(BigDecimal.class));

        // Other discrete non-container types:
        // First, Date/Time zoo:
        concrete.put(Calendar.class.getName(), CalendarSerializer.instance);
        concrete.put(java.util.Date.class.getName(), DateSerializer.instance);

        // And then other standard non-structured JDK types
        for (Map.Entry<Class<?>,Object> en : StdJdkSerializers.all()) {
            Object value = en.getValue();
            if (value instanceof JsonSerializer<?>) {
                concrete.put(en.getKey().getName(), (JsonSerializer<?>) value);
            } else {
                @SuppressWarnings("unchecked")
                Class<? extends JsonSerializer<?>> cls = (Class<? extends JsonSerializer<?>>) value;
                concLazy.put(en.getKey().getName(), cls);
            }
        }

        // Jackson-specific type(s)
        // (Q: can this ever be sub-classed?)
        concLazy.put(TokenBuffer.class.getName(), TokenBufferSerializer.class);

        _concrete = concrete;
        _concreteLazy = concLazy;
    }

    /*
    /**********************************************************
    /* Configuration
    /**********************************************************
     */

    /**
     * Configuration settings for this factory; immutable instance (just like this
     * factory), new version created via copy-constructor (fluent-style)
     */
    protected final SerializerFactoryConfig _factoryConfig;

    /*
    /**********************************************************
    /* Life cycle
    /**********************************************************
     */

    /**
     * We will provide default constructor to allow sub-classing,
     * but make it protected so that no non-singleton instances of
     * the class will be instantiated.
     */
    protected BasicSerializerFactory(SerializerFactoryConfig config) {
        _factoryConfig = (config == null) ? new SerializerFactoryConfig() : config;
    }

    /**
     * Method for getting current {@link SerializerFactoryConfig}.
      *<p>
     * Note that since instances are immutable, you can NOT change settings
     * by accessing an instance and calling methods: this will simply create
     * new instance of config object.
     */
    public SerializerFactoryConfig getFactoryConfig() {
        return _factoryConfig;
    }

    /**
     * Method used for creating a new instance of this factory, but with different
     * configuration. Reason for specifying factory method (instead of plain constructor)
     * is to allow proper sub-classing of factories.
     *<p>
     * Note that custom sub-classes generally <b>must override</b> implementation
     * of this method, as it usually requires instantiating a new instance of
     * factory type. Check out javadocs for
     * {@link com.fasterxml.jackson.databind.ser.BeanSerializerFactory} for more details.
     */
    public abstract SerializerFactory withConfig(SerializerFactoryConfig config);

    /**
     * Convenience method for creating a new factory instance with an additional
     * serializer provider.
     */
    @Override
    public final SerializerFactory withAdditionalSerializers(Serializers additional) {
        return withConfig(_factoryConfig.withAdditionalSerializers(additional));
    }

    /**
     * Convenience method for creating a new factory instance with an additional
     * key serializer provider.
     */
    @Override
    public final SerializerFactory withAdditionalKeySerializers(Serializers additional) {
        return withConfig(_factoryConfig.withAdditionalKeySerializers(additional));
    }

    /**
     * Convenience method for creating a new factory instance with additional bean
     * serializer modifier.
     */
    @Override
    public final SerializerFactory withSerializerModifier(BeanSerializerModifier modifier) {
        return withConfig(_factoryConfig.withSerializerModifier(modifier));
    }

    /*
    /**********************************************************
    /* SerializerFactory impl
    /**********************************************************
     */

    // Implemented by sub-classes
    @Override
    public abstract JsonSerializer<Object> createSerializer(SerializerProvider prov,
            JavaType type)
        throws JsonMappingException;

    @Override // since 2.11
    @SuppressWarnings("unchecked")
    public JsonSerializer<Object> createKeySerializer(SerializerProvider ctxt,
            JavaType keyType, JsonSerializer<Object> defaultImpl) throws JsonMappingException
    {
        // 16-Oct-2019, tatu: use to only introspect class annotations but we'll
        //    need methods too for `@JsonValue` later; and custom lookup might want it, too
        final SerializationConfig config = ctxt.getConfig();
        BeanDescription beanDesc = config.introspect(keyType);
        JsonSerializer<?> ser = null;
        // Minor optimization: to avoid constructing beanDesc, bail out if none registered
        if (_factoryConfig.hasKeySerializers()) {
            // Only thing we have here are module-provided key serializers:
            for (Serializers serializers : _factoryConfig.keySerializers()) {
                ser = serializers.findSerializer(config, keyType, beanDesc);
                if (ser != null) {
                    break;
                }
            }
        }
        if (ser == null) {
            // [databind#2452]: Support `@JsonSerialize(keyUsing = ...)` -- new in 2.11
            ser = _findKeySerializer(ctxt, beanDesc.getClassInfo());
            if (ser == null) {
                // 02-Nov-2020, tatu: Handling of "default key serializer" is bit convoluted
                //   (will be removed from 3.0) but for now it apparently needs to override
                //   standard handlers so (for backwards compatibility)
                ser = defaultImpl;
                if (ser == null) {
                    // Beyond that, we can check standard serializers as they are only for
                    // JDK types and should not interfere with other annotations
                    ser = StdKeySerializers.getStdKeySerializer(config, keyType.getRawClass(), false);
                    if (ser == null) {
                        AnnotatedMember acc = beanDesc.findJsonKeyAccessor();
                        if (acc == null) {
                            // As per [databind#47], also need to support @JsonValue
                            acc = beanDesc.findJsonValueAccessor();
                        }
                        if (acc != null) {
                            JsonSerializer<?> delegate = createKeySerializer(ctxt, acc.getType(), defaultImpl);
                            if (config.canOverrideAccessModifiers()) {
                                ClassUtil.checkAndFixAccess(acc.getMember(),
                                        config.isEnabled(MapperFeature.OVERRIDE_PUBLIC_ACCESS_MODIFIERS));
                            }
                            // null -> no TypeSerializer for key-serializer use case
                            ser = new JsonValueSerializer(acc, null, delegate);
                        } else {
                            ser = StdKeySerializers.getFallbackKeySerializer(config, keyType.getRawClass());
                        }
                    }
                }
            }
        }

        // [databind#120]: Allow post-processing
        if (_factoryConfig.hasSerializerModifiers()) {
            for (BeanSerializerModifier mod : _factoryConfig.serializerModifiers()) {
                ser = mod.modifyKeySerializer(config, keyType, beanDesc, ser);
            }
        }
        return (JsonSerializer<Object>) ser;
    }

    @Override
    @SuppressWarnings("unchecked")
    @Deprecated // since 2.11
    public JsonSerializer<Object> createKeySerializer(SerializationConfig config,
            JavaType keyType, JsonSerializer<Object> defaultImpl)
    {
        BeanDescription beanDesc = config.introspect(keyType);
        JsonSerializer<?> ser = null;
        if (_factoryConfig.hasKeySerializers()) {
            for (Serializers serializers : _factoryConfig.keySerializers()) {
                ser = serializers.findSerializer(config, keyType, beanDesc);
                if (ser != null) {
                    break;
                }
            }
        }
        if (ser == null) {
            ser = defaultImpl;
            if (ser == null) {
                ser = StdKeySerializers.getStdKeySerializer(config, keyType.getRawClass(), false);
                if (ser == null) {
                    ser = StdKeySerializers.getFallbackKeySerializer(config, keyType.getRawClass());
                }
            }
        }
        if (_factoryConfig.hasSerializerModifiers()) {
            for (BeanSerializerModifier mod : _factoryConfig.serializerModifiers()) {
                ser = mod.modifyKeySerializer(config, keyType, beanDesc, ser);
            }
        }
        return (JsonSerializer<Object>) ser;
    }

    /**
     * Method called to construct a type serializer for values with given declared
     * base type. This is called for values other than those of bean property
     * types.
     */
    @Override
    public TypeSerializer createTypeSerializer(SerializationConfig config,
            JavaType baseType)
    {
        BeanDescription bean = config.introspectClassAnnotations(baseType.getRawClass());
        AnnotatedClass ac = bean.getClassInfo();
        AnnotationIntrospector ai = config.getAnnotationIntrospector();
        TypeResolverBuilder<?> b = ai.findTypeResolver(config, ac, baseType);
        /* Ok: if there is no explicit type info handler, we may want to
         * use a default. If so, config object knows what to use.
         */
        Collection<NamedType> subtypes = null;
        if (b == null) {
            b = config.getDefaultTyper(baseType);
        } else {
            subtypes = config.getSubtypeResolver().collectAndResolveSubtypesByClass(config, ac);
        }
        if (b == null) {
            return null;
        }
        // 10-Jun-2015, tatu: Since not created for Bean Property, no need for post-processing
        //    wrt EXTERNAL_PROPERTY
        return b.buildTypeSerializer(config, baseType, subtypes);
    }

    /*
    /**********************************************************
    /* Additional API for other core classes
    /**********************************************************
     */

    protected abstract Iterable<Serializers> customSerializers();

    /*
    /**********************************************************
    /* Overridable secondary serializer accessor methods
    /**********************************************************
     */

    /**
     * Method that will use fast lookup (and identity comparison) methods to
     * see if we know serializer to use for given type.
     */
    protected final JsonSerializer<?> findSerializerByLookup(JavaType type,
            SerializationConfig config, BeanDescription beanDesc,
            boolean staticTyping)
    {
        Class<?> raw = type.getRawClass();
        String clsName = raw.getName();
        JsonSerializer<?> ser = _concrete.get(clsName);
        if (ser == null) {
            Class<? extends JsonSerializer<?>> serClass = _concreteLazy.get(clsName);
            if (serClass != null) {
                // 07-Jan-2017, tatu: Should never fail (since we control constructors),
                //   but if it does will throw `IllegalArgumentException` with description,
                //   which we could catch, re-title.
                return ClassUtil.createInstance(serClass, false);
            }
        }
        return ser;
    }

    /**
     * Method called to see if one of primary per-class annotations
     * (or related, like implementing of {@link JsonSerializable})
     * determines the serializer to use.
     *<p>
     * Currently handles things like:
     *<ul>
     * <li>If type implements {@link JsonSerializable}, use that
     *  </li>
     * <li>If type has {@link com.fasterxml.jackson.annotation.JsonValue} annotation (or equivalent), build serializer
     *    based on that property
     *  </li>
     *</ul>
     *
     * @since 2.0
     */
    protected final JsonSerializer<?> findSerializerByAnnotations(SerializerProvider prov,
            JavaType type, BeanDescription beanDesc)
        throws JsonMappingException
    {
        Class<?> raw = type.getRawClass();
        // First: JsonSerializable?
        if (JsonSerializable.class.isAssignableFrom(raw)) {
            return SerializableSerializer.instance;
        }
        // Second: @JsonValue for any type
        AnnotatedMember valueAccessor = beanDesc.findJsonValueAccessor();
        if (valueAccessor != null) {
            if (prov.canOverrideAccessModifiers()) {
                ClassUtil.checkAndFixAccess(valueAccessor.getMember(),
                        prov.isEnabled(MapperFeature.OVERRIDE_PUBLIC_ACCESS_MODIFIERS));
            }
            final JavaType valueType = valueAccessor.getType();
            JsonSerializer<Object> valueSerializer = findSerializerFromAnnotation(prov, valueAccessor);
            if (valueSerializer == null) {
                valueSerializer = valueType.getValueHandler();
            }
            TypeSerializer typeSerializer = valueType.getTypeHandler();
            if (typeSerializer == null) {
                typeSerializer = createTypeSerializer(prov.getConfig(), valueType);
            }
            return new JsonValueSerializer(valueAccessor, typeSerializer, valueSerializer);
        }
        // No well-known annotations...
        return null;
    }

    /**
     * Method for checking if we can determine serializer to use based on set of
     * known primary types, checking for set of known base types (exact matches
     * having been compared against with <code>findSerializerByLookup</code>).
     * This does not include "secondary" interfaces, but
     * mostly concrete or abstract base classes.
     */
    protected final JsonSerializer<?> findSerializerByPrimaryType(SerializerProvider prov,
            JavaType type, BeanDescription beanDesc,
            boolean staticTyping)
        throws JsonMappingException
    {
        if (type.isEnumType()) {
            return buildEnumSerializer(prov.getConfig(), type, beanDesc);
        }

        final Class<?> raw = type.getRawClass();
        // Then check for optional/external serializers
        JsonSerializer<?> ser = findOptionalStdSerializer(prov, type, beanDesc, staticTyping);
        if (ser != null) {
            return ser;
        }

        if (Calendar.class.isAssignableFrom(raw)) {
            return CalendarSerializer.instance;
        }
        if (java.util.Date.class.isAssignableFrom(raw)) {
            return DateSerializer.instance;
        }
        if (Map.Entry.class.isAssignableFrom(raw)) {
            // 18-Oct-2015, tatu: With 2.7, need to dig type info:
            JavaType mapEntryType = type.findSuperType(Map.Entry.class);

            // 28-Apr-2015, tatu: TypeFactory does it all for us already so
            JavaType kt = mapEntryType.containedTypeOrUnknown(0);
            JavaType vt = mapEntryType.containedTypeOrUnknown(1);
            return buildMapEntrySerializer(prov, type, beanDesc, staticTyping, kt, vt);
        }
        if (ByteBuffer.class.isAssignableFrom(raw)) {
            return new ByteBufferSerializer();
        }
        if (InetAddress.class.isAssignableFrom(raw)) {
            return new InetAddressSerializer();
        }
        if (InetSocketAddress.class.isAssignableFrom(raw)) {
            return new InetSocketAddressSerializer();
        }
        if (TimeZone.class.isAssignableFrom(raw)) {
            return new TimeZoneSerializer();
        }
        if (java.nio.charset.Charset.class.isAssignableFrom(raw)) {
            return ToStringSerializer.instance;
        }
        if (Number.class.isAssignableFrom(raw)) {
            // 21-May-2014, tatu: Couple of alternatives actually
            JsonFormat.Value format = beanDesc.findExpectedFormat(null);
            switch (format.getShape()) {
            case STRING:
                return ToStringSerializer.instance;
            case OBJECT: // need to bail out to let it be serialized as POJO
            case ARRAY: // or, I guess ARRAY; otherwise no point in speculating
                return null;
            default:
            }
            return NumberSerializer.instance;
        }
        // 23-Apr-2021, tatu: [databind#3130]: Suppress ClassLoader...
        if (ClassLoader.class.isAssignableFrom(raw)) {
            return new ToEmptyObjectSerializer(type);
        }
        return null;
    }

    /**
     * Overridable method called after checking all other types.
     *
     * @since 2.2
     */
    protected JsonSerializer<?> findOptionalStdSerializer(SerializerProvider prov,
            JavaType type, BeanDescription beanDesc, boolean staticTyping)
        throws JsonMappingException
    {
        return OptionalHandlerFactory.instance.findSerializer(prov.getConfig(), type, beanDesc);
    }

    /**
     * Reflection-based serialized find method, which checks if
     * given class implements one of recognized "add-on" interfaces.
     * Add-on here means a role that is usually or can be a secondary
     * trait: for example,
     * bean classes may implement {@link Iterable}, but their main
     * function is usually something else. The reason for
     */
    protected final JsonSerializer<?> findSerializerByAddonType(SerializationConfig config,
            JavaType javaType, BeanDescription beanDesc, boolean staticTyping) throws JsonMappingException
    {
        Class<?> rawType = javaType.getRawClass();

        if (Iterator.class.isAssignableFrom(rawType)) {
            JavaType[] params = config.getTypeFactory().findTypeParameters(javaType, Iterator.class);
            JavaType vt = (params == null || params.length != 1) ?
                    TypeFactory.unknownType() : params[0];
            return buildIteratorSerializer(config, javaType, beanDesc, staticTyping, vt);
        }
        if (Iterable.class.isAssignableFrom(rawType)) {
            JavaType[] params = config.getTypeFactory().findTypeParameters(javaType, Iterable.class);
            JavaType vt = (params == null || params.length != 1) ?
                    TypeFactory.unknownType() : params[0];
            return buildIterableSerializer(config, javaType, beanDesc,  staticTyping, vt);
        }
        if (CharSequence.class.isAssignableFrom(rawType)) {
            return ToStringSerializer.instance;
        }
        return null;
    }

    /**
     * Helper method called to check if a class or method
     * has an annotation
     * (@link com.fasterxml.jackson.databind.annotation.JsonSerialize#using)
     * that tells the class to use for serialization.
     * Returns null if no such annotation found.
     */
    @SuppressWarnings("unchecked")
    protected JsonSerializer<Object> findSerializerFromAnnotation(SerializerProvider prov,
            Annotated a)
        throws JsonMappingException
    {
        Object serDef = prov.getAnnotationIntrospector().findSerializer(a);
        if (serDef == null) {
            return null;
        }
        JsonSerializer<Object> ser = prov.serializerInstance(a, serDef);
        // One more thing however: may need to also apply a converter:
        return (JsonSerializer<Object>) findConvertingSerializer(prov, a, ser);
    }

    /**
     * Helper method that will check whether given annotated entity (usually class,
     * but may also be a property accessor) indicates that a {@link Converter} is to
     * be used; and if so, to construct and return suitable serializer for it.
     * If not, will simply return given serializer as is.
     */
    protected JsonSerializer<?> findConvertingSerializer(SerializerProvider prov,
            Annotated a, JsonSerializer<?> ser)
        throws JsonMappingException
    {
        Converter<Object,Object> conv = findConverter(prov, a);
        if (conv == null) {
            return ser;
        }
        JavaType delegateType = conv.getOutputType(prov.getTypeFactory());
        return new StdDelegatingSerializer(conv, delegateType, ser);
    }

    protected Converter<Object,Object> findConverter(SerializerProvider prov,
            Annotated a)
        throws JsonMappingException
    {
        Object convDef = prov.getAnnotationIntrospector().findSerializationConverter(a);
        if (convDef == null) {
            return null;
        }
        return prov.converterInstance(a, convDef);
    }

    /*
    /**********************************************************
    /* Factory methods, container types:
    /**********************************************************
     */

    /**
     * @since 2.1
     */
    protected JsonSerializer<?> buildContainerSerializer(SerializerProvider prov,
            JavaType type, BeanDescription beanDesc, boolean staticTyping)
        throws JsonMappingException
    {
        final SerializationConfig config = prov.getConfig();

        /* [databind#23], 15-Mar-2013, tatu: must force static handling of root value type,
         *   with just one important exception: if value type is "untyped", let's
         *   leave it as is; no clean way to make it work.
         */
        if (!staticTyping && type.useStaticType()) {
            if (!type.isContainerType() || !type.getContentType().isJavaLangObject()) {
                staticTyping = true;
            }
        }

        // Let's see what we can learn about element/content/value type, type serializer for it:
        JavaType elementType = type.getContentType();
        TypeSerializer elementTypeSerializer = createTypeSerializer(config,
                elementType);

        // if elements have type serializer, cannot force static typing:
        if (elementTypeSerializer != null) {
            staticTyping = false;
        }
        JsonSerializer<Object> elementValueSerializer = _findContentSerializer(prov,
                beanDesc.getClassInfo());
        if (type.isMapLikeType()) { // implements java.util.Map
            MapLikeType mlt = (MapLikeType) type;
            /* 29-Sep-2012, tatu: This is actually too early to (try to) find
             *  key serializer from property annotations, and can lead to caching
             *  issues (see [databind#75]). Instead, must be done from 'createContextual()' call.
             *  But we do need to check class annotations.
             */
            JsonSerializer<Object> keySerializer = _findKeySerializer(prov, beanDesc.getClassInfo());
            if (mlt instanceof MapType) {
                return buildMapSerializer(prov, (MapType) mlt, beanDesc, staticTyping,
                        keySerializer, elementTypeSerializer, elementValueSerializer);
            }
            // With Map-like, just 2 options: (1) Custom, (2) Annotations
            JsonSerializer<?> ser = null;
            MapLikeType mlType = (MapLikeType) type;
            for (Serializers serializers : customSerializers()) { // (1) Custom
                ser = serializers.findMapLikeSerializer(config,
                        mlType, beanDesc, keySerializer, elementTypeSerializer, elementValueSerializer);
                if (ser != null) {
                    break;
                }
            }
            if (ser == null) { // (2) Annotations-based ones:
                ser = findSerializerByAnnotations(prov, type, beanDesc);
            }
            if (ser != null) {
                if (_factoryConfig.hasSerializerModifiers()) {
                    for (BeanSerializerModifier mod : _factoryConfig.serializerModifiers()) {
                        ser = mod.modifyMapLikeSerializer(config, mlType, beanDesc, ser);
                    }
                }
            }
            return ser;
        }
        if (type.isCollectionLikeType()) {
            CollectionLikeType clt = (CollectionLikeType) type;
            if (clt instanceof CollectionType) {
                return buildCollectionSerializer(prov,  (CollectionType) clt, beanDesc, staticTyping,
                        elementTypeSerializer, elementValueSerializer);
            }
            // With Map-like, just 2 options: (1) Custom, (2) Annotations
            JsonSerializer<?> ser = null;
            CollectionLikeType clType = (CollectionLikeType) type;
            for (Serializers serializers : customSerializers()) { // (1) Custom
                ser = serializers.findCollectionLikeSerializer(config,
                        clType, beanDesc, elementTypeSerializer, elementValueSerializer);
                if (ser != null) {
                    break;
                }
            }
            if (ser == null) { // (2) Annotations-based ones:
                ser = findSerializerByAnnotations(prov, type, beanDesc);
            }
            if (ser != null) {
                if (_factoryConfig.hasSerializerModifiers()) {
                    for (BeanSerializerModifier mod : _factoryConfig.serializerModifiers()) {
                        ser = mod.modifyCollectionLikeSerializer(config, clType, beanDesc, ser);
                    }
                }
            }
            return ser;
        }
        if (type.isArrayType()) {
            return buildArraySerializer(prov, (ArrayType) type, beanDesc, staticTyping,
                    elementTypeSerializer, elementValueSerializer);
        }
        return null;
    }

    /**
     * Helper method that handles configuration details when constructing serializers for
     * {@link java.util.List} types that support efficient by-index access
     *
     * @since 2.1
     */
    protected JsonSerializer<?> buildCollectionSerializer(SerializerProvider prov,
            CollectionType type, BeanDescription beanDesc, boolean staticTyping,
            TypeSerializer elementTypeSerializer, JsonSerializer<Object> elementValueSerializer)
        throws JsonMappingException
    {
        SerializationConfig config = prov.getConfig();
        JsonSerializer<?> ser = null;
        // Order of lookups:
        // 1. Custom serializers
        // 2. Annotations (@JsonValue, @JsonDeserialize)
        // 3. Defaults
        for (Serializers serializers : customSerializers()) { // (1) Custom
            ser = serializers.findCollectionSerializer(config,
                    type, beanDesc, elementTypeSerializer, elementValueSerializer);
            if (ser != null) {
                break;
            }
        }

        if (ser == null) {
            ser = findSerializerByAnnotations(prov, type, beanDesc); // (2) Annotations
            if (ser == null) {
                // We may also want to use serialize Collections "as beans", if (and only if)
                // this is specified with `@JsonFormat(shape=Object)`
                JsonFormat.Value format = beanDesc.findExpectedFormat(null);
                if (format.getShape() == JsonFormat.Shape.OBJECT) {
                    return null;
                }
                Class<?> raw = type.getRawClass();
                if (EnumSet.class.isAssignableFrom(raw)) {
                    // this may or may not be available (Class doesn't; type of field/method does)
                    JavaType enumType = type.getContentType();
                    // and even if nominally there is something, only use if it really is enum
                    if (!enumType.isEnumImplType()) { // usually since it's `Enum.class`
                        enumType = null;
                    }
                    ser = buildEnumSetSerializer(enumType);
                } else {
                    Class<?> elementRaw = type.getContentType().getRawClass();
                    if (isIndexedList(raw)) {
                        if (elementRaw == String.class) {
                            // [JACKSON-829] Must NOT use if we have custom serializer
                            if (ClassUtil.isJacksonStdImpl(elementValueSerializer)) {
                                ser = IndexedStringListSerializer.instance;
                            }
                        } else {
                            ser = buildIndexedListSerializer(type.getContentType(), staticTyping,
                                elementTypeSerializer, elementValueSerializer);
                        }
                    } else if (elementRaw == String.class) {
                        // [JACKSON-829] Must NOT use if we have custom serializer
                        if (ClassUtil.isJacksonStdImpl(elementValueSerializer)) {
                            ser = StringCollectionSerializer.instance;
                        }
                    }
                    if (ser == null) {
                        ser = buildCollectionSerializer(type.getContentType(), staticTyping,
                                elementTypeSerializer, elementValueSerializer);
                    }
                }
            }
        }
        // [databind#120]: Allow post-processing
        if (_factoryConfig.hasSerializerModifiers()) {
            for (BeanSerializerModifier mod : _factoryConfig.serializerModifiers()) {
                ser = mod.modifyCollectionSerializer(config, type, beanDesc, ser);
            }
        }
        return ser;
    }

    /*
    /**********************************************************
    /* Factory methods, for Collections
    /**********************************************************
     */

    protected boolean isIndexedList(Class<?> cls)
    {
        return RandomAccess.class.isAssignableFrom(cls);
    }

    public  ContainerSerializer<?> buildIndexedListSerializer(JavaType elemType,
            boolean staticTyping, TypeSerializer vts, JsonSerializer<Object> valueSerializer) {
        return new IndexedListSerializer(elemType, staticTyping, vts, valueSerializer);
    }

    public ContainerSerializer<?> buildCollectionSerializer(JavaType elemType,
            boolean staticTyping, TypeSerializer vts, JsonSerializer<Object> valueSerializer) {
        return new CollectionSerializer(elemType, staticTyping, vts, valueSerializer);
    }

    public JsonSerializer<?> buildEnumSetSerializer(JavaType enumType) {
        return new EnumSetSerializer(enumType);
    }

    /*
    /**********************************************************
    /* Factory methods, for Maps
    /**********************************************************
     */

    /**
     * Helper method that handles configuration details when constructing serializers for
     * {@link java.util.Map} types.
     */
    protected JsonSerializer<?> buildMapSerializer(SerializerProvider prov,
            MapType type, BeanDescription beanDesc,
            boolean staticTyping, JsonSerializer<Object> keySerializer,
            TypeSerializer elementTypeSerializer, JsonSerializer<Object> elementValueSerializer)
        throws JsonMappingException
    {
        // [databind#467]: This is where we could allow serialization "as POJO": But! It's
        // nasty to undo, and does not apply on per-property basis. So, hardly optimal
        JsonFormat.Value format = beanDesc.findExpectedFormat(null);
        if (format.getShape() == JsonFormat.Shape.OBJECT) {
            return null;
        }

        JsonSerializer<?> ser = null;

        // Order of lookups:
        // 1. Custom serializers
        // 2. Annotations (@JsonValue, @JsonDeserialize)
        // 3. Defaults

        final SerializationConfig config = prov.getConfig();
        for (Serializers serializers : customSerializers()) { // (1) Custom
            ser = serializers.findMapSerializer(config, type, beanDesc,
                    keySerializer, elementTypeSerializer, elementValueSerializer);
            if (ser != null) { break; }
        }
        if (ser == null) {
            ser = findSerializerByAnnotations(prov, type, beanDesc); // (2) Annotations
            if (ser == null) {
                Object filterId = findFilterId(config, beanDesc);
                // 01-May-2016, tatu: Which base type to use here gets tricky, since
                //   most often it ought to be `Map` or `EnumMap`, but due to abstract
                //   mapping it will more likely be concrete type like `HashMap`.
                //   So, for time being, just pass `Map.class`
                JsonIgnoreProperties.Value ignorals = config.getDefaultPropertyIgnorals(Map.class,
                        beanDesc.getClassInfo());
                Set<String> ignored = (ignorals == null) ? null
                        : ignorals.findIgnoredForSerialization();
                JsonIncludeProperties.Value inclusions = config.getDefaultPropertyInclusions(Map.class,
                        beanDesc.getClassInfo());
                Set<String> included = (inclusions == null) ? null
                        : inclusions.getIncluded();
                MapSerializer mapSer = MapSerializer.construct(ignored, included,
                        type, staticTyping, elementTypeSerializer,
                        keySerializer, elementValueSerializer, filterId);
                ser = _checkMapContentInclusion(prov, beanDesc, mapSer);
            }
        }
        // [databind#120]: Allow post-processing
        if (_factoryConfig.hasSerializerModifiers()) {
            for (BeanSerializerModifier mod : _factoryConfig.serializerModifiers()) {
                ser = mod.modifyMapSerializer(config, type, beanDesc, ser);
            }
        }
        return ser;
    }

    /**
     * Helper method that does figures out content inclusion value to use, if any,
     * and construct re-configured {@link MapSerializer} appropriately.
     *
     * @since 2.9
     */
    @SuppressWarnings("deprecation")
    protected MapSerializer _checkMapContentInclusion(SerializerProvider prov,
            BeanDescription beanDesc, MapSerializer mapSer)
        throws JsonMappingException
    {
        final JavaType contentType = mapSer.getContentType();
        JsonInclude.Value inclV = _findInclusionWithContent(prov, beanDesc,
                contentType, Map.class);

        // Need to support global legacy setting, for now:
        JsonInclude.Include incl = (inclV == null) ? JsonInclude.Include.USE_DEFAULTS : inclV.getContentInclusion();
        if (incl == JsonInclude.Include.USE_DEFAULTS
                || incl == JsonInclude.Include.ALWAYS) {
            if (!prov.isEnabled(SerializationFeature.WRITE_NULL_MAP_VALUES)) {
                return mapSer.withContentInclusion(null, true);
            }
            return mapSer;
        }

        // NOTE: mostly copied from `PropertyBuilder`; would be nice to refactor
        // but code is not identical nor are these types related
        Object valueToSuppress;
        boolean suppressNulls = true; // almost always, but possibly not with CUSTOM

        switch (incl) {
        case NON_DEFAULT:
            valueToSuppress = BeanUtil.getDefaultValue(contentType);
            if (valueToSuppress != null) {
                if (valueToSuppress.getClass().isArray()) {
                    valueToSuppress = ArrayBuilders.getArrayComparator(valueToSuppress);
                }
            }
            break;
        case NON_ABSENT: // new with 2.6, to support Guava/JDK8 Optionals
            // and for referential types, also "empty", which in their case means "absent"
            valueToSuppress = contentType.isReferenceType()
                    ? MapSerializer.MARKER_FOR_EMPTY : null;
            break;
        case NON_EMPTY:
            valueToSuppress = MapSerializer.MARKER_FOR_EMPTY;
            break;
        case CUSTOM: // new with 2.9
            valueToSuppress = prov.includeFilterInstance(null, inclV.getContentFilter());
            if (valueToSuppress == null) { // is this legal?
                suppressNulls = true;
            } else {
                suppressNulls = prov.includeFilterSuppressNulls(valueToSuppress);
            }
            break;
        case NON_NULL:
        default: // should not matter but...
            valueToSuppress = null;
            break;
        }
        return mapSer.withContentInclusion(valueToSuppress, suppressNulls);
    }

    /**
     * @since 2.9
     */
    protected JsonSerializer<?> buildMapEntrySerializer(SerializerProvider prov,
            JavaType type, BeanDescription beanDesc, boolean staticTyping,
            JavaType keyType, JavaType valueType)
        throws JsonMappingException
    {
        // [databind#865]: Allow serialization "as POJO" -- note: to undo, declare
        //   serialization as `Shape.NATURAL` instead; that's JSON Object too.
        JsonFormat.Value formatOverride = prov.getDefaultPropertyFormat(Map.Entry.class);
        JsonFormat.Value formatFromAnnotation = beanDesc.findExpectedFormat(null);
        JsonFormat.Value format = JsonFormat.Value.merge(formatFromAnnotation, formatOverride);
        if (format.getShape() == JsonFormat.Shape.OBJECT) {
            return null;
        }
        MapEntrySerializer ser = new MapEntrySerializer(valueType, keyType, valueType,
                staticTyping, createTypeSerializer(prov.getConfig(), valueType), null);

        final JavaType contentType = ser.getContentType();
        JsonInclude.Value inclV = _findInclusionWithContent(prov, beanDesc,
                contentType, Map.Entry.class);

        // Need to support global legacy setting, for now:
        JsonInclude.Include incl = (inclV == null) ? JsonInclude.Include.USE_DEFAULTS : inclV.getContentInclusion();
        if (incl == JsonInclude.Include.USE_DEFAULTS
                || incl == JsonInclude.Include.ALWAYS) {
            return ser;
        }

        // NOTE: mostly copied from `PropertyBuilder`; would be nice to refactor
        // but code is not identical nor are these types related
        Object valueToSuppress;
        boolean suppressNulls = true; // almost always, but possibly not with CUSTOM

        switch (incl) {
        case NON_DEFAULT:
            valueToSuppress = BeanUtil.getDefaultValue(contentType);
            if (valueToSuppress != null) {
                if (valueToSuppress.getClass().isArray()) {
                    valueToSuppress = ArrayBuilders.getArrayComparator(valueToSuppress);
                }
            }
            break;
        case NON_ABSENT:
            valueToSuppress = contentType.isReferenceType()
                    ? MapSerializer.MARKER_FOR_EMPTY : null;
            break;
        case NON_EMPTY:
            valueToSuppress = MapSerializer.MARKER_FOR_EMPTY;
            break;
        case CUSTOM:
            valueToSuppress = prov.includeFilterInstance(null, inclV.getContentFilter());
            if (valueToSuppress == null) { // is this legal?
                suppressNulls = true;
            } else {
                suppressNulls = prov.includeFilterSuppressNulls(valueToSuppress);
            }
            break;
        case NON_NULL:
        default: // should not matter but...
            valueToSuppress = null;
            break;
        }
        return ser.withContentInclusion(valueToSuppress, suppressNulls);
    }

    /**
     * Helper method used for finding inclusion definitions for structured
     * container types like <code>Map</code>s and referential types
     * (like <code>AtomicReference</code>).
     *
     * @param contentType Declared full content type of container
     * @param configType Raw base type under which `configOverride`, if any, needs to be defined
     */
    protected JsonInclude.Value _findInclusionWithContent(SerializerProvider prov,
            BeanDescription beanDesc,
            JavaType contentType, Class<?> configType)
        throws JsonMappingException
    {
        final SerializationConfig config = prov.getConfig();

        // Defaulting gets complicated because we might have two distinct
        //   axis to consider: Container type itself , and then value (content) type.
        //  Start with Container-defaults, then use more-specific value override, if any.

        // Start by getting global setting, overridden by Map-type-override
        JsonInclude.Value inclV = beanDesc.findPropertyInclusion(config.getDefaultPropertyInclusion());
        inclV = config.getDefaultPropertyInclusion(configType, inclV);

        // and then merge content-type overrides, if any. But note that there's
        // content-to-value inclusion shift we have to do
        JsonInclude.Value valueIncl = config.getDefaultPropertyInclusion(contentType.getRawClass(), null);

        if (valueIncl != null) {
            switch (valueIncl.getValueInclusion()) {
            case USE_DEFAULTS:
                break;
            case CUSTOM:
                inclV = inclV.withContentFilter(valueIncl.getContentFilter());
                break;
            default:
                inclV = inclV.withContentInclusion(valueIncl.getValueInclusion());
            }
        }
        return inclV;
    }

    /*
    /**********************************************************
    /* Factory methods, for Arrays
    /**********************************************************
     */

    /**
     * Helper method that handles configuration details when constructing serializers for
     * <code>Object[]</code> (and subtypes, except for String).
     */
    protected JsonSerializer<?> buildArraySerializer(SerializerProvider prov,
            ArrayType type, BeanDescription beanDesc,
            boolean staticTyping,
            TypeSerializer elementTypeSerializer, JsonSerializer<Object> elementValueSerializer)
        throws JsonMappingException
    {
        // 25-Jun-2015, tatu: Note that unlike with Collection(Like) and Map(Like) types, array
        //   types cannot be annotated (in theory I guess we could have mix-ins but... ?)
        //   so we need not do primary annotation lookup here.
        //   So all we need is (1) Custom, (2) Default array serializers
        SerializationConfig config = prov.getConfig();
        JsonSerializer<?> ser = null;

        for (Serializers serializers : customSerializers()) { // (1) Custom
             ser = serializers.findArraySerializer(config,
                     type, beanDesc, elementTypeSerializer, elementValueSerializer);
             if (ser != null) {
                 break;
             }
        }

        if (ser == null) {
             Class<?> raw = type.getRawClass();
             // Important: do NOT use standard serializers if non-standard element value serializer specified
             if (elementValueSerializer == null || ClassUtil.isJacksonStdImpl(elementValueSerializer)) {
                 if (String[].class == raw) {
                     ser = StringArraySerializer.instance;
                 } else {
                     // other standard types?
                     ser = StdArraySerializers.findStandardImpl(raw);
                 }
             }
             if (ser == null) {
                 ser = new ObjectArraySerializer(type.getContentType(), staticTyping, elementTypeSerializer,
                         elementValueSerializer);
             }
         }
         // [databind#120]: Allow post-processing
         if (_factoryConfig.hasSerializerModifiers()) {
             for (BeanSerializerModifier mod : _factoryConfig.serializerModifiers()) {
                 ser = mod.modifyArraySerializer(config, type, beanDesc, ser);
             }
         }
         return ser;
    }

    /*
    /**********************************************************
    /* Factory methods for Reference types
    /* (demoted from BeanSF down here in 2.9)
    /**********************************************************
     */

    /**
     * @since 2.7
     */
    public JsonSerializer<?> findReferenceSerializer(SerializerProvider prov, ReferenceType refType,
            BeanDescription beanDesc, boolean staticTyping)
        throws JsonMappingException
    {
        JavaType contentType = refType.getContentType();
        TypeSerializer contentTypeSerializer = contentType.getTypeHandler();
        final SerializationConfig config = prov.getConfig();
        if (contentTypeSerializer == null) {
            contentTypeSerializer = createTypeSerializer(config, contentType);
        }
        JsonSerializer<Object> contentSerializer = contentType.getValueHandler();
        for (Serializers serializers : customSerializers()) {
            JsonSerializer<?> ser = serializers.findReferenceSerializer(config, refType, beanDesc,
                    contentTypeSerializer, contentSerializer);
            if (ser != null) {
                return ser;
            }
        }
        if (refType.isTypeOrSubTypeOf(AtomicReference.class)) {
            return buildAtomicReferenceSerializer(prov, refType, beanDesc, staticTyping,
                    contentTypeSerializer, contentSerializer);
        }
        return null;
    }

    protected JsonSerializer<?> buildAtomicReferenceSerializer(SerializerProvider prov,
            ReferenceType refType, BeanDescription beanDesc, boolean staticTyping,
            TypeSerializer contentTypeSerializer, JsonSerializer<Object> contentSerializer)
        throws JsonMappingException
    {
        final JavaType contentType = refType.getReferencedType();
        JsonInclude.Value inclV = _findInclusionWithContent(prov, beanDesc,
                contentType, AtomicReference.class);

        // Need to support global legacy setting, for now:
        JsonInclude.Include incl = (inclV == null) ? JsonInclude.Include.USE_DEFAULTS : inclV.getContentInclusion();
        Object valueToSuppress;
        boolean suppressNulls;

        if (incl == JsonInclude.Include.USE_DEFAULTS
                || incl == JsonInclude.Include.ALWAYS) {
            valueToSuppress = null;
            suppressNulls = false;
        } else {
            suppressNulls = true;
            switch (incl) {
            case NON_DEFAULT:
                valueToSuppress = BeanUtil.getDefaultValue(contentType);
                if (valueToSuppress != null) {
                    if (valueToSuppress.getClass().isArray()) {
                        valueToSuppress = ArrayBuilders.getArrayComparator(valueToSuppress);
                    }
                }
                break;
            case NON_ABSENT:
                valueToSuppress = contentType.isReferenceType()
                        ? MapSerializer.MARKER_FOR_EMPTY : null;
                break;
            case NON_EMPTY:
                valueToSuppress = MapSerializer.MARKER_FOR_EMPTY;
                break;
            case CUSTOM:
                valueToSuppress = prov.includeFilterInstance(null, inclV.getContentFilter());
                if (valueToSuppress == null) { // is this legal?
                    suppressNulls = true;
                } else {
                    suppressNulls = prov.includeFilterSuppressNulls(valueToSuppress);
                }
                break;
            case NON_NULL:
            default: // should not matter but...
                valueToSuppress = null;
                break;
            }
        }
        AtomicReferenceSerializer ser = new AtomicReferenceSerializer(refType, staticTyping,
                contentTypeSerializer, contentSerializer);
        return ser.withContentInclusion(valueToSuppress, suppressNulls);
    }

    /*
    /**********************************************************
    /* Factory methods, for non-container types
    /**********************************************************
     */

    /**
     * @since 2.5
     */
    protected JsonSerializer<?> buildIteratorSerializer(SerializationConfig config,
            JavaType type, BeanDescription beanDesc, boolean staticTyping,
            JavaType valueType)
        throws JsonMappingException
    {
        return new IteratorSerializer(valueType, staticTyping, createTypeSerializer(config, valueType));
    }

    /**
     * @since 2.5
     */
    protected JsonSerializer<?> buildIterableSerializer(SerializationConfig config,
            JavaType type, BeanDescription beanDesc, boolean staticTyping,
            JavaType valueType)
        throws JsonMappingException
    {
        return new IterableSerializer(valueType, staticTyping, createTypeSerializer(config, valueType));
    }

    protected JsonSerializer<?> buildEnumSerializer(SerializationConfig config,
            JavaType type, BeanDescription beanDesc)
        throws JsonMappingException
    {
        /* As per [databind#24], may want to use alternate shape, serialize as JSON Object.
         * Challenge here is that EnumSerializer does not know how to produce
         * POJO style serialization, so we must handle that special case separately;
         * otherwise pass it to EnumSerializer.
         */
        JsonFormat.Value format = beanDesc.findExpectedFormat(null);
        if (format.getShape() == JsonFormat.Shape.OBJECT) {
            // one special case: suppress serialization of "getDeclaringClass()"...
            ((BasicBeanDescription) beanDesc).removeProperty("declaringClass");
            // returning null will mean that eventually BeanSerializer gets constructed
            return null;
        }
        @SuppressWarnings("unchecked")
        Class<Enum<?>> enumClass = (Class<Enum<?>>) type.getRawClass();
        JsonSerializer<?> ser = EnumSerializer.construct(enumClass, config, beanDesc, format);
        // [databind#120]: Allow post-processing
        if (_factoryConfig.hasSerializerModifiers()) {
            for (BeanSerializerModifier mod : _factoryConfig.serializerModifiers()) {
                ser = mod.modifyEnumSerializer(config, type, beanDesc, ser);
            }
        }
        return ser;
    }

    /*
    /**********************************************************
    /* Other helper methods
    /**********************************************************
     */

    /**
     * Helper method called to try to find whether there is an annotation in the
     * class that indicates key serializer to use.
     * If so, will try to instantiate key serializer and return it; otherwise returns null.
     */
    protected JsonSerializer<Object> _findKeySerializer(SerializerProvider prov,
            Annotated a)
        throws JsonMappingException
    {
        AnnotationIntrospector intr = prov.getAnnotationIntrospector();
        Object serDef = intr.findKeySerializer(a);
        if (serDef != null) {
            return prov.serializerInstance(a, serDef);
        }
        return null;
    }

    /**
     * Helper method called to try to find whether there is an annotation in the
     * class that indicates content ("value") serializer to use.
     * If so, will try to instantiate value serializer and return it; otherwise returns null.
     */
    protected JsonSerializer<Object> _findContentSerializer(SerializerProvider prov,
            Annotated a)
        throws JsonMappingException
    {
        AnnotationIntrospector intr = prov.getAnnotationIntrospector();
        Object serDef = intr.findContentSerializer(a);
        if (serDef != null) {
            return prov.serializerInstance(a, serDef);
        }
        return null;
    }

    /**
     * Method called to find filter that is configured to be used with bean
     * serializer being built, if any.
     */
    protected Object findFilterId(SerializationConfig config, BeanDescription beanDesc) {
        return config.getAnnotationIntrospector().findFilterId((Annotated)beanDesc.getClassInfo());
    }

    /**
     * Helper method to check whether global settings and/or class
     * annotations for the bean class indicate that static typing
     * (declared types)  should be used for properties.
     * (instead of dynamic runtime types).
     *
     * @since 2.1 (earlier had variant with additional 'property' parameter)
     */
    protected boolean usesStaticTyping(SerializationConfig config,
            BeanDescription beanDesc, TypeSerializer typeSer)
    {
        /* 16-Aug-2010, tatu: If there is a (value) type serializer, we cannot force
         *    static typing; that would make it impossible to handle expected subtypes
         */
        if (typeSer != null) {
            return false;
        }
        AnnotationIntrospector intr = config.getAnnotationIntrospector();
        JsonSerialize.Typing t = intr.findSerializationTyping(beanDesc.getClassInfo());
        if (t != null && t != JsonSerialize.Typing.DEFAULT_TYPING) {
            return (t == JsonSerialize.Typing.STATIC);
        }
        return config.isEnabled(MapperFeature.USE_STATIC_TYPING);
    }

    // Commented out in 2.9
    /*
    protected Class<?> _verifyAsClass(Object src, String methodName, Class<?> noneClass)
    {
        if (src == null) {
            return null;
        }
        if (!(src instanceof Class)) {
            throw new IllegalStateException("AnnotationIntrospector."+methodName+"() returned value of type "+src.getClass().getName()+": expected type JsonSerializer or Class<JsonSerializer> instead");
        }
        Class<?> cls = (Class<?>) src;
        if (cls == noneClass || ClassUtil.isBogusClass(cls)) {
            return null;
        }
        return cls;
    }
    */
}
