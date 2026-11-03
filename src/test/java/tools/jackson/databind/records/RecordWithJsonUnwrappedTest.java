package tools.jackson.databind.records;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

import tools.jackson.databind.ObjectMapper;
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

    private final ObjectMapper MAPPER = newJsonMapper();

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
}
