package tools.jackson.databind.jsontype;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

class PolymorphicErrorToken6091Test extends DatabindTestUtil
{
    @JsonTypeName("message")
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind", defaultImpl = Message.class)
    static class Message {
        public int amount;
        public Message child;
    }

    static class Envelope {
        public Message message;
        public int amount;
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    @ParameterizedTest
    @ValueSource(strings = {
            """
            {"kind":"message","amount":"bad"}
            """,
            """
            {"amount":"bad"}
            """,
            """
            {"amount":"bad","kind":"message"}
            """
    })
    void reportsOffendingToken(String json) {
        _assertErrorToken(json, Message.class);
    }

    @Test
    void reportsNestedBufferedToken() {
        _assertErrorToken("""
                {"amount":1,"child":{"amount":"bad"}}
                """, Message.class);
    }

    @Test
    void restoresParserForFollowingProperty() {
        _assertErrorToken("""
                {"message":{"amount":1},"amount":"bad"}
                """, Envelope.class);
    }

    private void _assertErrorToken(String json, Class<?> type) {
        try (JsonParser input = MAPPER.createParser(json)) {
            MismatchedInputException error = assertThrows(MismatchedInputException.class,
                    () -> MAPPER.readValue(input, type));
            JsonParser parser = assertInstanceOf(JsonParser.class, error.processor());
            assertEquals(JsonToken.VALUE_STRING, parser.currentToken());
            assertEquals("bad", parser.getString());
        }
    }
}
