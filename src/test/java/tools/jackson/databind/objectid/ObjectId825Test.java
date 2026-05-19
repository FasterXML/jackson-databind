package tools.jackson.databind.objectid;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.testutil.NoCheckSubTypeValidator;

import static org.junit.jupiter.api.Assertions.*;

// [databind#825] + [databind#825B]: ObjectId + default typing, simple and complex hierarchies
@SuppressWarnings("serial")
public class ObjectId825Test extends DatabindTestUtil
{
    // // // [databind#825]: Simple hierarchy with PropertyGenerator + default typing

    @JsonIdentityInfo(generator=ObjectIdGenerators.PropertyGenerator.class, property="oidString")
    public static class AbstractEntity {
        public String oidString;
    }

    public static class TestA extends AbstractEntity {
        public TestAbst testAbst;
        public TestD d;
    }

    static class TestAbst extends AbstractEntity { }

    static class TestC extends TestAbst {
        public TestD d;
    }

    static class TestD extends AbstractEntity { }

    // // // [databind#825B]: Complex BPEL-like hierarchy with ObjectId + JsonTypeInfo + default typing

    @JsonIdentityInfo(generator=ObjectIdGenerators.PropertyGenerator.class, property="oidString")
    @JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
    static abstract class TypedEntity implements java.io.Serializable {
        public String oidString;

        protected TypedEntity() { }

        public String getOidString() { return oidString; }
        public void setOidString(String oidString) { this.oidString = oidString; }
    }

    static abstract class AbstractSym extends TypedEntity { }

    static abstract class AbstractData extends AbstractSym { }

    static abstract class AbstractAct extends TypedEntity {
        protected java.util.ArrayList<Tr> outTr;

        public java.util.ArrayList<Tr> getOutTr() { return this.outTr; }
        public void setOutTr(java.util.ArrayList<Tr> outTr) { this.outTr = outTr; }
    }

    static abstract class AbstractCond extends AbstractAct { }

    static abstract class AbstractDec extends AbstractAct {
        protected java.util.ArrayList<Dec> dec;

        public java.util.ArrayList<Dec> getDec() { return this.dec; }
        public void setDec(java.util.ArrayList<Dec> dec) { this.dec = dec; }
    }

    static class Ch extends TypedEntity {
        protected java.util.ArrayList<? extends AbstractAct> act;

        public java.util.ArrayList<? extends AbstractAct> getAct() { return this.act; }
        public void setAct(java.util.ArrayList<? extends AbstractAct> act) { this.act = act; }
    }

    static class CTC extends TypedEntity {
        protected java.util.ArrayList<CTV> var;

        public CTC() { }

        public java.util.ArrayList<CTV> getVar() {
            if (var == null) {
                var = new ArrayList<CTV>();
            }
            return new ArrayList<CTV>(var);
        }

        public void setVar(java.util.ArrayList<CTV> var) { this.var = var; }
    }

    static class CTD extends AbstractDec { }

    static class CTV extends TypedEntity {
        protected Ch ch;
        protected java.util.ArrayList<? extends AbstractData> locV;

        public Ch getCh() { return this.ch; }
        public void setCh(Ch ch) { this.ch = ch; }

        public java.util.ArrayList<? extends AbstractData> getLocV() { return this.locV; }
        public void setLocV(java.util.ArrayList<? extends AbstractData> locV) { this.locV = locV; }
    }

    static class Dec extends AbstractCond { }

    static class Ti extends AbstractAct {
        protected AbstractData timer;

        public AbstractData getTimer() { return this.timer; }
        public void setTimer(AbstractData timer) { this.timer = timer; }
    }

    static class Tr extends TypedEntity {
        protected AbstractAct target;

        public AbstractAct getTarget() { return this.target; }
        public void setTarget(AbstractAct target) { this.target = target; }
    }

    static class V extends AbstractData {
        private static final long serialVersionUID = 1L;
    }

    // // // [databind#2780]: Default Typing + @JsonIdentityInfo in untyped collections

    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    static class User2780 {
        public int id;
        public String login;

        public User2780() {}
        public User2780(int id, String login) {
            this.id = id;
            this.login = login;
        }
    }

    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    static class UserContainer2780 {
        public int id;
        public User2780 user;

        public UserContainer2780() {}
        public UserContainer2780(int id, User2780 user) {
            this.id = id;
            this.user = user;
        }
    }

    /*
    /**********************************************************
    /* Unit tests, simple hierarchy [databind#825]
    /**********************************************************
     */

    private final ObjectMapper DEF_TYPING_MAPPER = jsonMapperBuilder()
            .activateDefaultTyping(NoCheckSubTypeValidator.instance,
                    DefaultTyping.NON_FINAL)
            .build();

    @Test
    public void testDeserialize() throws Exception {
        TestA a = new TestA();
        a.oidString = "oidA";

        TestC c = new TestC();
        c.oidString = "oidC";

        a.testAbst = c;

        TestD d = new TestD();
        d.oidString = "oidD";

        c.d = d;
        a.d = d;

        String json = DEF_TYPING_MAPPER.writeValueAsString(a);
        TestA testADeserialized = DEF_TYPING_MAPPER.readValue(json, TestA.class);

        assertNotNull(testADeserialized);
        assertNotNull(testADeserialized.d);
        assertEquals("oidD", testADeserialized.d.oidString);
    }

    /*
    /**********************************************************************
    /* Unit tests, complex BPEL-like hierarchy [databind#825]
    /**********************************************************************
     */

    // [databind#825]
    @Test
    public void testFull825() throws Exception
    {
        final ObjectMapper mapper = jsonMapperBuilder()
                .activateDefaultTyping(NoCheckSubTypeValidator.instance,
                        DefaultTyping.OBJECT_AND_NON_CONCRETE)
                .build();

        String INPUT = """
                {
                    "@class": "_PKG_CTC",
                     "var": [{
                      "ch": {
                        "@class": "_PKG_Ch",
                         "act": [{
                            "@class": "_PKG_CTD",
                            "oidString": "oid1",
                            "dec": [{
                              "@class": "_PKG_Dec",
                                "oidString": "oid2",
                                "outTr": [{
                                  "@class": "_PKG_Tr",
                                  "target": {
                                    "@class": "_PKG_Ti",
                                    "oidString": "oid3",
                                    "timer": "problemoid",
                                    "outTr": [{
                                      "@class": "_PKG_Tr",
                                      "target": {
                                        "@class": "_PKG_Ti",
                                        "oidString": "oid4",
                                        "timer": {
                                          "@class": "_PKG_V",
                                          "oidString": "problemoid"
                                        }
                                      }
                                    }]
                                  }
                                }]
                              }]
                         }],
                         "oidString": "oid5"
                      },
                       "@class": "_PKG_CTV",
                       "oidString": "oid6",
                       "locV": ["problemoid"]
                    }],
                     "oidString": "oid7"
                }
                """;

        // Replace package placeholder with actual inner-class package
        final String newPkg = getClass().getName() + "\\$";
        INPUT = INPUT.replaceAll("_PKG_", newPkg);

        CTC result = mapper.readValue(INPUT, CTC.class);
        assertNotNull(result);
    }

    /*
    /**********************************************************************
    /* Unit tests, default typing + untyped collections [databind#2780]
    /**********************************************************************
     */

    // User appears first: back-reference resolves correctly
    @Test
    public void testUserFirstThenContainer2780() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
            .activateDefaultTyping(NoCheckSubTypeValidator.instance, DefaultTyping.NON_FINAL)
            .build();

        User2780 user = new User2780(42, "cool_man");
        UserContainer2780 container = new UserContainer2780(1, user);

        List<Object> list = new ArrayList<>();
        list.add(user);
        list.add(container);

        String json = mapper.writeValueAsString(list);
        List<?> result = mapper.readValue(json, List.class);

        assertEquals(2, result.size());
        assertInstanceOf(User2780.class, result.get(0), "First element should be User2780");
        assertInstanceOf(UserContainer2780.class, result.get(1), "Second element should be UserContainer2780");

        User2780 resultUser = (User2780) result.get(0);
        UserContainer2780 resultContainer = (UserContainer2780) result.get(1);

        assertEquals(42, resultUser.id);
        assertEquals("cool_man", resultUser.login);
        assertSame(resultUser, resultContainer.user,
            "Back-reference in container should point to the same User2780 instance");
    }

    // [databind#2780]: Container appears first, bare id back-reference must resolve with type info
    @Test
    public void testContainerFirstThenUser2780() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
            .activateDefaultTyping(NoCheckSubTypeValidator.instance, DefaultTyping.NON_FINAL)
            .build();

        User2780 user = new User2780(42, "cool_man");
        UserContainer2780 container = new UserContainer2780(1, user);

        List<Object> list = new ArrayList<>();
        list.add(container);
        list.add(user);

        String json = mapper.writeValueAsString(list);
        List<?> result = mapper.readValue(json, List.class);

        assertEquals(2, result.size());
        assertInstanceOf(UserContainer2780.class, result.get(0),
            "First element should be UserContainer2780");
        assertInstanceOf(User2780.class, result.get(1),
            "Second element should be User2780 (not Integer) -- bare id back-reference must be resolved with type info");

        UserContainer2780 resultContainer = (UserContainer2780) result.get(0);
        User2780 resultUser = (User2780) result.get(1);

        assertEquals(42, resultUser.id);
        assertEquals("cool_man", resultUser.login);
        assertSame(resultUser, resultContainer.user,
            "Container's user field and the list's second element should be the same instance");
    }
}
