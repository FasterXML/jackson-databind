package tools.jackson.databind.ser;

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

import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.cfg.SerializerFactoryConfig;
import tools.jackson.databind.ext.OptionalHandlerFactory;
import tools.jackson.databind.ext.jdk8.*;
import tools.jackson.databind.introspect.*;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.jackson.JacksonSerializableSerializer;
import tools.jackson.databind.ser.jackson.JsonValueSerializer;
import tools.jackson.databind.ser.jdk.*;
import tools.jackson.databind.ser.std.*;
import tools.jackson.databind.type.*;
import tools.jackson.databind.util.*;

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
     * {@link tools.jackson.databind.ser.BeanSerializerFactory} for more details.
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
    public final SerializerFactory withSerializerModifier(ValueSerializerModifier modifier) {
        return withConfig(_factoryConfig.withSerializerModifier(modifier));
    }

    @Override
    public final SerializerFactory withNullValueSerializer(ValueSerializer<?> nvs) {
        return withConfig(_factoryConfig.withNullValueSerializer(nvs));
    }

    @Override
    public final SerializerFactory withNullKeySerializer(ValueSerializer<?> nks) {
        return withConfig(_factoryConfig.withNullKeySerializer(nks));
    }

    /*
    /**********************************************************************
    /* `SerializerFactory` impl
    /**********************************************************************
     */

// Implemented by sub-classes
//    public abstract ValueSerializer<Object> createSerializer(SerializationContext ctxt, ....)

    @Override
    @SuppressWarnings("unchecked")
    public ValueSerializer<Object> createKeySerializer(SerializationContext ctxt, JavaType keyType)
    {
        BeanDescription.Supplier beanDescRef = ctxt.lazyIntrospectBeanDescription(keyType);
        final SerializationConfig config = ctxt.getConfig();
        ValueSerializer<?> ser = null;
        // Minor optimization: to avoid constructing beanDesc, bail out if none registered
        if (_factoryConfig.hasKeySerializers()) {
            // Only thing we have here are module-provided key serializers:
            for (Serializers serializers : _factoryConfig.keySerializers()) {
                ser = serializers.findSerializer(config, keyType, beanDescRef, null);
                if (ser != null) {
                    break;
                }
            }
        }
        if (ser == null) {
            // [databind#2503]: Support `@Json[De]Serialize(keyUsing)` on key type too
            ser = _findKeySerializer(ctxt, beanDescRef.getClassInfo());
            if (ser == null) {
                // If no explicit serializer, see if type is JDK one for which there is
                // explicit deserializer: if so, can avoid further annotation lookups:
                ser = JDKKeySerializers.getStdKeySerializer(config, keyType.getRawClass(), false);
                if (ser == null) {
                    final BeanDescription beanDesc = beanDescRef.get();
                    // Check `@JsonKey` and `@JsonValue`, in this order
                    AnnotatedMember acc = beanDesc.findJsonKeyAccessor();
                    if (acc == null) {
                        acc = beanDesc.findJsonValueAccessor();
                    }
                    if (acc != null) {
                        ValueSerializer<?> delegate = createKeySerializer(ctxt, acc.getType());
                        if (config.canOverrideAccessModifiers()) {
                            ClassUtil.checkAndFixAccess(acc.getMember(),
                                    config.isEnabled(MapperFeature.OVERRIDE_PUBLIC_ACCESS_MODIFIERS));
                        }
                        // need to pass both type of key Object (on which accessor called), and actual
                        // value type that `JsonType`-annotated accessor returns (or contains, in case of field)
                        ser = JsonValueSerializer.construct(config, keyType, acc.getType(),
                                false, null, delegate, acc);
                    } else {
                        ser = JDKKeySerializers.getFallbackKeySerializer(config, keyType.getRawClass(),
                                beanDesc.getClassInfo());
                    }
                }
            }
        }

        // [databind#120]: Allow post-processing
        if (_factoryConfig.hasSerializerModifiers()) {
            for (ValueSerializerModifier mod : _factoryConfig.serializerModifiers()) {
                ser = mod.modifyKeySerializer(config, keyType, beanDescRef, ser);
            }
        }
        return (ValueSerializer<Object>) ser;
    }

    @Override
    public ValueSerializer<Object> getDefaultNullKeySerializer() {
        return _factoryConfig.getNullKeySerializer();
    }

    @Override
    public ValueSerializer<Object> getDefaultNullValueSerializer() {
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
    public TypeSerializer findPropertyContentTypeSerializer(SerializationContext ctxt,
            JavaType containerType, AnnotatedMember accessor)
    {
        return ctxt.getConfig().getTypeResolverProvider()
                .findPropertyContentTypeSerializer(ctxt, accessor, containerType);
    }

    /*
    /**********************************************************************
    /* Secondary serializer accessor methods
    /**********************************************************************
     */

    /**
     * Method called to see if one of primary per-class annotations
     * (or related, like implementing of {@link JacksonSerializable})
     * determines the serializer to use.
     *<p>
     * Currently handles things like:
     *<ul>
     * <li>If type implements {@link JacksonSerializable}, use that
     *  </li>
     * <li>If type has {@link com.fasterxml.jackson.annotation.JsonValue} annotation (or equivalent), build serializer
     *    based on that property
     *  </li>
     *</ul>
     */
    protected final ValueSerializer<?> findSerializerByAnnotations(SerializationContext ctxt,
            JavaType type, BeanDescription.Supplier beanDescRef)
    {
        // First: serializable by Jackson-specific interface?
        if (type.isTypeOrSubTypeOf(JacksonSerializable.class)) {
            return JacksonSerializableSerializer.instance;
        }
        // Second: @JsonValue for any type
        AnnotatedMember valueAccessor = beanDescRef.get().findJsonValueAccessor();
        if (valueAccessor != null) {
            if (ctxt.canOverrideAccessModifiers()) {
                ClassUtil.checkAndFixAccess(valueAccessor.getMember(),
                        ctxt.isEnabled(MapperFeature.OVERRIDE_PUBLIC_ACCESS_MODIFIERS));
            }
            ValueSerializer<Object> ser = findSerializerFromAnnotation(ctxt, valueAccessor);
            JavaType valueType = valueAccessor.getType();
            TypeSerializer vts = ctxt.findTypeSerializer(valueType);
            return JsonValueSerializer.construct(ctxt.getConfig(), type, valueType,
                    /* static typing */ false, vts, ser, valueAccessor);
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
    protected final ValueSerializer<?> findSerializerByPrimaryType(SerializationContext ctxt,
            JavaType type, BeanDescription.Supplier beanDescRef, JsonFormat.Value formatOverrides,
            boolean staticTyping)
    {
        // First: simple lookups for concrete types
        final Class<?> raw = type.getRawClass();
        ValueSerializer<?> ser;
        
        if ((ser = JDKCoreSerializers.find(raw)) != null) {
             return ser;
        }
        if ((ser = JDKStringLikeSerializer.find(raw)) != null) {
             return ser;
        }
        if ((ser = JDKMiscSerializers.find(raw)) != null) {
             return ser;
        }

        if (type.isTypeOrSubTypeOf(Calendar.class)) {
            return JavaUtilCalendarSerializer.instance;
        }
        if (type.isTypeOrSubTypeOf(Date.class)) {
            // 06-Nov-2020, tatu: Strange precedence challenge; need to consider
            //   "java.sql.Date" unfortunately
            if (!type.hasRawClass(Date.class)) {
                if ((ser = OptionalHandlerFactory.instance.findSerializer(ctxt.getConfig(), type) ) != null) {
                    return ser;
                }
            }
            return JavaUtilDateSerializer.instance;
        }
        // 19-Sep-2017, tatu: Jackson 3.x adds Java 8 types.
        // NOTE: while seemingly more of an add-on type, we must handle here because
        //   otherwise Bean-handling would be used instead...
        if (type.isTypeOrSubTypeOf(Stream.class)) {
            return new Jdk8StreamSerializer(type,
                    ctxt.getTypeFactory().findFirstTypeParameter(type, Stream.class));
        }

        if (type.isTypeOrSubTypeOf(Number.class)) {
            JsonFormat.Value format = _calculateEffectiveFormat(ctxt,
                    beanDescRef, Number.class, formatOverrides);

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
        if (type.isTypeOrSubTypeOf(Map.Entry.class)) {
            // 18-Oct-2015, tatu: With 2.7, need to dig type info:
            JavaType mapEntryType = type.findSuperType(Map.Entry.class);
            // 28-Apr-2015, tatu: TypeFactory does it all for us already so
            JavaType kt = mapEntryType.containedTypeOrUnknown(0);
            JavaType vt = mapEntryType.containedTypeOrUnknown(1);
            return buildMapEntrySerializer(ctxt, type, beanDescRef,
                    _calculateEffectiveFormat(ctxt, beanDescRef, Map.Entry.class, formatOverrides),
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
            return JDKStringLikeSerializer.find(Path.class);
        }
        // 23-Apr-2021, tatu: [databind#3130]: Suppress ClassLoader...
        if (ClassLoader.class.isAssignableFrom(raw)) {
            return new ToEmptyObjectSerializer(type);
        }
        // Then check for optional/external serializers
        return OptionalHandlerFactory.instance.findSerializer(ctxt.getConfig(), type);
    }

    /**
     * Reflection-based serialized find method, which checks if
     * given class implements one of recognized "add-on" interfaces.
     * Add-on here means a role that is usually or can be a secondary
     * trait: for example,
     * bean classes may implement {@link Iterable}, but their main
     * function is usually something else. The reason for
     */
    protected final ValueSerializer<?> findSerializerByAddonType(SerializationContext ctxt,
            JavaType javaType, BeanDescription.Supplier beanDescRef, JsonFormat.Value formatOverrides,
            boolean staticTyping)
    {
        final TypeFactory tf = ctxt.getTypeFactory();
        if (javaType.isTypeOrSubTypeOf(Iterator.class)) {
            return buildIteratorSerializer(ctxt, javaType, beanDescRef, formatOverrides,
                    staticTyping,
                    tf.findFirstTypeParameter(javaType, Iterator.class));
        }
        if (javaType.isTypeOrSubTypeOf(Iterable.class)) {
            return buildIterableSerializer(ctxt, javaType, beanDescRef, formatOverrides,
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
     * (@link tools.jackson.databind.annotation.JsonSerialize#using)
     * that tells the class to use for serialization.
     * Returns null if no such annotation found.
     */
    @SuppressWarnings("unchecked")
    protected ValueSerializer<Object> findSerializerFromAnnotation(SerializationContext ctxt,
            Annotated a)
    {
        Object serDef = ctxt.getAnnotationIntrospector().findSerializer(ctxt.getConfig(), a);
        if (serDef == null) {
            return null;
        }
        // One more thing however: may need to also apply a converter:
        return (ValueSerializer<Object>) findConvertingSerializer(ctxt, a,
                ctxt.serializerInstance(a, serDef));
    }

    /**
     * Helper method that will check whether given annotated entity (usually class,
     * but may also be a property accessor) indicates that a {@link Converter} is to
     * be used; and if so, to construct and return suitable serializer for it.
     * If not, will simply return given serializer as is.
     */
    protected ValueSerializer<?> findConvertingSerializer(SerializationContext ctxt,
            Annotated a, ValueSerializer<?> ser)
    {
        Converter<Object,Object> conv = findConverter(ctxt, a);
        if (conv == null) {
            return ser;
        }
        JavaType delegateType = conv.getOutputType(ctxt.getTypeFactory());
        return new StdDelegatingSerializer(conv, delegateType, ser, null);
    }

    protected Converter<Object,Object> findConverter(SerializationContext ctxt,
            Annotated a)
    {
        Object convDef = ctxt.getAnnotationIntrospector().findSerializationConverter(ctxt.getConfig(), a);
        if (convDef == null) {
            return null;
        }
        return ctxt.converterInstance(a, convDef);
    }

    /*
    /**********************************************************************
    /* Factory methods, container types:
    /**********************************************************************
     */

    protected ValueSerializer<?> buildContainerSerializer(SerializationContext ctxt,
            JavaType type, BeanDescription.Supplier beanDescRef, JsonFormat.Value formatOverrides,
            boolean staticTyping)
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
        ValueSerializer<Object> elementValueSerializer = _findContentSerializer(ctxt,
                beanDescRef.getClassInfo());
        final SerializationConfig config = ctxt.getConfig();
        if (type.isMapLikeType()) { // implements java.util.Map
            MapLikeType mlt = (MapLikeType) type;
            /* 29-Sep-2012, tatu: This is actually too early to (try to) find
             *  key serializer from property annotations, and can lead to caching
             *  issues (see [databind#75]). Instead, must be done from 'createContextual()' call.
             *  But we do need to check class annotations.
             */
            ValueSerializer<Object> keySerializer = _findKeySerializer(ctxt, beanDescRef.getClassInfo());
            if (mlt instanceof MapType) {
                return buildMapSerializer(ctxt, (MapType) mlt,
                        beanDescRef, formatOverrides, staticTyping,
                        keySerializer, elementTypeSerializer, elementValueSerializer);
            }
            // With Map-like, just 2 options: (1) Custom, (2) Annotations
            ValueSerializer<?> ser = null;
            MapLikeType mlType = (MapLikeType) type;
            for (Serializers serializers : customSerializers()) { // (1) Custom
                ser = serializers.findMapLikeSerializer(config, mlType,
                        beanDescRef, formatOverrides,
                        keySerializer, elementTypeSerializer, elementValueSerializer);
                if (ser != null) {
                    break;
                }
            }
            if (ser == null) { // (2) Annotations-based ones:
                ser = findSerializerByAnnotations(ctxt, type, beanDescRef);
            }
            if (ser != null) {
                if (_factoryConfig.hasSerializerModifiers()) {
                    for (ValueSerializerModifier mod : _factoryConfig.serializerModifiers()) {
                        ser = mod.modifyMapLikeSerializer(config, mlType, beanDescRef, ser);
                    }
                }
            }
            return ser;
        }
        if (type.isCollectionLikeType()) {
            CollectionLikeType clt = (CollectionLikeType) type;
            if (clt instanceof CollectionType) {
                return buildCollectionSerializer(ctxt, (CollectionType) clt,
                        beanDescRef, formatOverrides, staticTyping,
                        elementTypeSerializer, elementValueSerializer);
            }
            // With Collection-like, just 2 options: (1) Custom, (2) Annotations
            ValueSerializer<?> ser = null;
            CollectionLikeType clType = (CollectionLikeType) type;
            for (Serializers serializers : customSerializers()) { // (1) Custom
                ser = serializers.findCollectionLikeSerializer(config, clType,
                        beanDescRef, formatOverrides,
                        elementTypeSerializer, elementValueSerializer);
                if (ser != null) {
                    break;
                }
            }
            if (ser == null) { // (2) Annotations-based ones:
                ser = findSerializerByAnnotations(ctxt, type, beanDescRef);
            }
            if (ser != null) {
                if (_factoryConfig.hasSerializerModifiers()) {
                    for (ValueSerializerModifier mod : _factoryConfig.serializerModifiers()) {
                        ser = mod.modifyCollectionLikeSerializer(config, clType, beanDescRef, ser);
                    }
                }
            }
            return ser;
        }
        if (type.isArrayType()) {
            return buildArraySerializer(ctxt, (ArrayType) type,
                    beanDescRef, formatOverrides, staticTyping,
                    elementTypeSerializer, elementValueSerializer);
        }
        return null;
    }

    /**
     * Helper method that handles configuration details when constructing serializers for
     * {@link java.util.List} types that support efficient by-index access
     */
    protected ValueSerializer<?> buildCollectionSerializer(SerializationContext ctxt,
            CollectionType type, BeanDescription.Supplier beanDescRef, JsonFormat.Value formatOverrides,
            boolean staticTyping,
            TypeSerializer elementTypeSerializer, ValueSerializer<Object> elementValueSerializer)
    {
        SerializationConfig config = ctxt.getConfig();
        ValueSerializer<?> ser = null;
        // Order of lookups:
        // 1. Custom serializers
        // 2. Annotations (@JsonValue, @JsonDeserialize)
        // 3. Defaults
        for (Serializers serializers : customSerializers()) { // (1) Custom
            ser = serializers.findCollectionSerializer(config, type, beanDescRef, formatOverrides,
                    elementTypeSerializer, elementValueSerializer);
            if (ser != null) {
                break;
            }
        }

        if (ser == null) {
            ser = findSerializerByAnnotations(ctxt, type, beanDescRef); // (2) Annotations
            if (ser == null) {
                JsonFormat.Value format = _calculateEffectiveFormat(ctxt,
                        beanDescRef, Collection.class, formatOverrides);
                // We may also want to use serialize Collections "as beans", if (and only if)
                // shape specified as "POJO"
                if (format.getShape() == JsonFormat.Shape.POJO) {
                    return null;
                }
                if (type.isTypeOrSubTypeOf(EnumSet.class)) {
                    // this may or may not be available (Class doesn't; type of field/method does)
                    JavaType enumType = type.getContentType();
                    // and even if nominally there is something, only use if it really is enum
                    if (!enumType.isEnumImplType()) { // usually since it's `Enum.class`
                        enumType = null;
                    }
                    ser = buildEnumSetSerializer(enumType);
                } else {
                    Class<?> elementRaw = type.getContentType().getRawClass();
                    if (isIndexedList(type.getRawClass())) {
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
            for (ValueSerializerModifier mod : _factoryConfig.serializerModifiers()) {
                ser = mod.modifyCollectionSerializer(config, type, beanDescRef, ser);
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

    public  StdContainerSerializer<?> buildIndexedListSerializer(JavaType elemType,
            boolean staticTyping, TypeSerializer vts, ValueSerializer<Object> valueSerializer) {
        return new IndexedListSerializer(elemType, staticTyping, vts, valueSerializer);
    }

    public StdContainerSerializer<?> buildCollectionSerializer(JavaType elemType,
            boolean staticTyping, TypeSerializer vts, ValueSerializer<Object> valueSerializer) {
        return new CollectionSerializer(elemType, staticTyping, vts, valueSerializer);
    }

    public ValueSerializer<?> buildEnumSetSerializer(JavaType enumType) {
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
    protected ValueSerializer<?> buildMapSerializer(SerializationContext ctxt,
            MapType type, BeanDescription.Supplier beanDescRef, JsonFormat.Value formatOverrides,
            boolean staticTyping, ValueSerializer<Object> keySerializer,
            TypeSerializer elementTypeSerializer, ValueSerializer<Object> elementValueSerializer)
    {
        ValueSerializer<?> ser = null;

        // Order of lookups:
        // 1. Custom serializers
        // 2. Annotations (@JsonValue, @JsonDeserialize)
        // 3. Defaults

        final SerializationConfig config = ctxt.getConfig();
        for (Serializers serializers : customSerializers()) { // (1) Custom
            ser = serializers.findMapSerializer(config, type, beanDescRef, formatOverrides,
                    keySerializer, elementTypeSerializer, elementValueSerializer);
            if (ser != null) { break; }
        }
        if (ser == null) {
            ser = findSerializerByAnnotations(ctxt, type, beanDescRef); // (2) Annotations
            if (ser == null) {
                JsonFormat.Value format = _calculateEffectiveFormat(ctxt,
                        beanDescRef, Map.class, formatOverrides);

                // [databind#467]: This is where we could allow serialization "as POJO": But! It's
                // nasty to undo, and does not apply on per-property basis. So, hardly optimal
                if (format.getShape() == JsonFormat.Shape.POJO) {
                    return null;
                }

                Object filterId = findFilterId(config, beanDescRef);
                // 01-May-2016, tatu: Which base type to use here gets tricky, since
                //   most often it ought to be `Map` or `EnumMap`, but due to abstract
                //   mapping it will more likely be concrete type like `HashMap`.
                //   So, for time being, just pass `Map.class`
                final AnnotatedClass classInfo = beanDescRef.getClassInfo();
                JsonIgnoreProperties.Value ignorals = config.getDefaultPropertyIgnorals(Map.class,
                        classInfo);
                Set<String> ignored = (ignorals == null) ? null
                        : ignorals.findIgnoredForSerialization();
                JsonIncludeProperties.Value inclusions = config.getDefaultPropertyInclusions(Map.class,
                        classInfo);
                Set<String> included = (inclusions == null) ? null
                        : inclusions.getIncluded();
                MapSerializer mapSer = MapSerializer.construct(type,
                        staticTyping, elementTypeSerializer,
                        keySerializer, elementValueSerializer, filterId,
                        ignored, included);
                ser = _checkMapContentInclusion(ctxt, beanDescRef, mapSer);
            }
        }
        // [databind#120]: Allow post-processing
        if (_factoryConfig.hasSerializerModifiers()) {
            for (ValueSerializerModifier mod : _factoryConfig.serializerModifiers()) {
                ser = mod.modifyMapSerializer(config, type, beanDescRef, ser);
            }
        }
        return ser;
    }

    /**
     * Helper method that does figures out content inclusion value to use, if any,
     * and construct re-configured {@link MapSerializer} appropriately.
     */
    protected MapSerializer _checkMapContentInclusion(SerializationContext ctxt,
            BeanDescription.Supplier beanDescRef, MapSerializer mapSer)
    {
        final JavaType contentType = mapSer.getContentType();
        JsonInclude.Value inclV = _findInclusionWithContent(ctxt, beanDescRef,
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
            valueToSuppress = ctxt.includeFilterInstance(null, inclV.getContentFilter());
            if (valueToSuppress != null) { // is this legal?
                suppressNulls = ctxt.includeFilterSuppressNulls(valueToSuppress);
            }
            break;
        case NON_NULL:
        default: // should not matter but...
            valueToSuppress = null;
            break;
        }
        return mapSer.withContentInclusion(valueToSuppress, suppressNulls);
    }

    protected ValueSerializer<?> buildMapEntrySerializer(SerializationContext ctxt,
            JavaType type, BeanDescription.Supplier beanDescRef,  JsonFormat.Value effectiveFormat,
            boolean staticTyping,
            JavaType keyType, JavaType valueType)
    {
        // [databind#865]: Allow serialization "as POJO" -- note: to undo, declare
        //   serialization as `Shape.NATURAL` instead; that's JSON Object too.
        if (effectiveFormat.getShape() == JsonFormat.Shape.POJO) {
            return null;
        }
        MapEntrySerializer ser = new MapEntrySerializer(valueType, keyType,
                valueType, staticTyping, ctxt.findTypeSerializer(valueType), null);

        final JavaType contentType = ser.getContentType();
        JsonInclude.Value inclV = _findInclusionWithContent(ctxt, beanDescRef,
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
            if (valueToSuppress != null) { // is this legal?
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
    protected JsonInclude.Value _findInclusionWithContent(SerializationContext ctxt,
            BeanDescription.Supplier beanDescRef,
            JavaType contentType, Class<?> configType)
    {
        final SerializationConfig config = ctxt.getConfig();

        // Defaulting gets complicated because we might have two distinct
        //   axis to consider: Container type itself , and then value (content) type.
        //  Start with Container-defaults, then use more-specific value override, if any.

        // Start by getting global setting, overridden by Map-type-override
        JsonInclude.Value inclV = beanDescRef.get().findPropertyInclusion(config.getDefaultPropertyInclusion());
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
    protected ValueSerializer<?> buildArraySerializer(SerializationContext ctxt,
            ArrayType type, BeanDescription.Supplier beanDescRef, JsonFormat.Value formatOverrides,
            boolean staticTyping,
            TypeSerializer elementTypeSerializer, ValueSerializer<Object> elementValueSerializer)
    {
        // 25-Jun-2015, tatu: Note that unlike with Collection(Like) and Map(Like) types, array
        //   types cannot be annotated (in theory I guess we could have mix-ins but... ?)
        //   so we need not do primary annotation lookup here.
        //   So all we need is (1) Custom, (2) Default array serializers
        SerializationConfig config = ctxt.getConfig();
        ValueSerializer<?> ser = null;

        for (Serializers serializers : customSerializers()) { // (1) Custom
             if ((ser = serializers.findArraySerializer(config, type, beanDescRef, formatOverrides,
                     elementTypeSerializer, elementValueSerializer)) != null) {
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
                     ser = JDKArraySerializers.findStandardImpl(raw);
                 }
             }
             if (ser == null) {
                 ser = new ObjectArraySerializer(type.getContentType(), staticTyping, elementTypeSerializer,
                         elementValueSerializer);
             }
         }
         // [databind#120]: Allow post-processing
         if (_factoryConfig.hasSerializerModifiers()) {
             for (ValueSerializerModifier mod : _factoryConfig.serializerModifiers()) {
                 ser = mod.modifyArraySerializer(config, type, beanDescRef, ser);
             }
         }
         return ser;
    }

    /*
    /**********************************************************************
    /* Factory methods for Reference types
    /**********************************************************************
     */

    public ValueSerializer<?> findReferenceSerializer(SerializationContext ctxt,
            ReferenceType refType, BeanDescription.Supplier beanDesc, JsonFormat.Value format,
            boolean staticTyping)
    {
        JavaType contentType = refType.getContentType();
        TypeSerializer contentTypeSerializer = (TypeSerializer) contentType.getTypeHandler();
        final SerializationConfig config = ctxt.getConfig();
        if (contentTypeSerializer == null) {
            contentTypeSerializer = ctxt.findTypeSerializer(contentType);
        }
        @SuppressWarnings("unchecked")
        ValueSerializer<Object> contentSerializer = (ValueSerializer<Object>) contentType.getValueHandler();
        for (Serializers serializers : customSerializers()) {
            ValueSerializer<?> ser = serializers.findReferenceSerializer(config, refType, beanDesc, format,
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

    protected ValueSerializer<?> _buildReferenceSerializer(SerializationContext ctxt, Class<?> baseType,
            ReferenceType refType, BeanDescription.Supplier beanDescRef, boolean staticTyping,
            TypeSerializer contentTypeSerializer, ValueSerializer<Object> contentSerializer)
    {
        final JavaType contentType = refType.getReferencedType();
        JsonInclude.Value inclV = _findInclusionWithContent(ctxt, beanDescRef,
                contentType, baseType);

        // Need to support global legacy setting, for now:
        JsonInclude.Include incl = (inclV == null) ? JsonInclude.Include.USE_DEFAULTS : inclV.getContentInclusion();
        Object valueToSuppress;
        boolean suppressNulls;

        if (incl == JsonInclude.Include.USE_DEFAULTS || incl == JsonInclude.Include.ALWAYS) {
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

    protected ValueSerializer<?> buildIteratorSerializer(SerializationContext ctxt,
            JavaType type, BeanDescription.Supplier beanDesc, JsonFormat.Value formatOverrides,
            boolean staticTyping,
            JavaType valueType)
    {
        return new IteratorSerializer(valueType, staticTyping,
                ctxt.findTypeSerializer(valueType));
    }

    protected ValueSerializer<?> buildIterableSerializer(SerializationContext ctxt,
            JavaType type, BeanDescription.Supplier beanDescRef,
            JsonFormat.Value effectiveFormat, boolean staticTyping,
            JavaType valueType)
    {
        return new IterableSerializer(valueType, staticTyping,
                ctxt.findTypeSerializer(valueType));
    }

    protected ValueSerializer<?> buildEnumSerializer(SerializationContext ctxt,
            JavaType type, BeanDescription.Supplier beanDescRef, JsonFormat.Value effectiveFormat)
    {
        // As per [databind#24], may want to use alternate shape, serialize as JSON Object.
        // Challenge here is that EnumSerializer does not know how to produce
        // POJO style serialization, so we must handle that special case separately;
        // otherwise pass it to EnumSerializer.
        JsonFormat.Shape shape = effectiveFormat.getShape();
        if (shape == JsonFormat.Shape.POJO || shape == JsonFormat.Shape.OBJECT) {
            final BeanDescription beanDesc = beanDescRef.get();
            // one special case: suppress serialization of "getDeclaringClass()"...
            ((BasicBeanDescription) beanDesc).removeProperty("declaringClass");
            // [databind#2787]: remove self-referencing enum fields introduced by annotation flattening of mixins
            if (type.isEnumType()){
                _removeEnumSelfReferences(beanDesc);
            }
            // returning null will mean that eventually BeanSerializer gets constructed
            return null;
        }
        @SuppressWarnings("unchecked")
        Class<Enum<?>> enumClass = (Class<Enum<?>>) type.getRawClass();
        final SerializationConfig config = ctxt.getConfig();
        ValueSerializer<?> ser = EnumSerializer.construct(enumClass, config,
                beanDescRef.get(), effectiveFormat);
        if (_factoryConfig.hasSerializerModifiers()) {
            for (ValueSerializerModifier mod : _factoryConfig.serializerModifiers()) {
                ser = mod.modifyEnumSerializer(config, type, beanDescRef, ser);
            }
        }
        return ser;
    }

    /**
     * Helper method used for serialization {@link Enum} as {@link JsonFormat.Shape#OBJECT}.
     * Removes any  self-referencing properties from its bean description before it is
     * transformed into a JSON Object as configured by {@link JsonFormat.Shape#OBJECT}.
     * <p>
     * Internally, this method iterates through {@link BeanDescription#findProperties()}
     * and removes self references.
     *
     * @param beanDesc the bean description to remove Enum properties from.
     *
     * @since 2.16
     */
    private void _removeEnumSelfReferences(BeanDescription beanDesc) {
        Class<?> aClass = ClassUtil.findEnumType(beanDesc.getBeanClass());
        Iterator<BeanPropertyDefinition> it = beanDesc.findProperties().iterator();
        while (it.hasNext()) {
            BeanPropertyDefinition property = it.next();
            JavaType propType = property.getPrimaryType();
            // is the property a self-reference?
            if (propType.isEnumType() && propType.isTypeOrSubTypeOf(aClass)
                    // [databind#4564] Since 2.16.3, Enum's should allow self as field, so let's remove only if static.
                    && property.getAccessor().isStatic())
            {
                it.remove();
            }
        }
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
    protected JsonFormat.Value _calculateEffectiveFormat(SerializationContext ctxt,
            BeanDescription.Supplier beanDescRef,
            Class<?> baseType, JsonFormat.Value formatOverrides)
    {
        JsonFormat.Value fromType = beanDescRef.findExpectedFormat(baseType);
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
    protected ValueSerializer<Object> _findKeySerializer(SerializationContext ctxt,
            Annotated a)
    {
        AnnotationIntrospector intr = ctxt.getAnnotationIntrospector();
        Object serDef = intr.findKeySerializer(ctxt.getConfig(), a);
        return ctxt.serializerInstance(a, serDef);
    }

    /**
     * Helper method called to try to find whether there is an annotation in the
     * class that indicates content ("value") serializer to use.
     * If so, will try to instantiate value serializer and return it; otherwise returns null.
     */
    protected ValueSerializer<Object> _findContentSerializer(SerializationContext ctxt,
            Annotated a)
    {
        AnnotationIntrospector intr = ctxt.getAnnotationIntrospector();
        Object serDef = intr.findContentSerializer(ctxt.getConfig(), a);
        return ctxt.serializerInstance(a, serDef); // ok to pass null
    }

    /**
     * Method called to find filter that is configured to be used with bean
     * serializer being built, if any.
     */
    protected Object findFilterId(SerializationConfig config, BeanDescription.Supplier beanDescRef) {
        return config.getAnnotationIntrospector().findFilterId(config,
                (Annotated)beanDescRef.getClassInfo());
    }

    /**
     * Helper method to check whether global settings and/or class
     * annotations for the bean class indicate that static typing
     * (declared types)  should be used for properties.
     * (instead of dynamic runtime types).
     */
    protected boolean usesStaticTyping(SerializationConfig config,
            BeanDescription.Supplier beanDescRef)
    {
        JsonSerialize.Typing t = config.getAnnotationIntrospector()
                .findSerializationTyping(config, beanDescRef.getClassInfo());
        if (t != null) {
            switch (t) {
            case DYNAMIC:
                return false;
            case STATIC:
                return true;
            case DEFAULT_TYPING:
                // fall through
            }
        }
        return config.isEnabled(MapperFeature.USE_STATIC_TYPING);
    }
}
