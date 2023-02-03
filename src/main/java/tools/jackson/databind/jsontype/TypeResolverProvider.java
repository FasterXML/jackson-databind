package tools.jackson.databind.jsontype;

import java.util.Collection;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.*;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.introspect.Annotated;
import tools.jackson.databind.introspect.AnnotatedClass;
import tools.jackson.databind.introspect.AnnotatedMember;
import tools.jackson.databind.jsontype.impl.StdTypeResolverBuilder;

/**
 * Abstraction used for allowing construction and registration of custom
 * {@link TypeResolverBuilder}s, used in turn for actual construction of
 * {@link tools.jackson.databind.jsontype.TypeSerializer}s
 * and {@link tools.jackson.databind.jsontype.TypeDeserializer}s
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
     * {@code findSubtypes()}
     *
     * @param baseType Base java type of value for which resolver is to be found
     * @param classInfo Introspected annotation information for the class (type)
     *
     * @return Type resolver builder for given type, if one found; null if none
     */
    public TypeSerializer findTypeSerializer(SerializerProvider ctxt,
            JavaType baseType, AnnotatedClass classInfo)
    {
        final SerializationConfig config = ctxt.getConfig();
        TypeResolverBuilder<?> b = _findTypeResolver(config, classInfo, baseType);
        // Ok: if there is no explicit type info handler, we may want to
        // use a default. If so, config object knows what to use.
        if (b == null) {
            b = config.getDefaultTyper(baseType);
            if (b == null) {
                return null;
            }
        }
        Collection<NamedType> subtypes = config.getSubtypeResolver().collectAndResolveSubtypesByClass(config, classInfo);

        // 10-Jun-2015, tatu: Since not created for Bean Property, no need for post-processing
        //    wrt EXTERNAL_PROPERTY
        return b.buildTypeSerializer(ctxt, baseType, subtypes);
    }

    public TypeDeserializer findTypeDeserializer(DeserializationContext ctxt,
            JavaType baseType, AnnotatedClass classInfo)
    {
        final DeserializationConfig config = ctxt.getConfig();
        TypeResolverBuilder<?> b = _findTypeResolver(config, classInfo, baseType);

        // Ok: if there is no explicit type info handler, we may want to
        // use a default. If so, config object knows what to use.
        if (b == null) {
            b = config.getDefaultTyper(baseType);
            if (b == null) {
                return null;
            }
        }
        Collection<NamedType> subtypes = config.getSubtypeResolver().collectAndResolveSubtypesByTypeId(config, classInfo);

        // May need to figure out default implementation, if none found yet
        // (note: check for abstract type is not 100% mandatory, more of an optimization)
        if ((b.getDefaultImpl() == null) && baseType.isAbstract()) {
            JavaType defaultType = config.mapAbstractType(baseType);
            if ((defaultType != null) && !defaultType.hasRawClass(baseType.getRawClass())) {
                b = b.withDefaultImpl(defaultType.getRawClass());
            }
        }
        return b.buildTypeDeserializer(ctxt, baseType, subtypes);
    }

    /*
    /**********************************************************************
    /* Public API, for property
    /**********************************************************************
     */

    public TypeSerializer findPropertyTypeSerializer(SerializerProvider ctxt,
            AnnotatedMember accessor, JavaType baseType)
    {
        TypeResolverBuilder<?> b = null;
        final SerializationConfig config = ctxt.getConfig();
        // As per definition of @JsonTypeInfo, check for annotation only for non-container types
        if (!baseType.isContainerType() && !baseType.isReferenceType()) {
            b = _findTypeResolver(config, accessor, baseType);
        }
        // No annotation on property? Then base it on actual type (and further, default typing if need be)
        if (b == null) {
            return findTypeSerializer(ctxt, baseType, ctxt.introspectClassAnnotations(baseType));
        }
        Collection<NamedType> subtypes = config.getSubtypeResolver().collectAndResolveSubtypesByClass(
                config, accessor, baseType);
        // 10-Jun-2015, tatu: Since not created for Bean Property, no need for post-processing
        //    wrt EXTERNAL_PROPERTY
        return b.buildTypeSerializer(ctxt, baseType, subtypes);
    }

    public TypeDeserializer findPropertyTypeDeserializer(DeserializationContext ctxt,
            AnnotatedMember accessor, JavaType baseType)
    {
        TypeResolverBuilder<?> b = null;
        final DeserializationConfig config = ctxt.getConfig();
        // As per definition of @JsonTypeInfo, check for annotation only for non-container types
        if (!baseType.isContainerType() && !baseType.isReferenceType()) {
            b = _findTypeResolver(config, accessor, baseType);
        }
        // No annotation on property? Then base it on actual type (and further, default typing if need be)
        if (b == null) {
            return findTypeDeserializer(ctxt, baseType, ctxt.introspectClassAnnotations(baseType));
        }
        Collection<NamedType> subtypes = config.getSubtypeResolver().collectAndResolveSubtypesByTypeId(config,
                    accessor, baseType);
        // May need to figure out default implementation, if none found yet
        // (note: check for abstract type is not 100% mandatory, more of an optimization)
        if ((b.getDefaultImpl() == null) && baseType.isAbstract()) {
            JavaType defaultType = config.mapAbstractType(baseType);
            if ((defaultType != null) && !defaultType.hasRawClass(baseType.getRawClass())) {
                b = b.withDefaultImpl(defaultType.getRawClass());
            }
        }
        return b.buildTypeDeserializer(ctxt, baseType, subtypes);
    }

    public TypeSerializer findPropertyContentTypeSerializer(SerializerProvider ctxt,
            AnnotatedMember accessor, JavaType containerType)
    {
        final JavaType contentType = containerType.getContentType();
        // First: let's ensure property is a container type: caller should have
        // verified but just to be sure
        if (contentType == null) {
            throw new IllegalArgumentException("Must call method with a container or reference type (got "+containerType+")");
        }
        final SerializationConfig config = ctxt.getConfig();
        TypeResolverBuilder<?> b = _findTypeResolver(config, accessor, containerType);
        // No annotation on property? Then base it on actual type (and further, default typing if need be)
        if (b == null) {
            return findTypeSerializer(ctxt, contentType,
                    ctxt.introspectClassAnnotations(contentType.getRawClass()));
        }
        Collection<NamedType> subtypes = config.getSubtypeResolver().collectAndResolveSubtypesByClass(
                config, accessor, contentType);
        return b.buildTypeSerializer(ctxt, contentType, subtypes);
    }

    public TypeDeserializer findPropertyContentTypeDeserializer(DeserializationContext ctxt,
            AnnotatedMember accessor, JavaType containerType)
    {
        final JavaType contentType = containerType.getContentType();
        final DeserializationConfig config = ctxt.getConfig();
        // First: let's ensure property is a container type: caller should have
        // verified but just to be sure
        if (contentType == null) {
            throw new IllegalArgumentException("Must call method with a container or reference type (got "+containerType+")");
        }
        TypeResolverBuilder<?> b = _findTypeResolver(config, accessor, containerType);
        if (b == null) {
            return findTypeDeserializer(ctxt, contentType, ctxt.introspectClassAnnotations(contentType));
        }
        Collection<NamedType> subtypes = config.getSubtypeResolver().collectAndResolveSubtypesByTypeId(config,
                accessor, contentType);
        // May need to figure out default implementation, if none found yet
        // (note: check for abstract type is not 100% mandatory, more of an optimization)
        if ((b.getDefaultImpl() == null) && contentType.isAbstract()) {
            JavaType defaultType = config.mapAbstractType(contentType);
            if ((defaultType != null) && !defaultType.hasRawClass(contentType.getRawClass())) {
                b = b.withDefaultImpl(defaultType.getRawClass());
            }
        }
        return b.buildTypeDeserializer(ctxt, contentType, subtypes);
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
