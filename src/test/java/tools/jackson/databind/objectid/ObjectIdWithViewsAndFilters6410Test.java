package tools.jackson.databind.objectid;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.*;
import tools.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import tools.jackson.databind.jsonFormatVisitors.JsonObjectFormatVisitor;
import tools.jackson.databind.ser.std.SimpleBeanPropertyFilter;
import tools.jackson.databind.ser.std.SimpleFilterProvider;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests to verify that Object Id property (of {@code PropertyGenerator}) is
 * considered metadata, not affected by {@code @JsonView} or {@code @JsonFilter}.
 */
public class ObjectIdWithViewsAndFilters6410Test extends DatabindTestUtil
{
    static class Public {}
    static class Internal {}

    // Object Id property with a View that excludes it
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    @JsonPropertyOrder({ "id", "email", "other" })
    static class Account {
        @JsonView(Internal.class) public int id;
        @JsonView(Public.class) public String email;
        @JsonView(Public.class) public Account other;
    }

    // Object Id property with no View (excluded by default in 3.x)
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    @JsonPropertyOrder({ "id", "email", "other" })
    static class NoViewId {
        public int id;
        @JsonView(Public.class) public String email;
        @JsonView(Public.class) public NoViewId other;
    }

    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    @JsonPropertyOrder({ "id", "email", "other" })
    static class CreatorAccount {
        @JsonView(Internal.class) public final int id;
        @JsonView(Public.class) public String email;
        @JsonView(Public.class) public CreatorAccount other;

        @JsonCreator
        CreatorAccount(@JsonProperty("id") int id) { this.id = id; }
    }

    @JsonFilter("idFilter")
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    @JsonPropertyOrder({ "id", "email", "secret", "other" })
    static class FilteredAccount {
        public int id;
        public String email;
        public String secret;
        public FilteredAccount other;
    }

    // No class-level Object Id: defined via property-level annotation
    @JsonPropertyOrder({ "name", "id" })
    static class NodeIdLast {
        @JsonView(Public.class) public String name;
        @JsonView(Internal.class) public int id;
    }

    @JsonPropertyOrder({ "id", "name" })
    static class NodeIdFirst {
        @JsonView(Internal.class) public int id;
        @JsonView(Public.class) public String name;
    }

    @JsonPropertyOrder({ "first", "second" })
    static class NodeHolder<T> {
        @JsonView(Public.class)
        @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
        public T first;

        @JsonView(Public.class)
        @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
        public T second;
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    /*
    /**********************************************************************
    /* Serialization, Views
    /**********************************************************************
     */

    @Test
    void writeIdExcludedByView() throws Exception
    {
        Account a = new Account();
        a.id = 1;
        a.email = "a@x";
        a.other = a;

        assertEquals("""
                {"id":1,"email":"a@x","other":1}""",
                MAPPER.writerWithView(Public.class).writeValueAsString(a));
        // and with other view, id still written but not other props
        assertEquals("""
                {"id":1}""",
                MAPPER.writerWithView(Internal.class).writeValueAsString(a));
    }

    @Test
    void writeIdWithoutViewRoundTrip() throws Exception
    {
        NoViewId n = new NoViewId();
        n.id = 2;
        n.email = "n@x";
        n.other = n;

        // 3.x defaults to `DEFAULT_VIEW_INCLUSION` disabled: id must still be written
        assertFalse(MAPPER.isEnabled(MapperFeature.DEFAULT_VIEW_INCLUSION));
        final String json = MAPPER.writerWithView(Public.class).writeValueAsString(n);
        assertEquals("""
                {"id":2,"email":"n@x","other":2}""", json);

        // and must be readable (previously failed with unresolved forward reference)
        NoViewId result = MAPPER.readerWithView(Public.class)
                .forType(NoViewId.class)
                .readValue(json);
        assertEquals(2, result.id);
        assertEquals("n@x", result.email);
        assertSame(result, result.other);
    }

    // Object Id defined using property-level annotation (contextual path)
    @Test
    void writePropertyLevelIdExcludedByView() throws Exception
    {
        // id property not first: gets reordered
        NodeIdLast last = new NodeIdLast();
        last.id = 3;
        last.name = "last";
        NodeHolder<NodeIdLast> h1 = new NodeHolder<>();
        h1.first = last;
        h1.second = last;
        assertEquals("""
                {"first":{"id":3,"name":"last"},"second":3}""",
                MAPPER.writerWithView(Public.class).writeValueAsString(h1));

        // id property already first: no reordering
        NodeIdFirst first = new NodeIdFirst();
        first.id = 4;
        first.name = "first";
        NodeHolder<NodeIdFirst> h2 = new NodeHolder<>();
        h2.first = first;
        h2.second = first;
        assertEquals("""
                {"first":{"id":4,"name":"first"},"second":4}""",
                MAPPER.writerWithView(Public.class).writeValueAsString(h2));
    }

    /*
    /**********************************************************************
    /* Serialization, @JsonFilter
    /**********************************************************************
     */

    @Test
    void writeIdExcludedByFilter() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .filterProvider(new SimpleFilterProvider().addFilter("idFilter",
                        SimpleBeanPropertyFilter.serializeAllExcept("id", "secret")))
                .build();
        FilteredAccount a = new FilteredAccount();
        a.id = 5;
        a.email = "f@x";
        a.secret = "shh";
        a.other = a;

        final String json = mapper.writeValueAsString(a);
        assertEquals("""
                {"id":5,"email":"f@x","other":5}""", json);
        FilteredAccount result = mapper.readValue(json, FilteredAccount.class);
        assertEquals(5, result.id);
        assertNull(result.secret);
        assertSame(result, result.other);

        // and same with "include only" filter not listing id
        mapper = jsonMapperBuilder()
                .filterProvider(new SimpleFilterProvider().addFilter("idFilter",
                        SimpleBeanPropertyFilter.filterOutAllExcept("email")))
                .build();
        assertEquals("""
                {"id":5,"email":"f@x"}""", mapper.writeValueAsString(a));
    }

    @Test
    void schemaIdExcludedByFilter() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .filterProvider(new SimpleFilterProvider().addFilter("idFilter",
                        SimpleBeanPropertyFilter.serializeAllExcept("id", "secret")))
                .build();
        final List<String> names = new ArrayList<>();
        mapper.acceptJsonFormatVisitor(FilteredAccount.class, new JsonFormatVisitorWrapper.Base() {
            @Override
            public JsonObjectFormatVisitor expectObjectFormat(JavaType type) {
                return new JsonObjectFormatVisitor.Base(getContext()) {
                    @Override
                    public void property(BeanProperty prop) {
                        names.add(prop.getName());
                    }

                    @Override
                    public void optionalProperty(BeanProperty prop) {
                        names.add(prop.getName());
                    }
                };
            }
        });
        assertEquals(Arrays.asList("id", "email", "other"), names);
    }

    /*
    /**********************************************************************
    /* Deserialization, Views
    /**********************************************************************
     */

    @Test
    void readIdExcludedByView() throws Exception
    {
        final String json = """
                {"id":7,"email":"r@x","other":7}""";
        ObjectReader r = MAPPER.readerWithView(Public.class)
                .with(DeserializationFeature.FAIL_ON_UNEXPECTED_VIEW_PROPERTIES)
                .forType(Account.class);
        Account a = r.readValue(json);
        assertEquals(7, a.id);
        assertEquals("r@x", a.email);
        assertSame(a, a.other);
    }

    @Test
    void readCreatorIdExcludedByView() throws Exception
    {
        final String json = """
                {"id":8,"email":"c@x","other":8}""";
        // Previously id was not passed to Creator (or failed, with
        // `FAIL_ON_UNEXPECTED_VIEW_PROPERTIES`), although Object Id was bound
        for (boolean failOnUnexpected : new boolean[] { false, true }) {
            ObjectReader r = MAPPER.readerWithView(Public.class)
                    .with(DeserializationFeature.FAIL_ON_UNEXPECTED_VIEW_PROPERTIES)
                    .forType(CreatorAccount.class);
            if (!failOnUnexpected) {
                r = r.without(DeserializationFeature.FAIL_ON_UNEXPECTED_VIEW_PROPERTIES);
            }
            CreatorAccount a = r.readValue(json);
            assertEquals(8, a.id);
            assertEquals("c@x", a.email);
            assertSame(a, a.other);
        }
    }
}
