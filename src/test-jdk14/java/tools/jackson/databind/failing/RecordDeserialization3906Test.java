package tools.jackson.databind.failing;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.PropertyAccessor;

import tools.jackson.databind.BaseMapTest;
import tools.jackson.databind.ObjectMapper;

/**
 * Test case that covers both failing-by-regression tests and passing tests.
 * <p>For more details, refer to
 * <a href="https://github.com/FasterXML/jackson-databind/issues/3906">
 * [databind#3906]: Regression: 2.15.0 breaks deserialization for records when mapper.setVisibility(ALL, NONE);</a>
 *<p>
 * NOTE: fixed for 2.x in 2.16; 2 failing cases still for 3.0.
 */
public class RecordDeserialization3906Test extends BaseMapTest
{
    record Record3906(String string, int integer) {
    }

    @JsonAutoDetect(creatorVisibility = Visibility.NON_PRIVATE)
    record Record3906Annotated(String string, int integer) {
    }

    record Record3906Creator(String string, int integer) {
        @JsonCreator
        Record3906Creator {
        }
    }

    /*
    /**********************************************************
    /* Failing tests that pass in 2.14, fixed in 2.16 but not 3.x
    /**********************************************************
     */

    // minimal config for reproduction
    public void testEmptyJsonToRecordMiminal() throws Exception {
        ObjectMapper mapper = jsonMapperBuilder()
                .changeDefaultVisibility(vc -> 
                   vc.withVisibility(PropertyAccessor.ALL, Visibility.NONE))
                .build();

        Record3906 recordDeser = mapper.readValue("{}", Record3906.class);

        assertEquals(new Record3906(null, 0), recordDeser);
    }

    // actual config used reproduction
    public void testEmptyJsonToRecordActualImpl() throws Exception {
        ObjectMapper mapper = jsonMapperBuilder()
                .changeDefaultVisibility(vc -> 
                   vc.withVisibility(PropertyAccessor.ALL, Visibility.NONE)
                   .withVisibility(PropertyAccessor.FIELD, Visibility.ANY))
                .build();
        Record3906 recordDeser = mapper.readValue("{}", Record3906.class);

        assertEquals(new Record3906(null, 0), recordDeser);
    }
}
