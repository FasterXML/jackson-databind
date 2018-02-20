package com.fasterxml.jackson.databind.module;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.type.TypeBindings;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.type.TypeModifier;

import java.lang.reflect.Type;

public class TestTypeModifierNameResolution extends BaseMapTest
{
    interface MyType {
        String getData();
        void setData(String data);
    }

    static class MyTypeImpl implements MyType {
        private String data;

        @Override
        public String getData() {
            return data;
        }

        @Override
        public void setData(String data) {
            this.data = data;
        }
    }

    private static class CustomTypeModifier extends TypeModifier {
        @Override
        public JavaType modifyType(JavaType type, Type jdkType, TypeBindings context, TypeFactory typeFactory) {
            if (type.hasRawClass(MyTypeImpl.class)) {
                return typeFactory.constructType(MyType.class);
            }
            return type;
        }
    }

    @JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.WRAPPER_OBJECT)
    public interface Mixin { }

    // Expect that the TypeModifier kicks in when the type id is written.
    public void testTypeModiferNameResolution() throws Exception
    {
        final ObjectMapper mapper = ObjectMapper.builder()
                .typeFactory(TypeFactory.defaultInstance().withModifier(new CustomTypeModifier()))
                .addMixIn(MyType.class, Mixin.class)
                .build();

        MyType obj = new MyTypeImpl();
        obj.setData("something");

        String s = mapper.writer().writeValueAsString(obj);
        final String EXP = "{\"TestTypeModifierNameResolution$MyType\":";
        if (!s.startsWith(EXP)) {
            fail("Should start with ["+EXP+"], does not ["+s+"]");
        }
    }
}
