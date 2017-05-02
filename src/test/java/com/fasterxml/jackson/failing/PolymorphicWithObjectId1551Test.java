package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.databind.*;

public class PolymorphicWithObjectId1551Test extends BaseMapTest
{
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY,
            property = "@class")
    static abstract class Vehicle {
        public String vehicleId;
    }

    static class Car extends Vehicle {
        public int numberOfDoors;
    }

    static class VehicleOwner {
        @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "vehicleId")
        @JsonIdentityReference(alwaysAsId = false)
        public Vehicle ownedVehicle;
    }

    public void testSerializeDeserialize() throws Exception {
        Car c = new Car();
        c.vehicleId = "123";
        c.numberOfDoors = 2;
        // both owners own the same vehicle (car sharing ;-))
        VehicleOwner v1 = new VehicleOwner();
        v1.ownedVehicle = c;
        VehicleOwner v2 = new VehicleOwner();
        v2.ownedVehicle = c;

        ObjectMapper objectMapper = new ObjectMapper();
        String serialized = objectMapper.writer()
//                .with(SerializationFeature.INDENT_OUTPUT)
                .writeValueAsString(new VehicleOwner[] { v1, v2 });
//        System.out.println(serialized);

        VehicleOwner[] deserialized = objectMapper.readValue(serialized, VehicleOwner[].class);
        assertEquals(2, deserialized.length);
        assertSame(deserialized[0].ownedVehicle, deserialized[1].ownedVehicle);
    }
}
