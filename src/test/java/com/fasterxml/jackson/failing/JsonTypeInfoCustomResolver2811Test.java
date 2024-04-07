package com.fasterxml.jackson.failing;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import com.fasterxml.jackson.databind.jsontype.impl.TypeIdResolverBase;

public class JsonTypeInfoCustomResolver2811Test extends BaseMapTest
{
    interface Vehicle {
    }

    static class Car implements Vehicle {
        public int wheels;
        public String color;
    }

    static class Bicycle implements Vehicle {
        public int wheels;
        public String bicycleType;
    }

    static class Person<T extends Vehicle> {
        public String name;
        public VehicleType vehicleType;

        @JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "vehicleType")
        @JsonTypeIdResolver(VehicleTypeResolver.class)
        public T vehicle;
    }

    public enum VehicleType {
        CAR(Car.class),
        BICYCLE(Bicycle.class);

        public final Class<? extends Vehicle> vehicleClass;

        VehicleType(Class<? extends Vehicle> vehicleClass) {
            this.vehicleClass = vehicleClass;
        }
    }

    static class VehicleTypeResolver extends TypeIdResolverBase {

        JavaType superType;

        @Override
        public void init(JavaType bt) {
            this.superType = bt;
        }

        @Override
        public String idFromValue(Object value) {
            return null;
        }

        @Override
        public String idFromValueAndType(Object value, Class<?> suggestedType) {
            return null;
        }

        @Override
        public JavaType typeFromId(DatabindContext context, String id) throws IOException {
            try {
                Class<? extends Vehicle> vehicleClass = VehicleType.valueOf(id).vehicleClass;
                return context.constructSpecializedType(superType, vehicleClass);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

        @Override
        public JsonTypeInfo.Id getMechanism() {
            return JsonTypeInfo.Id.NAME;
        }
    }

    private final ObjectMapper MAPPER = jsonMapperBuilder()
            .disable(DeserializationFeature.FAIL_ON_MISSING_EXTERNAL_TYPE_ID_PROPERTY)
            .disable(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE)
            .build();
    
    // [databind#2811]
    public void testTypeInfoWithCustomResolver2811NoTypeId() throws Exception
    {
        String json = "{ \"name\": \"kamil\", \"vehicle\": {\"wheels\": 4, \"color\": \"red\"}}";
        Person<?> person = MAPPER.readValue(json, Person.class);
        assertEquals("kamil", person.name);
        assertNull(person.vehicleType);
        assertNull(person.vehicle);        
    }

    // Passing case for comparison
    /*
    public void testTypeInfoWithCustomResolver2811WithTypeId() throws Exception
    {
        String json = "{\n" + 
                "  \"name\": \"kamil\",\n" + 
                "  \"vehicleType\": \"CAR\",\n" + 
                "  \"vehicle\": {\n" + 
                "    \"wheels\": 4,\n" + 
                "    \"color\": \"red\"\n" + 
                "  }\n" + 
                "}"
                ;
        Person<?> person = MAPPER.readValue(json, Person.class);
        assertEquals("kamil", person.name);
        assertEquals(VehicleType.CAR, person.vehicleType);
        assertNotNull(person.vehicle);
    }
    */
}
