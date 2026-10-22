package tools.jackson.databind.ser.filter;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [databind#4464] Since 2.15, NON_DEFAULT should be extension of NON_EMPTY
public class JsonInclude4464Test {

    public static class BarSerializer extends ValueSerializer<Bar> {

        public BarSerializer() {
        }

        @Override
        public void serialize(Bar value, JsonGenerator gen, SerializationContext provider) {
            gen.writePOJO(value);
        }

        @Override
        public boolean isEmpty(SerializationContext provider, Bar value) {
            return "I_AM_EMPTY".equals(value.getName());
        }
    }

    public static class Bar {
        public String getName() {
            return "I_AM_EMPTY";
        }
    }

    public static class Foo {
        @JsonSerialize(using = BarSerializer.class)
        public Bar getBar() {
            return new Bar();
        }
    }

    @Test
    public void test86() throws Exception {
        ObjectMapper mapper = JsonMapper.builder()
                .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_DEFAULT))
                .build();
        String json = mapper.writeValueAsString(new Foo());
        assertEquals("{}", json);
    }

    // [databind#6319]
    static class Money {
        final long cents;
        Money(long c) { cents = c; }
    }

    static class MoneySer extends ValueSerializer<Money> {
        @Override
        public void serialize(Money v, JsonGenerator g, SerializationContext ctxt) {
            g.writeNumber(v.cents);
        }

        @Override
        public boolean isEmpty(SerializationContext ctxt, Money v) {
            return v.cents == 0;
        }
    }

    // No default constructor: default bean cannot be created
    static class CreatorOnly {
        @JsonSerialize(using = MoneySer.class)
        public Money amount;

        @JsonCreator
        CreatorOnly(@JsonProperty("amount") Money amount) { this.amount = amount; }
    }

    @Test
    public void noDefaultBeanGlobalNonDefault() throws Exception {
        for (boolean real : new boolean[] { false, true }) {
            ObjectMapper mapper = JsonMapper.builder()
                    .configure(MapperFeature.USE_REAL_INCLUDE_NON_DEFAULT, real)
                    .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_DEFAULT))
                    .build();
            assertEquals("{}", mapper.writeValueAsString(new CreatorOnly(new Money(0))),
                    "real=" + real);
        }
    }
}
