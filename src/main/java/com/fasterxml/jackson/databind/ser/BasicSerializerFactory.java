package com.fasterxml.jackson.databind.ser;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.cfg.SerializerFactoryConfig;
import com.fasterxml.jackson.databind.ext.OptionalHandlerFactory;
import com.fasterxml.jackson.databind.ext.jdk8.DoubleStreamSerializer;
import com.fasterxml.jackson.databind.ext.jdk8.IntStreamSerializer;
import com.fasterxml.jackson.databind.ext.jdk8.LongStreamSerializer;
import com.fasterxml.jackson.databind.ext.jdk8.OptionalDoubleSerializer;
import com.fasterxml.jackson.databind.ext.jdk8.OptionalIntSerializer;
import com.fasterxml.jackson.databind.ext.jdk8.OptionalLongSerializer;
import com.fasterxml.jackson.databind.ext.jdk8.Jdk8OptionalSerializer;
import com.fasterxml.jackson.databind.ext.jdk8.Jdk8StreamSerializer;
import com.fasterxml.jackson.databind.introspect.*;
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
    /**********************************************************************
    /* Configuration, lookup tables/maps
    /**********************************************************************
     */

    /**
     * Since these are all JDK classes, we shouldn't have to worry
     * about ClassLoader used to load them. Rather, we can just
     * use the class name, and keep things simple and efficient.
     */
    protected final static HashMap<String, JsonSerializer<?>> _concrete;

    static {
        HashMap<String, JsonSerializer<?>> concrete
            = new HashMap<String, JsonSerializer<?>>();

        
        /* String and string-like types (note: date types explicitly
         * not included -- can use either textual or numeric serialization)
         */
        concrete.put(String.class.getName(), StringSerializer.instance);
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
        concrete.put(BigDecimal.class.getName(), new NumberSerializer(BigDecimal.class));

        // Other discrete non-container types:
        // First, Date/Time zoo:
        concrete.put(Calendar.class.getName(), CalendarSerializer.instance);
        concrete.put(java.util.Date.class.getName(), DateSerializer.instance);

        _concrete = concrete;
    }

    /*
    /**********************************************************************
    /* Configuration
    /**********************************************************************
     */
    
    /**
     * Configuration settings for this factory; immutable instance (just like this
     * factory), new version created via copy-constructor (fluent-style)
     */
    protected final SerializerFactoryConfig _factoryConfig;

    /*
    /**********************************************************************
    /* Life cycle
    /**********************************************************************
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
     * Method used for creating a new instance of this factory, but with different
     * configuration. Reason for specifying factory method (instead of plain constructor)
     * is to allow proper sub-classing of factories.
     *<p>
     * Note that custom sub-classes generally <b>must override</b> implementation
     * of this method, as it usually requires instantiating a new instance of
     * factory type. Check out javadocs for
     * {@link com.fasterxml.jackson.databind.ser.BeanSerializerFactory} for more details.
     */
    protected abstract SerializerFactory withConfig(SerializerFactoryConfig config);

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

    @Override
    public final SerializerFactory withNullValueSerializer(JsonSerializer<?> nvs) {
        return withConfig(_factoryConfig.withNullValueSerializer(nvs));
    }

    @Override
    public final SerializerFactory withNullKeySerializer(JsonSerializer<?> nks) {
        return withConfig(_factoryConfig.withNullKeySerializer(nks));
    }

    /*
    /**********************************************************************
    /* `SerializerFactory` impl
    /**********************************************************************
     */
    
// Implemented by sub-classes
//    public abstract JsonSerializer<Object> createSerializer(SerializerProvider prov, ....)

    @Override
    @SuppressWarnings("unchecked")
    public JsonSerializer<Object> createKeySerializer(SerializerProvider ctxt,
            JavaType keyType, JsonSerializer<Object> defaultImpl)
        throws JsonMappingException
    {
        // We should not need any member method info; at most class annotations for Map type
        // ... at least, not here.
        BeanDescription beanDesc = ctxt.introspectClassAnnotations(keyType);
        final SerializationConfig config = ctxt.getConfig();
        JsonSerializer<?> ser = null;
        // Minor optimization: to avoid constructing beanDesc, bail out if none registered
        if (_factoryConfig.hasKeySerializers()) {
            // Only thing we have here are module-provided key serializers:
            for (Serializers serializers : _factoryConfig.keySerializers()) {
                ser = serializers.findSerializer(config, keyType, beanDesc, null);
                if (ser != null) {
                    break;
                }
            }
        }
        if (ser == null) {
            ser = StdKeySerializers.getStdKeySerializer(config, keyType.getRawClass(), false);
            // As per [databind#47], also need to support @JsonValue
            if (ser == null) {
                beanDesc = ctxt.introspect(keyType);
                AnnotatedMember am = beanDesc.findJsonValueAccessor();
                if (am != null) {
                    final Class<?> rawType = am.getRawType();
                    JsonSerializer<?> delegate = StdKeySerializers.getStdKeySerializer(config,
                            rawType, true);
                    if (config.canOverrideAccessModifiers()) {
                        ClassUtil.checkAndFixAccess(am.getMember(),
                                config.isEnabled(MapperFeature.OVERRIDE_PUBLIC_ACCESS_MODIFIERS));
                    }
                    // need to pass both type of key Object (on which accessor called), and actual
                    // value type that `JsonType`-annotated accessor returns (or contains, in case of field)
                    ser = new JsonValueSerializer(keyType, am.getType(), false, null, delegate, am);
                } else {
                    // And aside from JDK defaults, use `defaultImpl` if any specified
                    ser = defaultImpl;
                    if (ser == null) {
                        ser = StdKeySerializers.getFallbackKeySerializer(config, keyType.getRawClass());
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
    public JsonSerializer<Object> getDefaultNullKeySerializer() {
        return _factoryConfig.getNullKeySerializer();
    }

    @Override
    public JsonSerializer<Object> getDefaultNullValueSerializer() {
        return _factoryConfig.getNullValueSerializer();
    }

    /*
    /**********************************************************************
    /* Additional API for other core classes
    /**********************************************************************
     */

    protected Iterable<Serializers> customSerializers() {
        return _factoryConfig.serializers();
    }

    /**
     * Method called to create a type information serializer for values of given
     * container property
     * if one is needed. If not needed (no polymorphic handling configured), should
     * return null.
     *
     * @param containerType Declared type of the container to use as the base type for type information serializer
     * 
     * @return Type serializer to use for property value contents, if one is needed; null if not.
     */    
    public TypeSerializer findPropertyContentTypeSerializer(SerializerProvider ctxt,
            JavaType containerType, AnnotatedMember accessor)
        throws JsonMappingException
    {
        return ctxt.getConfig().getTypeResolverProvider()
                .findPropertyContentTypeSerializer(ctxt, accessor, containerType);
    }

    /*
    /**********************************************************************
    /* Overridable secondary serializer accessor methods
    /**********************************************************************
     */

    /**
     * Method that will use fast lookup (and identity comparison) methods to
     * see if we know serializer to use for given type.
     */
    protected final JsonSerializer<?> findSerializerByLookup(JavaType type,
            SerializationConfig config, BeanDescription beanDesc, JsonFormat.Value format,
            boolean staticTyping)
    {
        final Class<?> raw = type.getRawClass();
        JsonSerializer<?> ser = StdJdkSerializers.find(raw);
        if (ser == null) {
            final String clsName = raw.getName();
            ser = _concrete.get(clsName);
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
     */
    protected final JsonSerializer<?> findSerializerByAnnotations(SerializerProvider ctxt, 
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
            if (ctxt.canOverrideAccessModifiers()) {
                ClassUtil.checkAndFixAccess(valueAccessor.getMember(),
                        ctxt.isEnabled(MapperFeature.OVERRIDE_PUBLIC_ACCESS_MODIFIERS));
            }
            JsonSerializer<Object> ser = findSerializerFromAnnotation(ctxt, valueAccessor);
            JavaType valueType = valueAccessor.getType();
            // note: must get different `BeanDescription` since valueType different
            TypeSerializer vts = ctxt.findTypeSerializer(valueType);
            return new JsonValueSerializer(type, valueType, /* static typing */ false,
                    vts, ser, valueAccessor);
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
    protected final JsonSerializer<?> findSerializerByPrimaryType(SerializerProvider ctxt, 
            JavaType type, BeanDescription beanDesc, JsonFormat.Value formatOverrides,
            boolean staticTyping)
        throws JsonMappingException
    {
        if (type.isTypeOrSubTypeOf(Calendar.class)) {
            return CalendarSerializer.instance;
        }
        if (type.isTypeOrSubTypeOf(java.util.Date.class)) {
            return DateSerializer.instance;
        }
        // 19-Sep-2017, tatu: Jackson 3.x adds Java 8 types.
        // NOTE: while seemingly more of an add-on type, we must handle here because
        //   otherwise Bean-handling would be used instead...
        if (type.isTypeOrSubTypeOf(Stream.class)) {
            return new Jdk8StreamSerializer(type,
                    ctxt.getTypeFactory().findFirstTypeParameter(type, Stream.class));
        }

        if (type.isTypeOrSubTypeOf(Number.class)) {
            JsonFormat.Value format = _calculateEffectiveFormat(beanDesc, Number.class, formatOverrides);

            // 21-May-2014, tatu: Couple of alternatives actually
            switch (format.getShape()) {
            case STRING:
                return ToStringSerializer.instance;
            case OBJECT: // need to bail out to let it be serialized as POJO
                return null;
            default:
            }
            return NumberSerializer.instance;
        }
        if (type.isTypeOrSubTypeOf(Enum.class)) {
            return buildEnumSerializer(ctxt, type, beanDesc,
                    _calculateEffectiveFormat(beanDesc, Enum.class, formatOverrides));
        }
        Class<?> raw = type.getRawClass();
        if (Map.Entry.class.isAssignableFrom(raw)) {
            // 18-Oct-2015, tatu: With 2.7, need to dig type info:
            JavaType mapEntryType = type.findSuperType(Map.Entry.class);
            // 28-Apr-2015, tatu: TypeFactory does it all for us already so
            JavaType kt = mapEntryType.containedTypeOrUnknown(0);
            JavaType vt = mapEntryType.containedTypeOrUnknown(1);
            return buildMapEntrySerializer(ctxt, type, beanDesc,
                    _calculateEffectiveFormat(beanDesc, Map.Entry.class, formatOverrides),
                    staticTyping, kt, vt);
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
        // 19-Sep-2017, tatu: Java 8 streams, except for main `Stream` (which is "add-on" interface?)
        if (LongStream.class.isAssignableFrom(raw)) {
            return LongStreamSerializer.INSTANCE;
        }
        if (IntStream.class.isAssignableFrom(raw)) {
            return IntStreamSerializer.INSTANCE;
        }
        if (DoubleStream.class.isAssignableFrom(raw)) {
            return DoubleStreamSerializer.INSTANCE;
        }
        // NOTE: not concrete, can not just add directly via StdJdkSerializers. Also, requires
        // bit of trickery wrt class name for polymorphic...
        if (Path.class.isAssignableFrom(raw)) {
            return StringLikeSerializer.find(Path.class);
        }
        // Then check for optional/external serializers 
        JsonSerializer<?> ser = OptionalHandlerFactory.instance.findSerializer(ctxt.getConfig(),
                type, beanDesc);
        if (ser != null) {
            return ser;
        }
        return null;
    }

    /**
     * Reflection-based serialized find method, which checks if
     * given class implements one of recognized "add-on" interfaces.
     * Add-on here means a role that is usually or can be a secondary
     * trait: for example,
     * bean classes may implement {@link Iterable}, but their main
     * function is usually something else. The reason for
     */
    protected final JsonSerializer<?> findSerializerByAddonType(SerializerProvider ctxt, 
            JavaType javaType, BeanDescription beanDesc, JsonFormat.Value formatOverrides,
            boolean staticTyping) throws JsonMappingException
    {
        final TypeFactory tf = ctxt.getTypeFactory();
        if (javaType.isTypeOrSubTypeOf(Iterator.class)) {
            return buildIteratorSerializer(ctxt, javaType, beanDesc, formatOverrides,
                    staticTyping,
                    tf.findFirstTypeParameter(javaType, Iterator.class));
        }
        if (javaType.isTypeOrSubTypeOf(Iterable.class)) {
            return buildIterableSerializer(ctxt, javaType, beanDesc, formatOverrides,
                    staticTyping,
                    tf.findFirstTypeParameter(javaType, Iterable.class));
        }
        if (javaType.isTypeOrSubTypeOf(CharSequence.class)) {
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
        Object serDef = prov.getAnnotationIntrospector().findSerializer(prov.getConfig(), a);
        if (serDef == null) {
            return null;
        }
        // One more thing however: may need to also apply a converter:
        return (JsonSerializer<Object>) findConvertingSerializer(prov, a,
                prov.serializerInstance(a, serDef));
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
        return new StdDelegatingSerializer(conv, delegateType, ser, null);
    }

    protected Converter<Object,Object> findConverter(SerializerProvider prov,
            Annotated a)
        throws JsonMappingException
    {
        Object convDef = prov.getAnnotationIntrospector().findSerializationConverter(prov.getConfig(), a);
        if (convDef == null) {
            return null;
        }
        return prov.converterInstance(a, convDef);
    }
    
    /*
    /**********************************************************************
    /* Factory methods, container types:
    /**********************************************************************
     */

    protected JsonSerializer<?> buildContainerSerializer(SerializerProvider ctxt,
            JavaType type, BeanDescription beanDesc, JsonFormat.Value formatOverrides,
            boolean staticTyping)
        throws JsonMappingException
    {
        // [databind#23], 15-Mar-2013, tatu: must force static handling of root value type,
        //   with just one important exception: if value type is "untyped", let's
        //   leave it as is; no clean way to make it work.
        if (!staticTyping && type.useStaticType()) {
            if (!type.isContainerType() || !type.getContentType().isJavaLangObject()) {
                staticTyping = true;
            }
        }
        // Let's see what we can learn about element/content/value type, type serializer for it:
        JavaType elementType = type.getContentType();
        TypeSerializer elementTypeSerializer = ctxt.findTypeSerializer(elementType);
        // if elements have type serializer, cannot force static typing:
        if (elementTypeSerializer != null) {
            staticTyping = false;
        }
        JsonSerializer<Object> elementValueSerializer = _findContentSerializer(ctxt,
                beanDesc.getClassInfo());
        final SerializationConfig config = ctxt.getConfig();
        if (type.isMapLikeType()) { // implements java.util.Map
            MapLikeType mlt = (MapLikeType) type;
            /* 29-Sep-2012, tatu: This is actually too early to (try to) find
             *  key serializer from property annotations, and can lead to caching
             *  issues (see [databind#75]). Instead, must be done from 'createContextual()' call.
             *  But we do need to check class annotations.
             */
            JsonSerializer<Object> keySerializer = _findKeySerializer(ctxt, beanDesc.getClassInfo());
            if (mlt.isTrueMapType()) {
                return buildMapSerializer(ctxt, (MapType) mlt,
                        beanDesc, formatOverrides, staticTyping,
                        keySerializer, elementTypeSerializer, elementValueSerializer);
            }
            // With Map-like, just 2 options: (1) Custom, (2) Annotations
            JsonSerializer<?> ser = null;
            MapLikeType mlType = (MapLikeType) type;
            for (Serializers serializers : customSerializers()) { // (1) Custom
                ser = serializers.findMapLikeSerializer(config, mlType,
                        beanDesc, formatOverrides,
                        keySerializer, elementTypeSerializer, elementValueSerializer);
                if (ser != null) {
                    break;
                }
            }
            if (ser == null) { // (2) Annotations-based ones:
                ser = findSerializerByAnnotations(ctxt, type, beanDesc);
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
            if (clt.isTrueCollectionType()) {
                return buildCollectionSerializer(ctxt, (CollectionType) clt,
                        beanDesc, formatOverrides, staticTyping,
                        elementTypeSerializer, elementValueSerializer);
            }
            // With Map-like, just 2 options: (1) Custom, (2) Annotations
            JsonSerializer<?> ser = null;
            CollectionLikeType clType = (CollectionLikeType) type;
            for (Serializers serializers : customSerializers()) { // (1) Custom
                ser = serializers.findCollectionLikeSerializer(config, clType,
                        beanDesc, formatOverrides,
                        elementTypeSerializer, elementValueSerializer);
                if (ser != null) {
                    break;
                }
            }
            if (ser == null) { // (2) Annotations-based ones:
                ser = findSerializerByAnnotations(ctxt, type, beanDesc);
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
            return buildArraySerializer(ctxt, (ArrayType) type,
                    beanDesc, formatOverrides, staticTyping,
                    elementTypeSerializer, elementValueSerializer);
        }
        return null;
    }

    /**
     * Helper method that handles configuration details when constructing serializers for
     * {@link java.util.List} types that support efficient by-index access
     */
    protected JsonSerializer<?> buildCollectionSerializer(SerializerProvider prov,
            CollectionType type, BeanDescription beanDesc,  JsonFormat.Value formatOverrides,
            boolean staticTyping,
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
            ser = serializers.findCollectionSerializer(config, type, beanDesc, formatOverrides,
                    elementTypeSerializer, elementValueSerializer);
            if (ser != null) {
                break;
            }
        }

        JsonFormat.Value format = _calculateEffectiveFormat(beanDesc, Collection.class, formatOverrides);
        if (ser == null) {
            ser = findSerializerByAnnotations(prov, type, beanDesc); // (2) Annotations
            if (ser == null) {
                // We may also want to use serialize Collections "as beans", if (and only if)
                // shape specified as "POJO"
                if (format.getShape() == JsonFormat.Shape.POJO) {
                    return null;
                }
                Class<?> raw = type.getRawClass();
                if (EnumSet.class.isAssignableFrom(raw)) {
                    // this may or may not be available (Class doesn't; type of field/method does)
                    JavaType enumType = type.getContentType();
                    // and even if nominally there is something, only use if it really is enum
                    if (!enumType.isEnumType()) {
                        enumType = null;
                    }
                    ser = buildEnumSetSerializer(enumType);
                } else {
                    Class<?> elementRaw = type.getContentType().getRawClass();
                    if (isIndexedList(raw)) {
                        if (elementRaw == String.class) {
                            // Only optimize if std implementation, not custom
                            if (ClassUtil.isJacksonStdImpl(elementValueSerializer)) {
                                ser = IndexedStringListSerializer.instance;
                            }
                        } else {
                            ser = buildIndexedListSerializer(type.getContentType(), staticTyping,
                                elementTypeSerializer, elementValueSerializer);
                        }
                    } else if (elementRaw == String.class) {
                        // Only optimize if std implementation, not custom
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
    /**********************************************************************
    /* Factory methods, for Collections
    /**********************************************************************
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
    /**********************************************************************
    /* Factory methods, for Maps
    /**********************************************************************
     */

    /**
     * Helper method that handles configuration details when constructing serializers for
     * {@link java.util.Map} types.
     */
    protected JsonSerializer<?> buildMapSerializer(SerializerProvider prov,
            MapType type, BeanDescription beanDesc, JsonFormat.Value formatOverrides,
            boolean staticTyping, JsonSerializer<Object> keySerializer,
            TypeSerializer elementTypeSerializer, JsonSerializer<Object> elementValueSerializer)
        throws JsonMappingException
    {
        JsonFormat.Value format = _calculateEffectiveFormat(beanDesc, Map.class, formatOverrides);

        // [databind#467]: This is where we could allow serialization "as POJO": But! It's
        // nasty to undo, and does not apply on per-property basis. So, hardly optimal
        if (format.getShape() == JsonFormat.Shape.POJO) {
            return null;
        }
        JsonSerializer<?> ser = null;

        // Order of lookups:
        // 1. Custom serializers
        // 2. Annotations (@JsonValue, @JsonDeserialize)
        // 3. Defaults
        
        final SerializationConfig config = prov.getConfig();
        for (Serializers serializers : customSerializers()) { // (1) Custom
            ser = serializers.findMapSerializer(config, type, beanDesc, formatOverrides,
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
                MapSerializer mapSer = MapSerializer.construct(ignored,
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
     */
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

    protected JsonSerializer<?> buildMapEntrySerializer(SerializerProvider ctxt,
            JavaType type, BeanDescription beanDesc,  JsonFormat.Value effectiveFormat,
            boolean staticTyping,
            JavaType keyType, JavaType valueType)
        throws JsonMappingException
    {
        // [databind#865]: Allow serialization "as POJO" -- note: to undo, declare
        //   serialization as `Shape.NATURAL` instead; that's JSON Object too.
        if (effectiveFormat.getShape() == JsonFormat.Shape.POJO) {
            return null;
        }
        MapEntrySerializer ser = new MapEntrySerializer(valueType, keyType,
                valueType, staticTyping, ctxt.findTypeSerializer(valueType), null);

        final JavaType contentType = ser.getContentType();
        JsonInclude.Value inclV = _findInclusionWithContent(ctxt, beanDesc,
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
            valueToSuppress = ctxt.includeFilterInstance(null, inclV.getContentFilter());
            if (valueToSuppress == null) { // is this legal?
                suppressNulls = true;
            } else {
                suppressNulls = ctxt.includeFilterSuppressNulls(valueToSuppress);
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
    /**********************************************************************
    /* Factory methods, for Arrays
    /**********************************************************************
     */
    
    /**
     * Helper method that handles configuration details when constructing serializers for
     * <code>Object[]</code> (and subtypes, except for String).
     */
    protected JsonSerializer<?> buildArraySerializer(SerializerProvider prov,
            ArrayType type, BeanDescription beanDesc, JsonFormat.Value formatOverrides,
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
             ser = serializers.findArraySerializer(config, type, beanDesc, formatOverrides,
                     elementTypeSerializer, elementValueSerializer);
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
    /**********************************************************************
    /* Factory methods for Reference types
    /**********************************************************************
     */

    public JsonSerializer<?> findReferenceSerializer(SerializerProvider ctxt,
            ReferenceType refType, BeanDescription beanDesc, JsonFormat.Value format,
            boolean staticTyping)
        throws JsonMappingException
    {
        JavaType contentType = refType.getContentType(); 
        TypeSerializer contentTypeSerializer = contentType.getTypeHandler();
        final SerializationConfig config = ctxt.getConfig();
        if (contentTypeSerializer == null) {
            contentTypeSerializer = ctxt.findTypeSerializer(contentType);
        }
        JsonSerializer<Object> contentSerializer = contentType.getValueHandler();
        for (Serializers serializers : customSerializers()) {
            JsonSerializer<?> ser = serializers.findReferenceSerializer(config, refType, beanDesc, format,
                    contentTypeSerializer, contentSerializer);
            if (ser != null) {
                return ser;
            }
        }
        if (refType.isTypeOrSubTypeOf(AtomicReference.class)) {
            return _buildReferenceSerializer(ctxt, AtomicReference.class,
                    refType, beanDesc, staticTyping,
                    contentTypeSerializer, contentSerializer);
        }
        if (refType.isTypeOrSubTypeOf(Optional.class)) {
            return _buildReferenceSerializer(ctxt, Optional.class,
                    refType, beanDesc, staticTyping,
                    contentTypeSerializer, contentSerializer);
        }
        if (refType.isTypeOrSubTypeOf(OptionalInt.class)) {
            return new OptionalIntSerializer();
        }
        if (refType.isTypeOrSubTypeOf(OptionalLong.class)) {
            return new OptionalLongSerializer();
        }
        if (refType.isTypeOrSubTypeOf(OptionalDouble.class)) {
            return new OptionalDoubleSerializer();
        }
        return null;
    }

    protected JsonSerializer<?> _buildReferenceSerializer(SerializerProvider prov, Class<?> baseType,
            ReferenceType refType, BeanDescription beanDesc, boolean staticTyping,
            TypeSerializer contentTypeSerializer, JsonSerializer<Object> contentSerializer)
        throws JsonMappingException
    {
        final JavaType contentType = refType.getReferencedType();
        JsonInclude.Value inclV = _findInclusionWithContent(prov, beanDesc,
                contentType, baseType);
        
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
        ReferenceTypeSerializer<?> ser;
        if (baseType == Optional.class) {
            ser = new Jdk8OptionalSerializer(refType, staticTyping,
                    contentTypeSerializer, contentSerializer);
        } else {
            ser = new AtomicReferenceSerializer(refType, staticTyping,
                    contentTypeSerializer, contentSerializer);
        }
        return ser.withContentInclusion(valueToSuppress, suppressNulls);
    }

    /*
    /**********************************************************************
    /* Factory methods, for non-container types
    /**********************************************************************
     */

    protected JsonSerializer<?> buildIteratorSerializer(SerializerProvider ctxt,
            JavaType type, BeanDescription beanDesc, JsonFormat.Value formatOverrides,
            boolean staticTyping,
            JavaType valueType)
        throws JsonMappingException
    {
        return new IteratorSerializer(valueType, staticTyping,
                ctxt.findTypeSerializer(valueType));
    }

    protected JsonSerializer<?> buildIterableSerializer(SerializerProvider ctxt,
            JavaType type, BeanDescription beanDesc, JsonFormat.Value effectiveFormat,
            boolean staticTyping,
            JavaType valueType)
        throws JsonMappingException
    {
        return new IterableSerializer(valueType, staticTyping,
                ctxt.findTypeSerializer(valueType));
    }

    protected JsonSerializer<?> buildEnumSerializer(SerializerProvider ctxt,
            JavaType type, BeanDescription beanDesc, JsonFormat.Value effectiveFormat)
        throws JsonMappingException
    {

        // As per [databind#24], may want to use alternate shape, serialize as JSON Object.
        // Challenge here is that EnumSerializer does not know how to produce
        // POJO style serialization, so we must handle that special case separately;
        // otherwise pass it to EnumSerializer.
        if (effectiveFormat.getShape() == JsonFormat.Shape.POJO) {
            // one special case: suppress serialization of "getDeclaringClass()"...
            ((BasicBeanDescription) beanDesc).removeProperty("declaringClass");
            // returning null will mean that eventually BeanSerializer gets constructed
            return null;
        }
        @SuppressWarnings("unchecked")
        Class<Enum<?>> enumClass = (Class<Enum<?>>) type.getRawClass();
        final SerializationConfig config = ctxt.getConfig();
        JsonSerializer<?> ser = EnumSerializer.construct(enumClass, config, beanDesc, effectiveFormat);
        if (_factoryConfig.hasSerializerModifiers()) {
            for (BeanSerializerModifier mod : _factoryConfig.serializerModifiers()) {
                ser = mod.modifyEnumSerializer(config, type, beanDesc, ser);
            }
        }
        return ser;
    }

    /*
    /**********************************************************************
    /* Other helper methods
    /**********************************************************************
     */

    /**
     * Helper method that will combine all available pieces of format configuration
     * and calculate effective format settings to use.
     *
     * @since 3.0
     */
    protected JsonFormat.Value _calculateEffectiveFormat(BeanDescription beanDesc,
            Class<?> baseType, JsonFormat.Value formatOverrides)
    {
        JsonFormat.Value fromType = beanDesc.findExpectedFormat(baseType);
        if (formatOverrides == null) {
            return fromType;
        }
        return JsonFormat.Value.merge(fromType, formatOverrides);
    }

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
        Object serDef = intr.findKeySerializer(prov.getConfig(), a);
        return prov.serializerInstance(a, serDef);
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
        Object serDef = intr.findContentSerializer(prov.getConfig(), a);
        return prov.serializerInstance(a, serDef); // ok to pass null
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
     */
    protected boolean usesStaticTyping(SerializationConfig config,
            BeanDescription beanDesc, TypeSerializer typeSer)
    {
        // 16-Aug-2010, tatu: If there is a (value) type serializer, we cannot force
        //    static typing; that would make it impossible to handle expected subtypes
        if (typeSer != null) {
            return false;
        }
        AnnotationIntrospector intr = config.getAnnotationIntrospector();
        JsonSerialize.Typing t = intr.findSerializationTyping(config, beanDesc.getClassInfo());
        if (t != null && t != JsonSerialize.Typing.DEFAULT_TYPING) {
            return (t == JsonSerialize.Typing.STATIC);
        }
        return config.isEnabled(MapperFeature.USE_STATIC_TYPING);
    }
}
