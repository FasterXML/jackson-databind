package tools.jackson.databind.deser.creators;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

// [databind#5281] Reading into existing instance uses creator property setup instead
// of accessor (setter, field)
public class CreatorPropertyReaderForUpdating5281Test
    extends DatabindTestUtil
{
    public static class ArrayListHolder {
        Collection<String> values;

        public ArrayListHolder(String... values) {
            this.values = new ArrayList<>();
            this.values.addAll(Arrays.asList(values));
        }

        public void setValues(Collection<String> values) {
            this.values = values;
        }
    }

    // Verify that readerForUpdating uses setter type (Collection<String>),
    // not creator parameter type (String[]), avoiding ClassCastException
    @Test
    public void readerForUpdatingUsesSetterType() throws Exception {
        ObjectMapper mapper = JsonMapper.builder().build();

        ArrayListHolder holder = mapper.readerForUpdating(new ArrayListHolder("A"))
                .readValue("{ \"values\" : [ \"A\", \"B\" ]}");

        // Setter replaces values (no merge), so we get the new array only
        assertThat(holder.values).hasSize(2)
                .containsExactly("A", "B");
    }
}
