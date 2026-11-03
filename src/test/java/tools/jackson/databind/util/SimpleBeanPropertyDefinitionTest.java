package tools.jackson.databind.util;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;

import tools.jackson.databind.*;
import tools.jackson.databind.introspect.*;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.type.TypeFactory;

import static org.junit.jupiter.api.Assertions.*;

public class SimpleBeanPropertyDefinitionTest extends DatabindTestUtil
{
    static class SimpleBean {
        public String name;

        public SimpleBean(String name) {
            this.name = name;
        }

        public String getName() { return name; }

        public void setName(String name) { this.name = name; }
    }

    static class AliasedBean {
        @JsonAlias({ "nm", "fullName" })
        public String name;
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */

    private AnnotatedClass _resolveAnnotatedClass(Class<?> cls) {
        DeserializationConfig config = MAPPER.deserializationConfig();
        JavaType type = MAPPER.constructType(cls);
        return AnnotatedClassResolver.resolve(config, type, config);
    }

    private AnnotatedField _fieldOf(Class<?> cls) {
        return _resolveAnnotatedClass(cls).fields().iterator().next();
    }

    private AnnotatedMethod _getterOf(Class<?> cls, String name) {
        return _resolveAnnotatedClass(cls).findMethod(name, new Class<?>[0]);
    }

    private AnnotatedMethod _setterOf(Class<?> cls, String name, Class<?>... paramTypes) {
        return _resolveAnnotatedClass(cls).findMethod(name, paramTypes);
    }

    private AnnotatedParameter _constructorParamOf(Class<?> cls, int index) {
        return _resolveAnnotatedClass(cls).getConstructors().get(0).getParameter(index);
    }

    private DeserializationConfig _config() {
        return MAPPER.deserializationConfig();
    }

    /*
    /**********************************************************************
    /* Test methods: construction / factory methods
    /**********************************************************************
     */

    @Test
    public void testConstructWithMember() {
        AnnotatedField field = _fieldOf(SimpleBean.class);
        SimpleBeanPropertyDefinition prop = SimpleBeanPropertyDefinition.construct(
                _config(), field);
        assertEquals("name", prop.getName());
        assertSame(field, prop.getPrimaryMember());
    }

    @Test
    public void testConstructWithName() {
        AnnotatedField field = _fieldOf(SimpleBean.class);
        PropertyName pn = PropertyName.construct("foo");
        SimpleBeanPropertyDefinition prop = SimpleBeanPropertyDefinition.construct(
                _config(), field, pn);
        assertEquals("foo", prop.getName());
    }

    @Test
    public void testConstructWithIncludeNull() {
        AnnotatedField field = _fieldOf(SimpleBean.class);
        SimpleBeanPropertyDefinition prop = SimpleBeanPropertyDefinition.construct(
                _config(), field, PropertyName.construct("x"), null,
                (JsonInclude.Include) null);
        assertNotNull(prop.findInclusion());
    }

    @Test
    public void testConstructWithIncludeUseDefaults() {
        AnnotatedField field = _fieldOf(SimpleBean.class);
        SimpleBeanPropertyDefinition prop = SimpleBeanPropertyDefinition.construct(
                _config(), field, PropertyName.construct("x"), null,
                JsonInclude.Include.USE_DEFAULTS);
        assertNotNull(prop.findInclusion());
    }

    @Test
    public void testConstructWithIncludeNonNull() {
        AnnotatedField field = _fieldOf(SimpleBean.class);
        SimpleBeanPropertyDefinition prop = SimpleBeanPropertyDefinition.construct(
                _config(), field, PropertyName.construct("x"), null,
                JsonInclude.Include.NON_NULL);
        assertEquals(JsonInclude.Include.NON_NULL,
                prop.findInclusion().getValueInclusion());
    }

    @Test
    public void testConstructWithNullMetadata() {
        AnnotatedField field = _fieldOf(SimpleBean.class);
        SimpleBeanPropertyDefinition prop = SimpleBeanPropertyDefinition.construct(
                _config(), field, PropertyName.construct("x"), null,
                (JsonInclude.Value) null);
        // null metadata should default to STD_OPTIONAL
        assertEquals(PropertyMetadata.STD_OPTIONAL, prop.getMetadata());
    }

    @Test
    public void testConstructWithExplicitMetadata() {
        AnnotatedField field = _fieldOf(SimpleBean.class);
        PropertyMetadata md = PropertyMetadata.STD_REQUIRED;
        SimpleBeanPropertyDefinition prop = SimpleBeanPropertyDefinition.construct(
                _config(), field, PropertyName.construct("x"), md,
                (JsonInclude.Value) null);
        assertEquals(PropertyMetadata.STD_REQUIRED, prop.getMetadata());
    }

    /*
    /**********************************************************************
    /* Test methods: fluent factories
    /**********************************************************************
     */

    @Test
    public void testWithSimpleNameSameName() {
        AnnotatedField field = _fieldOf(SimpleBean.class);
        SimpleBeanPropertyDefinition prop = SimpleBeanPropertyDefinition.construct(
                _config(), field, PropertyName.construct("foo"));
        BeanPropertyDefinition same = prop.withSimpleName("foo");
        assertSame(prop, same);
    }

    @Test
    public void testWithSimpleNameDifferentName() {
        AnnotatedField field = _fieldOf(SimpleBean.class);
        SimpleBeanPropertyDefinition prop = SimpleBeanPropertyDefinition.construct(
                _config(), field, PropertyName.construct("foo"));
        BeanPropertyDefinition diff = prop.withSimpleName("bar");
        assertNotSame(prop, diff);
        assertEquals("bar", diff.getName());
    }

    @Test
    public void testWithSimpleNameHasNamespace() {
        AnnotatedField field = _fieldOf(SimpleBean.class);
        // PropertyName with a namespace
        PropertyName nameWithNs = PropertyName.construct("foo", "http://ns");
        SimpleBeanPropertyDefinition prop = SimpleBeanPropertyDefinition.construct(
                _config(), field, nameWithNs);
        // Same simple name but has namespace, so should create new instance
        BeanPropertyDefinition diff = prop.withSimpleName("foo");
        assertNotSame(prop, diff);
        assertEquals("foo", diff.getName());
    }

    @Test
    public void testWithNameSameName() {
        AnnotatedField field = _fieldOf(SimpleBean.class);
        PropertyName pn = PropertyName.construct("foo");
        SimpleBeanPropertyDefinition prop = SimpleBeanPropertyDefinition.construct(
                _config(), field, pn);
        BeanPropertyDefinition same = prop.withName(pn);
        assertSame(prop, same);
    }

    @Test
    public void testWithNameDifferentName() {
        AnnotatedField field = _fieldOf(SimpleBean.class);
        SimpleBeanPropertyDefinition prop = SimpleBeanPropertyDefinition.construct(
                _config(), field, PropertyName.construct("foo"));
        PropertyName newName = PropertyName.construct("bar");
        BeanPropertyDefinition diff = prop.withName(newName);
        assertNotSame(prop, diff);
        assertEquals("bar", diff.getName());
    }

    @Test
    public void testWithMetadataSame() {
        AnnotatedField field = _fieldOf(SimpleBean.class);
        PropertyMetadata md = PropertyMetadata.STD_REQUIRED;
        SimpleBeanPropertyDefinition prop = SimpleBeanPropertyDefinition.construct(
                _config(), field, PropertyName.construct("x"), md,
                (JsonInclude.Value) null);
        BeanPropertyDefinition same = prop.withMetadata(PropertyMetadata.STD_REQUIRED);
        assertSame(prop, same);
    }

    @Test
    public void testWithMetadataDifferent() {
        AnnotatedField field = _fieldOf(SimpleBean.class);
        SimpleBeanPropertyDefinition prop = SimpleBeanPropertyDefinition.construct(
                _config(), field, PropertyName.construct("x"), PropertyMetadata.STD_REQUIRED,
                (JsonInclude.Value) null);
        BeanPropertyDefinition diff = prop.withMetadata(PropertyMetadata.STD_OPTIONAL);
        assertNotSame(prop, diff);
    }

    @Test
    public void testWithInclusionSame() {
        AnnotatedField field = _fieldOf(SimpleBean.class);
        JsonInclude.Value incl = JsonInclude.Value.construct(JsonInclude.Include.NON_NULL, null);
        SimpleBeanPropertyDefinition prop = SimpleBeanPropertyDefinition.construct(
                _config(), field, PropertyName.construct("x"), null, incl);
        BeanPropertyDefinition same = prop.withInclusion(incl);
        assertSame(prop, same);
    }

    @Test
    public void testWithInclusionDifferent() {
        AnnotatedField field = _fieldOf(SimpleBean.class);
        JsonInclude.Value incl1 = JsonInclude.Value.construct(JsonInclude.Include.NON_NULL, null);
        JsonInclude.Value incl2 = JsonInclude.Value.construct(JsonInclude.Include.NON_EMPTY, null);
        SimpleBeanPropertyDefinition prop = SimpleBeanPropertyDefinition.construct(
                _config(), field, PropertyName.construct("x"), null, incl1);
        BeanPropertyDefinition diff = prop.withInclusion(incl2);
        assertNotSame(prop, diff);
    }

    /*
    /**********************************************************************
    /* Test methods: basic property info
    /**********************************************************************
     */

    @Test
    public void testGetName() {
        AnnotatedField field = _fieldOf(SimpleBean.class);
        SimpleBeanPropertyDefinition prop = SimpleBeanPropertyDefinition.construct(
                _config(), field, PropertyName.construct("myProp"));
        assertEquals("myProp", prop.getName());
        assertEquals("myProp", prop.getInternalName());
    }

    @Test
    public void testGetFullName() {
        PropertyName pn = PropertyName.construct("myProp");
        AnnotatedField field = _fieldOf(SimpleBean.class);
        SimpleBeanPropertyDefinition prop = SimpleBeanPropertyDefinition.construct(
                _config(), field, pn);
        assertEquals(pn, prop.getFullName());
    }

    @Test
    public void testHasName() {
        AnnotatedField field = _fieldOf(SimpleBean.class);
        PropertyName pn = PropertyName.construct("myProp");
        SimpleBeanPropertyDefinition prop = SimpleBeanPropertyDefinition.construct(
                _config(), field, pn);
        assertTrue(prop.hasName(PropertyName.construct("myProp")));
        assertFalse(prop.hasName(PropertyName.construct("other")));
    }

    @Test
    public void testIsExplicitlyIncludedAndNamed() {
        AnnotatedField field = _fieldOf(SimpleBean.class);
        SimpleBeanPropertyDefinition prop = SimpleBeanPropertyDefinition.construct(
                _config(), field);
        assertFalse(prop.isExplicitlyIncluded());
        assertFalse(prop.isExplicitlyNamed());
    }

    @Test
    public void testGetWrapperNameWithMember() {
        AnnotatedField field = _fieldOf(SimpleBean.class);
        SimpleBeanPropertyDefinition prop = SimpleBeanPropertyDefinition.construct(
                _config(), field);
        // No @JsonProperty(wrapperName) on the field, should return null
        PropertyName wn = prop.getWrapperName();
        assertNull(wn);
    }

    @Test
    public void testGetPrimaryTypeWithMember() {
        AnnotatedField field = _fieldOf(SimpleBean.class);
        SimpleBeanPropertyDefinition prop = SimpleBeanPropertyDefinition.construct(
                _config(), field);
        JavaType type = prop.getPrimaryType();
        assertEquals(String.class, type.getRawClass());
    }

    @Test
    public void testGetPrimaryTypeWithNullMember() {
        SimpleBeanPropertyDefinition prop = SimpleBeanPropertyDefinition.construct(
                _config(), null, PropertyName.construct("virtual"), null,
                (JsonInclude.Value) null);
        JavaType type = prop.getPrimaryType();
        // Null member should return unknownType
        assertEquals(TypeFactory.unknownType(), type);
    }

    @Test
    public void testGetRawPrimaryTypeWithMember() {
        AnnotatedField field = _fieldOf(SimpleBean.class);
        SimpleBeanPropertyDefinition prop = SimpleBeanPropertyDefinition.construct(
                _config(), field);
        assertEquals(String.class, prop.getRawPrimaryType());
    }

    @Test
    public void testGetRawPrimaryTypeWithNullMember() {
        SimpleBeanPropertyDefinition prop = SimpleBeanPropertyDefinition.construct(
                _config(), null, PropertyName.construct("virtual"), null,
                (JsonInclude.Value) null);
        assertEquals(Object.class, prop.getRawPrimaryType());
    }

    @Test
    public void testFindAliasesNoMember() {
        SimpleBeanPropertyDefinition prop = SimpleBeanPropertyDefinition.construct(
                _config(), null, PropertyName.construct("virtual"), null,
                (JsonInclude.Value) null);
        List<PropertyName> aliases = prop.findAliases();
        assertEquals(Collections.emptyList(), aliases);
    }

    @Test
    public void testFindAliasesWithMember() {
        AnnotatedField field = _fieldOf(SimpleBean.class);
        SimpleBeanPropertyDefinition prop = SimpleBeanPropertyDefinition.construct(
                _config(), field);
        // No aliases defined on the field
        List<PropertyName> aliases = prop.findAliases();
        assertEquals(Collections.emptyList(), aliases);
    }

    /*
    /**********************************************************************
    /* Test methods: accessor detection with field member
    /**********************************************************************
     */

    @Test
    public void testAccessorsWithFieldMember() {
        AnnotatedField field = _fieldOf(SimpleBean.class);
        SimpleBeanPropertyDefinition prop = SimpleBeanPropertyDefinition.construct(
                _config(), field);

        // Field-based: hasField true, hasGetter/hasSetter/hasConstructorParameter false
        assertTrue(prop.hasField());
        assertFalse(prop.hasGetter());
        assertFalse(prop.hasSetter());
        assertFalse(prop.hasConstructorParameter());

        assertSame(field, prop.getField());
        assertNull(prop.getGetter());
        assertNull(prop.getSetter());
        assertNull(prop.getConstructorParameter());

        // getConstructorParameters should return empty iterator
        Iterator<AnnotatedParameter> it = prop.getConstructorParameters();
        assertFalse(it.hasNext());
    }

    /*
    /**********************************************************************
    /* Test methods: accessor detection with getter member
    /**********************************************************************
     */

    @Test
    public void testAccessorsWithGetterMember() {
        AnnotatedMethod getter = _getterOf(SimpleBean.class, "getName");
        assertNotNull(getter);
        SimpleBeanPropertyDefinition prop = SimpleBeanPropertyDefinition.construct(
                _config(), getter);

        // Getter-based: hasGetter true, rest false
        assertTrue(prop.hasGetter());
        assertFalse(prop.hasSetter());
        assertFalse(prop.hasField());
        assertFalse(prop.hasConstructorParameter());

        assertSame(getter, prop.getGetter());
        assertNull(prop.getSetter());
        assertNull(prop.getField());
        assertNull(prop.getConstructorParameter());
    }

    /*
    /**********************************************************************
    /* Test methods: accessor detection with setter member
    /**********************************************************************
     */

    @Test
    public void testAccessorsWithSetterMember() {
        AnnotatedMethod setter = _setterOf(SimpleBean.class, "setName", String.class);
        assertNotNull(setter);
        SimpleBeanPropertyDefinition prop = SimpleBeanPropertyDefinition.construct(
                _config(), setter);

        // Setter-based: hasSetter true, hasGetter false (1 param = not a getter)
        assertTrue(prop.hasSetter());
        assertFalse(prop.hasGetter());
        assertFalse(prop.hasField());
        assertFalse(prop.hasConstructorParameter());

        assertSame(setter, prop.getSetter());
        assertNull(prop.getGetter());
        assertNull(prop.getField());
        assertNull(prop.getConstructorParameter());
    }

    /*
    /**********************************************************************
    /* Test methods: accessor detection with constructor param member
    /**********************************************************************
     */

    @Test
    public void testAccessorsWithConstructorParameter() {
        AnnotatedParameter param = _constructorParamOf(SimpleBean.class, 0);
        assertNotNull(param);
        SimpleBeanPropertyDefinition prop = SimpleBeanPropertyDefinition.construct(
                _config(), param);

        assertTrue(prop.hasConstructorParameter());
        assertFalse(prop.hasGetter());
        assertFalse(prop.hasSetter());
        assertFalse(prop.hasField());

        assertSame(param, prop.getConstructorParameter());
        assertNull(prop.getGetter());
        assertNull(prop.getSetter());
        assertNull(prop.getField());

        // getConstructorParameters should return single-element iterator
        Iterator<AnnotatedParameter> it = prop.getConstructorParameters();
        assertTrue(it.hasNext());
        assertSame(param, it.next());
        assertFalse(it.hasNext());
    }

    /*
    /**********************************************************************
    /* Test methods: accessor detection with null member (virtual prop)
    /**********************************************************************
     */

    @Test
    public void testAccessorsWithNullMember() {
        SimpleBeanPropertyDefinition prop = SimpleBeanPropertyDefinition.construct(
                _config(), null, PropertyName.construct("virtual"), null,
                (JsonInclude.Value) null);

        assertFalse(prop.hasGetter());
        assertFalse(prop.hasSetter());
        assertFalse(prop.hasField());
        assertFalse(prop.hasConstructorParameter());

        assertNull(prop.getGetter());
        assertNull(prop.getSetter());
        assertNull(prop.getField());
        assertNull(prop.getConstructorParameter());
        assertNull(prop.getPrimaryMember());
    }

    @Test
    public void testFindInclusion() {
        AnnotatedField field = _fieldOf(SimpleBean.class);
        JsonInclude.Value incl = JsonInclude.Value.construct(JsonInclude.Include.NON_NULL, null);
        SimpleBeanPropertyDefinition prop = SimpleBeanPropertyDefinition.construct(
                _config(), field, PropertyName.construct("x"), null, incl);
        assertSame(incl, prop.findInclusion());
    }

    @Test
    public void testGetWrapperNameNullMember() {
        SimpleBeanPropertyDefinition prop = SimpleBeanPropertyDefinition.construct(
                _config(), null, PropertyName.construct("virtual"), null,
                (JsonInclude.Value) null);
        assertNull(prop.getWrapperName());
    }

    @Test
    public void testFindAliasesNullMember() {
        SimpleBeanPropertyDefinition prop = SimpleBeanPropertyDefinition.construct(
                _config(), null, PropertyName.construct("virtual"), null,
                (JsonInclude.Value) null);
        assertTrue(prop.findAliases().isEmpty());
    }

    /*
    /**********************************************************************
    /* Test methods: findAliases returning non-null aliases
    /**********************************************************************
     */

    @Test
    public void testFindAliasesWithJsonAlias() {
        // Use AliasedBean which has @JsonAlias on its field
        DeserializationConfig config = MAPPER.deserializationConfig();
        JavaType type = MAPPER.constructType(AliasedBean.class);
        AnnotatedClass ac = AnnotatedClassResolver.resolve(config, type, config);
        AnnotatedField field = ac.fields().iterator().next();

        SimpleBeanPropertyDefinition prop = SimpleBeanPropertyDefinition.construct(
                config, field);
        List<PropertyName> aliases = prop.findAliases();
        assertNotNull(aliases);
        assertEquals(2, aliases.size());
    }

    /*
    /**********************************************************************
    /* Test methods: null AnnotationIntrospector branches
    /**********************************************************************
     */

    @Test
    public void testGetWrapperNameNullIntrospector() {
        ObjectMapper noIntrMapper = JsonMapper.builder()
                .annotationIntrospector(null)
                .build();
        DeserializationConfig config = noIntrMapper.deserializationConfig();
        JavaType type = noIntrMapper.constructType(SimpleBean.class);
        AnnotatedClass ac = AnnotatedClassResolver.resolve(config, type, config);
        AnnotatedField field = ac.fields().iterator().next();

        SimpleBeanPropertyDefinition prop = SimpleBeanPropertyDefinition.construct(
                config, field);
        assertNull(prop.getWrapperName());
    }

    @Test
    public void testFindAliasesNullIntrospector() {
        ObjectMapper noIntrMapper = JsonMapper.builder()
                .annotationIntrospector(null)
                .build();
        DeserializationConfig config = noIntrMapper.deserializationConfig();
        JavaType type = noIntrMapper.constructType(SimpleBean.class);
        AnnotatedClass ac = AnnotatedClassResolver.resolve(config, type, config);
        AnnotatedField field = ac.fields().iterator().next();

        SimpleBeanPropertyDefinition prop = SimpleBeanPropertyDefinition.construct(
                config, field);
        assertTrue(prop.findAliases().isEmpty());
    }
}
