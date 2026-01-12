package tools.jackson.databind.records;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.PropertyAccessor;

import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RecordIgnoreNonAccessorGetterTest extends DatabindTestUtil {

    // [databind#3628]
    interface InterfaceWithGetter {

        String getId();

        String getName();
    }

    @JsonPropertyOrder({"id", "name", "count"}) // easier to assert when JSON field ordering is always the same
    record RecordWithInterfaceWithGetter(String name) implements InterfaceWithGetter {

        @Override
        public String getId() {
            return "ID:" + name;
        }

        @Override
        public String getName() {
            return name;
        }

        // [databind#3895]
        public int getCount() {
            return 999;
        }
    }

    @Test
    public void testSerializeIgnoreInterfaceGetter_WithoutUsingVisibilityConfig()
    {
        final ObjectMapper mapper = newJsonMapper();
        String json = mapper.writeValueAsString(new RecordWithInterfaceWithGetter("Bob"));
        assertEquals("{\"id\":\"ID:Bob\",\"name\":\"Bob\",\"count\":999}", json);
    }

    @Test
    public void testSerializeIgnoreInterfaceGetter_UsingVisibilityConfig()
    {
        final ObjectMapper mapper = jsonMapperBuilder()
                .changeDefaultVisibility(vc ->
                    vc.withVisibility(PropertyAccessor.GETTER, Visibility.NONE)
                        .withVisibility(PropertyAccessor.FIELD, Visibility.ANY)
                )
                .build();

        String json = mapper.writeValueAsString(new RecordWithInterfaceWithGetter("Bob"));

        assertEquals("{\"name\":\"Bob\"}", json);
    }

    // [databind#4157]
    @Test
    public void testWithInferGettersFromComponentsOnlyFeature()
    {
        final ObjectMapper mapper = jsonMapperBuilder()
                .enable(MapperFeature.INFER_RECORD_GETTERS_FROM_COMPONENTS_ONLY)
                .build();

        String json = mapper.writeValueAsString(new RecordWithInterfaceWithGetter("Bob"));

        // With feature enabled, only the actual record component should be serialized
        assertEquals("{\"name\":\"Bob\"}", json);
    }
}
