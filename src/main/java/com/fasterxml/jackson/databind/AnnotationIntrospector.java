package com.fasterxml.jackson.databind;

import java.lang.annotation.Annotation;
import java.util.*;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.Versioned;

import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.deser.ValueInstantiator;
import com.fasterxml.jackson.databind.introspect.*;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.util.Converter;
import com.fasterxml.jackson.databind.util.NameTransformer;

/**
 * Abstract class that defines API used for introspecting annotation-based
 * configuration for serialization and deserialization. Separated
 * so that different sets of annotations can be supported, and support
 * plugged-in dynamically.
 *<p>
 * Although default implementations are based on using annotations as the only
 * (or at least main) information source, custom implementations are not limited
 * in such a way, and in fact there is no expectation they should be. So the name
 * is bit of misnomer; this is a general configuration introspection facility.
 *<p>
 * NOTE: due to rapid addition of new methods (and changes to existing methods),
 * it is <b>strongly</b> recommended that custom implementations should not directly
 * extend this class, but rather extend {@link NopAnnotationIntrospector}.
 * This way added methods will not break backwards compatibility of custom annotation
 * introspectors.
 */
@SuppressWarnings("serial")
public abstract class AnnotationIntrospector
    implements Versioned, java.io.Serializable
{
    /*
    /**********************************************************************
    /* Helper types
    /**********************************************************************
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

    /**
     * Add-on extension used for XML-specific configuration, needed to decouple
     * format module functionality from pluggable introspection functionality
     * (especially JAXB-annotation related one).
     *
     * @since 2.13
     */
    public interface XmlExtensions
    {
        /**
         * Method that can be called to figure out generic namespace
         * property for an annotated object.
         *
         * @param config Configuration settings in effect
         * @param ann Annotated entity to introspect
         *
         * @return Null if annotated thing does not define any
         *   namespace information; non-null namespace (which may
         *   be empty String) otherwise.
         */
        public String findNamespace(MapperConfig<?> config, Annotated ann);

        /**
         * Method used to check whether given annotated element
         * (field, method, constructor parameter) has indicator that suggests
         * it be output as an XML attribute or not (if not, then as element)
         *
         * @param config Configuration settings in effect
         * @param ann Annotated entity to introspect
         *
         * @return Null if no indicator found; {@code True} or {@code False} otherwise
         */
        public Boolean isOutputAsAttribute(MapperConfig<?> config, Annotated ann);

        /**
         * Method used to check whether given annotated element
         * (field, method, constructor parameter) has indicator that suggests
         * it should be serialized as text, without element wrapper.
         *
         * @param config Configuration settings in effect
         * @param ann Annotated entity to introspect
         *
         * @return Null if no indicator found; {@code True} or {@code False} otherwise
         */
        public Boolean isOutputAsText(MapperConfig<?> config, Annotated ann);

        /**
         * Method used to check whether given annotated element
         * (field, method, constructor parameter) has indicator that suggests
         * it should be wrapped in a CDATA tag.
         *
         * @param config Configuration settings in effect
         * @param ann Annotated entity to introspect
         *
         * @return Null if no indicator found; {@code True} or {@code False} otherwise
         */
        public Boolean isOutputAsCData(MapperConfig<?> config, Annotated ann);
    }

    /*
    /**********************************************************************
    /* Factory methods
    /**********************************************************************
     */

    /**
     * Factory method for accessing "no operation" implementation
     * of introspector: instance that will never find any annotation-based
     * configuration.
     *
     * @return "no operation" instance
     */
    public static AnnotationIntrospector nopInstance() {
        return NopAnnotationIntrospector.instance;
    }

    public static AnnotationIntrospector pair(AnnotationIntrospector a1, AnnotationIntrospector a2) {
        return new AnnotationIntrospectorPair(a1, a2);
    }

    /*
    /**********************************************************
    /* Access to possibly chained introspectors
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
     *
     * @return Collection of all introspectors starting with this one, in case
     *    multiple introspectors are chained
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
     *
     * @param result Container to add introspectors to
     *
     * @return Passed in {@code Collection} filled with introspectors as explained
     *    above
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
    /* Meta-annotations (annotations for annotation types)
    /**********************************************************
     */

    /**
     * Method for checking whether given annotation is considered an
     * annotation bundle: if so, all meta-annotations it has will
     * be used instead of annotation ("bundle") itself.
     *
     * @param ann Annotated entity to introspect
     *
     * @return True if given annotation is considered an annotation
     *    bundle; false if not
     */
    public boolean isAnnotationBundle(Annotation ann) {
        return false;
    }

    /*
    /**********************************************************
    /* Annotations for Object Id handling
    /**********************************************************
     */

    /**
     * Method for checking whether given annotated thing
     * (type, or accessor) indicates that values
     * referenced (values of type of annotated class, or
     * values referenced by annotated property; latter
     * having precedence) should include Object Identifier,
     * and if so, specify details of Object Identity used.
     *
     * @param ann Annotated entity to introspect
     *
     * @return Details of Object Id as explained above, if Object Id
     *    handling to be applied; {@code null} otherwise.
     */
    public ObjectIdInfo findObjectIdInfo(Annotated ann) {
        return null;
    }

    /**
     * Method for figuring out additional properties of an Object Identity reference
     *
     * @param ann Annotated entity to introspect
     * @param objectIdInfo (optional) Base Object Id information, if any; {@code null} if none
     *
     * @return {@link ObjectIdInfo} augmented with possible additional information
     *
     * @since 2.1
     */
    public ObjectIdInfo findObjectReferenceInfo(Annotated ann, ObjectIdInfo objectIdInfo) {
        return objectIdInfo;
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
     *<p>
     * NOTE: method signature changed in 2.1, to return {@link PropertyName}
     * instead of String.
     *
     * @param ac Annotated class to introspect
     *
     * @return Root name to use, if any; {@code null} if not
     */
    public PropertyName findRootName(AnnotatedClass ac) {
        return null;
    }

    /**
     * Method for checking whether properties that have specified type
     * (class, not generics aware) should be completely ignored for
     * serialization and deserialization purposes.
     *
     * @param ac Annotated class to introspect
     *
     * @return Boolean.TRUE if properties of type should be ignored;
     *   Boolean.FALSE if they are not to be ignored, null for default
     *   handling (which is 'do not ignore')
     */
    public Boolean isIgnorableType(AnnotatedClass ac) { return null; }

    /**
     * Method for finding information about properties to ignore either by
     * name, or by more general specification ("ignore all unknown").
     * This method combines multiple aspects of ignorals and deprecates
     * earlier methods such as
     * {@link #findPropertiesToIgnore(Annotated, boolean)} and
     * {@link #findIgnoreUnknownProperties(AnnotatedClass)}.
     *
     * @param config Configuration settings in effect (for serialization or deserialization)
     * @param ann Annotated entity (Class, Accessor) to introspect
     *
     * @return Property ignoral settings to use;
     *   {@code JsonIgnoreProperties.Value.empty()} for defaults (should not return {@code null})
     *
     * @since 2.12 (to replace {@code findPropertyIgnorals()})
     */
    public JsonIgnoreProperties.Value findPropertyIgnoralByName(MapperConfig<?> config, Annotated ann)
    {
        // In 2.12, remove redirection in future
        return findPropertyIgnorals(ann);
    }

    /**
     * Method for finding information about names of properties to included.
     * This is typically used to strictly limit properties to include based
     * on fully defined set of names ("allow-listing"), as opposed to excluding
     * potential properties by exclusion ("deny-listing").
     *
     * @param config Configuration settings in effect (for serialization or deserialization)
     * @param ann Annotated entity (Class, Accessor) to introspect
     *
     * @return Property inclusion settings to use;
     *   {@code JsonIncludeProperties.Value.all()} for defaults (should not return {@code null})
     *
     * @since 2.12
     */
    public JsonIncludeProperties.Value findPropertyInclusionByName(MapperConfig<?> config, Annotated ann) {
        return JsonIncludeProperties.Value.all();
    }

    /**
     * Method for finding if annotated class has associated filter; and if so,
     * to return id that is used to locate filter.
     *
     * @param ann Annotated entity to introspect
     *
     * @return Id of the filter to use for filtering properties of annotated
     *    class, if any; or null if none found.
     */
    public Object findFilterId(Annotated ann) { return null; }

    /**
     * Method for finding {@link PropertyNamingStrategy} for given
     * class, if any specified by annotations; and if so, either return
     * a {@link PropertyNamingStrategy} instance, or Class to use for
     * creating instance
     *
     * @param ac Annotated class to introspect
     *
     * @return Sub-class or instance of {@link PropertyNamingStrategy}, if one
     *   is specified for given class; null if not.
     *
     * @since 2.1
     */
    public Object findNamingStrategy(AnnotatedClass ac) { return null; }

    /**
     * Method used to check whether specified class defines a human-readable
     * description to use for documentation.
     * There are no further definitions for contents; for example, whether
     * these may be marked up using HTML (or something like wiki format like Markup)
     * is not defined.
     *
     * @param ac Annotated class to introspect
     *
     * @return Human-readable description, if any.
     *
     * @since 2.7
     */
    public String findClassDescription(AnnotatedClass ac) { return null; }

    /**
     * @param forSerialization True if requesting properties to ignore for serialization;
     *   false if for deserialization
     * @param ac Annotated class to introspect
     *
     * @return Array of names of properties to ignore
     *
     * @since 2.6
     *
     * @deprecated Since 2.8, use {@link #findPropertyIgnoralByName} instead
     */
    @Deprecated // since 2.8
    public String[] findPropertiesToIgnore(Annotated ac, boolean forSerialization) {
        return null;
    }

    /**
     * Method for checking whether an annotation indicates that all unknown properties
     * should be ignored.
     *
     * @param ac Annotated class to introspect
     *
     * @return True if class has something indicating "ignore [all] unknown properties"
     *
     * @deprecated Since 2.8, use {@link #findPropertyIgnoralByName} instead
     */
    @Deprecated // since 2.8
    public Boolean findIgnoreUnknownProperties(AnnotatedClass ac) { return null; }

    /**
     * @param ac Annotated class to introspect
     *
     * @since 2.8
     * @deprecated 2.12, use {@link #findPropertyIgnoralByName} instead.
     *
     * @return Property ignoral settings to use;
     *   {@code JsonIgnoreProperties.Value.empty()} for defaults (should not return {@code null})
     */
    @Deprecated // since 2.12
    public JsonIgnoreProperties.Value findPropertyIgnorals(Annotated ac) {
        return JsonIgnoreProperties.Value.empty();
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
     *
     * @param ac Annotated class to introspect
     * @param checker Default visibility settings in effect before any override
     *
     * @return Visibility settings after possible annotation-based overrides
     */
    public VisibilityChecker<?> findAutoDetectVisibility(AnnotatedClass ac,
            VisibilityChecker<?> checker) {
        return checker;
    }

    /*
    /**********************************************************
    /* Annotations for Polymorphic type handling
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
     *
     * @return List of subtype definitions found if any; {@code null} if none
     */
    public List<NamedType> findSubtypes(Annotated a) { return null; }

    /**
     * Method for checking if specified type has explicit name.
     *
     * @param ac Class to check for type name annotations
     *
     * @return Explicit type name (aka Type Id) found, if any; {@code null} if none
     */
    public String findTypeName(AnnotatedClass ac) { return null; }

    /**
     * Method for checking whether given accessor claims to represent
     * type id: if so, its value may be used as an override,
     * instead of generated type id.
     *
     * @param am Annotated accessor (field/method/constructor parameter) to check for annotations
     *
     * @return Boolean to indicate whether member is a type id or not, if annotation
     *    found; {@code null} if no information found.
     */
    public Boolean isTypeId(AnnotatedMember am) { return null; }

    /*
    /**********************************************************
    /* General member (field, method/constructor) annotations
    /**********************************************************
     */

    /**
     * Method for checking if given member indicates that it is part
     * of a reference (parent/child).
     */
    public ReferenceProperty findReferenceType(AnnotatedMember member) { return null; }

    /**
     * Method called to check whether given property is marked to be "unwrapped"
     * when being serialized (and appropriately handled in reverse direction,
     * i.e. expect unwrapped representation during deserialization).
     * Return value is the name transformation to use, if wrapping/unwrapping
     * should  be done, or null if not -- note that transformation may simply
     * be identity transformation (no changes).
     */
    public NameTransformer findUnwrappingNameTransformer(AnnotatedMember member) { return null; }

    /**
     * Method called to check whether given property is marked to
     * be ignored. This is used to determine whether to ignore
     * properties, on per-property basis, usually combining
     * annotations from multiple accessors (getters, setters, fields,
     * constructor parameters).
     */
    public boolean hasIgnoreMarker(AnnotatedMember m) { return false; }

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
     *
     * @since 2.9
     */
    public JacksonInject.Value findInjectableValue(AnnotatedMember m) {
        // 05-Apr-2017, tatu: Just for 2.9, call deprecated method to help
        //    with some cases of overrides for legacy code
        Object id = findInjectableValueId(m);
        if (id != null) {
            return JacksonInject.Value.forId(id);
        }
        return null;
    }

    /**
     * Method that can be called to check whether this member has
     * an annotation that suggests whether value for matching property
     * is required or not.
     */
    public Boolean hasRequiredMarker(AnnotatedMember m) { return null; }

    /**
     * Method for checking if annotated property (represented by a field or
     * getter/setter method) has definitions for views it is to be included in.
     * If null is returned, no view definitions exist and property is always
     * included (or always excluded as per default view inclusion configuration);
     * otherwise it will only be included for views included in returned
     * array. View matches are checked using class inheritance rules (sub-classes
     * inherit inclusions of super-classes)
     *<p>
     * Since 2.9 this method may also be called to find "default view(s)" for
     * {@link AnnotatedClass}
     *
     * @param a Annotated property (represented by a method, field or ctor parameter)
     * @return Array of views (represented by classes) that the property is included in;
     *    if null, always included (same as returning array containing <code>Object.class</code>)
     */
    public Class<?>[] findViews(Annotated a) { return null; }

    /**
     * Method for finding format annotations for property or class.
     * Return value is typically used by serializers and/or
     * deserializers to customize presentation aspects of the
     * serialized value.
     *
     * @since 2.1
     */
    public JsonFormat.Value findFormat(Annotated memberOrClass) {
        return JsonFormat.Value.empty();
    }

    /**
     * Method used to check if specified property has annotation that indicates
     * that it should be wrapped in an element; and if so, name to use.
     * Note that not all serializers and deserializers support use this method:
     * currently (2.1) it is only used by XML-backed handlers.
     *
     * @return Wrapper name to use, if any, or {@link PropertyName#USE_DEFAULT}
     *   to indicate that no wrapper element should be used.
     *
     * @since 2.1
     */
    public PropertyName findWrapperName(Annotated ann) { return null; }

    /**
     * Method for finding suggested default value (as simple textual serialization)
     * for the property. While core databind does not make any use of it, it is exposed
     * for extension modules to use: an expected use is generation of schema representations
     * and documentation.
     *
     * @since 2.5
     */
    public String findPropertyDefaultValue(Annotated ann) { return null; }

    /**
     * Method used to check whether specified property member (accessor
     * or mutator) defines human-readable description to use for documentation.
     * There are no further definitions for contents; for example, whether
     * these may be marked up using HTML is not defined.
     *
     * @return Human-readable description, if any.
     *
     * @since 2.3
     */
    public String findPropertyDescription(Annotated ann) { return null; }

    /**
     * Method used to check whether specified property member (accessor
     * or mutator) defines numeric index, and if so, what is the index value.
     * Possible use cases for index values included use by underlying data format
     * (some binary formats mandate use of index instead of name) and ordering
     * of properties (for documentation, or during serialization).
     *
     * @since 2.4
     *
     * @return Explicitly specified index for the property, if any
     */
    public Integer findPropertyIndex(Annotated ann) { return null; }

    /**
     * Method for finding implicit name for a property that given annotated
     * member (field, method, creator parameter) may represent.
     * This is different from explicit, annotation-based property name, in that
     * it is "weak" and does not either proof that a property exists (for example,
     * if visibility is not high enough), or override explicit names.
     * In practice this method is used to introspect optional names for creator
     * parameters (which may or may not be available and cannot be detected
     * by standard databind); or to provide alternate name mangling for
     * fields, getters and/or setters.
     *
     * @since 2.4
     */
    public String findImplicitPropertyName(AnnotatedMember member) { return null; }

    /**
     * Method called to find if given property has alias(es) defined.
     *
     * @return `null` if member has no information; otherwise a `List` (possibly
     *   empty) of aliases to use.
     *
     * @since 2.9
     */
    public List<PropertyName> findPropertyAliases(Annotated ann) { return null; }

    /**
     * Method for finding optional access definition for a property, annotated
     * on one of its accessors. If a definition for read-only, write-only
     * or read-write cases, visibility rules may be modified. Note, however,
     * that even more specific annotations (like one for ignoring specific accessor)
     * may further override behavior of the access definition.
     *
     * @since 2.6
     */
    public JsonProperty.Access findPropertyAccess(Annotated ann) { return null; }

    /**
     * Method called in cases where a class has two methods eligible to be used
     * for the same logical property, and default logic is not enough to figure
     * out clear precedence. Introspector may try to choose one to use; or, if
     * unable, return `null` to indicate it cannot resolve the problem.
     *
     * @since 2.7
     */
    public AnnotatedMethod resolveSetterConflict(MapperConfig<?> config,
            AnnotatedMethod setter1, AnnotatedMethod setter2) {
        return null;
    }

    /**
     * Method called on fields that are eligible candidates for properties
     * (that is, non-static member fields), but not necessarily selected (may
     * or may not be visible), to let fields affect name linking.
     * Call will be made after finding implicit name (which by default is just
     * name of the field, but may be overridden by introspector), but before
     * discovering other accessors.
     * If non-null name returned, it is to be used to find other accessors (getters,
     * setters, creator parameters) and replace their implicit names with that
     * of field's implicit name (assuming they differ).
     *<p>
     * Specific example (and initial use case is for support Kotlin's "is getter"
     * matching (see
     * <a href="https://kotlinlang.org/docs/reference/java-to-kotlin-interop.html">Kotling Interop</a>
     * for details), in which field like '{@code isOpen}' would have implicit name of
     * "isOpen", match getter {@code getOpen()} and setter {@code setOpen(boolean)},
     * but use logical external name of "isOpen" (and not implicit name of getter/setter, "open"!).
     * To achieve this, field implicit name needs to remain "isOpen" but this method needs
     * to return name {@code PropertyName.construct("open")}: doing so will "pull in" getter
     * and/or setter, and rename them as "isOpen".
     *
     * @param config Effective mapper configuration in use
     * @param f Field to check
     * @param implName Implicit name of the field; usually name of field itself but not always,
     *    used as the target name for accessors to rename.
     *
     * @return Name used to find other accessors to rename, if any; {@code null} to indicate
     *    no renaming
     *
     * @since 2.11
     */
    public PropertyName findRenameByField(MapperConfig<?> config,
            AnnotatedField f, PropertyName implName) {
        return null;
    }

    /**
     * @deprecated Since 2.9 Use {@link #findInjectableValue} instead
     */
    @Deprecated // since 2.9
    public Object findInjectableValueId(AnnotatedMember m) {
        return null;
    }

    /*
    /**********************************************************
    /* Serialization: general annotations
    /**********************************************************
     */

    /**
     * Method for getting a serializer definition on specified method
     * or field. Type of definition is either instance (of type {@link JsonSerializer})
     * or Class (of {@code Class<JsonSerializer>} implementation subtype);
     * if value of different type is returned, a runtime exception may be thrown by caller.
     */
    public Object findSerializer(Annotated am) {
        return null;
    }

    /**
     * Method for getting a serializer definition for keys of associated {@code java.util.Map} property.
     * Type of definition is either instance (of type {@link JsonSerializer})
     * or Class (of type  {@code Class<JsonSerializer>});
     * if value of different type is returned, a runtime exception may be thrown by caller.
     */
    public Object findKeySerializer(Annotated am) {
        return null;
    }

    /**
     * Method for getting a serializer definition for content (values) of
     * associated <code>Collection</code>, <code>array</code> or {@code Map} property.
     * Type of definition is either instance (of type {@link JsonSerializer})
     * or Class (of type  {@code Class<JsonSerializer>});
     * if value of different
     * type is returned, a runtime exception may be thrown by caller.
     */
    public Object findContentSerializer(Annotated am) {
        return null;
    }

    /**
     * Method for getting a serializer definition for serializer to use
     * for nulls (null values) of associated property or type.
     *
     * @since 2.3
     */
    public Object findNullSerializer(Annotated am) {
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

    /**
     * Method for finding {@link Converter} that annotated entity
     * (property or class) has indicated to be used as part of
     * serialization. If not null, either has to be actual
     * {@link Converter} instance, or class for such converter;
     * and resulting converter will be used first to convert property
     * value to converter target type, and then serializer for that
     * type is used for actual serialization.
     *<p>
     * This feature is typically used to convert internal values into types
     * that Jackson can convert.
     *<p>
     * Note also that this feature does not necessarily work well with polymorphic
     * type handling, or object identity handling; if such features are needed
     * an explicit serializer is usually better way to handle serialization.
     *
     * @param a Annotated property (field, method) or class to check for
     *   annotations
     *
     * @since 2.2
     */
    public Object findSerializationConverter(Annotated a) {
        return null;
    }

    /**
     * Method for finding {@link Converter} that annotated property
     * has indicated needs to be used for values of container type
     * (this also means that method should only be called for properties
     * of container types, List/Map/array properties).
     *<p>
     * If not null, either has to be actual
     * {@link Converter} instance, or class for such converter;
     * and resulting converter will be used first to convert property
     * value to converter target type, and then serializer for that
     * type is used for actual serialization.
     *<p>
     * Other notes are same as those for {@link #findSerializationConverter}
     *
     * @param a Annotated property (field, method) to check.
     *
     * @since 2.2
     */
    public Object findSerializationContentConverter(AnnotatedMember a) {
        return null;
    }

    /**
     * Method for checking inclusion criteria for a type (Class) or property (yes, method
     * name is bit unfortunate -- not just for properties!).
     * In case of class, acts as the default for properties POJO contains; for properties
     * acts as override for class defaults and possible global defaults.
     *
     * @since 2.6
     */
    public JsonInclude.Value findPropertyInclusion(Annotated a) {
        return JsonInclude.Value.empty();
    }

    /**
     * Method for checking whether given annotated entity (class, method,
     * field) defines which Bean/Map properties are to be included in
     * serialization.
     * If no annotation is found, method should return given second
     * argument; otherwise value indicated by the annotation.
     *<p>
     * Note that meaning of inclusion value depends on whether it is for
     * a Class or property (field/method/constructor): in former case,
     * it is the default for all properties; in latter case it is specific
     * override for annotated property.
     *
     * @return Enumerated value indicating which properties to include
     *   in serialization
     *
     * @deprecated Since 2.7 Use {@link #findPropertyInclusion} instead
     */
    @Deprecated // since 2.7
    public JsonInclude.Include findSerializationInclusion(Annotated a, JsonInclude.Include defValue) {
        return defValue;
    }

    /**
     * Method for checking whether content (entries) of a {@link java.util.Map} property
     * are to be included during serialization or not.
     * NOTE: this is NOT called for POJO properties, or array/Collection elements.
     *
     * @since 2.5
     *
     * @deprecated Since 2.7 Use {@link #findPropertyInclusion} instead
     */
    @Deprecated // since 2.7
    public JsonInclude.Include findSerializationInclusionForContent(Annotated a, JsonInclude.Include defValue) {
        return defValue;
    }

    /*
    /**********************************************************
    /* Serialization: type refinements
    /**********************************************************
     */

    /**
     * Method called to find out possible type refinements to use
     * for deserialization, including not just value itself but
     * key and/or content type, if type has those.
     *
     * @since 2.7
     */
    public JavaType refineSerializationType(final MapperConfig<?> config,
            final Annotated a, final JavaType baseType) throws JsonMappingException
    {
        return baseType;
    }

    /**
     * @deprecated Since 2.7 call {@link #refineSerializationType} instead
     */
    @Deprecated // since 2.7
    public Class<?> findSerializationType(Annotated a) {
        return null;
    }

    /**
     * @deprecated Since 2.7 call {@link #refineSerializationType} instead
     */
    @Deprecated // since 2.7
    public Class<?> findSerializationKeyType(Annotated am, JavaType baseType) {
        return null;
    }

    /**
     * @deprecated Since 2.7 call {@link #refineSerializationType} instead
     */
    @Deprecated // since 2.7
    public Class<?> findSerializationContentType(Annotated am, JavaType baseType) {
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
    public Boolean findSerializationSortAlphabetically(Annotated ann) {
        return null;
    }

    /**
     * Method for adding possible virtual properties to be serialized along
     * with regular properties.
     *
     * @since 2.5
     */
    public void findAndAddVirtualProperties(MapperConfig<?> config, AnnotatedClass ac,
            List<BeanPropertyWriter> properties) { }

    /*
    /**********************************************************
    /* Serialization: property annotations
    /**********************************************************
     */

    /**
     * Method for checking whether given property accessors (method,
     * field) has an annotation that suggests property name to use
     * for serialization.
     * Should return null if no annotation
     * is found; otherwise a non-null name (possibly
     * {@link PropertyName#USE_DEFAULT}, which means "use default heuristics").
     *
     * @param a Property accessor to check
     *
     * @return Name to use if found; null if not.
     *
     * @since 2.1
     */
    public PropertyName findNameForSerialization(Annotated a) {
        return null;
    }

    /**
     * Method for checking whether given method has an annotation
     * that suggests the return value of annotated field or method
     * should be used as "the key" of the object instance; usually
     * serialized as a primitive value such as String or number.
     *
     * @return {@link Boolean#TRUE} if such annotation is found and is not disabled;
     *   {@link Boolean#FALSE} if disabled annotation (block) is found (to indicate
     *   accessor is definitely NOT to be used "as value"); or `null` if no
     *   information found.
     *
     * @since 2.12
     */
    public Boolean hasAsKey(MapperConfig<?> config, Annotated a) {
        return null;
    }

    /**
     * Method for checking whether given method has an annotation
     * that suggests that the return value of annotated method
     * should be used as "the value" of the object instance; usually
     * serialized as a primitive value such as String or number.
     *
     * @return {@link Boolean#TRUE} if such annotation is found and is not disabled;
     *   {@link Boolean#FALSE} if disabled annotation (block) is found (to indicate
     *   accessor is definitely NOT to be used "as value"); or `null` if no
     *   information found.
     *
     * @since 2.9
     */
    public Boolean hasAsValue(Annotated a) {
        // 20-Nov-2016, tatu: Delegate in 2.9; remove redirect from later versions
        if (a instanceof AnnotatedMethod) {
            if (hasAsValueAnnotation((AnnotatedMethod) a)) {
                return true;
            }
        }
        return null;
    }

    /**
     * Method for checking whether given method has an annotation
     * that suggests that the method is to serve as "any setter";
     * method to be used for accessing set of miscellaneous "extra"
     * properties, often bound with matching "any setter" method.
     *
     * @param ann Annotated entity to check
     *
     * @return True if such annotation is found (and is not disabled),
     *   false otherwise
     *
     * @since 2.9
     */
    public Boolean hasAnyGetter(Annotated ann) {
        // 21-Nov-2016, tatu: Delegate in 2.9; remove redirect from later versions
        if (ann instanceof AnnotatedMethod) {
            if (hasAnyGetterAnnotation((AnnotatedMethod) ann)) {
                return true;
            }
        }
        return null;
    }

    /**
     * Method for efficiently figuring out which if given set of <code>Enum</code> values
     * have explicitly defined name. Method will overwrite entries in incoming <code>names</code>
     * array with explicit names found, if any, leaving other entries unmodified.
     *
     * @param enumType Type of Enumeration
     * @param enumValues Values of enumeration
     * @param names Matching declared names of enumeration values (with indexes
     *     matching {@code enumValues} entries)
     *
     * @return Array of names to use (possible {@code names} passed as argument)
     *
     * @since 2.7
     */
    public String[] findEnumValues(Class<?> enumType, Enum<?>[] enumValues, String[] names) {
        // 18-Oct-2016, tatu: In 2.8 delegated to deprecated method; not so in 2.9 and beyond
        return names;
    }

    /**
     * Method that is related to {@link #findEnumValues} but is called to check if
     * there are alternative names (aliased) that can be accepted for entries, in
     * addition to primary names introspected earlier.
     * If so, these aliases should be returned in {@code aliases} {@link List} passed
     * as argument (and initialized for proper size by caller).
     *
     * @param enumType Type of Enumeration
     * @param enumValues Values of enumeration
     * @param aliases (in/out) Pre-allocated array where aliases found, if any, may be
     *     added (in indexes matching those of {@code enumValues})
     *
     * @since 2.11
     */
    public void findEnumAliases(Class<?> enumType, Enum<?>[] enumValues, String[][] aliases) {
        ;
    }

    /**
     * Finds the Enum value that should be considered the default value, if possible.
     *
     * @param enumCls The Enum class to scan for the default value
     *
     * @return null if none found or it's not possible to determine one
     *
     * @since 2.8
     */
    public Enum<?> findDefaultEnumValue(Class<Enum<?>> enumCls) {
        return null;
    }

    /**
     * Method for determining the String value to use for serializing
     * given enumeration entry; used when serializing enumerations
     * as Strings (the standard method).
     *
     * @param value Enum value to introspect
     *
     * @return Serialized enum value.
     *
     * @deprecated Since 2.8: use {@link #findEnumValues} instead because this method
     *    does not properly handle override settings (defaults to <code>enum.name</code>
     *    without indicating whether that is explicit or not), and is inefficient to
     *    call one-by-one.
     */
    @Deprecated
    public String findEnumValue(Enum<?> value) {
        return value.name();
    }

    /**
     * @param am Annotated method to check
     *
     * @deprecated Since 2.9 Use {@link #hasAsValue(Annotated)} instead.
     */
    @Deprecated // since 2.9
    public boolean hasAsValueAnnotation(AnnotatedMethod am) {
        return false;
    }

    /**
     * @param am Annotated method to check
     *
     * @deprecated Since 2.9 Use {@link #hasAnyGetter} instead
     */
    @Deprecated
    public boolean hasAnyGetterAnnotation(AnnotatedMethod am) {
        return false;
    }

    /*
    /**********************************************************
    /* Deserialization: general annotations
    /**********************************************************
     */

    /**
     * Method for getting a deserializer definition on specified method
     * or field.
     * Type of definition is either instance (of type {@link JsonDeserializer})
     * or Class (of type  {@code Class&<JsonDeserializer>});
     * type is returned, a runtime exception may be thrown by caller.
     */
    public Object findDeserializer(Annotated am) {
        return null;
    }

    /**
     * Method for getting a deserializer definition for keys of
     * associated <code>Map</code> property.
     * Type of definition is either instance (of type {@link JsonDeserializer})
     * or Class (of type  {@code Class<JsonDeserializer>});
     * if value of different
     * type is returned, a runtime exception may be thrown by caller.
     */
    public Object findKeyDeserializer(Annotated am) {
        return null;
    }

    /**
     * Method for getting a deserializer definition for content (values) of
     * associated <code>Collection</code>, <code>array</code> or
     * <code>Map</code> property.
     * Type of definition is either instance (of type {@link JsonDeserializer})
     * or Class (of type  {@code Class<JsonDeserializer>});
     * if value of different
     * type is returned, a runtime exception may be thrown by caller.
     */
    public Object findContentDeserializer(Annotated am) {
        return null;
    }

    /**
     * Method for finding {@link Converter} that annotated entity
     * (property or class) has indicated to be used as part of
     * deserialization.
     * If not null, either has to be actual
     * {@link Converter} instance, or class for such converter;
     * and resulting converter will be used after Jackson has deserializer
     * data into intermediate type (Converter input type), and Converter
     * needs to convert this into its target type to be set as property value.
     *<p>
     * This feature is typically used to convert intermediate Jackson types
     * (that default deserializers can produce) into custom type instances.
     *<p>
     * Note also that this feature does not necessarily work well with polymorphic
     * type handling, or object identity handling; if such features are needed
     * an explicit deserializer is usually better way to handle deserialization.
     *
     * @param a Annotated property (field, method) or class to check for
     *   annotations
     *
     * @since 2.2
     */
    public Object findDeserializationConverter(Annotated a) {
        return null;
    }

    /**
     * Method for finding {@link Converter} that annotated property
     * has indicated needs to be used for values of container type
     * (this also means that method should only be called for properties
     * of container types, List/Map/array properties).
     *<p>
     * If not null, either has to be actual
     * {@link Converter} instance, or class for such converter;
     * and resulting converter will be used after Jackson has deserializer
     * data into intermediate type (Converter input type), and Converter
     * needs to convert this into its target type to be set as property value.
     *<p>
     * Other notes are same as those for {@link #findDeserializationConverter}
     *
     * @param a Annotated property (field, method) to check.
     *
     * @since 2.2
     */
    public Object findDeserializationContentConverter(AnnotatedMember a) {
        return null;
    }

    /*
    /**********************************************************
    /* Deserialization: type refinements
    /**********************************************************
     */

    /**
     * Method called to find out possible type refinements to use
     * for deserialization.
     *
     * @since 2.7
     */
    public JavaType refineDeserializationType(final MapperConfig<?> config,
            final Annotated a, final JavaType baseType) throws JsonMappingException
    {
        return baseType;
    }

    /**
     * Method for accessing annotated type definition that a
     * property can have, to be used as the type for deserialization
     * instead of the static (declared) type.
     * Type is usually narrowing conversion (i.e.subtype of declared type).
     * Declared return type of the method is also considered acceptable.
     *
     * @param ann Annotated entity to introspect
     * @param baseType Assumed type before considering annotations
     *
     * @return Class to use for deserialization instead of declared type
     *
     * @deprecated Since 2.7 call {@link #refineDeserializationType} instead
     */
    @Deprecated
    public Class<?> findDeserializationType(Annotated ann, JavaType baseType) {
        return null;
    }

    /**
     * Method for accessing additional narrowing type definition that a
     * method can have, to define more specific key type to use.
     * It should be only be used with {@link java.util.Map} types.
     *
     * @param ann Annotated entity to introspect
     * @param baseKeyType Assumed key type before considering annotations
     *
     * @return Class specifying more specific type to use instead of
     *   declared type, if annotation found; null if not
     *
     * @deprecated Since 2.7 call {@link #refineDeserializationType} instead
     */
    @Deprecated
    public Class<?> findDeserializationKeyType(Annotated ann, JavaType baseKeyType) {
        return null;
    }

    /**
     * Method for accessing additional narrowing type definition that a
     * method can have, to define more specific content type to use;
     * content refers to Map values and Collection/array elements.
     * It should be only be used with Map, Collection and array types.
     *
     * @param ann Annotated entity to introspect
     * @param baseContentType Assumed content (value) type before considering annotations
     *
     * @return Class specifying more specific type to use instead of
     *   declared type, if annotation found; null if not
     *
     * @deprecated Since 2.7 call {@link #refineDeserializationType} instead
     */
    @Deprecated
    public Class<?> findDeserializationContentType(Annotated ann, JavaType baseContentType) {
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
     *
     * @param ac Annotated class to introspect
     *
     * @return Either {@link ValueInstantiator} instance to use, or
     *    {@link Class} of one to create; or {@code null} if no annotations
     *    found to indicate custom value instantiator.
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
     * @param ac Annotated class to introspect
     *
     * @return Builder class to use, if annotation found; {@code null} if not.
     *
     * @since 2.0
     */
    public Class<?> findPOJOBuilder(AnnotatedClass ac) {
        return null;
    }

    /**
     * @param ac Annotated class to introspect
     *
     * @return Builder settings to use, if any found; {@code null} if not.
     *
     * @since 2.0
     */
    public JsonPOJOBuilder.Value findPOJOBuilderConfig(AnnotatedClass ac) {
        return null;
    }

    /*
    /**********************************************************
    /* Deserialization: property annotations
    /**********************************************************
     */

    /**
     * Method for checking whether given property accessors (method,
     * field) has an annotation that suggests property name to use
     * for deserialization (reading JSON into POJOs).
     * Should return null if no annotation
     * is found; otherwise a non-null name (possibly
     * {@link PropertyName#USE_DEFAULT}, which means "use default heuristics").
     *
     * @param ann Annotated entity to check
     *
     * @return Name to use if found; {@code null} if not.
     *
     * @since 2.1
     */
    public PropertyName findNameForDeserialization(Annotated ann) {
        return null;
    }

    /**
     * Method for checking whether given method has an annotation
     * that suggests that the method is to serve as "any setter";
     * method to be used for setting values of any properties for
     * which no dedicated setter method is found.
     *
     * @param ann Annotated entity to check
     *
     * @return {@code Boolean.TRUE} or {@code Boolean.FALSE} if explicit
     *   "any setter" marker found; {@code null} otherwise.
     *
     * @since 2.9
     */
    public Boolean hasAnySetter(Annotated ann) {
        return null;
    }

    /**
     * Method for finding possible settings for property, given annotations
     * on an accessor.
     *
     * @param ann Annotated entity to check
     *
     * @return Setter info value found, if any;
     *   {@code JsonSetter.Value.empty()} if none (should not return {@code null})
     *
     * @since 2.9
     */
    public JsonSetter.Value findSetterInfo(Annotated ann) {
        return JsonSetter.Value.empty();
    }

    /**
     * Method for finding merge settings for property, if any.
     *
     * @param ann Annotated entity to check
     *
     * @return {@code Boolean.TRUE} or {@code Boolean.FALSE} if explicit
     *    merge enable/disable found; {@code null} otherwise.
     *
     * @since 2.9
     */
    public Boolean findMergeInfo(Annotated ann) {
        return null;
    }

    /**
     * Method called to check whether potential Creator (constructor or static factory
     * method) has explicit annotation to indicate it as actual Creator; and if so,
     * which {@link com.fasterxml.jackson.annotation.JsonCreator.Mode} to use.
     *<p>
     * NOTE: caller needs to consider possibility of both `null` (no annotation found)
     * and {@link com.fasterxml.jackson.annotation.JsonCreator.Mode#DISABLED} (annotation found,
     * but disabled); latter is necessary as marker in case multiple introspectors are chained,
     * as well as possibly as when using mix-in annotations.
     *
     * @param config Configuration settings in effect (for serialization or deserialization)
     * @param ann Annotated accessor (usually constructor or static method) to check
     *
     * @return Creator mode found, if any; {@code null} if none
     *
     * @since 2.9
     */
    public JsonCreator.Mode findCreatorAnnotation(MapperConfig<?> config, Annotated ann) {
        // 13-Sep-2016, tatu: for backwards compatibility, implement using delegation
        ///   (remove from version AFTER 2.9)
        if (hasCreatorAnnotation(ann)) {
            JsonCreator.Mode mode = findCreatorBinding(ann);
            if (mode == null) {
                mode = JsonCreator.Mode.DEFAULT;
            }
            return mode;
        }
        return null;
    }

    /**
     * Method for checking whether given annotated item (method, constructor)
     * has an annotation
     * that suggests that the method is a "creator" (aka factory)
     * method to be used for construct new instances of deserialized
     * values.
     *
     * @param ann Annotated entity to check
     *
     * @return True if such annotation is found (and is not disabled),
     *   false otherwise
     *
     * @deprecated Since 2.9 use {@link #findCreatorAnnotation} instead.
     */
    @Deprecated
    public boolean hasCreatorAnnotation(Annotated ann) {
        return false;
    }

    /**
     * Method for finding indication of creator binding mode for
     * a creator (something for which {@link #hasCreatorAnnotation} returns
     * true), for cases where there may be ambiguity (currently: single-argument
     * creator with implicit but no explicit name for the argument).
     *
     * @param ann Annotated entity to check
     *
     * @return Creator mode found, if any; {@code null} if none
     *
     * @since 2.5
     * @deprecated Since 2.9 use {@link #findCreatorAnnotation} instead.
     */
    @Deprecated
    public JsonCreator.Mode findCreatorBinding(Annotated ann) {
        return null;
    }

    /**
     * @param am Annotated method to check
     *
     * @deprecated Since 2.9 use {@link #hasAnySetter} instead.
     *
     * @return {@code true} if "any-setter" annotation was found; {@code false} otherwise
     */
    @Deprecated // since 2.9
    public boolean hasAnySetterAnnotation(AnnotatedMethod am) {
        return false;
    }

    /*
    /**********************************************************
    /* Overridable methods: may be used as low-level extension
    /* points.
    /**********************************************************
     */

    /**
     * Method that should be used by sub-classes for ALL
     * annotation access;
     * overridable so
     * that sub-classes may, if they choose to, mangle actual access to
     * block access ("hide" annotations) or perhaps change it.
     *<p>
     * Default implementation is simply:
     *<code>
     *  return annotated.getAnnotation(annoClass);
     *</code>
     *
     * @param <A> Annotation type being checked
     * @param ann Annotated entity to check for specified annotation
     * @param annoClass Type of annotation to find
     *
     * @return Value of given annotation (as per {@code annoClass}), if entity
     *    has one; {@code null} otherwise
     *
     * @since 2.5
     */
    protected <A extends Annotation> A _findAnnotation(Annotated ann,
            Class<A> annoClass) {
        return ann.getAnnotation(annoClass);
    }

    /**
     * Method that should be used by sub-classes for ALL
     * annotation existence access;
     * overridable so  that sub-classes may, if they choose to, mangle actual access to
     * block access ("hide" annotations) or perhaps change value seen.
     *<p>
     * Default implementation is simply:
     *<code>
     *  return annotated.hasAnnotation(annoClass);
     *</code>
     *
     * @param ann Annotated entity to check for specified annotation
     * @param annoClass Type of annotation to find
     *
     * @return {@code true} if specified annotation exists in given entity; {@code false} if not
     *
     * @since 2.5
     */
    protected boolean _hasAnnotation(Annotated ann, Class<? extends Annotation> annoClass) {
        return ann.hasAnnotation(annoClass);
    }

    /**
     * Alternative lookup method that is used to see if annotation has at least one of
     * annotations of types listed in second argument.
     *
     * @param ann Annotated entity to check for specified annotation
     * @param annoClasses Types of annotation to find
     *
     * @return {@code true} if at least one of specified annotation exists in given entity;
     *    {@code false} otherwise
     *
     * @since 2.7
     */
    protected boolean _hasOneOf(Annotated ann, Class<? extends Annotation>[] annoClasses) {
        return ann.hasOneOf(annoClasses);
    }
}
