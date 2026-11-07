package tools.jackson.databind.objectid;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;
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

    // // // [databind#3185]: PropertyGenerator + @JsonCreator must not overwrite
    // // //                   final field after construction

    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    static class Pojo3185 {
        private final String fieldForId;

        @JsonCreator
        public Pojo3185(@JsonProperty("id") String fieldForId) {
            this.fieldForId = fieldForId + "-from-constructor";
        }

        @JsonGetter("id")
        public String getFieldForId() {
            return fieldForId;
        }
    }

    // // // [databind#3030]: Forward reference with @JsonCreator

    static class ContainerABC3030 {
        public List<RefTarget3030> bs;
        public List<RefSource3030> cs;
    }

    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    static class RefTarget3030 {
        public String id;
    }

    static class RefSource3030 {
        private RefTarget3030 b;

        @JsonCreator
        public RefSource3030(@JsonProperty("b") RefTarget3030 b) {
            this.b = b;
        }

        @JsonGetter("b")
        public RefTarget3030 getB() {
            return b;
        }
    }

    // // // [databind#1706]: @JsonCreator(mode=DELEGATING) with @JsonIdentityInfo

    @JsonSerialize(as = ImmutableItem1706.class)
    @JsonDeserialize(as = ImmutableItem1706.class)
    @JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class)
    interface Item1706 {
        Integer getId();
        String getName();
    }

    static class ImmutableItem1706 implements Item1706 {
        private final Integer id;
        private final String name;

        ImmutableItem1706(Integer id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public Integer getId() { return id; }

        @Override
        public String getName() { return name; }

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        static ImmutableItem1706 fromJson(MutableItem1706 json) {
            return new ImmutableItem1706(json.id, json.name);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ImmutableItem1706 that = (ImmutableItem1706) o;
            return id.equals(that.id) && name.equals(that.name);
        }

        @Override
        public int hashCode() {
            return 31 * id.hashCode() + name.hashCode();
        }
    }

    @JsonDeserialize
    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE)
    static class MutableItem1706 implements Item1706 {
        private String name;
        private Integer id;

        public void setId(Integer id) { this.id = id; }
        public void setName(String name) { this.name = name; }

        @Override
        public String getName() { throw new UnsupportedOperationException(); }

        @Override
        public Integer getId() { throw new UnsupportedOperationException(); }
    }

    // // // [databind#639]: ObjectId with @JacksonInject

    @JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class)
    public static final class InjectParent639 {
        @JsonProperty
        public InjectChild639 child;

        @JsonCreator
        public InjectParent639(@JacksonInject("context") String context) {
        }
    }

    @JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class)
    public static final class InjectChild639 {
        @JsonProperty
        private final InjectParent639 parent;

        @JsonCreator
        public InjectChild639(@JsonProperty("parent") InjectParent639 parent) {
            this.parent = parent;
        }
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

    /*
    /**********************************************************
    /* Unit tests, PropertyGenerator + @JsonCreator [databind#3185]
    /**********************************************************
     */

    // [databind#3185]: value computed in creator must be preserved; id field
    // must NOT be written to after construction (breaks final fields + GraalVM
    // native image). Fixed as a side-effect of [databind#5238].
    @Test
    public void testCreatorValuePreservedWithIdentityInfo3185() throws Exception
    {
        Pojo3185 result = MAPPER.readValue(
                a2q("{'id': 'valueFromJson'}"), Pojo3185.class);
        assertEquals("valueFromJson-from-constructor", result.getFieldForId());
    }

    /*
    /**********************************************************
    /* Unit tests, Forward reference with @JsonCreator [databind#3030]
    /**********************************************************
     */

    // No forward reference: Bs comes before Cs in JSON
    @Test
    public void testNoForwardReferenceWithCreator3030() throws Exception
    {
        String json = "{\"bs\":[{\"id\":\"b1\"},{\"id\":\"b2\"}],\"cs\":[{\"b\":\"b1\"},{\"b\":\"b2\"}]}";

        ContainerABC3030 result = MAPPER.readValue(json, ContainerABC3030.class);

        assertNotNull(result);
        assertEquals(2, result.bs.size());
        assertEquals(2, result.cs.size());
        assertEquals("b1", result.bs.get(0).id);
        assertEquals("b2", result.bs.get(1).id);
        assertSame(result.bs.get(0), result.cs.get(0).getB());
        assertSame(result.bs.get(1), result.cs.get(1).getB());
    }

    // [databind#3030] Forward reference WITH @JsonCreator: cs comes before bs
    @Test
    public void testForwardReferenceWithCreator3030() throws Exception
    {
        String json = "{\"cs\":[{\"b\":\"b1\"},{\"b\":\"b2\"}],\"bs\":[{\"id\":\"b1\"},{\"id\":\"b2\"}]}";

        ContainerABC3030 result = MAPPER.readValue(json, ContainerABC3030.class);

        assertNotNull(result);
        assertEquals(2, result.bs.size());
        assertEquals(2, result.cs.size());
        assertSame(result.bs.get(0), result.cs.get(0).getB());
        assertSame(result.bs.get(1), result.cs.get(1).getB());
    }

    /*
    /**********************************************************
    /* Unit tests, @JsonCreator(mode=DELEGATING) [databind#1706]
    /**********************************************************
     */

    // [databind#1706]
    @Test
    public void testObjectIdWithDelegatingCreator() throws Exception
    {
        ImmutableItem1706 item = new ImmutableItem1706(1, "test");

        String json = MAPPER.writeValueAsString(Arrays.asList(item, item));

        List<Item1706> result = MAPPER.readValue(json, new TypeReference<List<Item1706>>() {});

        assertEquals(2, result.size());
        assertInstanceOf(ImmutableItem1706.class, result.get(0),
                "First item should be ImmutableItem1706");
        assertInstanceOf(ImmutableItem1706.class, result.get(1),
                "Second item (back-reference) should be ImmutableItem1706, not intermediate MutableItem1706");
        assertEquals(result.get(0), result.get(1));
    }

    /*
    /**********************************************************
    /* Unit tests, ObjectId with @JacksonInject [databind#639]
    /**********************************************************
     */

    // [databind#639]
    @Test
    public void testObjectIdWithInjectable639() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .injectableValues(new InjectableValues.Std().
                        addValue("context", "Stuff"))
                .build();
        InjectParent639 parent = new InjectParent639("foo");
        InjectChild639 child = new InjectChild639(parent);
        parent.child = child;

        String json = mapper.writeValueAsString(parent);
        parent = mapper.readValue(json, InjectParent639.class);
        assertNotNull(parent);
        assertNotNull(parent.child);
    }
}
