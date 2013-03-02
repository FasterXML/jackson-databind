package com.fasterxml.jackson.databind.introspect;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.annotation.NoClass;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.databind.util.NameTransformer;

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
 *<p>
 * Note: up until 2.0, this class was an inner class of
 * {@link AnnotationIntrospector}; moved here for convenience.
 * 
 * @since 2.1
 */
public class AnnotationIntrospectorPair
    extends AnnotationIntrospector
    implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    protected final AnnotationIntrospector _primary, _secondary;

    public AnnotationIntrospectorPair(AnnotationIntrospector p, AnnotationIntrospector s)
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
        return new AnnotationIntrospectorPair(primary, secondary);
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
    public boolean isAnnotationBundle(Annotation ann) {
        return _primary.isAnnotationBundle(ann) || _secondary.isAnnotationBundle(ann);
    }
    
    /*
    /******************************************************
    /* General class annotations
    /******************************************************
     */

    @Override
    public PropertyName findRootName(AnnotatedClass ac)
    {
        PropertyName name1 = _primary.findRootName(ac);
        if (name1 == null) {
            return _secondary.findRootName(ac);
        }
        if (name1.hasSimpleName()) {
            return name1;
        }
        // name1 is empty; how about secondary?
        PropertyName name2 = _secondary.findRootName(ac);
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

    @Override
    public Object findNamingStrategy(AnnotatedClass ac)
    {
        Object str = _primary.findNamingStrategy(ac);
        if (str == null) {
            str = _secondary.findNamingStrategy(ac);
        }
        return str;
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
    public Object findSerializationConverter(Annotated a)
    {
        Object ob = _primary.findSerializationConverter(a);
        if (ob == null) {
            ob = _secondary.findSerializationConverter(a);
        }
        return ob;
    }

    @Override
    public Object findSerializationContentConverter(AnnotatedMember a)
    {
        Object ob = _primary.findSerializationContentConverter(a);
        if (ob == null) {
            ob = _secondary.findSerializationContentConverter(a);
        }
        return ob;
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

    @Override
    public ObjectIdInfo findObjectReferenceInfo(Annotated ann, ObjectIdInfo objectIdInfo) {
        // to give precedence for primary, must start with secondary:
        objectIdInfo = _secondary.findObjectReferenceInfo(ann, objectIdInfo);
        objectIdInfo = _primary.findObjectReferenceInfo(ann, objectIdInfo);
        return objectIdInfo;
    }
    
    @Override
    public JsonFormat.Value findFormat(Annotated ann) {
        JsonFormat.Value result = _primary.findFormat(ann);
        if (result == null) {
            result = _secondary.findFormat(ann);
        }
        return result;
    }

    @Override
    public PropertyName findWrapperName(Annotated ann) {
        PropertyName name = _primary.findWrapperName(ann);
        if (name == null) {
            name = _secondary.findWrapperName(ann);
        } else if (name == PropertyName.USE_DEFAULT) {
            // does the other introspector have a better idea?
            PropertyName name2 = _secondary.findWrapperName(ann);
            if (name2 != null) {
                name = name2;
            }
        }
        return name;
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

    // // // Serialization: property annotations
    
    @Override
    public PropertyName findNameForSerialization(Annotated a) {
        PropertyName n = _primary.findNameForSerialization(a);
        // note: "use default" should not block explicit answer, so:
        if (n == null) {
            n = _secondary.findNameForSerialization(a);
        } else if (n == PropertyName.USE_DEFAULT) {
            PropertyName n2 = _secondary.findNameForSerialization(a);
            if (n2 != null) {
                n = n2;
            }
        }
        return n;
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
    public Class<?> findDeserializationContentType(Annotated am, JavaType baseContentType) {
        Class<?> result = _primary.findDeserializationContentType(am, baseContentType);
        if (result == null) {
            result = _secondary.findDeserializationContentType(am, baseContentType);
        }
        return result;
    }

    @Override
    public Object findDeserializationConverter(Annotated a) {
        Object ob = _primary.findDeserializationConverter(a);
        if (ob == null) {
            ob = _secondary.findDeserializationConverter(a);
        }
        return ob;
    }

    @Override
    public Object findDeserializationContentConverter(AnnotatedMember a) {
        Object ob = _primary.findDeserializationContentConverter(a);
        if (ob == null) {
            ob = _secondary.findDeserializationContentConverter(a);
        }
        return ob;
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
    public PropertyName findNameForDeserialization(Annotated a)
    {
        // note: "use default" should not block explicit answer, so:
        PropertyName n = _primary.findNameForDeserialization(a);
        if (n == null) {
            n = _secondary.findNameForDeserialization(a);
        } else if (n == PropertyName.USE_DEFAULT) {
            PropertyName n2 = _secondary.findNameForDeserialization(a);
            if (n2 != null) {
                n = n2;
            }
        }
        return n;
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
 
    /*
    /******************************************************
    /* Deprecated methods
    /******************************************************
     */
    
    @Deprecated
    @Override
    public boolean isHandled(Annotation ann) {
        return _primary.isHandled(ann) || _secondary.isHandled(ann);
    }

    // // // Deserialization: property annotations

    @Deprecated
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
    
    @Deprecated
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

    @Deprecated
    @Override
    public String findDeserializationName(AnnotatedParameter param)
    {
        String result = _primary.findDeserializationName(param);
        if (result == null) {
            result = _secondary.findDeserializationName(param);
        }
        return result;
    }

    // // // Serialization: property annotations
    
    @Deprecated
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

    @Deprecated
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

}
