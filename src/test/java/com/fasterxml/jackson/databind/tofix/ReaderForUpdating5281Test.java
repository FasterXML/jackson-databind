package com.fasterxml.jackson.databind.tofix;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;
import com.fasterxml.jackson.databind.testutil.failure.JacksonTestFailureExpected;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// [databind#5281] Reading into existing instance uses creator property setup instead
// of accessor #5281
public class ReaderForUpdating5281Test
    extends DatabindTestUtil
{
    public static class ArrayListHolder {
        // Works when annotated with...
        // @JsonMerge
        Collection<String> values;

        public ArrayListHolder(String... values) {
            this.values = new ArrayList<>();
            this.values.addAll(Arrays.asList(values));
        }

        public void setValues(Collection<String> values) {
            this.values = values;
        }
    }

    @JacksonTestFailureExpected
    @Test
    public void readsIntoCreator() throws Exception {
        ObjectMapper mapper = JsonMapper.builder().build();

        ArrayListHolder holder = mapper.readerForUpdating(new ArrayListHolder("A"))
                .readValue("{ \"values\" : [ \"A\", \"B\" ]}");

        assertThat(holder.values).hasSize(3)
                .containsAll(Arrays.asList("A", "A", "B"));
    }
}
