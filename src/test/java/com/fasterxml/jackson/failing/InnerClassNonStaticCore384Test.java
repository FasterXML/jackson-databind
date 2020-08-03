package com.fasterxml.jackson.failing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.testutil.NoCheckSubTypeValidator;

// see [https://github.com/FasterXML/jackson-core/issues/384]: most likely
// can not be fixed, but could we improve error message to indicate issue
// with non-static type of `Car` and `Truck`, which prevent instantiation?
public class InnerClassNonStaticCore384Test extends BaseMapTest
{
    static class Fleet {
        private List<Vehicle> vehicles;

        public List<Vehicle> getVehicles() {
            return vehicles;
        }

        public void setVehicles(List<Vehicle> vehicles) {
            this.vehicles = vehicles;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Fleet fleet = (Fleet) o;
            return Objects.equals(vehicles, fleet.vehicles);
        }

        @Override
        public int hashCode() {
            return Objects.hash(vehicles);
        }
    }

    static abstract class Vehicle {
        private String make;
        private String model;

        protected Vehicle(String make, String model) {
            this.make = make;
            this.model = model;
        }

        public Vehicle() {
        }

        public String getMake() {
            return make;
        }

        public void setMake(String make) {
            this.make = make;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Vehicle)) return false;
            Vehicle vehicle = (Vehicle) o;
            return Objects.equals(make, vehicle.make) &&
                    Objects.equals(model, vehicle.model);
        }

        @Override
        public int hashCode() {
            return Objects.hash(make, model);
        }
    }

    class Car extends Vehicle {
        private int seatingCapacity;
        private double topSpeed;

        public Car(String make, String model, int seatingCapacity, double topSpeed) {
            super(make, model);
            this.seatingCapacity = seatingCapacity;
            this.topSpeed = topSpeed;
        }

        public Car() {
        }

        public int getSeatingCapacity() {
            return seatingCapacity;
        }

        public void setSeatingCapacity(int seatingCapacity) {
            this.seatingCapacity = seatingCapacity;
        }

        public double getTopSpeed() {
            return topSpeed;
        }

        public void setTopSpeed(double topSpeed) {
            this.topSpeed = topSpeed;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;
            Car car = (Car) o;
            return seatingCapacity == car.seatingCapacity &&
                    Double.compare(car.topSpeed, topSpeed) == 0;
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), seatingCapacity, topSpeed);
        }
    }

    class Truck extends Vehicle {
        private double payloadCapacity;

        public Truck(String make, String model, double payloadCapacity) {
            super(make, model);
            this.payloadCapacity = payloadCapacity;
        }

        public Truck() {
        }

        public double getPayloadCapacity() {
            return payloadCapacity;
        }

        public void setPayloadCapacity(double payloadCapacity) {
            this.payloadCapacity = payloadCapacity;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;
            Truck truck = (Truck) o;
            return Double.compare(truck.payloadCapacity, payloadCapacity) == 0;
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), payloadCapacity);
        }
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    public void testHierarchy() throws IOException {
        ObjectMapper mapper = jsonMapperBuilder()
                .activateDefaultTyping(NoCheckSubTypeValidator.instance)
                .build();

        Fleet fleet = initVehicle();

        /*
for (Vehicle v : fleet.vehicles) {
    System.out.println("Vehicle, type: "+v.getClass());
}
*/
        String serializedFleet = mapper
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(fleet);

//System.out.println(serializedFleet);

        Fleet deserializedFleet = mapper.readValue(serializedFleet, Fleet.class);

        assertTrue(deserializedFleet.getVehicles().get(0) instanceof Car);
        assertTrue(deserializedFleet.getVehicles().get(1) instanceof Truck);

        assertEquals(fleet, deserializedFleet);
    }

    private Fleet initVehicle() {
        Car car = new Car("Mercedes-Benz", "S500", 5, 250.0);
        Truck truck = new Truck("Isuzu", "NQR", 7500.0);

        List<Vehicle> vehicles = new ArrayList<>();
        vehicles.add(car);
        vehicles.add(truck);

        Fleet serializedFleet = new Fleet();
        serializedFleet.setVehicles(vehicles);
        return serializedFleet;
    }
}
