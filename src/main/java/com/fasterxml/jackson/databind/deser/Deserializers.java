package com.fasterxml.jackson.databind.deser;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.type.*;

/**
 * Interface that defines API for simple extensions that can provide additional deserializers
 * for various types. Access is by a single callback method; instance is to either return
 * a configured {@link JsonDeserializer} for specified type, or null to indicate that it
 * does not support handling of the type. In latter case, further calls can be made
 * for other providers; in former case returned deserializer is used for handling of
 * instances of specified type.
 *<p>
 * It is <b>strongly recommended</b> that implementations always extend {@link Deserializers.Base}
 * and NOT just implement {@link Deserializers}.
 */
public interface Deserializers
{
    // // // Scalar types first:

    /**
     * Method called to locate deserializer for specified {@link java.lang.Enum} type.
     * 
     * @param type Type of {@link java.lang.Enum} instances to deserialize
     * @param config Configuration in effect
     * @param beanDesc Definition of the enumeration type that contains class annotations and
     *    other information typically needed for building deserializers
     * 
     * @return Deserializer to use for the type; or null if this provider does not know how to construct it
     */
    public JsonDeserializer<?> findEnumDeserializer(Class<?> type,
            DeserializationConfig config, BeanDescription beanDesc)
        throws JsonMappingException;

    /**
     * Method called to locate deserializer for specified JSON tree node type.
     * 
     * @param nodeType Specific type of JSON tree nodes to deserialize
     *  (subtype of {@link com.fasterxml.jackson.databind.JsonNode})
     * @param config Configuration in effect
     * 
     * @return Deserializer to use for the type; or null if this provider does not know how to construct it
     */
    public JsonDeserializer<?> findTreeNodeDeserializer(Class<? extends JsonNode> nodeType,
            DeserializationConfig config, BeanDescription beanDesc)
        throws JsonMappingException;

    /**
     * Method called to locate deserializer for specified value type which does not belong to any other
     * category (not an Enum, Collection, Map, Array, reference value or tree node)
     * 
     * @param type Bean type to deserialize
     * @param config Configuration in effect
     * @param beanDesc Definition of the enumeration type that contains class annotations and
     *    other information typically needed for building deserializers
     * 
     * @return Deserializer to use for the type; or null if this provider does not know how to construct it
     */
    public JsonDeserializer<?> findBeanDeserializer(JavaType type,
            DeserializationConfig config, BeanDescription beanDesc)
        throws JsonMappingException;
    
    // // // Then container types
    
    /**
     * Method called to locate deserializer for value that is of referential
     * type,
     * 
     * @param refType Specific referential type to deserialize
     * @param config Configuration in effect
     * @param beanDesc Definition of the reference type that contains class annotations and
     *    other information typically needed for building deserializers
     * @param contentTypeDeserializer Possible type deserializer for referenced value
     * @param contentDeserializer Value deserializer to use for referenced value, if indicated
     *    by property annotation
     * 
     * @return Deserializer to use for the type; or null if this provider does not know how to construct it
     *
     * @since 2.7
     */
    public JsonDeserializer<?> findReferenceDeserializer(ReferenceType refType,
            DeserializationConfig config, BeanDescription beanDesc,
            TypeDeserializer contentTypeDeserializer, JsonDeserializer<?> contentDeserializer)
        throws JsonMappingException;

    /**
     * Method called to locate serializer for specified array type.
     *<p>
     * Deserializer for element type may be passed, if configured explicitly at higher level (by
     * annotations, typically), but usually are not.
     * Type deserializer for element is passed if one is needed based on contextual information
     * (annotations on declared element class; or on field or method type is associated with).
     * 
     * @param type Type of array instances to deserialize
     * @param config Configuration in effect
     * @param beanDesc Definition of the enumeration type that contains class annotations and
     *    other information typically needed for building deserializers
     * @param elementTypeDeserializer If element type needs polymorphic type handling, this is
     *    the type information deserializer to use; should usually be used as is when constructing
     *    array deserializer.
     * @param elementDeserializer Deserializer to use for elements, if explicitly defined (by using
     *    annotations, for example). May be null, in which case it will need to be resolved
     *    by deserializer at a later point.
     * 
     * @return Deserializer to use for the type; or null if this provider does not know how to construct it
     */
    public JsonDeserializer<?> findArrayDeserializer(ArrayType type,
            DeserializationConfig config, BeanDescription beanDesc,
            TypeDeserializer elementTypeDeserializer, JsonDeserializer<?> elementDeserializer)
        throws JsonMappingException;

    /**
     * Method called to locate serializer for specified {@link java.util.Collection} (List, Set etc) type.
     *<p>
     * Deserializer for element type may be passed, if configured explicitly at higher level (by
     * annotations, typically), but usually are not.
     * Type deserializer for element is passed if one is needed based on contextual information
     * (annotations on declared element class; or on field or method type is associated with).
     * 
     * @param type Type of collection instances to deserialize
     * @param config Configuration in effect
     * @param beanDesc Definition of the enumeration type that contains class annotations and
     *    other information typically needed for building deserializers
     * @param elementTypeDeserializer If element type needs polymorphic type handling, this is
     *    the type information deserializer to use; should usually be used as is when constructing
     *    array deserializer.
     * @param elementDeserializer Deserializer to use for elements, if explicitly defined (by using
     *    annotations, for example). May be null, in which case it will need to be resolved
     *    by deserializer at a later point.
     * 
     * @return Deserializer to use for the type; or null if this provider does not know how to construct it
     */
    public JsonDeserializer<?> findCollectionDeserializer(CollectionType type,
            DeserializationConfig config, BeanDescription beanDesc,
            TypeDeserializer elementTypeDeserializer, JsonDeserializer<?> elementDeserializer)
        throws JsonMappingException;

    /**
     * Method called to locate serializer for specified
     * "Collection-like" type (one that acts
     * like {@link java.util.Collection} but does not implement it).
     *<p>
     * Deserializer for element type may be passed, if configured explicitly at higher level (by
     * annotations, typically), but usually are not.
     * Type deserializer for element is passed if one is needed based on contextual information
     * (annotations on declared element class; or on field or method type is associated with).
     * 
     * @param type Type of instances to deserialize
     * @param config Configuration in effect
     * @param beanDesc Definition of the enumeration type that contains class annotations and
     *    other information typically needed for building deserializers
     * @param elementTypeDeserializer If element type needs polymorphic type handling, this is
     *    the type information deserializer to use; should usually be used as is when constructing
     *    array deserializer.
     * @param elementDeserializer Deserializer to use for elements, if explicitly defined (by using
     *    annotations, for example). May be null, in which case it will need to be resolved
     *    by deserializer at a later point.
     * 
     * @return Deserializer to use for the type; or null if this provider does not know how to construct it
     */
    public JsonDeserializer<?> findCollectionLikeDeserializer(CollectionLikeType type,
            DeserializationConfig config, BeanDescription beanDesc,
            TypeDeserializer elementTypeDeserializer, JsonDeserializer<?> elementDeserializer)
        throws JsonMappingException;

    /**
     * Method called to locate deserializer for specified {@link java.util.Map} type.
     *<p>
     * Deserializer for element type may be passed, if configured explicitly at higher level (by
     * annotations, typically), but usually are not.
     * Type deserializer for element is passed if one is needed based on contextual information
     * (annotations on declared element class; or on field or method type is associated with).
     *<p>
     * Similarly, a {@link KeyDeserializer} may be passed, but this is only done if there is
     * a specific configuration override (annotations) to indicate instance to use.
     * Otherwise null is passed, and key deserializer needs to be obtained later during
     * resolution of map serializer constructed here.
     * 
     * @param type Type of {@link java.util.Map} instances to deserialize
     * @param config Configuration in effect
     * @param beanDesc Definition of the enumeration type that contains class annotations and
     *    other information typically needed for building deserializers
     * @param keyDeserializer Key deserializer use, if it is defined via annotations or other configuration;
     *    null if default key deserializer for key type can be used.
     * @param elementTypeDeserializer If element type needs polymorphic type handling, this is
     *    the type information deserializer to use; should usually be used as is when constructing
     *    array deserializer.
     * @param elementDeserializer Deserializer to use for elements, if explicitly defined (by using
     *    annotations, for example). May be null, in which case it will need to be resolved
     *    by deserializer at a later point.
     * 
     * @return Deserializer to use for the type; or null if this provider does not know how to construct it
     */
    public JsonDeserializer<?> findMapDeserializer(MapType type,
            DeserializationConfig config, BeanDescription beanDesc,
            KeyDeserializer keyDeserializer,
            TypeDeserializer elementTypeDeserializer, JsonDeserializer<?> elementDeserializer)
        throws JsonMappingException;

    /**
     * Method called to locate serializer for specified
     * "Map-like" type (one that acts
     * like {@link java.util.Map} but does not implement it).
     *<p>
     * Deserializer for element type may be passed, if configured explicitly at higher level (by
     * annotations, typically), but usually are not.
     * Type deserializer for element is passed if one is needed based on contextual information
     * (annotations on declared element class; or on field or method type is associated with).
     *<p>
     * Similarly, a {@link KeyDeserializer} may be passed, but this is only done if there is
     * a specific configuration override (annotations) to indicate instance to use.
     * Otherwise null is passed, and key deserializer needs to be obtained later during
     * resolution, by deserializer constructed here.
     * 
     * @param type Type of {@link java.util.Map} instances to deserialize
     * @param config Configuration in effect
     * @param beanDesc Definition of the enumeration type that contains class annotations and
     *    other information typically needed for building deserializers
     * @param keyDeserializer Key deserializer use, if it is defined via annotations or other configuration;
     *    null if default key deserializer for key type can be used.
     * @param elementTypeDeserializer If element type needs polymorphic type handling, this is
     *    the type information deserializer to use; should usually be used as is when constructing
     *    array deserializer.
     * @param elementDeserializer Deserializer to use for elements, if explicitly defined (by using
     *    annotations, for example). May be null, in which case it will need to be resolved
     *    by deserializer at a later point.
     * 
     * @return Deserializer to use for the type; or null if this provider does not know how to construct it
     */
    public JsonDeserializer<?> findMapLikeDeserializer(MapLikeType type,
            DeserializationConfig config, BeanDescription beanDesc,
            KeyDeserializer keyDeserializer,
            TypeDeserializer elementTypeDeserializer, JsonDeserializer<?> elementDeserializer)
        throws JsonMappingException;

    /*
    /**********************************************************
    /* Helper classes
    /**********************************************************
     */

    /**
     * Basic {@link Deserializers} implementation that implements all methods but provides
     * no deserializers. Its main purpose is to serve as a base class so that
     * sub-classes only need to override methods they need, as most of the time some
     * of methods are not needed (especially enumeration and array deserializers are
     * very rarely overridden).
     */
    public static class Base implements Deserializers
    {
        @Override
        public JsonDeserializer<?> findEnumDeserializer(Class<?> type,
                DeserializationConfig config, BeanDescription beanDesc)
            throws JsonMappingException
        {
            return null;
        }
        
        @Override
        public JsonDeserializer<?> findTreeNodeDeserializer(Class<? extends JsonNode> nodeType,
                DeserializationConfig config, BeanDescription beanDesc)
            throws JsonMappingException
        {
            return null;
        }

        @Override // since 2.7
        public JsonDeserializer<?> findReferenceDeserializer(ReferenceType refType,
                DeserializationConfig config, BeanDescription beanDesc,
                TypeDeserializer contentTypeDeserializer, JsonDeserializer<?> contentDeserializer)
            throws JsonMappingException {
            // 21-Oct-2015, tatu: For backwards compatibility, let's delegate to "bean" variant,
            //    for 2.7 -- remove work-around from 2.8 or later
            return findBeanDeserializer(refType, config, beanDesc);
        }
        
        @Override
        public JsonDeserializer<?> findBeanDeserializer(JavaType type,
                DeserializationConfig config, BeanDescription beanDesc)
            throws JsonMappingException
        {
            return null;
        }
        
        @Override
        public JsonDeserializer<?> findArrayDeserializer(ArrayType type,
                DeserializationConfig config, BeanDescription beanDesc,
                TypeDeserializer elementTypeDeserializer, JsonDeserializer<?> elementDeserializer)
            throws JsonMappingException
        {
            return null;
        }

        @Override
        public JsonDeserializer<?> findCollectionDeserializer(CollectionType type,
                DeserializationConfig config, BeanDescription beanDesc,
                TypeDeserializer elementTypeDeserializer, JsonDeserializer<?> elementDeserializer)
            throws JsonMappingException
        {
            return null;
        }

        @Override
        public JsonDeserializer<?> findCollectionLikeDeserializer(CollectionLikeType type,
                DeserializationConfig config, BeanDescription beanDesc,
                TypeDeserializer elementTypeDeserializer, JsonDeserializer<?> elementDeserializer)
            throws JsonMappingException
        {
            return null;
        }

        @Override
        public JsonDeserializer<?> findMapDeserializer(MapType type,
                DeserializationConfig config, BeanDescription beanDesc,
                KeyDeserializer keyDeserializer,
                TypeDeserializer elementTypeDeserializer, JsonDeserializer<?> elementDeserializer)
            throws JsonMappingException
        {
            return null;
        }

        @Override
        public JsonDeserializer<?> findMapLikeDeserializer(MapLikeType type,
                DeserializationConfig config, BeanDescription beanDesc,
                KeyDeserializer keyDeserializer,
                TypeDeserializer elementTypeDeserializer, JsonDeserializer<?> elementDeserializer)
            throws JsonMappingException
        {
            return null;
        }
    }
}
