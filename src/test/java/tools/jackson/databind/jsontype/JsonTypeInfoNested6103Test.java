package tools.jackson.databind.jsontype;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

// Tests wrt [databind#6103]
public class JsonTypeInfoNested6103Test extends DatabindTestUtil
{

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY,
            property = "type", visible = true)
    @JsonSubTypes({
            @JsonSubTypes.Type(value = Car.class, name = "car"),
            @JsonSubTypes.Type(value = Motorbike.class, name = "motorbike"),
    })
    public interface Vehicle {
        String type();

        String model();
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY,
            property = "model", visible = true)
    @JsonSubTypes({
            @JsonSubTypes.Type(value = Minivan.class, name = "minivan"),
            @JsonSubTypes.Type(value = Sedan.class, name = "sedan"),
    })
    public interface Car extends Vehicle {
        boolean isConvertible();
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY,
            property = "model", visible = true)
    @JsonSubTypes({
            @JsonSubTypes.Type(value = Scooter.class, name = "scooter"),
            @JsonSubTypes.Type(value = Chopper.class, name = "sedan"),
    })
    public interface Motorbike extends Vehicle {
        boolean hasSidecar();
    }

    public record Minivan(String type, String model) implements Car {
        @Override
        public boolean isConvertible() {
            return false;
        }
    }

    public record Sedan(String type, String model, boolean isConvertible) implements Car {
    }

    public record Scooter(String type, String model) implements Motorbike {
        @Override
        public boolean hasSidecar() {
            return false;
        }
    }

    public record Chopper(String type, String model, boolean hasSidecar) implements Motorbike {
    }

    // verify failures when exact matching required:
    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    void readMixedCaseSubclass() throws Exception
    {
        final String serialised = """
                {
                  "type": "car",
                  "model": "minivan"
                }
                """;
        // Type id ("value") mismatch, should work now:
        Vehicle result = MAPPER.readValue(serialised, Vehicle.class);

        assertInstanceOf(Minivan.class, result);
    }
}
