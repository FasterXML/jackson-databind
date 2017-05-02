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

    static class VehicleOwnerViaProp {
        @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "vehicleId")
        @JsonIdentityReference(alwaysAsId = false)
        public Vehicle ownedVehicle;
    }

    public void testWithAbstractUsingProp() throws Exception {
        Car c = new Car();
        c.vehicleId = "123";
        c.numberOfDoors = 2;
        // both owners own the same vehicle (car sharing ;-))
        VehicleOwnerViaProp v1 = new VehicleOwnerViaProp();
        v1.ownedVehicle = c;
        VehicleOwnerViaProp v2 = new VehicleOwnerViaProp();
        v2.ownedVehicle = c;

        ObjectMapper objectMapper = new ObjectMapper();
        String serialized = objectMapper.writer()
                .writeValueAsString(new VehicleOwnerViaProp[] { v1, v2 });

        // 02-May-2017, tatu: Not possible to support as of Jackson 2.8 at least, so:

        try {
            /*VehicleOwnerViaProp[] deserialized = */
            objectMapper.readValue(serialized, VehicleOwnerViaProp[].class);
            fail("Should not pass");
        } catch (JsonMappingException e) {
            verifyException(e, "Invalid Object Id definition for abstract type");
        }
//        assertEquals(2, deserialized.length);
//        assertSame(deserialized[0].ownedVehicle, deserialized[1].ownedVehicle);
    }
}
