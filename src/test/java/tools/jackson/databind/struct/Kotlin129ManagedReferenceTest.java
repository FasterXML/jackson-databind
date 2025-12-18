package tools.jackson.databind.struct;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

// For [databind#1516], https://github.com/FasterXML/jackson-module-kotlin/issues/129
public class Kotlin129ManagedReferenceTest extends DatabindTestUtil
{
    private final ObjectMapper MAPPER = newJsonMapper();

    static class Car {
        private final long id;

        @JsonManagedReference
        private final List<Color> colors;

        @JsonCreator
        public Car(@JsonProperty("id") long id,
                   @JsonProperty("colors") List<Color> colors) {
            this.id = id;
            this.colors = colors != null ? colors : new ArrayList<>();
        }

        public long getId() { return id; }
        public List<Color> getColors() { return colors; }
    }

    static class Color {
        private final long id;
        private final String code;

        @JsonBackReference
        private Car car;

        @JsonCreator
        public Color(@JsonProperty("id") long id,
                     @JsonProperty("code") String code) {
            this.id = id;
            this.code = code;
        }

        public long getId() { return id; }
        public String getCode() { return code; }
        public Car getCar() { return car; }
        public void setCar(Car car) { this.car = car; }
    }

    @Test
    public void testManagedReferenceOnCreator() throws Exception
    {
        Car car = new Car(100, new ArrayList<>());
        Color color = new Color(100, "#FFFFF");
        color.setCar(car);
        car.getColors().add(color);

        String json = MAPPER.writeValueAsString(car);
        Car result = MAPPER.readValue(json, Car.class);

        assertNotNull(result);
        assertEquals(100, result.getId());
        assertNotNull(result.getColors());
        assertEquals(1, result.getColors().size());

        Color resultColor = result.getColors().get(0);
        assertEquals(100, resultColor.getId());
        assertEquals("#FFFFF", resultColor.getCode());

        assertNotNull(resultColor.getCar());
        assertSame(result, resultColor.getCar());
    }
}