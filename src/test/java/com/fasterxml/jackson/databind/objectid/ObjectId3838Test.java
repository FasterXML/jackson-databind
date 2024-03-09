package com.fasterxml.jackson.databind.objectid;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

// [databind#3838]: Difference in the handling of ObjectId-property in JsonIdentityInfo depending on the deserialization route.
public class ObjectId3838Test extends DatabindTestUtil
{
    interface ResultGetter {
        String result();
    }

    @JsonIdentityInfo(property = "id", generator = ObjectIdGenerators.PropertyGenerator.class)
    static class SetterBased implements ResultGetter {
        private String id;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        @Override
        public String result() {
            return id;
        }
    }

    @JsonIdentityInfo(property = "id", generator = ObjectIdGenerators.PropertyGenerator.class)
    static class CreatorBased implements ResultGetter {
        private final String id;

        @JsonCreator
        CreatorBased(@JsonProperty(value = "id") String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }

        @Override
        public String result() {
            return id;
        }
    }

    @JsonIdentityInfo(property = "id", generator = ObjectIdGenerators.PropertyGenerator.class)
    static class DefaultConstructorBased implements ResultGetter {
        public String id;

        @Override
        public String result() {
            return id;
        }
    }

    @JsonIdentityInfo(property = "id", generator = ObjectIdGenerators.PropertyGenerator.class)
    static class StaticFactoryMethodBased implements ResultGetter {
        private final String id;

        private StaticFactoryMethodBased(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }

        @JsonCreator
        public static StaticFactoryMethodBased create(@JsonProperty("id") String id) {
            return new StaticFactoryMethodBased(id);
        }

        @Override
        public String result() {
            return id;
        }
    }

    @JsonIdentityInfo(property = "id", generator = ObjectIdGenerators.PropertyGenerator.class)
    static class MultiArgConstructorBased implements ResultGetter {
        private final String id;
        private final int value;

        @JsonCreator
        MultiArgConstructorBased(@JsonProperty("id") String id, @JsonProperty("value") int value) {
            this.id = id;
            this.value = value;
        }

        public String getId() {
            return id;
        }

        public int getValue() {
            return value;
        }

        @Override
        public String result() {
            return id;
        }
    }

    @JsonIdentityInfo(
        generator = ObjectIdGenerators.PropertyGenerator.class,
        property = "id"
    )
    @JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.WRAPPER_OBJECT,
        property = "type"
    )
    @JsonSubTypes({
        @JsonSubTypes.Type(value = Concrete3838.class, name = "concrete_3838")
    })
    static class BaseType3838 implements ResultGetter {
        public String id;

        @Override
        public String result() {
            return id;
        }
    }

    @JsonTypeName("concrete_3838")
    static class Concrete3838 extends BaseType3838 {
        public String location;

        protected Concrete3838() {}

        public Concrete3838(String id, String loc) {
            this.id = id;
            location = loc;
        }
    }

    @JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "id")
    static class IntSequencedBean implements ResultGetter{
        public String value;

        @Override
        public String result() {
            return value;
        }
    }
    
    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
    */

    private final ObjectMapper MAPPER = newJsonMapper();

    final static Object[][] CLASS_AND_JSON_STRING = new Object[][]{
        {SetterBased.class, "{'id':'great'}"},
        {CreatorBased.class, "{'id':'great'}"},
        {DefaultConstructorBased.class, "{'id':'great'}"},
        {StaticFactoryMethodBased.class, "{'id':'great'}"},
        {MultiArgConstructorBased.class, "{'id':'great','value':42}"},
        {BaseType3838.class, "{'concrete_3838':{'id':'great','location':'Bangkok'}}"},
        {IntSequencedBean.class, "{'id':-1,'value':'great'}"}
    };

    @Test
    public void testUniformHandlingForMissingObjectId() throws Exception
    {
        for (Object[] classAndJsonStrEntry : CLASS_AND_JSON_STRING) {
            final Class<?> cls = (Class<?>) classAndJsonStrEntry[0];
            final String jsonStr = (String) classAndJsonStrEntry[1];

            // 1. throws MismatchedInputException with empty JSON object
            try {
                MAPPER.readValue("{}", cls);
                fail("should not pass");
            } catch (MismatchedInputException e) {
                verifyException(e,
                    "No Object Id found for an instance of", "to",
                    "to assign to property 'id'");
            }

            // 2. but works with non-empty JSON object
            ResultGetter nonEmptyObj = (ResultGetter) MAPPER.readValue(a2q(jsonStr), cls);
            assertEquals("great", nonEmptyObj.result());
        }
    }
}
