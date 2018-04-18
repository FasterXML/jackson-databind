package com.fasterxml.jackson.databind.jsontype;

import java.util.Collection;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;

import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.jsontype.impl.StdTypeResolverBuilder;

/**
 * Abstraction used for allowing construction and registration of custom
 * {@link TypeResolverBuilder}s, used in turn for actual construction of
 * {@link com.fasterxml.jackson.databind.jsontype.TypeSerializer}s
 * and {@link com.fasterxml.jackson.databind.jsontype.TypeDeserializer}s
 * for Polymorphic type handling.
 * At this point contains both API and default implementation.
 *
 * @since 3.0
 */
public class TypeResolverProvider
    implements java.io.Serializable
{
    private static final long serialVersionUID = 3L;

    protected final static StdTypeResolverBuilder NO_RESOLVER = StdTypeResolverBuilder.noTypeInfoBuilder();

    /*
    /**********************************************************************
    /* Public API, for class
    /**********************************************************************
     */

    /**
     * Method for checking if given class has annotations that indicate
     * that specific type resolver is to be used for handling instances of given type.
     * This includes not only
     * instantiating resolver builder, but also configuring it based on
     * relevant annotations (not including ones checked with a call to
     * {@link #findSubtypes}
     *
     * @param config Configuration settings in effect (for serialization or deserialization)
     * @param classInfo Introspected annotation information for the class (type)
     * @param baseType Base java type of value for which resolver is to be found
     * 
     * @return Type resolver builder for given type, if one found; null if none
     */
    public TypeSerializer findTypeSerializer(SerializationConfig config,
            JavaType baseType, AnnotatedClass classInfo)
        throws JsonMappingException
    {
        TypeResolverBuilder<?> b = _findTypeResolver(config, classInfo, baseType);
        // Ok: if there is no explicit type info handler, we may want to
        // use a default. If so, config object knows what to use.
        Collection<NamedType> subtypes = null;
        if (b == null) {
            b = config.getDefaultTyper(baseType);
        } else {
            subtypes = config.getSubtypeResolver().collectAndResolveSubtypesByClass(config, classInfo);
        }
        if (b == null) {
            return null;
        }
        // 10-Jun-2015, tatu: Since not created for Bean Property, no need for post-processing
        //    wrt EXTERNAL_PROPERTY
        return b.buildTypeSerializer(config, baseType, subtypes);
    }

    public TypeDeserializer findTypeDeserializer(DeserializationConfig config,
            JavaType baseType, AnnotatedClass classInfo)
        throws JsonMappingException
    {
        TypeResolverBuilder<?> b = _findTypeResolver(config, classInfo, baseType);

        // Ok: if there is no explicit type info handler, we may want to
        // use a default. If so, config object knows what to use.
        Collection<NamedType> subtypes = null;
        if (b == null) {
            b = config.getDefaultTyper(baseType);
            if (b == null) {
                return null;
            }
        } else {
            subtypes = config.getSubtypeResolver().collectAndResolveSubtypesByTypeId(config, classInfo);
        }
        // May need to figure out default implementation, if none found yet
        // (note: check for abstract type is not 100% mandatory, more of an optimization)
        if ((b.getDefaultImpl() == null) && baseType.isAbstract()) {
            JavaType defaultType = config.mapAbstractType(baseType);
            if ((defaultType != null) && !defaultType.hasRawClass(baseType.getRawClass())) {
                b = b.defaultImpl(defaultType.getRawClass());
            }
        }
        return b.buildTypeDeserializer(config, baseType, subtypes);
    }

    /*
    /**********************************************************************
    /* Public API, for property
    /**********************************************************************
     */

    public TypeSerializer findPropertyTypeSerializer(SerializationConfig config,
            AnnotatedMember accessor, JavaType baseType)
        throws JsonMappingException
    {
        TypeResolverBuilder<?> b = null;
        // As per definition of @JsonTypeInfo, check for annotation only for non-container types
        if (!baseType.isContainerType() && !baseType.isReferenceType()) {
            b = _findTypeResolver(config, accessor, baseType);
        }
        // No annotation on property? Then base it on actual type (and further, default typing if need be)
        if (b == null) {
            BeanDescription bean = config.introspectClassAnnotations(baseType.getRawClass());
            return findTypeSerializer(config, baseType, bean.getClassInfo());
        }
        Collection<NamedType> subtypes = config.getSubtypeResolver().collectAndResolveSubtypesByClass(
                config, accessor, baseType);
        // 10-Jun-2015, tatu: Since not created for Bean Property, no need for post-processing
        //    wrt EXTERNAL_PROPERTY
        return b.buildTypeSerializer(config, baseType, subtypes);
    }

    public TypeDeserializer findPropertyTypeDeserializer(DeserializationConfig config,
            AnnotatedMember accessor, JavaType baseType)
        throws JsonMappingException
    {
        TypeResolverBuilder<?> b = null;
        // As per definition of @JsonTypeInfo, check for annotation only for non-container types
        if (!baseType.isContainerType() && !baseType.isReferenceType()) {
            b = _findTypeResolver(config, accessor, baseType);
        }
        // No annotation on property? Then base it on actual type (and further, default typing if need be)
        if (b == null) {
            return findTypeDeserializer(config, baseType,
                    config.introspectClassAnnotations(baseType.getRawClass()).getClassInfo());
        }
        Collection<NamedType> subtypes = config.getSubtypeResolver().collectAndResolveSubtypesByTypeId(config,
                    accessor, baseType);
        // May need to figure out default implementation, if none found yet
        // (note: check for abstract type is not 100% mandatory, more of an optimization)
        if ((b.getDefaultImpl() == null) && baseType.isAbstract()) {
            JavaType defaultType = config.mapAbstractType(baseType);
            if ((defaultType != null) && !defaultType.hasRawClass(baseType.getRawClass())) {
                b = b.defaultImpl(defaultType.getRawClass());
            }
        }
        return b.buildTypeDeserializer(config, baseType, subtypes);
    }

    public TypeSerializer findPropertyContentTypeSerializer(SerializationConfig config,
            AnnotatedMember accessor, JavaType containerType)
        throws JsonMappingException
    {
        final JavaType contentType = containerType.getContentType();
        // First: let's ensure property is a container type: caller should have
        // verified but just to be sure
        if (contentType == null) {
            throw new IllegalArgumentException("Must call method with a container or reference type (got "+containerType+")");
        }
        TypeResolverBuilder<?> b = _findTypeResolver(config, accessor, containerType);
        // No annotation on property? Then base it on actual type (and further, default typing if need be)
        if (b == null) {
            BeanDescription beanDesc = config.introspectClassAnnotations(contentType.getRawClass());
            return findTypeSerializer(config, contentType, beanDesc.getClassInfo());
        }
        Collection<NamedType> subtypes = config.getSubtypeResolver().collectAndResolveSubtypesByClass(
                config, accessor, contentType);
        return b.buildTypeSerializer(config, contentType, subtypes);
    }

    public TypeDeserializer findPropertyContentTypeDeserializer(DeserializationConfig config,
            AnnotatedMember accessor, JavaType containerType)
        throws JsonMappingException
    {
        final JavaType contentType = containerType.getContentType();
        // First: let's ensure property is a container type: caller should have
        // verified but just to be sure
        if (contentType == null) {
            throw new IllegalArgumentException("Must call method with a container or reference type (got "+containerType+")");
        }
        TypeResolverBuilder<?> b = _findTypeResolver(config, accessor, containerType);
        if (b == null) {
            return findTypeDeserializer(config, contentType,
                    config.introspectClassAnnotations(contentType.getRawClass()).getClassInfo());
        }
        Collection<NamedType> subtypes = config.getSubtypeResolver().collectAndResolveSubtypesByTypeId(config,
                accessor, contentType);
        // May need to figure out default implementation, if none found yet
        // (note: check for abstract type is not 100% mandatory, more of an optimization)
        if ((b.getDefaultImpl() == null) && contentType.isAbstract()) {
            JavaType defaultType = config.mapAbstractType(contentType);
            if ((defaultType != null) && !defaultType.hasRawClass(contentType.getRawClass())) {
                b = b.defaultImpl(defaultType.getRawClass());
            }
        }
        return b.buildTypeDeserializer(config, contentType, subtypes);
    }

    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */

    protected TypeResolverBuilder<?> _findTypeResolver(MapperConfig<?> config,
            Annotated ann, JavaType baseType)
    {
        final AnnotationIntrospector ai = config.getAnnotationIntrospector();
        JsonTypeInfo.Value typeInfo = ai.findPolymorphicTypeInfo(config, ann);

        // First: maybe we have explicit type resolver?
        TypeResolverBuilder<?> b;
        Object customResolverOb = ai.findTypeResolverBuilder(config, ann);
        if (customResolverOb != null) {
            // 08-Mar-2018, tatu: Should `NONE` block custom one? Or not?
            if ((typeInfo != null) && (typeInfo.getIdType() == JsonTypeInfo.Id.NONE)) {
                return null;
            }
            if (customResolverOb instanceof Class<?>) {
                @SuppressWarnings("unchecked")
                Class<TypeResolverBuilder<?>> cls = (Class<TypeResolverBuilder<?>>) customResolverOb;
                b = config.typeResolverBuilderInstance(ann, cls);
            } else {
                b = (TypeResolverBuilder<?>) customResolverOb;
            }
        } else { // if not, use standard one, but only if indicated by annotations
            if (typeInfo == null) {
                return null;
            }
            // bit special; must return 'marker' to block use of default typing:
            if (typeInfo.getIdType() == JsonTypeInfo.Id.NONE) {
                return NO_RESOLVER;
            }
            // 13-Aug-2011, tatu: One complication; external id
            //   only works for properties; so if declared for a Class, we will need
            //   to map it to "PROPERTY" instead of "EXTERNAL_PROPERTY"
            if (ann instanceof AnnotatedClass) {
                JsonTypeInfo.As inclusion = typeInfo.getInclusionType();
                if (inclusion == JsonTypeInfo.As.EXTERNAL_PROPERTY && (ann instanceof AnnotatedClass)) {
                    typeInfo = typeInfo.withInclusionType(JsonTypeInfo.As.PROPERTY);
                }
            }
            b = _constructStdTypeResolverBuilder(config, typeInfo, baseType);
        }
        // Does it define a custom type id resolver?
        Object customIdResolverOb = ai.findTypeIdResolver(config, ann);
        TypeIdResolver idResolver = null;

        if (customIdResolverOb != null) {
            if (customIdResolverOb instanceof Class<?>) {
                @SuppressWarnings("unchecked")
                Class<TypeIdResolver> cls = (Class<TypeIdResolver>) customIdResolverOb;
                idResolver = config.typeIdResolverInstance(ann, cls);
                idResolver.init(baseType);
            }
        }
        b = b.init(typeInfo, idResolver);
        return b;
    }

    protected TypeResolverBuilder<?> _constructStdTypeResolverBuilder(MapperConfig<?> config,
            JsonTypeInfo.Value typeInfo, JavaType baseType) {
        return new StdTypeResolverBuilder(typeInfo);
    }
}
