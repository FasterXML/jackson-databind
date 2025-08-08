package tools.jackson.databind.deser.jdk;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.exc.InvalidNullException;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// For [databind#5139]
public class CollectionDeserializer5139Test
{
    static class Dst {
        private List<Integer> list;

        public List<Integer> getList() {
            return list;
        }

        public void setList(List<Integer> list) {
            this.list = list;
        }
    }

    @Test
    public void nullsFailTest() {
        ObjectMapper mapper = JsonMapper.builder()
                .changeDefaultNullHandling(n -> JsonSetter.Value.forContentNulls(Nulls.FAIL))
                .build();

        assertThrows(
                InvalidNullException.class,
                () -> mapper.readValue("{\"list\":[\"\"]}", new TypeReference<Dst>(){})
        );
    }

    @Test
    public void nullsSkipTest() throws Exception {
        ObjectMapper mapper = JsonMapper.builder()
                .changeDefaultNullHandling(n -> JsonSetter.Value.forContentNulls(Nulls.SKIP))
                .build();

        Dst dst = mapper.readValue("{\"list\":[\"\"]}", new TypeReference<Dst>() {});

        assertTrue(dst.getList().isEmpty());
    }
}
