package tools.jackson.databind.objectid;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.*;
import tools.jackson.databind.cfg.EnumFeature;
import tools.jackson.databind.exc.InvalidDefinitionException;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.testutil.NoCheckSubTypeValidator;

import static org.junit.jupiter.api.Assertions.*;

public class ObjectIdWithPolymorphicTest extends DatabindTestUtil
{
    // // // Simple polymorphic roundtrip

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    @JsonIdentityInfo(generator=ObjectIdGenerators.IntSequenceGenerator.class, property="id")
    static abstract class Base
    {
        public int value;
        public Base next;

        public Base() { this(0); }
        protected Base(int v) { value = v; }
    }

    static class Impl extends Base
    {
        public int extra;

        public Impl() { this(0, 0); }
        protected Impl(int v, int e) {
            super(v);
            extra = e;
        }
    }

    // // // [databind#811] types -- deeply nested polymorphic BPEL-like structure

    @JsonIdentityInfo(generator=ObjectIdGenerators.PropertyGenerator.class, property="id")
    public static class Base811 {
        public int id;
        public Base811 owner;

        protected Base811() {}
        protected Base811(Process owner) {
            this.owner = owner;
            if (owner == null) {
                id = 0;
            } else {
                id = ++owner.childIdCounter;
                owner.children.add(this);
            }
        }
    }

    public static class Process extends Base811 {
        protected int childIdCounter = 0;
        protected List<Base811> children = new ArrayList<>();

        public Process() { super(null); }
    }

    public static abstract class Activity extends Base811 {
        protected Activity parent;
        public Activity(Process owner, Activity parent) {
            super(owner);
            this.parent = parent;
        }
        protected Activity() { super(); }
    }

    public static class Scope extends Activity {
        public final List<FaultHandler> faultHandlers = new ArrayList<>();
        public Scope(Process owner, Activity parent) { super(owner, parent); }
        protected Scope() { super(); }
    }

    public static class FaultHandler extends Base811 {
        public final List<Catch> catchBlocks = new ArrayList<>();

        public FaultHandler(Process owner) { super(owner); }
        protected FaultHandler() {}
    }

    public static class Catch extends Scope {
        public Catch(Process owner, Activity parent) { super(owner, parent); }
        protected Catch() {}
    }

    // // // [databind#877]: interface + abstract class with ObjectId and default typing

    interface BaseInterface877 { }

    @JsonIdentityInfo(generator=ObjectIdGenerators.IntSequenceGenerator.class, property="@id")
    static class BaseInterfaceImpl877 implements BaseInterface877 {
        @JsonProperty
        private List<BaseInterfaceImpl877> myInstances = new ArrayList<>();

        void addInstance(BaseInterfaceImpl877 instance) {
            myInstances.add(instance);
        }
    }

    static class ListWrapper877<T extends BaseInterface877> {
        @JsonProperty
        private List<T> myList = new ArrayList<>();

        void add(T t) { myList.add(t); }
        int size() { return myList.size(); }
    }

    // // // [databind#1551]: Polymorphic types (abstract classes) with property-based ObjectId

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

    // // // [TestObjectId / databind#(no issue)]: ObjectId + JsonTypeInfo combination

    @JsonIdentityInfo(generator=ObjectIdGenerators.IntSequenceGenerator.class, property="@id")
    @JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
    public static class BaseEntity { }

    public static class Foo extends BaseEntity {
        public BaseEntity ref;
    }

    public static class Bar extends BaseEntity {
        public Foo next;
    }

    /*
    /**********************************************************
    /* Unit tests for polymorphic type handling
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testPolymorphicRoundtrip() throws Exception
    {
        // create simple 2 node loop:
        Impl in1 = new Impl(123, 456);
        in1.next = new Impl(111, 222);
        in1.next.next = in1;

        String json = MAPPER.writeValueAsString(in1);

        Base result0 = MAPPER.readValue(json, Base.class);
        assertNotNull(result0);
        assertSame(Impl.class, result0.getClass());
        Impl result = (Impl) result0;
        assertEquals(123, result.value);
        assertEquals(456, result.extra);
        Impl result2 = (Impl) result.next;
        assertEquals(111, result2.value);
        assertEquals(222, result2.extra);
        assertSame(result, result2.next);
    }

    @Test
    public void testIssue811() throws Exception
    {
        ObjectMapper om = jsonMapperBuilder()
                .activateDefaultTypingAsProperty(NoCheckSubTypeValidator.instance,
                        DefaultTyping.NON_FINAL, "@class")
                .enable(SerializationFeature.INDENT_OUTPUT)
                .enable(EnumFeature.WRITE_ENUMS_USING_INDEX)
                .build();

        Process p = new Process();
        Scope s = new Scope(p, null);
        FaultHandler fh = new FaultHandler(p);
        Catch c = new Catch(p, s);
        fh.catchBlocks.add(c);
        s.faultHandlers.add(fh);

        String json = om.writeValueAsString(p);
        Process restored = om.readValue(json, Process.class);
        assertNotNull(restored);

        assertEquals(0, p.id);
        assertEquals(3, p.children.size());
        assertSame(p, p.children.get(0).owner);
        assertSame(p, p.children.get(1).owner);
        assertSame(p, p.children.get(2).owner);
    }

    // [databind#877]
    @Test
    public void testIssue877() throws Exception
    {
        BaseInterfaceImpl877 one = new BaseInterfaceImpl877();
        BaseInterfaceImpl877 two = new BaseInterfaceImpl877();
        one.addInstance(two);
        two.addInstance(one);

        ListWrapper877<BaseInterfaceImpl877> myList = new ListWrapper877<>();
        myList.add(one);
        myList.add(two);

        ObjectMapper mapper = jsonMapperBuilder()
                .activateDefaultTypingAsProperty(NoCheckSubTypeValidator.instance,
                        DefaultTyping.NON_FINAL, "@class")
                .build();

        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(myList);
        ListWrapper877<BaseInterfaceImpl877> result =
                mapper.readValue(json, new TypeReference<ListWrapper877<BaseInterfaceImpl877>>() { });

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    public void testObjectAndTypeId() throws Exception
    {
        Bar inputRoot = new Bar();
        Foo inputChild = new Foo();
        inputRoot.next = inputChild;
        inputChild.ref = inputRoot;

        String json = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(inputRoot);

        BaseEntity resultRoot = MAPPER.readValue(json, BaseEntity.class);
        assertNotNull(resultRoot);
        assertInstanceOf(Bar.class, resultRoot);
        Bar first = (Bar) resultRoot;

        assertNotNull(first.next);
        assertInstanceOf(Foo.class, first.next);
        Foo second = (Foo) first.next;
        assertNotNull(second.ref);
        assertSame(first, second.ref);
    }

    /*
    /**********************************************************
    /* Unit tests, abstract class with property-based ObjectId [databind#1551]
    /**********************************************************
     */

    @Test
    public void testWithAbstractUsingProp1551() throws Exception
    {
        Car c = new Car();
        c.vehicleId = "123";
        c.numberOfDoors = 2;
        VehicleOwnerViaProp v1 = new VehicleOwnerViaProp();
        v1.ownedVehicle = c;
        VehicleOwnerViaProp v2 = new VehicleOwnerViaProp();
        v2.ownedVehicle = c;

        ObjectMapper mapper = newJsonMapper();
        String serialized = mapper.writer()
                .writeValueAsString(new VehicleOwnerViaProp[] { v1, v2 });

        VehicleOwnerViaProp[] deserialized = mapper.readValue(serialized, VehicleOwnerViaProp[].class);
        assertEquals(2, deserialized.length);
        assertSame(deserialized[0].ownedVehicle, deserialized[1].ownedVehicle);
    }

    @Test
    public void testFailingAbstractUsingProp1551() throws Exception
    {
        Car c = new Car();
        c.vehicleId = "123";
        c.numberOfDoors = 2;
        VehicleOwnerBroken v1 = new VehicleOwnerBroken();
        v1.ownedVehicle = c;
        VehicleOwnerBroken v2 = new VehicleOwnerBroken();
        v2.ownedVehicle = c;

        ObjectMapper mapper = newJsonMapper();
        try {
            mapper.writer()
                .writeValueAsString(new VehicleOwnerBroken[] { v1, v2 });
        } catch (InvalidDefinitionException e) {
            assertEquals(Car.class, e.getType().getRawClass());
            verifyException(e, "Invalid Object Id definition");
            verifyException(e, "cannot find property with name 'bogus'");
        }

        final String JSON = a2q(
"[{'ownedVehicle':{'@class':'com.fasterxml.jackson.failing.PolymorphicWithObjectId1551Test$Car','vehicleId':'123',"
+"'numberOfDoors':2}},{'ownedVehicle':'123'}]"
                );
        try {
            mapper.readValue(JSON, VehicleOwnerBroken[].class);
            fail("Should not pass");
        } catch (InvalidDefinitionException e) {
            assertEquals(Vehicle.class, e.getType().getRawClass());
            verifyException(e, "Invalid Object Id definition");
            verifyException(e, "cannot find property with name 'bogus'");
        }
    }
}
