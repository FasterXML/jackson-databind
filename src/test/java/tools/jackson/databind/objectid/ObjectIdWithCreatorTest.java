package tools.jackson.databind.objectid;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Object Id handling in conjunction with {@code @JsonCreator}.
 */
public class ObjectIdWithCreatorTest extends DatabindTestUtil
{
    // // // [databind#687]: PropertyGenerator + @JsonCreator

    @JsonIdentityInfo(generator=ObjectIdGenerators.PropertyGenerator.class, property="label")
    static class ReferredWithCreator {
        public String label;

        @JsonCreator
        ReferredWithCreator(@JsonProperty("label") String label) {
            this.label = label;
        }
    }

    @JsonIdentityInfo(generator=ObjectIdGenerators.PropertyGenerator.class, property="label")
    static class ReferringToObjWithCreator {
        public String label = "test1";
        public List<ReferredWithCreator> refs = new ArrayList<>();

        public void addRef(ReferredWithCreator r) { refs.add(r); }
    }

    @JsonIdentityInfo(generator=ObjectIdGenerators.PropertyGenerator.class, property="label")
    static class EnclosingForRefsWithCreator {
        public String label = "enclosing1";
        public ReferredWithCreator baseRef;
        public ReferringToObjWithCreator nextRef;
    }

    // // // [databind#687]: PropertyGenerator without @JsonCreator

    @JsonIdentityInfo(generator=ObjectIdGenerators.PropertyGenerator.class, property="label")
    static class ReferredWithNoCreator {
        public String label = "label2";
    }

    @JsonIdentityInfo(generator=ObjectIdGenerators.PropertyGenerator.class, property="label")
    static class ReferringToObjWithNoCreator {
        public String label = "test2";
        public List<ReferredWithNoCreator> refs = new ArrayList<>();

        public void addRef(ReferredWithNoCreator r) { refs.add(r); }
    }

    @JsonIdentityInfo(generator=ObjectIdGenerators.PropertyGenerator.class, property="label")
    static class EnclosingForRefWithNoCreator {
        public String label = "enclosing2";
        public ReferredWithNoCreator baseRef;
        public ReferringToObjWithNoCreator nextRef;
    }

    // // // [databind#1261]: IntSequenceGenerator + @JsonCreator with complex nesting

    static class Answer {
        public SortedMap<String, Parent> parents;

        public Answer() { parents = new TreeMap<>(); }
    }

    @JsonIdentityInfo(generator=ObjectIdGenerators.IntSequenceGenerator.class, property="@id")
    static class Parent {
        public Map<String, Child> children;

        @JsonIdentityReference(alwaysAsId=true)
        public Child favoriteChild;

        public String name;

        protected Parent() { }

        protected Parent(String name, boolean ignored) {
            children = new TreeMap<>();
            this.name = name;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIdentityInfo(generator=ObjectIdGenerators.IntSequenceGenerator.class, property="@id")
    static class Child {
        public String name;

        @JsonIdentityReference(alwaysAsId=true)
        public Parent parent;

        @JsonIdentityReference(alwaysAsId=true)
        public List<Parent> parentAsList;

        public String someNullProperty;

        protected Child() { }

        @JsonCreator
        public Child(@JsonProperty("name") String name,
                @JsonProperty("someNullProperty") String someNullProperty) {
            this.name = name;
            this.someNullProperty = someNullProperty;
        }

        public Child(String n) { name = n; }
    }

    // // // [databind#2944]: PropertyGenerator + @JsonCreator, verify setter not called

    @JsonIdentityInfo(generator=ObjectIdGenerators.PropertyGenerator.class, property="id")
    static class JsonBean2944 {
        String _id, _value;
        String _setterId;

        @JsonCreator
        public JsonBean2944(@JsonProperty("id") String id, @JsonProperty("value") String value) {
            _id = id;
            _value = value;
        }

        public void setId(String v) { _setterId = v; }
    }

    /*
    /**********************************************************
    /* Unit tests, PropertyGenerator + @JsonCreator [databind#687]
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testSerializeDeserializeWithCreator() throws Exception {
        ReferredWithCreator base = new ReferredWithCreator("label1");
        ReferringToObjWithCreator r = new ReferringToObjWithCreator();
        r.addRef(base);
        EnclosingForRefsWithCreator e = new EnclosingForRefsWithCreator();
        e.baseRef = base;
        e.nextRef = r;

        String json = MAPPER.writeValueAsString(e);

        EnclosingForRefsWithCreator result = MAPPER.readValue(json, EnclosingForRefsWithCreator.class);
        assertNotNull(result);
        assertEquals(result.label, e.label);
        // also, compare by re-serializing:
        assertEquals(json, MAPPER.writeValueAsString(result));
    }

    @Test
    public void testSerializeDeserializeNoCreator() throws Exception {
        ReferredWithNoCreator base = new ReferredWithNoCreator();
        ReferringToObjWithNoCreator r = new ReferringToObjWithNoCreator();
        r.addRef(base);
        EnclosingForRefWithNoCreator e = new EnclosingForRefWithNoCreator();
        e.baseRef = base;
        e.nextRef = r;

        String json = MAPPER.writeValueAsString(e);

        EnclosingForRefWithNoCreator result = MAPPER.readValue(json, EnclosingForRefWithNoCreator.class);
        assertNotNull(result);
        assertEquals(result.label, e.label);
        // also, compare by re-serializing:
        assertEquals(json, MAPPER.writeValueAsString(result));
    }

    /*
    /**********************************************************
    /* Unit tests, IntSequenceGenerator + @JsonCreator [databind#1261]
    /**********************************************************
     */

    @Test
    public void testObjectIds1261() throws Exception {
        ObjectMapper mapper = jsonMapperBuilder()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                .build();

        Answer initialAnswer = _createInitialAnswer();
        String initialAnswerString = mapper.writeValueAsString(initialAnswer);
        JsonNode tree = mapper.readTree(initialAnswerString);
        Answer deserializedAnswer = mapper.readValue(initialAnswerString, Answer.class);
        String reserializedAnswerString = mapper.writeValueAsString(deserializedAnswer);
        JsonNode newTree = mapper.readTree(reserializedAnswerString);
        if (!tree.equals(newTree)) {
            fail("Original and recovered Json are different. Recovered = \n"
                    + reserializedAnswerString + "\n");
        }
    }

    private Answer _createInitialAnswer() {
        Answer answer = new Answer();
        String child1Name = "child1";
        String child2Name = "child2";
        String parent1Name = "parent1";
        String parent2Name = "parent2";
        Parent parent1 = new Parent(parent1Name, false);
        answer.parents.put(parent1Name, parent1);
        Child child1 = new Child(child1Name);
        child1.parent = parent1;
        child1.parentAsList = Collections.singletonList(parent1);
        Child child2 = new Child(child2Name);
        Parent parent2 = new Parent(parent2Name, false);
        child2.parent = parent2;
        child2.parentAsList = Collections.singletonList(parent2);
        parent1.children.put(child1Name, child1);
        parent1.children.put(child2Name, child2);
        answer.parents.put(parent2Name, parent2);
        return answer;
    }

    /*
    /**********************************************************
    /* Unit tests, PropertyGenerator + @JsonCreator [databind#2944]
    /**********************************************************
     */

    // [databind#2944]
    @Test
    public void testObjectIdWithCreator() throws Exception {
        JsonBean2944 result = MAPPER.readValue(a2q("{'id': 'myId','value': 'myValue'}"),
                JsonBean2944.class);
        assertNotNull(result);
        assertEquals("myId", result._id,
            "Incorrect creator-passed-id (setter id: ["+result._setterId+"])");
    }
}
