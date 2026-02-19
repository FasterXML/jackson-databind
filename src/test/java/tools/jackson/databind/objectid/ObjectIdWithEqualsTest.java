package tools.jackson.databind.objectid;

import java.net.URI;
import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

public class ObjectIdWithEqualsTest extends DatabindTestUtil
{
    @JsonIdentityInfo(generator=ObjectIdGenerators.PropertyGenerator.class, property="id", scope=Foo.class)
    @JsonPropertyOrder({ "id", "bars", "otherBars" })
    static class Foo {
        public int id;

        public List<Bar> bars = new ArrayList<>();
        public List<Bar> otherBars = new ArrayList<>();

        public Foo() { }
        public Foo(int i) { id = i; }
    }

    @JsonIdentityInfo(generator=ObjectIdGenerators.PropertyGenerator.class, property="id", scope=Bar.class)
    static class Bar
    {
        public int id;

        public Bar() { }
        public Bar(int i) { id = i; }

        @Override
        public int hashCode() { return id; }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Bar)) {
                return false;
            }
            return ((Bar) obj).id == id;
        }
    }

    // for [databind#1002]
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "uri")
    static class Element {
        public URI uri;
        public String name;

        @Override
        public boolean equals(Object object) {
            if (object == this) {
                return true;
            } else if (object == null || !(object instanceof Element)) {
                return false;
            } else {
                Element element = (Element) object;
                if (element.uri.toString().equalsIgnoreCase(this.uri.toString())) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public int hashCode() { return uri.hashCode(); }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    @Test
    public void testSimpleEquals() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .enable(SerializationFeature.USE_EQUALITY_FOR_OBJECT_ID)
                .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
                .build();

        Foo foo = new Foo(1);

        Bar bar1 = new Bar(1);
        Bar bar2 = new Bar(2);
        // this anotherBar1 is "equal" to bar1 due to same ID and Bar.equals()
        Bar anotherBar1 = new Bar(1);

        foo.bars.add(bar1);
        foo.bars.add(bar2);
        foo.otherBars.add(anotherBar1);
        foo.otherBars.add(bar2);

        String json = mapper.writeValueAsString(foo);
        assertEquals("{\"id\":1,\"bars\":[{\"id\":1},{\"id\":2}],\"otherBars\":[1,2]}", json);
        Foo foo2 = mapper.readValue(json, Foo.class);
        assertNotNull(foo2);
        assertEquals(foo.id, foo2.id);
    }

    @Test
    public void testEqualObjectIdsExternal() throws Exception
    {
        Element element = new Element();
        element.uri = URI.create("URI");
        element.name = "Element1";

        Element element2 = new Element();
        element2.uri = URI.create("URI");
        element2.name = "Element2";

        List<Element> input = Arrays.asList(element, element2);

        ObjectMapper mapper = jsonMapperBuilder()
                .enable(SerializationFeature.USE_EQUALITY_FOR_OBJECT_ID)
                .build();

        String json = mapper.writerFor(new TypeReference<List<Element>>() { })
                .writeValueAsString(input);

        Element[] output = mapper.readValue(json, Element[].class);
        assertNotNull(output);
        assertEquals(2, output.length);
    }
}
