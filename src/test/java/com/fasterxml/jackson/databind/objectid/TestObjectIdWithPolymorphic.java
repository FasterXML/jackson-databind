package com.fasterxml.jackson.databind.objectid;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;
import com.fasterxml.jackson.databind.testutil.NoCheckSubTypeValidator;

public class TestObjectIdWithPolymorphic extends BaseMapTest
{
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    @JsonIdentityInfo(generator=ObjectIdGenerators.IntSequenceGenerator.class, property="id")
    static abstract class Base
    {
        public int value;
        public Base next;

        public Base() { this(0); }
        public Base(int v) {
            value = v;
        }
    }

    static class Impl extends Base
    {
        public int extra;

        public Impl() { this(0, 0); }
        public Impl(int v, int e) {
            super(v);
            extra = e;
        }
    }

    // [JACKSON-811] types

    @JsonIdentityInfo(generator=ObjectIdGenerators.PropertyGenerator.class, property="id")
    public static class Base811 {
        public int id;
        public Base811 owner;

        protected Base811() {}
        public Base811(Process owner) {
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
        protected List<Base811> children = new ArrayList<Base811>();

        public Process() { super(null); }
    }

    public static abstract class Activity extends Base811 {
        protected Activity parent;
        public Activity(Process owner, Activity parent) {
                super(owner);
                this.parent = parent;
        }
        protected Activity() {
            super();
        }
    }

    public static class Scope extends Activity {
        public final List<FaultHandler> faultHandlers = new ArrayList<FaultHandler>();
        public Scope(Process owner, Activity parent) {
            super(owner, parent);
        }
        protected Scope() {
            super();
        }
    }

    public static class FaultHandler extends Base811 {
        public final List<Catch> catchBlocks = new ArrayList<Catch>();

        public FaultHandler(Process owner) {
            super(owner);
        }

        protected FaultHandler() {}
    }

    public static class Catch extends Scope {
        public Catch(Process owner, Activity parent) {
            super(owner, parent);
        }
        protected Catch() {};
    }

    /*
    /*****************************************************
    /* Unit tests for polymorphic type handling
    /*****************************************************
     */

    private final ObjectMapper mapper = new ObjectMapper();

    public void testPolymorphicRoundtrip() throws Exception
    {
        // create simple 2 node loop:
        Impl in1 = new Impl(123, 456);
        in1.next = new Impl(111, 222);
        in1.next.next = in1;

        String json = mapper.writeValueAsString(in1);

        // then bring back...
        Base result0 = mapper.readValue(json, Base.class);
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

    public void testIssue811() throws Exception
    {
        ObjectMapper om = new ObjectMapper();
        om.enable(SerializationFeature.WRITE_ENUMS_USING_INDEX);
        om.enable(SerializationFeature.INDENT_OUTPUT);
        om.activateDefaultTypingAsProperty(NoCheckSubTypeValidator.instance,
                DefaultTyping.NON_FINAL, "@class");

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
}
