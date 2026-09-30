package com.fasterxml.jackson.databind.jsontype.impl;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator.Validity;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.LRUMap;
import com.fasterxml.jackson.databind.util.LookupCache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

public class BaseTypeAllowingValidatorTest extends DatabindTestUtil
{
    // A custom `TypeResolverBuilder` may hand the id resolver a `JavaType` that is
    // equal to, but not the same instance as, the approved base type.
    @Test
    public void approvalAppliesToEqualBaseTypeInstances() throws Exception
    {
        TypeFactory tf = TypeFactory.defaultInstance();
        JavaType allowedBase = tf.constructParametricType(List.class, String.class);
        LookupCache<Object, JavaType> emptyCache = new LRUMap<Object, JavaType>(4, 4);
        JavaType equalBase = tf.withCache(emptyCache)
                .constructParametricType(List.class, String.class);
        assertNotSame(allowedBase, equalBase);
        assertEquals(allowedBase, equalBase);
        JavaType otherBase = tf.constructParametricType(List.class, Integer.class);
        JavaType subType = tf.constructParametricType(ArrayList.class, String.class);
        MapperConfig<?> config = newJsonMapper().getDeserializationConfig();

        BaseTypeAllowingValidator ptv = new BaseTypeAllowingValidator(allowedBase,
                BasicPolymorphicTypeValidator.builder().build());

        assertEquals(Validity.ALLOWED,
                ptv.validateSubClassName(config, equalBase, ArrayList.class.getName()));
        assertEquals(Validity.ALLOWED, ptv.validateSubType(config, equalBase, subType));
        assertEquals(Validity.INDETERMINATE,
                ptv.validateSubClassName(config, otherBase, ArrayList.class.getName()));
        assertEquals(Validity.INDETERMINATE, ptv.validateSubType(config, otherBase, subType));
    }
}
