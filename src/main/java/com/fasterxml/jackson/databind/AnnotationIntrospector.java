package com.fasterxml.jackson.databind;

import java.lang.annotation.Annotation;
import java.util.*;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.Versioned;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.annotation.NoClass;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.deser.ValueInstantiator;
import com.fasterxml.jackson.databind.introspect.*;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.databind.util.NameTransformer;

/**
 * Abstract class that defines API used for introspecting annotation-based
 * configuration for serialization and deserialization. Separated
 * so that different sets of annotations can be supported, and support
 * plugged-in dynamically.
 *<p>
 * NOTE: due to rapid addition of new methods (and changes to existing methods),
 * it is <b>strongly</b> recommended that custom implementations should not directly
 * extend this class, but rather extend {@link NopAnnotationIntrospector}.
 * This way added methods will not break backwards compatibility of custom annotation
 * introspectors.
 */
public abstract class AnnotationIntrospector implements Versioned
{    
    /*
    /**********************************************************
    /* Helper types
    /**********************************************************
     */

    /**
     * Value type used with managed and back references; contains type and
     * logic name, used to link related references
     */
    public static class ReferenceProperty
    {
        public enum Type {
            /**
             * Reference property that Jackson manages and that is serialized normally (by serializing
             * reference object), but is used for resolving back references during
             * deserialization.
             * Usually this can be defined by using
             * {@link com.fasterxml.jackson.annotation.JsonManagedReference}
             */
            MANAGED_REFERENCE
    
            /**
             * Reference property that Jackson manages by suppressing it during serialization,
             * and reconstructing during deserialization.
             * Usually this can be defined by using
             * {@link com.fasterxml.jackson.annotation.JsonBackReference}
             */
            ,BACK_REFERENCE
            ;
        }

        private final Type _type;
        private final String _name;

        public ReferenceProperty(Type t, String n) {
            _type = t;
            _name = n;
        }

        public static ReferenceProperty managed(String name) { return new ReferenceProperty(Type.MANAGED_REFERENCE, name); }
        public static ReferenceProperty back(String name) { return new ReferenceProperty(Type.BACK_REFERENCE, name); }
        
        public Type getType() { return _type; }
        public String getName() { return _name; }

        public boolean isManagedReference() { return _type == Type.MANAGED_REFERENCE; }
        public boolean isBackReference() { return _type == Type.BACK_REFERENCE; }
    }
    
    /*
    /**********************************************************
    /* Factory methods
    /**********************************************************
     */
    
    /**
     * Factory method for accessing "no operation" implementation
     * of introspector: instance that will never find any annotation-based
     * configuration.
     */
    public static AnnotationIntrospector nopInstance() {
        return NopAnnotationIntrospector.instance;
    }

    public static AnnotationIntrospector pair(AnnotationIntrospector a1, AnnotationIntrospector a2) {
        return new Pair(a1, a2);
    }

    /*
    /**********************************************************
    /* Access to possibly chained introspectors (1.7)
    /**********************************************************
     */

    /**
     * Method that can be used to collect all "real" introspectors that
     * this introspector contains, if any; or this introspector
     * if it is not a container. Used to get access to all container
     * introspectors in their priority order.
     *<p>
     * Default implementation returns a Singleton list with this introspector
     * as contents.
     * This usually works for sub-classes, except for proxy or delegating "container
     * introspectors" which need to override implementation.
     */
    public Collection<AnnotationIntrospector> allIntrospectors() {
        return Collections.singletonList(this);
    }
    
    /**
     * Method that can be used to collect all "real" introspectors that
     * this introspector contains, if any; or this introspector
     * if it is not a container. Used to get access to all container
     * introspectors in their priority order.
     *<p>
     * Default implementation adds this introspector in result; this usually
     * works for sub-classes, except for proxy or delegating "container
     * introspectors" which need to override implementation.
     */
    public Collection<AnnotationIntrospector> allIntrospectors(Collection<AnnotationIntrospector> result) {
        result.add(this);
        return result;
    }
    
    /*
    /**********************************************************
    /* Default Versioned impl
    /**********************************************************
     */

    @Override
    public abstract Version version();
    
    /*
    /**********************************************************
    /* Generic annotation properties, lookup
    /**********************************************************
     */
    
    /**
     * Method called by framework to determine whether given annotation
     * is handled by this introspector.
     */
    public boolean isHandled(Annotation ann) {
        return false;
    }

    /**
     * Method for checking whether given annotation is considered an
     * annotation bundle: if so, all meta-annotations it has will
     * be used instead of annotation ("bundle") itself.
     * 
     * @since 2.0
     */
    public boolean isAnnotationBundle(Annotation ann) {
        return false;
    }
    
    /*
    /**********************************************************
    /* General class annotations
    /**********************************************************
     */

    /**
     * Method for locating name used as "root name" (for use by
     * some serializers when outputting root-level object -- mostly
     * for XML compatibility purposes) for given class, if one
     * is defined. Returns null if no declaration found; can return
     * explicit empty String, which is usually ignored as well as null.
     */
    public String findRootName(AnnotatedClass ac) {
        return null;
    }

    /**
     * Method for finding list of properties to ignore for given class
     * (null is returned if not specified).
     * List of property names is applied
     * after other detection mechanisms, to filter out these specific
     * properties from being serialized and deserialized.
     */
    public String[] findPropertiesToIgnore(Annotated ac) {
        return null;
    }

    /**
     * Method for checking whether an annotation indicates that all unknown properties
     */
    public Boolean findIgnoreUnknownProperties(AnnotatedClass ac) {
        return null;
    }

    /**
     * Method for checking whether properties that have specified type
     * (class, not generics aware) should be completely ignored for
     * serialization and deserialization purposes.
     * 
     * @param ac Type to check
     * 
     * @return Boolean.TRUE if properties of type should be ignored;
     *   Boolean.FALSE if they are not to be ignored, null for default
     *   handling (which is 'do not ignore')
     */
    public Boolean isIgnorableType(AnnotatedClass ac) {
        return null;
    }

    /**
     * Method for finding if annotated class has associated filter; and if so,
     * to return id that is used to locate filter.
     * 
     * @return Id of the filter to use for filtering properties of annotated
     *    class, if any; or null if none found.
     */
    public Object findFilterId(AnnotatedClass ac) {
        return null;
    }
    
    /*
    /**********************************************************
    /* Property auto-detection
    /**********************************************************
     */

    /**
     * Method for checking if annotations indicate changes to minimum visibility levels
     * needed for auto-detecting property elements (fields, methods, constructors).
     * A baseline checker is given, and introspector is to either return it as is
     * (if no annotations are found), or build and return a derived instance (using
     * checker's build methods).
     */
    public VisibilityChecker<?> findAutoDetectVisibility(AnnotatedClass ac,
            VisibilityChecker<?> checker) {
        return checker;
    }
    
    /*
    /**********************************************************
    /* Class annotations for Polymorphic type handling (1.5+)
    /**********************************************************
    */
    
    /**
     * Method for checking if given class has annotations that indicate
     * that specific type resolver is to be used for handling instances.
     * This includes not only
     * instantiating resolver builder, but also configuring it based on
     * relevant annotations (not including ones checked with a call to
     * {@link #findSubtypes}
     * 
     * @param config Configuration settings in effect (for serialization or deserialization)
     * @param ac Annotated class to check for annotations
     * @param baseType Base java type of value for which resolver is to be found
     * 
     * @return Type resolver builder for given type, if one found; null if none
     */
    public TypeResolverBuilder<?> findTypeResolver(MapperConfig<?> config,
            AnnotatedClass ac, JavaType baseType) {
        return null;
    }

    /**
     * Method for checking if given property entity (field or method) has annotations
     * that indicate that specific type resolver is to be used for handling instances.
     * This includes not only
     * instantiating resolver builder, but also configuring it based on
     * relevant annotations (not including ones checked with a call to
     * {@link #findSubtypes}
     * 
     * @param config Configuration settings in effect (for serialization or deserialization)
     * @param am Annotated member (field or method) to check for annotations
     * @param baseType Base java type of property for which resolver is to be found
     * 
     * @return Type resolver builder for properties of given entity, if one found;
     *    null if none
     */
    public TypeResolverBuilder<?> findPropertyTypeResolver(MapperConfig<?> config,
            AnnotatedMember am, JavaType baseType) {
        return null;
    }

    /**
     * Method for checking if given structured property entity (field or method that
     * has nominal value of Map, Collection or array type) has annotations
     * that indicate that specific type resolver is to be used for handling type
     * information of contained values.
     * This includes not only
     * instantiating resolver builder, but also configuring it based on
     * relevant annotations (not including ones checked with a call to
     * {@link #findSubtypes}
     * 
     * @param config Configuration settings in effect (for serialization or deserialization)
     * @param am Annotated member (field or method) to check for annotations
     * @param containerType Type of property for which resolver is to be found (must be a container type)
     * 
     * @return Type resolver builder for values contained in properties of given entity,
     *    if one found; null if none
     */    
    public TypeResolverBuilder<?> findPropertyContentTypeResolver(MapperConfig<?> config,
            AnnotatedMember am, JavaType containerType) {
        return null;
    }

    /**
     * Method for locating annotation-specified subtypes related to annotated
     * entity (class, method, field). Note that this is only guaranteed to be
     * a list of directly
     * declared subtypes, no recursive processing is guarantees (i.e. caller
     * has to do it if/as necessary)
     * 
     * @param a Annotated entity (class, field/method) to check for annotations
     */
    public List<NamedType> findSubtypes(Annotated a) {
        return null;
    }

    /**
     * Method for checking if specified type has explicit name.
     * 
     * @param ac Class to check for type name annotations
     */
    public String findTypeName(AnnotatedClass ac) {
        return null;
    }

    /*
    /**********************************************************
    /* General member (field, method/constructor) annotations
    /**********************************************************
     */

    /**
     * Method for checking if given member indicates that it is part
     * of a reference (parent/child).
     */
    public ReferenceProperty findReferenceType(AnnotatedMember member) {
        return null;
    }

    /**
     * Method called to check whether given property is marked to be "unwrapped"
     * when being serialized (and appropriately handled in reverse direction,
     * i.e. expect unwrapped representation during deserialization).
     * Return value is the name transformation to use, if wrapping/unwrapping
     * should  be done, or null if not -- note that transformation may simply
     * be identity transformation (no changes).
     */
    public NameTransformer findUnwrappingNameTransformer(AnnotatedMember member) {
        return null;
    }

    /**
     * Method called to check whether given property is marked to
     * be ignored. This is used to determine whether to ignore
     * properties, on per-property basis, usually combining
     * annotations from multiple accessors (getters, setters, fields,
     * constructor parameters).
     */
    public boolean hasIgnoreMarker(AnnotatedMember m) {
        return false;
    }

    /**
     * Method called to find out whether given member expectes a value
     * to be injected, and if so, what is the identifier of the value
     * to use during injection.
     * Type if identifier needs to be compatible with provider of
     * values (of type {@link InjectableValues}); often a simple String
     * id is used.
     * 
     * @param m Member to check
     * 
     * @return Identifier of value to inject, if any; null if no injection
     *   indicator is found
     */
    public Object findInjectableValueId(AnnotatedMember m) {
        return null;
    }

    /**
     * Method that can be called to check whether this member has
     * an annotation that suggests whether value for matching property
     * is required or not.
     * 
     * @since 2.0
     */
    public Boolean hasRequiredMarker(AnnotatedMember m) {
        return null;
    }
    
    /**
     * Method for checking if annotated property (represented by a field or
     * getter/setter method) has definitions for views it is to be included in.
     * If null is returned, no view definitions exist and property is always
     * included (or always excluded as per default view inclusion configuration);
     * otherwise it will only be included for views included in returned
     * array. View matches are checked using class inheritance rules (sub-classes
     * inherit inclusions of super-classes)
     * 
     * @param a Annotated property (represented by a method, field or ctor parameter)
     * @return Array of views (represented by classes) that the property is included in;
     *    if null, always included (same as returning array containing <code>Object.class</code>)
     */
    public Class<?>[] findViews(Annotated a) {
        return null;
    }

    /**
     * Method for checking whether given annotated thing
     * (type, or accessor) indicates that values
     * referenced (values of type of annotated class, or
     * values referenced by annotated property; latter
     * having precedence) should include Object Identifier,
     * and if so, specify details of Object Identity used.
     * 
     * @since 2.0
     */
    public ObjectIdInfo findObjectIdInfo(Annotated ann) {
        return null;
    }
    
    /**
     * Method for checking whether given accessor claims to represent
     * type id: if so, its value may be used as an override,
     * instead of generated type id.
     * 
     * @since 2.0
     */
    public Boolean isTypeId(AnnotatedMember member) {
        return null;
    }
    
    /*
    /**********************************************************
    /* Serialization: general annotations
    /**********************************************************
     */

    /**
     * Method for getting a serializer definition on specified method
     * or field. Type of definition is either instance (of type
     * {@link JsonSerializer}) or Class (of type
     * <code>Class<JsonSerializer></code>); if value of different
     * type is returned, a runtime exception may be thrown by caller.
     */
    public Object findSerializer(Annotated am) {
        return null;
    }

    /**
     * Method for getting a serializer definition for keys of associated <code>Map</code> property.
     * Type of definition is either instance (of type
     * {@link JsonSerializer}) or Class (of type
     * <code>Class<JsonSerializer></code>); if value of different
     * type is returned, a runtime exception may be thrown by caller.
     */
    public Object findKeySerializer(Annotated am) {
        return null;
    }

    /**
     * Method for getting a serializer definition for content (values) of
     * associated <code>Collection</code>, <code>array</code> or <code>Map</code> property.
     * Type of definition is either instance (of type
     * {@link JsonSerializer}) or Class (of type
     * <code>Class<JsonSerializer></code>); if value of different
     * type is returned, a runtime exception may be thrown by caller.
     */
    public Object findContentSerializer(Annotated am) {
        return null;
    }
    
    /**
     * Method for checking whether given annotated entity (class, method,
     * field) defines which Bean/Map properties are to be included in
     * serialization.
     * If no annotation is found, method should return given second
     * argument; otherwise value indicated by the annotation
     *
     * @return Enumerated value indicating which properties to include
     *   in serialization
     */
    public JsonInclude.Include findSerializationInclusion(Annotated a, JsonInclude.Include defValue) {
        return defValue;
    }

    /**
     * Method for accessing annotated type definition that a
     * method/field can have, to be used as the type for serialization
     * instead of the runtime type.
     * Type returned (if any) needs to be widening conversion (super-type).
     * Declared return type of the method is also considered acceptable.
     *
     * @return Class to use instead of runtime type
     */
    public Class<?> findSerializationType(Annotated a) {
        return null;
    }

    /**
     * Method for finding possible widening type definition that a property
     * value can have, to define less specific key type to use for serialization.
     * It should be only be used with {@link java.util.Map} types.
     * 
     * @return Class specifying more general type to use instead of
     *   declared type, if annotation found; null if not
     */
    public Class<?> findSerializationKeyType(Annotated am, JavaType baseType) {
        return null;
    }

    /**
     * Method for finding possible widening type definition that a property
     * value can have, to define less specific key type to use for serialization.
     * It should be only used with structured types (arrays, collections, maps).
     * 
     * @return Class specifying more general type to use instead of
     *   declared type, if annotation found; null if not
     */
    public Class<?> findSerializationContentType(Annotated am, JavaType baseType) {
        return null;
    }
    
    /**
     * Method for accessing declared typing mode annotated (if any).
     * This is used for type detection, unless more granular settings
     * (such as actual exact type; or serializer to use which means
     * no type information is needed) take precedence.
     *
     * @return Typing mode to use, if annotation is found; null otherwise
     */
    public JsonSerialize.Typing findSerializationTyping(Annotated a) {
        return null;
    }
    
    /*
    /**********************************************************
    /* Serialization: class annotations
    /**********************************************************
     */

    /**
     * Method for accessing defined property serialization order (which may be
     * partial). May return null if no ordering is defined.
     */
    public String[] findSerializationPropertyOrder(AnnotatedClass ac) {
        return null;
    }

    /**
     * Method for checking whether an annotation indicates that serialized properties
     * for which no explicit is defined should be alphabetically (lexicograpically)
     * ordered
     */
    public Boolean findSerializationSortAlphabetically(AnnotatedClass ac) {
        return null;
    }
    
    /*
    /**********************************************************
    /* Serialization: method annotations
    /**********************************************************
     */

    /**
     * Method for checking whether given method has an annotation
     * that suggests property name associated with method that
     * may be a "getter". Should return null if no annotation
     * is found; otherwise a non-null String.
     * If non-null value is returned, it is used as the property
     * name, except for empty String ("") which is taken to mean
     * "use standard bean name detection if applicable;
     * method name if not".
     */
    public String findSerializationName(AnnotatedMethod am) {
        return null;
    }

    /**
     * Method for checking whether given method has an annotation
     * that suggests that the return value of annotated method
     * should be used as "the value" of the object instance; usually
     * serialized as a primitive value such as String or number.
     *
     * @return True if such annotation is found (and is not disabled);
     *   false if no enabled annotation is found
     */
    public boolean hasAsValueAnnotation(AnnotatedMethod am) {
        return false;
    }

    /**
     * Method for determining the String value to use for serializing
     * given enumeration entry; used when serializing enumerations
     * as Strings (the standard method).
     *
     * @return Serialized enum value.
     */
    public String findEnumValue(Enum<?> value) {
        return null;
    }

    /*
    /**********************************************************
    /* Serialization: field annotations
    /**********************************************************
     */

    /**
     * Method for checking whether given member field represent
     * a serializable logical property; and if so, returns the
     * name of that property.
     * Should return null if no annotation is found (indicating it
     * is not a serializable field); otherwise a non-null String.
     * If non-null value is returned, it is used as the property
     * name, except for empty String ("") which is taken to mean
     * "use the field name as is".
     */
    public String findSerializationName(AnnotatedField af) {
        return null;
    }

    /*
    /**********************************************************
    /* Deserialization: general annotations
    /**********************************************************
     */

    /**
     * Method for getting a deserializer definition on specified method
     * or field.
     * Type of definition is either instance (of type
     * {@link JsonDeserializer}) or Class (of type
     * <code>Class<JsonDeserializer></code>); if value of different
     * type is returned, a runtime exception may be thrown by caller.
     */
    public Object findDeserializer(Annotated am) {
        return null;
    }

    /**
     * Method for getting a deserializer definition for keys of
     * associated <code>Map</code> property.
     * Type of definition is either instance (of type
     * {@link JsonDeserializer}) or Class (of type
     * <code>Class<JsonDeserializer></code>); if value of different
     * type is returned, a runtime exception may be thrown by caller.
     */
    public Object findKeyDeserializer(Annotated am) {
        return null;
    }

    /**
     * Method for getting a deserializer definition for content (values) of
     * associated <code>Collection</code>, <code>array</code> or
     * <code>Map</code> property.
     * Type of definition is either instance (of type
     * {@link JsonDeserializer}) or Class (of type
     * <code>Class<JsonDeserializer></code>); if value of different
     * type is returned, a runtime exception may be thrown by caller.
     */
    public Object findContentDeserializer(Annotated am) {
        return null;
    }

    /**
     * Method for accessing annotated type definition that a
     * method can have, to be used as the type for serialization
     * instead of the runtime type.
     * Type must be a narrowing conversion
     * (i.e.subtype of declared type).
     * Declared return type of the method is also considered acceptable.
     *
     * @param baseType Assumed type before considering annotations
     *
     * @return Class to use for deserialization instead of declared type
     */
    public Class<?> findDeserializationType(Annotated am, JavaType baseType) {
        return null;
    }

    /**
     * Method for accessing additional narrowing type definition that a
     * method can have, to define more specific key type to use.
     * It should be only be used with {@link java.util.Map} types.
     * 
     * @param baseKeyType Assumed key type before considering annotations
     *
     * @return Class specifying more specific type to use instead of
     *   declared type, if annotation found; null if not
     */
    public Class<?> findDeserializationKeyType(Annotated am, JavaType baseKeyType) {
        return null;
    }

    /**
     * Method for accessing additional narrowing type definition that a
     * method can have, to define more specific content type to use;
     * content refers to Map values and Collection/array elements.
     * It should be only be used with Map, Collection and array types.
     * 
     * @param baseContentType Assumed content (value) type before considering annotations
     *
     * @return Class specifying more specific type to use instead of
     *   declared type, if annotation found; null if not
     */
    public Class<?> findDeserializationContentType(Annotated am, JavaType baseContentType) {
        return null;
    }

    /*
    /**********************************************************
    /* Deserialization: class annotations
    /**********************************************************
     */

    /**
     * Method getting {@link ValueInstantiator} to use for given
     * type (class): return value can either be an instance of
     * instantiator, or class of instantiator to create.
     */
    public Object findValueInstantiator(AnnotatedClass ac) {
        return null;
    }

    /**
     * Method for finding Builder object to use for constructing
     * value instance and binding data (sort of combining value
     * instantiators that can construct, and deserializers
     * that can bind data).
     *<p>
     * Note that unlike accessors for some helper Objects, this
     * method does not allow returning instances: the reason is
     * that builders have state, and a separate instance needs
     * to be created for each deserialization call.
     * 
     * @since 2.0
     */
    public Class<?> findPOJOBuilder(AnnotatedClass ac) {
    	return null;
    }

    /**
     * @since 2.0
     */
    public JsonPOJOBuilder.Value findPOJOBuilderConfig(AnnotatedClass ac) {
        return null;
    }
    
    /*
    /**********************************************************
    /* Deserialization: method annotations
    /**********************************************************
     */

    /**
     * Method for checking whether given method has an annotation
     * that suggests property name associated with method that
     * may be a "setter". Should return null if no annotation
     * is found; otherwise a non-null String.
     * If non-null value is returned, it is used as the property
     * name, except for empty String ("") which is taken to mean
     * "use standard bean name detection if applicable;
     * method name if not".
     */
    public String findDeserializationName(AnnotatedMethod am) {
        return null;
    }

    /**
     * Method for checking whether given method has an annotation
     * that suggests that the method is to serve as "any setter";
     * method to be used for setting values of any properties for
     * which no dedicated setter method is found.
     *
     * @return True if such annotation is found (and is not disabled),
     *   false otherwise
     */
    public boolean hasAnySetterAnnotation(AnnotatedMethod am) {
        return false;
    }

    /**
     * Method for checking whether given method has an annotation
     * that suggests that the method is to serve as "any setter";
     * method to be used for accessing set of miscellaneous "extra"
     * properties, often bound with matching "any setter" method.
     *
     * @return True if such annotation is found (and is not disabled),
     *   false otherwise
     */
    public boolean hasAnyGetterAnnotation(AnnotatedMethod am) {
        return false;
    }
    
    /**
     * Method for checking whether given annotated item (method, constructor)
     * has an annotation
     * that suggests that the method is a "creator" (aka factory)
     * method to be used for construct new instances of deserialized
     * values.
     *
     * @return True if such annotation is found (and is not disabled),
     *   false otherwise
     */
    public boolean hasCreatorAnnotation(Annotated a) {
        return false;
    }

    /*
    /**********************************************************
    /* Deserialization: field annotations
    /**********************************************************
     */

    /**
     * Method for checking whether given member field represent
     * a deserializable logical property; and if so, returns the
     * name of that property.
     * Should return null if no annotation is found (indicating it
     * is not a deserializable field); otherwise a non-null String.
     * If non-null value is returned, it is used as the property
     * name, except for empty String ("") which is taken to mean
     * "use the field name as is".
     */
    public String findDeserializationName(AnnotatedField af) {
        return null;
    }

    /*
    /**********************************************************
    /* Deserialization: parameter annotations (for
    /* creator method parameters)
    /**********************************************************
     */

    /**
     * Method for checking whether given set of annotations indicates
     * property name for associated parameter.
     * No actual parameter object can be passed since JDK offers no
     * representation; just annotations.
     */
    public String findDeserializationName(AnnotatedParameter param) {
        return null;
    }

    /*
    /**********************************************************
    /* Helper classes
    /**********************************************************
     */

    /**
     * Helper class that allows using 2 introspectors such that one
     * introspector acts as the primary one to use; and second one
     * as a fallback used if the primary does not provide conclusive
     * or useful result for a method.
     *<p>
     * An obvious consequence of priority is that it is easy to construct
     * longer chains of introspectors by linking multiple pairs.
     * Currently most likely combination is that of using the default
     * Jackson provider, along with JAXB annotation introspector.
     */
    public static class Pair
        extends AnnotationIntrospector
    {
        protected final AnnotationIntrospector _primary, _secondary;

        public Pair(AnnotationIntrospector p, AnnotationIntrospector s)
        {
            _primary = p;
            _secondary = s;
        }

        @Override
        public Version version() {
            return _primary.version();
        }
        
        /**
         * Helper method for constructing a Pair from two given introspectors (if
         * neither is null); or returning non-null introspector if one is null
         * (and return just null if both are null)
         */
        public static AnnotationIntrospector create(AnnotationIntrospector primary,
                AnnotationIntrospector secondary)
        {
            if (primary == null) {
                return secondary;
            }
            if (secondary == null) {
                return primary;
            }
            return new Pair(primary, secondary);
        }

        @Override
        public Collection<AnnotationIntrospector> allIntrospectors() {
            return allIntrospectors(new ArrayList<AnnotationIntrospector>());
        }

        @Override
        public Collection<AnnotationIntrospector> allIntrospectors(Collection<AnnotationIntrospector> result)
        {
            _primary.allIntrospectors(result);
            _secondary.allIntrospectors(result);
            return result;
        }
        
        // // // Generic annotation properties, lookup
        
        @Override
        public boolean isHandled(Annotation ann) {
            return _primary.isHandled(ann) || _secondary.isHandled(ann);
        }

        @Override
        public boolean isAnnotationBundle(Annotation ann) {
            return _primary.isAnnotationBundle(ann) || _secondary.isAnnotationBundle(ann);
        }
        
        /*
        /******************************************************
        /* General class annotations
        /******************************************************
         */

        @Override
        public String findRootName(AnnotatedClass ac)
        {
            String name1 = _primary.findRootName(ac);
            if (name1 == null) {
                return _secondary.findRootName(ac);
            } else if (name1.length() > 0) {
                return name1;
            }
            // name1 is empty; how about secondary?
            String name2 = _secondary.findRootName(ac);
            return (name2 == null) ? name1 : name2;
        }

        @Override
        public String[] findPropertiesToIgnore(Annotated ac)
        {
            String[] result = _primary.findPropertiesToIgnore(ac);
            if (result == null) {
                result = _secondary.findPropertiesToIgnore(ac);
            }
            return result;            
        }

        @Override
        public Boolean findIgnoreUnknownProperties(AnnotatedClass ac)
        {
            Boolean result = _primary.findIgnoreUnknownProperties(ac);
            if (result == null) {
                result = _secondary.findIgnoreUnknownProperties(ac);
            }
            return result;
        }        

        @Override
        public Boolean isIgnorableType(AnnotatedClass ac)
        {
            Boolean result = _primary.isIgnorableType(ac);
            if (result == null) {
                result = _secondary.isIgnorableType(ac);
            }
            return result;
        }

        @Override
        public Object findFilterId(AnnotatedClass ac)
        {
            Object id = _primary.findFilterId(ac);
            if (id == null) {
                id = _secondary.findFilterId(ac);
            }
            return id;
        }
        
        /*
        /******************************************************
        /* Property auto-detection
        /******************************************************
        */
        
        @Override
        public VisibilityChecker<?> findAutoDetectVisibility(AnnotatedClass ac,
            VisibilityChecker<?> checker)
        {
            /* Note: to have proper priorities, we must actually call delegatees
             * in reverse order:
             */
            checker = _secondary.findAutoDetectVisibility(ac, checker);
            return _primary.findAutoDetectVisibility(ac, checker);
        }

        /*
        /******************************************************
        /* Type handling
        /******************************************************
        */
        
        @Override
        public TypeResolverBuilder<?> findTypeResolver(MapperConfig<?> config,
                AnnotatedClass ac, JavaType baseType)
        {
            TypeResolverBuilder<?> b = _primary.findTypeResolver(config, ac, baseType);
            if (b == null) {
                b = _secondary.findTypeResolver(config, ac, baseType);
            }
            return b;
        }

        @Override
        public TypeResolverBuilder<?> findPropertyTypeResolver(MapperConfig<?> config,
                AnnotatedMember am, JavaType baseType)
        {
            TypeResolverBuilder<?> b = _primary.findPropertyTypeResolver(config, am, baseType);
            if (b == null) {
                b = _secondary.findPropertyTypeResolver(config, am, baseType);
            }
            return b;
        }

        @Override
        public TypeResolverBuilder<?> findPropertyContentTypeResolver(MapperConfig<?> config,
                AnnotatedMember am, JavaType baseType)
        {
            TypeResolverBuilder<?> b = _primary.findPropertyContentTypeResolver(config, am, baseType);
            if (b == null) {
                b = _secondary.findPropertyContentTypeResolver(config, am, baseType);
            }
            return b;
        }
        
        @Override
        public List<NamedType> findSubtypes(Annotated a)
        {
            List<NamedType> types1 = _primary.findSubtypes(a);
            List<NamedType> types2 = _secondary.findSubtypes(a);
            if (types1 == null || types1.isEmpty()) return types2;
            if (types2 == null || types2.isEmpty()) return types1;
            ArrayList<NamedType> result = new ArrayList<NamedType>(types1.size() + types2.size());
            result.addAll(types1);
            result.addAll(types2);
            return result;
        }

        @Override
        public String findTypeName(AnnotatedClass ac)
        {
            String name = _primary.findTypeName(ac);
            if (name == null || name.length() == 0) {
                name = _secondary.findTypeName(ac);                
            }
            return name;
        }
        
        // // // General member (field, method/constructor) annotations
        
        @Override        
        public ReferenceProperty findReferenceType(AnnotatedMember member)
        {
            ReferenceProperty ref = _primary.findReferenceType(member);
            if (ref == null) {
                ref = _secondary.findReferenceType(member);
            }
            return ref; 
        }

        @Override        
        public NameTransformer findUnwrappingNameTransformer(AnnotatedMember member)
        {
            NameTransformer value = _primary.findUnwrappingNameTransformer(member);
            if (value == null) {
                value = _secondary.findUnwrappingNameTransformer(member);
            }
            return value;
        }

        @Override
        public Object findInjectableValueId(AnnotatedMember m)
        {
            Object value = _primary.findInjectableValueId(m);
            if (value == null) {
                value = _secondary.findInjectableValueId(m);
            }
            return value;
        }

        @Override
        public boolean hasIgnoreMarker(AnnotatedMember m) {
            return _primary.hasIgnoreMarker(m) || _secondary.hasIgnoreMarker(m);
        }
        
        @Override
        public Boolean hasRequiredMarker(AnnotatedMember m)
        {
            Boolean value = _primary.hasRequiredMarker(m);
            if (value == null) {
                value = _secondary.hasRequiredMarker(m);
            }
            return value;
        }
        
        // // // Serialization: general annotations

        @Override
        public Object findSerializer(Annotated am)
        {
            Object result = _primary.findSerializer(am);
            if (result == null) {
                result = _secondary.findSerializer(am);
            }
            return result;
        }
        
        @Override
        public Object findKeySerializer(Annotated a)
        {
            Object result = _primary.findKeySerializer(a);
            if (result == null || result == JsonSerializer.None.class || result == NoClass.class) {
                result = _secondary.findKeySerializer(a);
            }
            return result;
        }

        @Override
        public Object findContentSerializer(Annotated a)
        {
            Object result = _primary.findContentSerializer(a);
            if (result == null || result == JsonSerializer.None.class || result == NoClass.class) {
                result = _secondary.findContentSerializer(a);
            }
            return result;
        }
        
        @Override
        public JsonInclude.Include findSerializationInclusion(Annotated a,
                JsonInclude.Include defValue)
        {
            /* This is bit trickier: need to combine results in a meaningful
             * way. Seems like it should be a disjoint; that is, most
             * restrictive value should be returned.
             * For enumerations, comparison is done by indexes, which
             * works: largest value is the last one, which is the most
             * restrictive value as well.
             */
            /* 09-Mar-2010, tatu: Actually, as per [JACKSON-256], it is probably better to just
             *    use strict overriding. Simpler, easier to understand.
             */
            // note: call secondary first, to give lower priority
            defValue = _secondary.findSerializationInclusion(a, defValue);
            defValue = _primary.findSerializationInclusion(a, defValue);
            return defValue;
        }
        
        @Override
        public Class<?> findSerializationType(Annotated a)
        {
            Class<?> result = _primary.findSerializationType(a);
            if (result == null) {
                result = _secondary.findSerializationType(a);
            }
            return result;
        }

        @Override
        public Class<?> findSerializationKeyType(Annotated am, JavaType baseType)
        {
            Class<?> result = _primary.findSerializationKeyType(am, baseType);
            if (result == null) {
                result = _secondary.findSerializationKeyType(am, baseType);
            }
            return result;
        }

        @Override
        public Class<?> findSerializationContentType(Annotated am, JavaType baseType)
        {
            Class<?> result = _primary.findSerializationContentType(am, baseType);
            if (result == null) {
                result = _secondary.findSerializationContentType(am, baseType);
            }
            return result;
        }
        
        @Override
        public JsonSerialize.Typing findSerializationTyping(Annotated a)
        {
            JsonSerialize.Typing result = _primary.findSerializationTyping(a);
            if (result == null) {
                result = _secondary.findSerializationTyping(a);
            }
            return result;
        }

        @Override
        public Class<?>[] findViews(Annotated a)
        {
            /* Theoretically this could be trickier, if multiple introspectors
             * return non-null entries. For now, though, we'll just consider
             * first one to return non-null to win.
             */
            Class<?>[] result = _primary.findViews(a);
            if (result == null) {
                result = _secondary.findViews(a);
            }
            return result;
        }

        @Override
        public Boolean isTypeId(AnnotatedMember member) {
            Boolean b = _primary.isTypeId(member);
            if (b == null) {
                b = _secondary.isTypeId(member);
            }
            return b;
        }

        @Override
        public ObjectIdInfo findObjectIdInfo(Annotated ann) {
            ObjectIdInfo result = _primary.findObjectIdInfo(ann);
            if (result == null) {
                result = _secondary.findObjectIdInfo(ann);
            }
            return result;
        }
        
        // // // Serialization: class annotations

        @Override
        public String[] findSerializationPropertyOrder(AnnotatedClass ac) {
            String[] result = _primary.findSerializationPropertyOrder(ac);
            if (result == null) {
                result = _secondary.findSerializationPropertyOrder(ac);
            }
            return result;            
        }

        /**
         * Method for checking whether an annotation indicates that serialized properties
         * for which no explicit is defined should be alphabetically (lexicograpically)
         * ordered
         */
        @Override
        public Boolean findSerializationSortAlphabetically(AnnotatedClass ac) {
            Boolean result = _primary.findSerializationSortAlphabetically(ac);
            if (result == null) {
                result = _secondary.findSerializationSortAlphabetically(ac);
            }
            return result;            
        }

        // // // Serialization: method annotations
        
        @Override
        public String findSerializationName(AnnotatedMethod am)
        {
            String result = _primary.findSerializationName(am);
            if (result == null) {
                result = _secondary.findSerializationName(am);
            } else if (result.length() == 0) {
                /* Empty String is a default; can be overridden by
                 * more explicit answer from secondary entry
                 */
                String str2 = _secondary.findSerializationName(am);
                if (str2 != null) {
                    result = str2;
                }
            }
            return result;
        }
        
        @Override
        public boolean hasAsValueAnnotation(AnnotatedMethod am)
        {
            return _primary.hasAsValueAnnotation(am) || _secondary.hasAsValueAnnotation(am);
        }
        
        @Override
        public String findEnumValue(Enum<?> value)
        {
            String result = _primary.findEnumValue(value);
            if (result == null) {
                result = _secondary.findEnumValue(value);
            }
            return result;
        }        

        // // // Serialization: field annotations

        @Override
        public String findSerializationName(AnnotatedField af)
        {
            String result = _primary.findSerializationName(af);
            if (result == null) {
                result = _secondary.findSerializationName(af);
            } else if (result.length() == 0) {
                /* Empty String is a default; can be overridden by
                 * more explicit answer from secondary entry
                 */
                String str2 = _secondary.findSerializationName(af);
                if (str2 != null) {
                    result = str2;
                }
            }
            return result;
        }

        // // // Deserialization: general annotations

        @Override
        public Object findDeserializer(Annotated am)
        {
            Object result = _primary.findDeserializer(am);
            if (result == null) {
                result = _secondary.findDeserializer(am);
            }
            return result;
        }
        
        @Override
        public Object findKeyDeserializer(Annotated am)
        {
            Object result = _primary.findKeyDeserializer(am);
            if (result == null || result == KeyDeserializer.None.class || result == NoClass.class) {
                result = _secondary.findKeyDeserializer(am);
            }
            return result;
        }

        @Override
        public Object findContentDeserializer(Annotated am)
        {
            Object result = _primary.findContentDeserializer(am);
            if (result == null || result == JsonDeserializer.None.class || result == NoClass.class) {
                result = _secondary.findContentDeserializer(am);
            }
            return result;
        }
        
        @Override
        public Class<?> findDeserializationType(Annotated am, JavaType baseType)
        {
            Class<?> result = _primary.findDeserializationType(am, baseType);
            if (result == null) {
                result = _secondary.findDeserializationType(am, baseType);
            }
            return result;
        }

        @Override
        public Class<?> findDeserializationKeyType(Annotated am, JavaType baseKeyType)
        {
            Class<?> result = _primary.findDeserializationKeyType(am, baseKeyType);
            if (result == null) {
                result = _secondary.findDeserializationKeyType(am, baseKeyType);
            }
            return result;
        }

        @Override
        public Class<?> findDeserializationContentType(Annotated am, JavaType baseContentType)
        {
            Class<?> result = _primary.findDeserializationContentType(am, baseContentType);
            if (result == null) {
                result = _secondary.findDeserializationContentType(am, baseContentType);
            }
            return result;
        }

        // // // Deserialization: class annotations

        @Override
        public Object findValueInstantiator(AnnotatedClass ac)
        {
            Object result = _primary.findValueInstantiator(ac);
            if (result == null) {
                result = _secondary.findValueInstantiator(ac);
            }
            return result;
        }

        @Override
        public Class<?> findPOJOBuilder(AnnotatedClass ac)
        {
        	Class<?> result = _primary.findPOJOBuilder(ac);
        	if (result == null) {
        		result = _secondary.findPOJOBuilder(ac);
        	}
        	return result;
        }

        @Override
        public JsonPOJOBuilder.Value findPOJOBuilderConfig(AnnotatedClass ac)
        {
            JsonPOJOBuilder.Value result = _primary.findPOJOBuilderConfig(ac);
            if (result == null) {
                result = _secondary.findPOJOBuilderConfig(ac);
            }
            return result;
        }
        
        // // // Deserialization: method annotations

        @Override
        public String findDeserializationName(AnnotatedMethod am)
        {
            String result = _primary.findDeserializationName(am);
            if (result == null) {
                result = _secondary.findDeserializationName(am);
            } else if (result.length() == 0) {
                /* Empty String is a default; can be overridden by
                 * more explicit answer from secondary entry
                 */
                String str2 = _secondary.findDeserializationName(am);
                if (str2 != null) {
                    result = str2;
                }
            }
            return result;
        }
        
        @Override
        public boolean hasAnySetterAnnotation(AnnotatedMethod am)
        {
            return _primary.hasAnySetterAnnotation(am) || _secondary.hasAnySetterAnnotation(am);
        }

        @Override
        public boolean hasAnyGetterAnnotation(AnnotatedMethod am)
        {
            return _primary.hasAnyGetterAnnotation(am) || _secondary.hasAnyGetterAnnotation(am);
        }
        
        @Override
        public boolean hasCreatorAnnotation(Annotated a)
        {
            return _primary.hasCreatorAnnotation(a) || _secondary.hasCreatorAnnotation(a);
        }
        
        // // // Deserialization: field annotations

        @Override
        public String findDeserializationName(AnnotatedField af)
        {
            String result = _primary.findDeserializationName(af);
            if (result == null) {
                result = _secondary.findDeserializationName(af);
            } else if (result.length() == 0) {
                /* Empty String is a default; can be overridden by
                 * more explicit answer from secondary entry
                 */
                String str2 = _secondary.findDeserializationName(af);
                if (str2 != null) {
                    result = str2;
                }
            }
            return result;
        }

        // // // Deserialization: parameter annotations (for creators)

        @Override
        public String findDeserializationName(AnnotatedParameter param)
        {
            String result = _primary.findDeserializationName(param);
            if (result == null) {
                result = _secondary.findDeserializationName(param);
            }
            return result;
        }
    }

}
