package tools.jackson.databind.jsontype.impl;

import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import tools.jackson.databind.AnnotationIntrospector;
import tools.jackson.databind.DatabindContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.PropertyName;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.introspect.AnnotatedClass;
import tools.jackson.databind.introspect.AnnotatedClassResolver;
import tools.jackson.databind.introspect.AnnotatedMethod;
import tools.jackson.databind.jsontype.NamedType;
import tools.jackson.databind.util.ClassUtil;

/**
 * {@link tools.jackson.databind.jsontype.TypeIdResolver} implementation
 * that converts using explicitly (annotation-) specified type names
 * and maps to implementation classes; or, in absence of annotated type name,
 * defaults to fully-qualified {@link Class} names (obtained with {@link Class#getName()}
 */
public class TypeNameIdResolver extends TypeIdResolverBase
    implements java.io.Serializable // @since 2.16.2
{
    private static final long serialVersionUID = 3L;

    /**
     * Mappings from class name to type id, used for serialization.
     *<p>
     * Since lazily constructed will require synchronization (either internal
     * by type, or external)
     */
    protected final ConcurrentHashMap<String, String> _typeToId;

    /**
     * Mappings from type id to JavaType, used for deserialization.
     *<p>
     * Eagerly constructed, not modified, can use regular unsynchronized {@link Map}.
     */
    protected final Map<String, JavaType> _idToType;

    protected final boolean _caseInsensitive;

    protected TypeNameIdResolver(JavaType baseType,
            ConcurrentHashMap<String, String> typeToId,
            HashMap<String, JavaType> idToType,
            boolean caseInsensitive)
    {
        super(baseType);
        _typeToId = typeToId;
        _idToType = idToType;
        _caseInsensitive = caseInsensitive;
    }

    public static TypeNameIdResolver construct(MapperConfig<?> config, JavaType baseType,
            Collection<NamedType> subtypes, boolean forSer, boolean forDeser)
    {
        // sanity check
        if (forSer == forDeser) throw new IllegalArgumentException();

        final ConcurrentHashMap<String, String> typeToId;
        final HashMap<String, JavaType> idToType;

        if (forSer) {
            // Only need Class-to-id for serialization; but synchronized since may be
            // lazily built (if adding type-id-mappings dynamically)
            typeToId = new ConcurrentHashMap<>();
            idToType = null;
        } else {
            idToType = new HashMap<>();
            // 14-Apr-2016, tatu: Apparently needed for special case of `defaultImpl`;
            //    see [databind#1198] for details: but essentially we only need room
            //    for a single value.
            typeToId = new ConcurrentHashMap<>(4);
        }
        final boolean caseInsensitive = config.isEnabled(MapperFeature.ACCEPT_CASE_INSENSITIVE_VALUES);

        if (subtypes != null) {
            for (NamedType t : subtypes) {
                // no name? Need to figure out default; for now, let's just
                // use non-qualified class name
                Class<?> cls = t.getType();
                String id = t.hasName() ? t.getName() : _defaultTypeId(cls);
                if (forSer) {
                    String creatorId = t.hasName() ? null
                            : _findSealedCreatorId(config, cls, subtypes, caseInsensitive,
                                    TypeNameIdResolver::_defaultTypeId);
                    typeToId.put(cls.getName(), (creatorId == null) ? id : creatorId);
                }
                if (forDeser) {
                    // [databind#1983]: for case-insensitive lookups must canonicalize:
                    if (caseInsensitive) {
                        id = id.toLowerCase();
                    }
                    // One more problem; sometimes we have same name for multiple types;
                    // if so, use most specific
                    JavaType prev = idToType.get(id); // lgtm [java/dereferenced-value-may-be-null]
                    if (prev != null) { // Can only override if more specific
                        if (cls.isAssignableFrom(prev.getRawClass())) { // nope, more generic (or same)
                            continue;
                        }
                    }
                    idToType.put(id, config.constructType(cls));
                }
            }
        }
        return new TypeNameIdResolver(baseType, typeToId, idToType, caseInsensitive);
    }

    // A sealed parent can serve as a named factory for an otherwise unnamed leaf.
    // Keep distinct default IDs when the parent cannot create the leaf, or when
    // sharing its ID would collapse multiple unnamed concrete subtypes.
    static String _findSealedCreatorId(MapperConfig<?> config, Class<?> subtype,
            Collection<NamedType> subtypes, boolean caseInsensitive,
            Function<Class<?>, String> defaultTypeId) {
        if (!config.isAnnotationProcessingEnabled() || !ClassUtil.isConcrete(subtype)
                || !Modifier.isFinal(subtype.getModifiers())) {
            return null;
        }
        AnnotationIntrospector intr = config.getAnnotationIntrospector();
        for (NamedType parent : subtypes) {
            Class<?> parentType = parent.getType();
            if (!parent.hasName() || ClassUtil.isConcrete(parentType)
                    || !parentType.isAssignableFrom(subtype) || !parentType.isSealed()) {
                continue;
            }
            Class<?>[] permitted = parentType.getPermittedSubclasses();
            if (permitted.length != 1 || permitted[0] != subtype) {
                continue;
            }
            String id = parent.getName();
            boolean ambiguous = subtypes.stream().anyMatch(other -> {
                Class<?> otherType = other.getType();
                if (otherType == parentType || otherType == subtype) {
                    return false;
                }
                String otherId = other.hasName() ? other.getName() : defaultTypeId.apply(otherType);
                return caseInsensitive ? id.equalsIgnoreCase(otherId) : id.equals(otherId);
            });
            if (ambiguous) {
                continue;
            }
            AnnotatedClass parentInfo = AnnotatedClassResolver.resolve(config,
                    config.constructType(parentType), config);
            for (AnnotatedMethod factory : parentInfo.getFactoryMethods()) {
                Class<?> returnType = factory.getRawReturnType();
                if (!parentType.isAssignableFrom(returnType) || !returnType.isAssignableFrom(subtype)) {
                    continue;
                }
                if (_hasNamedCreatorParameters(config, intr, factory)) {
                    AnnotatedClass subtypeInfo = AnnotatedClassResolver.resolve(config,
                            config.constructType(subtype), config);
                    if (id.equals(intr.findTypeName(config, subtypeInfo))) {
                        return id;
                    }
                    break;
                }
            }
        }
        return null;
    }

    private static boolean _hasNamedCreatorParameters(MapperConfig<?> config,
            AnnotationIntrospector intr, AnnotatedMethod factory) {
        JsonCreator.Mode mode = intr.findCreatorAnnotation(config, factory);
        if ((mode != JsonCreator.Mode.DEFAULT && mode != JsonCreator.Mode.PROPERTIES)
                || factory.getParameterCount() == 0) {
            return false;
        }
        for (int i = 0; i < factory.getParameterCount(); ++i) {
            PropertyName name = intr.findNameForDeserialization(config, factory.getParameter(i));
            if (name == null || !name.hasSimpleName()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public JsonTypeInfo.Id getMechanism() { return JsonTypeInfo.Id.NAME; }

    @Override
    public String idFromValue(DatabindContext ctxt, Object value) {
        return idFromClass(ctxt, value.getClass());
    }

    protected String idFromClass(DatabindContext ctxt, Class<?> cls)
    {
        if (cls == null) {
            return null;
        }
        // 04-Nov-2024, tatu: [databind#4733] Need to resolve enum sub-classes
        //   same way "ClassNameIdResolver" does
        cls = _resolveToParentAsNecessary(cls);

        // 12-Oct-2019, tatu: This looked weird; was done in 2.x to force application
        //   of `TypeModifier`. But that just... does not seem right, at least not in
        //   the sense that raw class would be changed (intent for modifier is to change
        //   `JavaType` being resolved, not underlying class. Hence commented out in
        //   3.x. There should be better way to support whatever the use case is.

        // 29-Nov-2019, tatu: Looking at 2.x, test in `TestTypeModifierNameResolution` suggested
        //   that use of `TypeModifier` was used for demoting some types (from impl class to
        //   interface. For what that's worth. Still not supported for 3.x until proven necessary

//        cls = _typeFactory.constructType(cls).getRawClass();

        final String key = cls.getName();
        String name = _typeToId.get(key);

        if (name == null) {
            // 24-Feb-2011, tatu: As per [JACKSON-498], may need to dynamically look up name
            // can either throw an exception, or use default name...
            if (ctxt.isAnnotationProcessingEnabled()) {
                name = ctxt.getAnnotationIntrospector().findTypeName(ctxt.getConfig(),
                        ctxt.introspectClassAnnotations(cls));
            }
            if (name == null) {
                // And if still not found, let's choose default?
                name = _defaultTypeId(cls);
            }
            _typeToId.put(key, name);
        }
        return name;
    }

    @Override
    public String idFromValueAndType(DatabindContext ctxt, Object value, Class<?> type) {
        // 18-Jan-2013, tatu: We may be called with null value occasionally
        //   it seems; nothing much we can figure out that way.
        if (value == null) {
            return idFromClass(ctxt, type);
        }
        return idFromValue(ctxt, value);
    }

    @Override
    public JavaType typeFromId(DatabindContext context, String id) {
        return _typeFromId(id);
    }

    protected JavaType _typeFromId(String id) {
        // [databind#1983]: for case-insensitive lookups must canonicalize:
        if (_caseInsensitive) {
            id = id.toLowerCase();
        }
        // Now: if no type is found, should we try to locate it by
        // some other means? (specifically, if in same package as base type,
        // could just try Class.forName)
        // For now let's not add any such workarounds; can add if need be
        return _idToType.get(id);
    }

    @Override
    public String getDescForKnownTypeIds() {
        // 05-May-2020, tatu: As per [databind#1919], only include ids for
        //    non-abstract types
        final TreeSet<String> ids = new TreeSet<>();
        for (Map.Entry<String, JavaType> entry : _idToType.entrySet()) {
            if (entry.getValue().isConcrete()) {
                ids.add(entry.getKey());
            }
        }
        return ids.toString();
    }

    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */

    /**
     * If no name was explicitly given for a class, we will just
     * use non-qualified class name
     */
    protected static String _defaultTypeId(Class<?> cls)
    {
        String n = cls.getName();
        int ix = n.lastIndexOf('.');
        return (ix < 0) ? n : n.substring(ix+1);
    }
}
