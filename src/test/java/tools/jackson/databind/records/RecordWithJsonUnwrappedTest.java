package tools.jackson.databind.records;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

public class RecordWithJsonUnwrappedTest extends DatabindTestUtil
{
    record RecordWithJsonUnwrapped(String unrelated, @JsonUnwrapped Inner inner) {
    }

    record Inner(String property1, String property2) {
    }

    // [databind#5115]
    record FooRecord5115(int a, int b) { }
    record BarRecordFail5115(@JsonUnwrapped FooRecord5115 a, int c) { }
    record BarRecordPass5115(@JsonUnwrapped FooRecord5115 foo, int c) { }

    static class FooPojo5115 {
        public int a;
        public int b;
    }

    static class BarPojo5115 {
        @JsonUnwrapped
        public FooPojo5115 a;
        public int c;
    }

    // [databind#5716]
    record Name5716(String first, String last) { }
    record Row5716(long time, @JsonUnwrapped Name5716 name, double score) { }

    // [databind#3178]
    record Location3178(int x, int y) { }
    record Inner3178(String name, Location3178 location) { }
    record WithPrefix3178(@JsonUnwrapped(prefix = "_") Inner3178 unwrapped) { }

    private final ObjectMapper MAPPER = newJsonMapper();

    private final ObjectMapper MAPPER_5716 = JsonMapper.builder()
            .disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
            .enable(MapperFeature.SORT_CREATOR_PROPERTIES_FIRST)
            .build();

    /*
    /**********************************************************************
    /* Test methods, basic @JsonUnwrapped
    /**********************************************************************
     */

    @Test
    public void testUnwrappedWithRecord() throws Exception
    {
        RecordWithJsonUnwrapped initial = new RecordWithJsonUnwrapped("unrelatedValue", new Inner("value1", "value2"));

        ObjectNode tree = MAPPER.valueToTree(initial);

        assertEquals("unrelatedValue", tree.get("unrelated").stringValue());
        assertEquals("value1", tree.get("property1").stringValue());
        assertEquals("value2", tree.get("property2").stringValue());

        RecordWithJsonUnwrapped outer = MAPPER.treeToValue(tree, RecordWithJsonUnwrapped.class);

        assertEquals("unrelatedValue", outer.unrelated());
        assertEquals("value1", outer.inner().property1());
        assertEquals("value2", outer.inner().property2());
    }

    /*
    /**********************************************************************
    /* Test methods, name collision handling [databind#5115]
    /**********************************************************************
     */

    // [databind#5115]
    @Test
    void unwrappedPojoShouldRoundTrip() throws Exception
    {
        BarPojo5115 input  = new BarPojo5115();
        input.a = new FooPojo5115();
        input.c = 4;
        input.a.a = 1;
        input.a.b = 2;

        String json = MAPPER.writeValueAsString(input);
        BarPojo5115 output = MAPPER.readValue(json, BarPojo5115.class);

        assertEquals(4, output.c);
        assertEquals(1, output.a.a);
        assertEquals(2, output.a.b);
    }

    @Test
    void unwrappedRecordShouldRoundTripPass() throws Exception
    {
        BarRecordPass5115 input = new BarRecordPass5115(new FooRecord5115(1, 2), 3);

        String json = MAPPER.writeValueAsString(input);
        BarRecordPass5115 output = MAPPER.readValue(json, BarRecordPass5115.class);

        assertEquals(input, output);
    }

    @Test
    void unwrappedRecordShouldRoundTrip() throws Exception
    {
        BarRecordFail5115 input = new BarRecordFail5115(new FooRecord5115(1, 2), 3);

        String json = MAPPER.writeValueAsString(input);
        BarRecordFail5115 output = MAPPER.readValue(json, BarRecordFail5115.class);

        assertEquals(input, output);
    }

    /*
    /**********************************************************************
    /* Test methods, declaration order [databind#5716]
    /**********************************************************************
     */

    // [databind#5716] @JsonUnwrapped record properties should stay at declaration position
    @Test
    void unwrappedRecordShouldKeepDeclarationOrder() throws Exception
    {
        Row5716 input = new Row5716(1L, new Name5716("a", "b"), 2.5d);

        String json = MAPPER_5716.writeValueAsString(input);

        assertEquals(a2q("{'time':1,'first':'a','last':'b','score':2.5}"), json);
    }

    /*
    /**********************************************************************
    /* Test methods, prefix [databind#3178]
    /**********************************************************************
     */

    // [databind#3178]: records variant — exercises PropertyBasedCreator rename path
    @Test
    public void testPrefixedUnwrappingWithRecord() throws Exception {
        WithPrefix3178 source = new WithPrefix3178(new Inner3178("Bubba", new Location3178(2, 3)));
        String json = MAPPER.writeValueAsString(source);
        assertEquals("{\"_name\":\"Bubba\",\"_location\":{\"x\":2,\"y\":3}}", json);
        WithPrefix3178 bean = MAPPER.readValue(json, WithPrefix3178.class);
        assertNotNull(bean.unwrapped());
        assertNotNull(bean.unwrapped().location());
        assertEquals(source.unwrapped().name(), bean.unwrapped().name());
        assertEquals(source.unwrapped().location().x(), bean.unwrapped().location().x());
        assertEquals(source.unwrapped().location().y(), bean.unwrapped().location().y());
    }
}
