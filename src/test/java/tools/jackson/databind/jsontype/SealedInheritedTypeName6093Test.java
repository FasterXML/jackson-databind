package tools.jackson.databind.jsontype;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SealedInheritedTypeName6093Test {
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes({@JsonSubTypes.Type(Action.class), @JsonSubTypes.Type(ExplicitAction.class)})
    sealed interface Settings permits Action, ExplicitAction { }

    @JsonTypeName("action")
    sealed interface Action extends Settings permits OpenWindow {
        @JsonProperty("actionName")
        String actionName();

        @JsonCreator
        static Action create(@JsonProperty("actionName") String name) {
            return new OpenWindow(name);
        }
    }

    record OpenWindow(String actionName) implements Action { }

    @JsonTypeName("explicit")
    record ExplicitAction(String actionName) implements Settings { }

    record Document(Settings settings) { }

    private final ObjectMapper mapper = JsonMapper.builder().build();

    @Test
    void typedWriterUsesInheritedName() {
        String json = mapper.writerFor(Settings.class).writeValueAsString(new OpenWindow("example"));
        assertEquals("action", mapper.readTree(json).get("type").asString());
        assertEquals(new OpenWindow("example"), mapper.readValue(json, Settings.class));
    }

    @Test
    void propertyUsesInheritedName() {
        String json = mapper.writeValueAsString(new Document(new OpenWindow("example")));
        assertEquals("action", mapper.readTree(json).get("settings").get("type").asString());
        assertEquals(new Document(new OpenWindow("example")), mapper.readValue(json, Document.class));
    }

    @Test
    void dynamicWriterStillUsesInheritedName() {
        String json = mapper.writeValueAsString(new OpenWindow("example"));
        assertEquals("action", mapper.readTree(json).get("type").asString());
    }

    @Test
    void directTypeNameTakesPrecedence() {
        String json = mapper.writerFor(Settings.class).writeValueAsString(new ExplicitAction("example"));
        assertEquals("explicit", mapper.readTree(json).get("type").asString());
        assertEquals(new ExplicitAction("example"), mapper.readValue(json, Settings.class));
    }
    @JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME, property = "type")
    abstract static class SimpleNameMixin { }

    @Test
    void simpleNameWriterUsesInheritedName() {
        ObjectMapper simple = JsonMapper.builder().addMixIn(Settings.class, SimpleNameMixin.class).build();
        String json = simple.writerFor(Settings.class).writeValueAsString(new OpenWindow("example"));
        assertEquals("action", simple.readTree(json).get("type").asString());
    }

    @Test
    void simpleNameWriterRetainsDirectName() {
        ObjectMapper simple = JsonMapper.builder().addMixIn(Settings.class, SimpleNameMixin.class).build();
        String json = simple.writerFor(Settings.class).writeValueAsString(new ExplicitAction("example"));
        assertEquals("explicit", simple.readTree(json).get("type").asString());
    }


    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes({@JsonSubTypes.Type(Dog.class), @JsonSubTypes.Type(Beagle.class)})
    abstract static class Animal { }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    sealed interface NoFactoryRoot permits NoFactoryParent { }

    @JsonTypeName("noFactory")
    sealed interface NoFactoryParent extends NoFactoryRoot permits NoFactoryLeaf { }

    record NoFactoryLeaf(int value) implements NoFactoryParent { }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    sealed interface SiblingRoot permits SiblingParent { }

    @JsonTypeName("siblings")
    sealed interface SiblingParent extends SiblingRoot permits FirstLeaf, SecondLeaf {
        @JsonCreator
        static SiblingParent create(@JsonProperty("value") int value) {
            return new FirstLeaf(value);
        }
    }

    record FirstLeaf(int value) implements SiblingParent { }

    record SecondLeaf(int value) implements SiblingParent { }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    sealed interface MixedRoot permits MixedParent { }

    @JsonTypeName("mixed")
    sealed interface MixedParent extends MixedRoot permits UnnamedLeaf, NamedLeaf {
        @JsonCreator
        static MixedParent create(@JsonProperty("value") int value) {
            return new NamedLeaf(value);
        }
    }

    record UnnamedLeaf(int value) implements MixedParent { }

    @JsonTypeName("named")
    record NamedLeaf(int value) implements MixedParent { }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    sealed interface CollisionRoot permits CollisionParent, CollisionSibling { }

    @JsonTypeName("SealedInheritedTypeName6093Test$CollisionSibling")
    sealed interface CollisionParent extends CollisionRoot permits CollisionLeaf {
        @JsonCreator
        static CollisionParent create(@JsonProperty("value") int value) {
            return new CollisionLeaf(value);
        }
    }

    @JsonTypeName("CollisionSibling")
    abstract static class SimpleCollisionNameMixin { }

    record CollisionLeaf(int value) implements CollisionParent { }

    record CollisionSibling(int value) implements CollisionRoot { }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    sealed interface DelegatingRoot permits DelegatingParent { }

    @JsonTypeName("delegating")
    sealed interface DelegatingParent extends DelegatingRoot permits DelegatingLeaf {
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        static DelegatingLeaf create(String value) {
            return new DelegatingLeaf(Integer.parseInt(value));
        }
    }

    record DelegatingLeaf(int value) implements DelegatingParent { }

    @Test
    void inheritedNameDoesNotReplaceDistinctLeafIds() {
        assertLeafRoundTrip(mapper, NoFactoryRoot.class, new NoFactoryLeaf(1),
                "SealedInheritedTypeName6093Test$NoFactoryLeaf");
        assertLeafRoundTrip(mapper, SiblingRoot.class, new FirstLeaf(2),
                "SealedInheritedTypeName6093Test$FirstLeaf");
        assertLeafRoundTrip(mapper, SiblingRoot.class, new SecondLeaf(3),
                "SealedInheritedTypeName6093Test$SecondLeaf");
        assertLeafRoundTrip(mapper, MixedRoot.class, new UnnamedLeaf(4),
                "SealedInheritedTypeName6093Test$UnnamedLeaf");
        assertLeafRoundTrip(mapper, MixedRoot.class, new NamedLeaf(5), "named");
        assertLeafRoundTrip(mapper, CollisionRoot.class, new CollisionLeaf(6),
                "SealedInheritedTypeName6093Test$CollisionLeaf");
        assertLeafRoundTrip(mapper, DelegatingRoot.class, new DelegatingLeaf(7),
                "SealedInheritedTypeName6093Test$DelegatingLeaf");
    }

    @Test
    void simpleInheritedNameDoesNotReplaceDistinctLeafIds() {
        ObjectMapper simple = JsonMapper.builder()
                .addMixIn(NoFactoryRoot.class, SimpleNameMixin.class)
                .addMixIn(SiblingRoot.class, SimpleNameMixin.class)
                .addMixIn(MixedRoot.class, SimpleNameMixin.class)
                .addMixIn(CollisionRoot.class, SimpleNameMixin.class)
                .addMixIn(CollisionParent.class, SimpleCollisionNameMixin.class)
                .addMixIn(DelegatingRoot.class, SimpleNameMixin.class)
                .build();
        assertLeafRoundTrip(simple, NoFactoryRoot.class, new NoFactoryLeaf(1), "NoFactoryLeaf");
        assertLeafRoundTrip(simple, SiblingRoot.class, new FirstLeaf(2), "FirstLeaf");
        assertLeafRoundTrip(simple, SiblingRoot.class, new SecondLeaf(3), "SecondLeaf");
        assertLeafRoundTrip(simple, MixedRoot.class, new UnnamedLeaf(4), "UnnamedLeaf");
        assertLeafRoundTrip(simple, MixedRoot.class, new NamedLeaf(5), "named");
        assertLeafRoundTrip(simple, CollisionRoot.class, new CollisionLeaf(6), "CollisionLeaf");
        assertLeafRoundTrip(simple, DelegatingRoot.class, new DelegatingLeaf(7), "DelegatingLeaf");
    }

    private void assertLeafRoundTrip(ObjectMapper jsonMapper, Class<?> root, Object leaf, String id) {
        String json = jsonMapper.writerFor(root).writeValueAsString(leaf);
        assertEquals(id, jsonMapper.readTree(json).get("type").asString());
        assertEquals(leaf, jsonMapper.readValue(json, root));
    }

    @JsonTypeName("dog")
    static class Dog extends Animal {
        public String name = "sample";
    }

    static class Beagle extends Dog { }

    @Test
    void concreteParentNameDoesNotReplaceListedSubtypeName() {
        String json = mapper.writerFor(Animal.class).writeValueAsString(new Beagle());
        assertEquals("SealedInheritedTypeName6093Test$Beagle", mapper.readTree(json).get("type").asString());
        assertEquals(Beagle.class, mapper.readValue(json, Animal.class).getClass());
    }

    @Test
    void simpleConcreteParentNameDoesNotReplaceListedSubtypeName() {
        ObjectMapper simple = JsonMapper.builder().addMixIn(Animal.class, SimpleNameMixin.class).build();
        String json = simple.writerFor(Animal.class).writeValueAsString(new Beagle());
        assertEquals("Beagle", simple.readTree(json).get("type").asString());
        assertEquals(Beagle.class, simple.readValue(json, Animal.class).getClass());
    }
}
