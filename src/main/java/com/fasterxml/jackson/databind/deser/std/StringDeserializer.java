package com.fasterxml.jackson.databind.deser.std;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.type.LogicalType;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;

@JacksonStdImpl
public class StringDeserializer extends StdScalarDeserializer<String> // non-final since 2.9
{
    private static final long serialVersionUID = 1L;

    /**
     * @since 2.2
     */
    public final static StringDeserializer instance = new StringDeserializer();

    public StringDeserializer() { super(String.class); }

    @Override // since 2.12
    public LogicalType logicalType() {
        return LogicalType.Textual;
    }

    // since 2.6, slightly faster lookups for this very common type
    @Override
    public boolean isCachable() { return true; }

    @Override // since 2.9
    public Object getEmptyValue(DeserializationContext ctxt) throws JsonMappingException {
        return "";
    }

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException
    {
        // disabled, execute default serialization
        if (!ctxt.isEnabled(DeserializationFeature.ACCEPT_SUB_JSON_AS_STRING)) {
            return defaultDeserialize(p, ctxt);
        }

        JsonToken currentToken = p.getCurrentToken();

        // not a JSON substring, execute default serialization
        if (currentToken != JsonToken.START_OBJECT && currentToken != JsonToken.START_ARRAY) {
            return defaultDeserialize(p, ctxt);
        }

        StringBuilder builder = new StringBuilder();
        Deque<JsonToken> stack = new ArrayDeque<>();

        builder.append(p.getText());
        stack.push(currentToken);

        final boolean isArray = currentToken == JsonToken.START_ARRAY;
        while (!stack.isEmpty()) {
            // an empty stack indicates that the current sub JSON string has been searched and completed
            JsonToken nextToken = p.nextToken();
            if (isArray && nextToken == JsonToken.END_ARRAY ||
                    !isArray && nextToken == JsonToken.END_OBJECT) {
                stack.pop();
            }
            if (isArray && nextToken == JsonToken.START_ARRAY ||
                    !isArray && nextToken == JsonToken.START_OBJECT) {
                stack.push(nextToken);
            }

            // start the sub JSON string, add comma if necessary
            if (nextToken.isStructStart()) {
                appendCommaIfNecessary(builder).append(p.getText());
            }

            // end of sub JSON string, delete comma if necessary
            else if (nextToken.isStructEnd()) {
                deleteCommaIfNecessary(builder).append(p.getText());
            }

            // number, Boolean type, without double quotation marks
            else if (nextToken.isNumeric() || nextToken.isBoolean()) {
                builder.append(p.getText());
            }

            // other types automatically add double quotation marks
            else {
                appendCommaIfNecessary(builder).append('"').append(p.getText()).append('"');
            }

            // automatically add colon if field
            if (nextToken == JsonToken.FIELD_NAME) {
                builder.append(':');
            }
            // automatically add commas if value
            else if (nextToken.isScalarValue()) {
                builder.append(',');
            }
        }

        return builder.toString();
    }

    // Since we can never have type info ("natural type"; String, Boolean, Integer, Double):
    // (is it an error to even call this version?)
    @Override
    public String deserializeWithType(JsonParser p, DeserializationContext ctxt,
            TypeDeserializer typeDeserializer) throws IOException {
        return deserialize(p, ctxt);
    }

    protected String defaultDeserialize(JsonParser p,
                                        DeserializationContext ctxt) throws IOException
    {
        // The critical path: ensure we handle the common case first.
        if (p.hasToken(JsonToken.VALUE_STRING)) {
            return p.getText();
        }
        // [databind#381]
        if (p.hasToken(JsonToken.START_ARRAY)) {
            return _deserializeFromArray(p, ctxt);
        }
        return _parseString(p, ctxt, this);
    }

    private static StringBuilder appendCommaIfNecessary(StringBuilder builder) {
        char lastChar = builder.charAt(builder.length() - 1);
        if (lastChar != '{' && lastChar != '[' && lastChar != ':' && lastChar != ',') {
            builder.append(',');
        }
        return builder;
    }

    private static StringBuilder deleteCommaIfNecessary(StringBuilder builder) {
        int lastIndex = builder.length() - 1;
        if (builder.charAt(lastIndex) == ',') {
            builder.deleteCharAt(lastIndex);
        }
        return builder;
    }
}
