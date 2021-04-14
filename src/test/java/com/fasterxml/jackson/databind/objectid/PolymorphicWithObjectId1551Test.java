package com.fasterxml.jackson.databind.objectid;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;

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

    static class VehicleOwnerBroken {
        @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "bogus")
        @JsonIdentityReference(alwaysAsId = false)
        public Vehicle ownedVehicle;
    }

    public void testWithAbstractUsingProp() throws Exception
    {
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

        VehicleOwnerViaProp[] deserialized = objectMapper.readValue(serialized, VehicleOwnerViaProp[].class);
        assertEquals(2, deserialized.length);
        assertSame(deserialized[0].ownedVehicle, deserialized[1].ownedVehicle);
    }

    public void testFailingAbstractUsingProp() throws Exception
    {
        Car c = new Car();
        c.vehicleId = "123";
        c.numberOfDoors = 2;
        // both owners own the same vehicle (car sharing ;-))
        VehicleOwnerBroken v1 = new VehicleOwnerBroken();
        v1.ownedVehicle = c;
        VehicleOwnerBroken v2 = new VehicleOwnerBroken();
        v2.ownedVehicle = c;

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            objectMapper.writer()
                .writeValueAsString(new VehicleOwnerBroken[] { v1, v2 });
        } catch (InvalidDefinitionException e) {
            // on serialization, reported for different type
            assertEquals(Car.class, e.getType().getRawClass());
            verifyException(e, "Invalid Object Id definition");
            verifyException(e, "cannot find property with name 'bogus'");
        }

        // and same for deser
        final String JSON = a2q(
"[{'ownedVehicle':{'@class':'com.fasterxml.jackson.failing.PolymorphicWithObjectId1551Test$Car','vehicleId':'123',"
+"'numberOfDoors':2}},{'ownedVehicle':'123'}]"
                );
        try {
            objectMapper.readValue(JSON, VehicleOwnerBroken[].class);
            fail("Should not pass");
        } catch (InvalidDefinitionException e) {
            assertEquals(Vehicle.class, e.getType().getRawClass());
            verifyException(e, "Invalid Object Id definition");
            verifyException(e, "cannot find property with name 'bogus'");
        }
    }
}
