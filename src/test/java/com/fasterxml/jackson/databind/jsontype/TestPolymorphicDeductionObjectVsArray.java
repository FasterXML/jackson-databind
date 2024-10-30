package com.fasterxml.jackson.databind.jsontype;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.json.WriterBasedJsonGenerator;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestPolymorphicDeductionObjectVsArray extends DatabindTestUtil {
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

    static class DataArray implements Data, Iterable<DataItem> {
        final ArrayList<DataItem> items;

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        DataArray(Collection<DataItem> items) {
            this.items = new ArrayList<>(items);
        }

        @Override
        public boolean isObject() {
            return false;
        }

        @Override
        public Iterator<DataItem> iterator() {
            return items.iterator();
        }
    }

    static class Container {
        @JsonProperty("data")
        Data data;
    }

    private final ObjectMapper MAPPER = new ObjectMapper(new TestJsonFactory());

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
        assertSame(container.data.getClass(), DataArray.class);
        assertFalse(container.data.isObject());

        var arrayDataIterator = ((Iterable<DataItem>) container.data).iterator();

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
        container.data = new DataArray(new ArrayList<>(List.of(new DataItem("#1"))));
        json = MAPPER.writeValueAsString(container);
        assertEquals(containerWithArrayData, json);
    }

    private static class TestJsonFactory extends JsonFactory {
        @Override
        protected JsonGenerator _createGenerator(Writer out, IOContext ctxt) throws IOException {
            FixedJsonGenerator gen = new FixedJsonGenerator(ctxt, _generatorFeatures, _objectCodec, out, _quoteChar);

            if (_maximumNonEscapedChar > 0) {
                gen.setHighestNonEscapedChar(_maximumNonEscapedChar);
            }

            if (_characterEscapes != null) {
                gen.setCharacterEscapes(_characterEscapes);
            }

            SerializableString rootSep = _rootValueSeparator;

            if (rootSep != DEFAULT_ROOT_VALUE_SEPARATOR) {
                gen.setRootValueSeparator(rootSep);
            }

            return _decorate(gen);
        }
    }

    private static class FixedJsonGenerator extends WriterBasedJsonGenerator {

        public FixedJsonGenerator(
                IOContext ctxt,
                int features,
                ObjectCodec codec,
                Writer w,
                char quoteChar) {
            super(ctxt, features, codec, w, quoteChar);
        }

        @Override
        public WritableTypeId writeTypePrefix(WritableTypeId typeIdDef) throws IOException {
            Object id = typeIdDef.id;

            final JsonToken valueShape = typeIdDef.valueShape;
            if (canWriteTypeId()) {
                typeIdDef.wrapperWritten = false;
                // just rely on native type output method (sub-classes likely to override)
                writeTypeId(id);
            } else {
                // No native type id; write wrappers
                // Normally we only support String type ids (non-String reserved for native type ids)
                String idStr = (id instanceof String) ? (String) id : Objects.toString(id, null);
                typeIdDef.wrapperWritten = true;

                WritableTypeId.Inclusion incl = typeIdDef.include;
                // first: can not output "as property" if value not Object; if so, must do "as array"
                if ((valueShape != JsonToken.START_OBJECT)
                        && idStr != null
                        && incl.requiresObjectContext()) {
                    typeIdDef.include = incl = WritableTypeId.Inclusion.WRAPPER_ARRAY;
                }

                switch (incl) {
                    case PARENT_PROPERTY:
                        // nothing to do here, as it has to be written in suffix...
                        break;
                    case PAYLOAD_PROPERTY:
                        // only output as native type id; otherwise caller must handle using some
                        // other mechanism, so...
                        break;
                    case METADATA_PROPERTY:
                        // must have Object context by now, so simply write as field name
                        // Note, too, that it's bit tricky, since we must print START_OBJECT that is part
                        // of value first -- and then NOT output it later on: hence return "early"
                        writeStartObject(typeIdDef.forValue);
                        writeStringField(typeIdDef.asProperty, idStr);
                        return typeIdDef;

                    case WRAPPER_OBJECT:
                        // NOTE: this is wrapper, not directly related to value to output, so don't pass
                        writeStartObject();
                        writeFieldName(idStr);
                        break;
                    case WRAPPER_ARRAY:
                    default: // should never occur but translate as "as-array"
                        writeStartArray(); // wrapper, not actual array object to write
                        writeString(idStr);
                }
            }
            // and finally possible start marker for value itself:
            if (valueShape == JsonToken.START_OBJECT) {
                writeStartObject(typeIdDef.forValue);
            } else if (valueShape == JsonToken.START_ARRAY) {
                // should we now set the current object?
                writeStartArray();
            }
            return typeIdDef;
        }
    }
}
