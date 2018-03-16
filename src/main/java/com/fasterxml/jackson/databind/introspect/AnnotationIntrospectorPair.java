package com.fasterxml.jackson.databind.introspect;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.core.Version;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.util.ClassUtil;
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
    /**********************************************************************
    /* General class annotations
    /**********************************************************************
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
    public JsonIgnoreProperties.Value findPropertyIgnorals(Annotated a)
    {
        JsonIgnoreProperties.Value v2 = _secondary.findPropertyIgnorals(a);
        JsonIgnoreProperties.Value v1 = _primary.findPropertyIgnorals(a);
        return (v2 == null) // shouldn't occur but
            ? v1 : v2.withOverrides(v1);
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
    public Object findFilterId(Annotated ann)
    {
        Object id = _primary.findFilterId(ann);
        if (id == null) {
            id = _secondary.findFilterId(ann);
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

    @Override
    public String findClassDescription(AnnotatedClass ac) {
        String str = _primary.findClassDescription(ac);
        if ((str == null) || str.isEmpty()) {
            str = _secondary.findClassDescription(ac);
        }
        return str;
    }

    /*
    /**********************************************************************
    /* Property auto-detection
    /**********************************************************************
     */
    
    @Override
    public VisibilityChecker findAutoDetectVisibility(MapperConfig<?> config,
            AnnotatedClass ac, VisibilityChecker checker)
    {
        /* Note: to have proper priorities, we must actually call delegatees
         * in reverse order:
         */
        checker = _secondary.findAutoDetectVisibility(config, ac, checker);
        return _primary.findAutoDetectVisibility(config, ac, checker);
    }

    /*
    /**********************************************************************
    /* Type handling
    /**********************************************************************
     */

    @Override
    public JsonTypeInfo.Value findPolymorphicTypeInfo(MapperConfig<?> config,
            Annotated ann)
    {
        JsonTypeInfo.Value v = _primary.findPolymorphicTypeInfo(config, ann);
        if (v == null) {
            v = _secondary.findPolymorphicTypeInfo(config, ann);
        }
        return v;
    }

    @Override
    public Object findTypeResolverBuilder(MapperConfig<?> config,
            Annotated ann) {
        Object b = _primary.findTypeResolverBuilder(config, ann);
        if (b == null) {
            b = _secondary.findTypeResolverBuilder(config, ann);
        }
        return b;
    }

    @Override
    public Object findTypeIdResolver(MapperConfig<?> config, Annotated ann) {
        Object b = _primary.findTypeIdResolver(config, ann);
        if (b == null) {
            b = _secondary.findTypeIdResolver(config, ann);
        }
        return b;
    }

    /*
    @Override
    public TypeResolverBuilder<?> findPropertyTypeResolver(MapperConfig<?> config,
            Annotated ann, JavaType baseType, JsonTypeInfo.Value typeInfo)
    {
        TypeResolverBuilder<?> b = _primary.findPropertyTypeResolver(config, ann, baseType, typeInfo);
        if (b == null) {
            b = _secondary.findPropertyTypeResolver(config, ann, baseType, typeInfo);
        }
        return b;
    }

    @Override
    public TypeResolverBuilder<?> findPropertyContentTypeResolver(MapperConfig<?> config,
            Annotated ann, JavaType baseType, JsonTypeInfo.Value typeInfo)
    {
        TypeResolverBuilder<?> b = _primary.findPropertyContentTypeResolver(config, ann, baseType, typeInfo);
        if (b == null) {
            b = _secondary.findPropertyContentTypeResolver(config, ann, baseType, typeInfo);
        }
        return b;
    }
    */
    
    @Override
    public List<NamedType> findSubtypes(MapperConfig<?> config, Annotated a)
    {
        List<NamedType> types1 = _primary.findSubtypes(config, a);
        List<NamedType> types2 = _secondary.findSubtypes(config, a);
        if (types1 == null || types1.isEmpty()) return types2;
        if (types2 == null || types2.isEmpty()) return types1;
        ArrayList<NamedType> result = new ArrayList<NamedType>(types1.size() + types2.size());
        result.addAll(types1);
        result.addAll(types2);
        return result;
    }

    @Override
    public String findTypeName(MapperConfig<?> config, AnnotatedClass ac)
    {
        String name = _primary.findTypeName(config, ac);
        if (name == null || name.length() == 0) {
            name = _secondary.findTypeName(config, ac);                
        }
        return name;
    }

    /*
    /**********************************************************************
    /* General member (field, method/constructor) annotations
    /**********************************************************************
     */
    
    @Override        
    public ReferenceProperty findReferenceType(AnnotatedMember member) {
        ReferenceProperty r = _primary.findReferenceType(member);
        return (r == null) ? _secondary.findReferenceType(member) : r;
    }

    @Override        
    public NameTransformer findUnwrappingNameTransformer(AnnotatedMember member) {
        NameTransformer r = _primary.findUnwrappingNameTransformer(member);
        return (r == null) ? _secondary.findUnwrappingNameTransformer(member) : r;
    }

    @Override
    public JacksonInject.Value findInjectableValue(AnnotatedMember m) {
        JacksonInject.Value r = _primary.findInjectableValue(m);
        return (r == null) ? _secondary.findInjectableValue(m) : r;
    }

    @Override
    public boolean hasIgnoreMarker(AnnotatedMember m) {
        return _primary.hasIgnoreMarker(m) || _secondary.hasIgnoreMarker(m);
    }

    @Override
    public Boolean hasRequiredMarker(AnnotatedMember m) {
        Boolean r = _primary.hasRequiredMarker(m);
        return (r == null) ? _secondary.hasRequiredMarker(m) : r;
    }

    // // // Serialization: general annotations

    @Override
    public Object findSerializer(MapperConfig<?> config, Annotated am) {
        Object r = _primary.findSerializer(config, am);
        if (_isExplicitClassOrOb(r, JsonSerializer.None.class)) {
            return r;
        }
        return _explicitClassOrOb(_secondary.findSerializer(config, am),
                JsonSerializer.None.class);
    }
    
    @Override
    public Object findKeySerializer(MapperConfig<?> config, Annotated a) {
        Object r = _primary.findKeySerializer(config, a);
        if (_isExplicitClassOrOb(r, JsonSerializer.None.class)) {
            return r;
        }
        return _explicitClassOrOb(_secondary.findKeySerializer(config, a),
                JsonSerializer.None.class);
    }

    @Override
    public Object findContentSerializer(MapperConfig<?> config, Annotated a) {
        Object r = _primary.findContentSerializer(config, a);
        if (_isExplicitClassOrOb(r, JsonSerializer.None.class)) {
            return r;
        }
        return _explicitClassOrOb(_secondary.findContentSerializer(config, a),
                JsonSerializer.None.class);
    }
    
    @Override
    public Object findNullSerializer(MapperConfig<?> config, Annotated a) {
        Object r = _primary.findNullSerializer(config, a);
        if (_isExplicitClassOrOb(r, JsonSerializer.None.class)) {
            return r;
        }
        return _explicitClassOrOb(_secondary.findNullSerializer(config, a),
                JsonSerializer.None.class);
    }

    @Override
    public JsonInclude.Value findPropertyInclusion(MapperConfig<?> config, Annotated a)
    {
        JsonInclude.Value v2 = _secondary.findPropertyInclusion(config, a);
        JsonInclude.Value v1 = _primary.findPropertyInclusion(config, a);

        if (v2 == null) { // shouldn't occur but
            return v1;
        }
        return v2.withOverrides(v1);
    }

    @Override
    public JsonSerialize.Typing findSerializationTyping(MapperConfig<?> config, Annotated a) {
        JsonSerialize.Typing r = _primary.findSerializationTyping(config, a);
        return (r == null) ? _secondary.findSerializationTyping(config, a) : r;
    }

    @Override
    public Object findSerializationConverter(MapperConfig<?> config, Annotated a) {
        Object r = _primary.findSerializationConverter(config, a);
        return (r == null) ? _secondary.findSerializationConverter(config, a) : r;
    }

    @Override
    public Object findSerializationContentConverter(MapperConfig<?> config, AnnotatedMember a) {
        Object r = _primary.findSerializationContentConverter(config, a);
        return (r == null) ? _secondary.findSerializationContentConverter(config, a) : r;
    }

    @Override
    public Class<?>[] findViews(Annotated a) {
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
    public Boolean isTypeId(MapperConfig<?> config, AnnotatedMember member) {
        Boolean b = _primary.isTypeId(config, member);
        return (b == null) ? _secondary.isTypeId(config, member) : b;
    }

    @Override
    public ObjectIdInfo findObjectIdInfo(MapperConfig<?> config, Annotated ann) {
        ObjectIdInfo r = _primary.findObjectIdInfo(config, ann);
        return (r == null) ? _secondary.findObjectIdInfo(config, ann) : r;
    }

    @Override
    public ObjectIdInfo findObjectReferenceInfo(MapperConfig<?> config, 
            Annotated ann, ObjectIdInfo objectIdInfo) {
        // to give precedence for primary, must start with secondary:
        objectIdInfo = _secondary.findObjectReferenceInfo(config, ann, objectIdInfo);
        objectIdInfo = _primary.findObjectReferenceInfo(config, ann, objectIdInfo);
        return objectIdInfo;
    }

    @Override
    public JsonFormat.Value findFormat(Annotated ann) {
        JsonFormat.Value v1 = _primary.findFormat(ann);
        JsonFormat.Value v2 = _secondary.findFormat(ann);
        if (v2 == null) { // shouldn't occur but just in case
            return v1;
        }
        return v2.withOverrides(v1);
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

    @Override
    public String findPropertyDefaultValue(Annotated ann) {
        String str = _primary.findPropertyDefaultValue(ann);
        return (str == null || str.isEmpty()) ? _secondary.findPropertyDefaultValue(ann) : str;
    }

    @Override
    public String findPropertyDescription(Annotated ann) {
        String r = _primary.findPropertyDescription(ann);
        return (r == null) ? _secondary.findPropertyDescription(ann) : r;
    }

    @Override
    public Integer findPropertyIndex(Annotated ann) {
        Integer r = _primary.findPropertyIndex(ann);
        return (r == null) ? _secondary.findPropertyIndex(ann) : r;
    }

    @Override
    public String findImplicitPropertyName(AnnotatedMember ann) {
        String r = _primary.findImplicitPropertyName(ann);
        return (r == null) ? _secondary.findImplicitPropertyName(ann) : r;
    }

    @Override
    public List<PropertyName> findPropertyAliases(Annotated ann) {
        List<PropertyName> r = _primary.findPropertyAliases(ann);
        return (r == null) ? _secondary.findPropertyAliases(ann) : r;
    }

    @Override
    public JsonProperty.Access findPropertyAccess(Annotated ann) {
        JsonProperty.Access acc = _primary.findPropertyAccess(ann);
        if ((acc != null) && (acc != JsonProperty.Access.AUTO)) {
            return acc;
        }
        acc = _secondary.findPropertyAccess(ann);
        if (acc != null) {
            return acc;
        }
        return JsonProperty.Access.AUTO;
    }

    @Override
    public AnnotatedMethod resolveSetterConflict(MapperConfig<?> config,
            AnnotatedMethod setter1, AnnotatedMethod setter2)
    {
        AnnotatedMethod res = _primary.resolveSetterConflict(config, setter1, setter2);
        if (res == null) {
            res = _secondary.resolveSetterConflict(config, setter1, setter2);
        }
        return res;
    }

    // // // Serialization: type refinements

    @Override
    public JavaType refineSerializationType(MapperConfig<?> config,
            Annotated a, JavaType baseType) throws JsonMappingException
    {
        JavaType t = _secondary.refineSerializationType(config, a, baseType);
        return _primary.refineSerializationType(config, a, t);
    }

    // // // Serialization: class annotations

    @Override
    public String[] findSerializationPropertyOrder(AnnotatedClass ac) {
        String[] r = _primary.findSerializationPropertyOrder(ac);
        return (r == null) ? _secondary.findSerializationPropertyOrder(ac) : r;
    }

    @Override
    public Boolean findSerializationSortAlphabetically(Annotated ann) {
        Boolean r = _primary.findSerializationSortAlphabetically(ann);
        return (r == null) ? _secondary.findSerializationSortAlphabetically(ann) : r;
    }

    @Override
    public void findAndAddVirtualProperties(MapperConfig<?> config, AnnotatedClass ac,
            List<BeanPropertyWriter> properties) {
        // first secondary, then primary, to give proper precedence
        _primary.findAndAddVirtualProperties(config, ac, properties);
        _secondary.findAndAddVirtualProperties(config, ac, properties);
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
    public Boolean hasAsValue(Annotated a) {
        Boolean b = _primary.hasAsValue(a);
        if (b == null) {
            b = _secondary.hasAsValue(a);
        }
        return b;
    }

    @Override
    public Boolean hasAnyGetter(Annotated a) {
        Boolean b = _primary.hasAnyGetter(a);
        if (b == null) {
            b = _secondary.hasAnyGetter(a);
        }
        return b;
    }

    @Override
    public  String[] findEnumValues(Class<?> enumType, Enum<?>[] enumValues, String[] names) {
        // reverse order to give _primary higher precedence
        names = _secondary.findEnumValues(enumType, enumValues, names);
        names = _primary.findEnumValues(enumType, enumValues, names);
        return names;
    }

    @Override
    public Enum<?> findDefaultEnumValue(Class<Enum<?>> enumCls) {
        Enum<?> en = _primary.findDefaultEnumValue(enumCls);
        return (en == null) ? _secondary.findDefaultEnumValue(enumCls) : en;
    }

    // // // Deserialization: general annotations

    @Override
    public Object findDeserializer(MapperConfig<?> config, Annotated a) {
        Object r = _primary.findDeserializer(config, a);
        if (_isExplicitClassOrOb(r, JsonDeserializer.None.class)) {
            return r;
        }
        return _explicitClassOrOb(_secondary.findDeserializer(config, a),
                JsonDeserializer.None.class);
    }

    @Override
    public Object findKeyDeserializer(MapperConfig<?> config, Annotated a) {
        Object r = _primary.findKeyDeserializer(config, a);
        if (_isExplicitClassOrOb(r, KeyDeserializer.None.class)) {
            return r;
        }
        return _explicitClassOrOb(_secondary.findKeyDeserializer(config, a),
                KeyDeserializer.None.class);
    }

    @Override
    public Object findContentDeserializer(MapperConfig<?> config, Annotated am) {
        Object r = _primary.findContentDeserializer(config, am);
        if (_isExplicitClassOrOb(r, JsonDeserializer.None.class)) {
            return r;
        }
        return _explicitClassOrOb(_secondary.findContentDeserializer(config, am),
                JsonDeserializer.None.class);
                
    }

    @Override
    public Object findDeserializationConverter(MapperConfig<?> config, Annotated a) {
        Object ob = _primary.findDeserializationConverter(config, a);
        return (ob == null) ? _secondary.findDeserializationConverter(config, a) : ob;
    }

    @Override
    public Object findDeserializationContentConverter(MapperConfig<?> config, AnnotatedMember a) {
        Object ob = _primary.findDeserializationContentConverter(config, a);
        return (ob == null) ? _secondary.findDeserializationContentConverter(config, a) : ob;
    }

    // // // Deserialization: type refinements

    @Override
    public JavaType refineDeserializationType(MapperConfig<?> config,
            Annotated a, JavaType baseType) throws JsonMappingException
    {
        JavaType t = _secondary.refineDeserializationType(config, a, baseType);
        return _primary.refineDeserializationType(config, a, t);
    }

    // // // Deserialization: class annotations

    @Override
    public Object findValueInstantiator(MapperConfig<?> config, AnnotatedClass ac) {
        Object result = _primary.findValueInstantiator(config, ac);
        return (result == null) ? _secondary.findValueInstantiator(config, ac) : result;
    }

    @Override
    public Class<?> findPOJOBuilder(MapperConfig<?> config, AnnotatedClass ac) {
        Class<?> result = _primary.findPOJOBuilder(config, ac);
        return (result == null) ? _secondary.findPOJOBuilder(config, ac) : result;
    }

    @Override
    public JsonPOJOBuilder.Value findPOJOBuilderConfig(MapperConfig<?> config, AnnotatedClass ac) {
        JsonPOJOBuilder.Value result = _primary.findPOJOBuilderConfig(config, ac);
        return (result == null) ? _secondary.findPOJOBuilderConfig(config, ac) : result;
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
    public Boolean hasAnySetter(Annotated a) {
        Boolean b = _primary.hasAnySetter(a);
        if (b == null) {
            b = _secondary.hasAnySetter(a);
        }
        return b;
    }

    @Override
    public JsonSetter.Value findSetterInfo(Annotated a) {
        JsonSetter.Value v2 = _secondary.findSetterInfo(a);
        JsonSetter.Value v1 = _primary.findSetterInfo(a);
        return (v2 == null) // shouldn't occur but
            ? v1 : v2.withOverrides(v1);
    }

    @Override
    public Boolean findMergeInfo(Annotated a) {
        Boolean b = _primary.findMergeInfo(a);
        if (b == null) {
            b = _secondary.findMergeInfo(a);
        }
        return b;
    }

    @Override
    public JsonCreator.Mode findCreatorAnnotation(MapperConfig<?> config, Annotated a) {
        JsonCreator.Mode mode = _primary.findCreatorAnnotation(config, a);
        return (mode == null) ? _secondary.findCreatorAnnotation(config, a) : mode;
    }

    protected boolean _isExplicitClassOrOb(Object maybeCls, Class<?> implicit) {
        if ((maybeCls == null) || (maybeCls == implicit)) {
            return false;
        }
        if (maybeCls instanceof Class<?>) {
            return !ClassUtil.isBogusClass((Class<?>) maybeCls);
        }
        return true;
    }

    protected Object _explicitClassOrOb(Object maybeCls, Class<?> implicit) {
        if ((maybeCls == null) || (maybeCls == implicit)) {
            return null;
        }
        if ((maybeCls instanceof Class<?>) && ClassUtil.isBogusClass((Class<?>) maybeCls)) {
            return null;
        }
        return maybeCls;
    }
}
