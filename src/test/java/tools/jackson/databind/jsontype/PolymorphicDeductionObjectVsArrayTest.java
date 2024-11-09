package tools.jackson.databind.jsontype;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

public class PolymorphicDeductionObjectVsArrayTest extends DatabindTestUtil
{
    @JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION, defaultImpl = DataArray.class)
    @JsonSubTypes({@JsonSubTypes.Type(DataObject.class), @JsonSubTypes.Type(DataArray.class)})
    interface Data {
        @JsonIgnore
        boolean isObject();
    }

    static class DataItem {
        final String id;

        @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
        DataItem(@JsonProperty("id") String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }

    static class DataObject extends DataItem implements Data {

        @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
        DataObject(@JsonProperty("id") String id) {
            super(id);
        }

        @Override
        public boolean isObject() {
            return true;
        }
    }

    static class DataArray extends ArrayList<DataItem> implements Data {
        private static final long serialVersionUID = 1L;

        @JsonCreator
        DataArray(Collection<DataItem> items) {
            super(new ArrayList<>(items));
        }

        @Override
        public boolean isObject() {
            return false;
        }
    }

    static class Container {
        @JsonProperty("data")
        Data data;
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    private static final String containerWithObjectData = a2q("{'data':{'id':'#1'}}");

    private static final String containerWithArrayData = a2q("{'data':[{'id':'#1'}]}");

    @Test
    public void testDeserialization() throws Exception {
        Container container = MAPPER.readValue(containerWithObjectData, Container.class);

        assertInstanceOf(DataObject.class, container.data);
        assertSame(container.data.getClass(), DataObject.class);
        assertTrue(container.data.isObject());
        assertEquals("#1", ((DataItem) container.data).id);

        container = MAPPER.readValue(containerWithArrayData, Container.class);
        assertInstanceOf(DataArray.class, container.data);
        assertEquals(container.data.getClass(), DataArray.class);
        assertFalse(container.data.isObject());

        @SuppressWarnings("unchecked")
        Iterator<DataItem> arrayDataIterator = ((Iterable<DataItem>) container.data).iterator();

        assertTrue(arrayDataIterator.hasNext());
        assertEquals("#1", arrayDataIterator.next().id);
    }

    @Test
    public void testSerialization() throws Exception {
        Container container = new Container();
        container.data = new DataObject("#1");
        String json = MAPPER.writeValueAsString(container);
        assertEquals(containerWithObjectData, json);

        container = new Container();
        container.data = new DataArray(Arrays.asList(new DataItem("#1")));
        json = MAPPER.writeValueAsString(container);
        assertEquals(containerWithArrayData, json);
    }
}
