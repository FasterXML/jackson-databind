package com.fasterxml.jackson.databind.jsontype.ext;

import java.util.HashMap;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import com.fasterxml.jackson.databind.jsontype.impl.TypeIdResolverBase;


public class ExternalTypeIdWithCreator3045Test
    extends BaseMapTest
{
    public static class ChildBaseByParentTypeResolver extends TypeIdResolverBase {
        public ChildBaseByParentTypeResolver() {
//           System.out.println("Create ChildBaseByParentTypeResolver");
        }

        private JavaType superType;

        @Override
        public void init(JavaType baseType) {
             superType = baseType;
        }

        @Override
        public Id getMechanism() {
             return Id.NAME;
        }

        @Override
        public JavaType typeFromId(DatabindContext context, String id) {
             switch (id) {
             case "track":
                 return context.constructSpecializedType(superType, MyData.class);
             }
             throw new IllegalArgumentException("No type with id '"+id+"'");
        }

        @Override
        public String idFromValue(Object value) {
//        public String idFromValue(DatabindContext ctxt, Object value) {
             return null;
        }

        @Override
        public String idFromValueAndType(Object value, Class<?> suggestedType) {
//        public String idFromValueAndType(DatabindContext ctxt, Object value, Class<?> suggestedType) {
             return null;
        }
   }

    static class MyData
    {
        @JsonAnySetter
        public HashMap<String,Object> data = new HashMap<>();

        public int size() { return data.size(); }
        public Object find(String key) { return data.get(key); }

        @Override
        public String toString() {
            return String.valueOf(data);
        }
    }

    public static class MyJson3045 {
       public final long time;
       public String type;
       public Object data;

       @JsonCreator
       public MyJson3045(@JsonProperty("time") long t) {
           time = t;
       }

       @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
               property = "type")
       @JsonTypeIdResolver(ChildBaseByParentTypeResolver.class)
       public void setData(Object data) {
           this.data = data;
       }

       @Override
       public String toString() {
           return "[time="+time+", type="+type+", data="+data+"]";
       }
    }

   private static ObjectMapper MAPPER = newJsonMapper();

   public void testExternalIdWithAnySetter3045() throws Exception
   {
       // First cases where the last Creator argument comes last:
       _testExternalIdWithAnySetter3045(a2q(
               "{'type':'track','data':{'data-internal':'toto'},'time':345}"));
       _testExternalIdWithAnySetter3045(a2q(
               "{'data':{'data-internal':'toto'},'type':'track', 'time':345}"));

       // then a case where it comes in the middle
       _testExternalIdWithAnySetter3045(a2q(
               "{'data':{'data-internal':'toto'},'time':345, 'type':'track'}"));

       // and finally one where we'll start with it
       _testExternalIdWithAnySetter3045(a2q(
               "{'time':345, 'type':'track', 'data':{'data-internal':'toto'}}"));
   }

   private void _testExternalIdWithAnySetter3045(String input) throws Exception
   {
       MyJson3045 result = MAPPER.readValue(input, MyJson3045.class);

       assertEquals(345, result.time);
       if (result.data == null) {
           fail("Expected non-null data; result object = "+result);
       }
       assertEquals("track", result.type);
       assertEquals(MyData.class, result.data.getClass());
       MyData data = (MyData) result.data;
       assertEquals(1, data.size());
       assertEquals("toto", data.find("data-internal"));
   }
}
