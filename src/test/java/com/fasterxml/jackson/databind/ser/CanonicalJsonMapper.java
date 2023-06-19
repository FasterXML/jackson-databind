package com.fasterxml.jackson.databind.ser;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGeneratorDecorator;
import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.core.StreamWriteFeature;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.Separators;
import com.fasterxml.jackson.core.util.Separators.Spacing;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.cfg.JsonNodeFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;

public class CanonicalJsonMapper {
    public static final DefaultIndenter CANONICAL_INDENTEER = new DefaultIndenter("    ", "\n");

    public static final PrettyPrinter CANONICAL_PRETTY_PRINTER = new DefaultPrettyPrinter()
        .withObjectIndenter(CANONICAL_INDENTEER)
        .withSeparators(Separators.createDefaultInstance().withObjectFieldValueSpacing(Spacing.AFTER));    

    public static class Builder {
        private CanonicalNumberSerializerProvider _numberSerializerProvider = CanonicalBigDecimalSerializer.PROVIDER;
        private boolean _enablePrettyPrinting = false;
        
        private Builder() {
            // Don't allow to create except via builder method
        }
        
        public Builder prettyPrint() {
            _enablePrettyPrinting = true;
            _numberSerializerProvider = PrettyBigDecimalSerializer.PROVIDER;
            return this;
        }
        
        public JsonMapper build() {
            JsonGeneratorDecorator decorator = new CanonicalJsonGeneratorDecorator(_numberSerializerProvider.getValueToString());
            CanonicalJsonModule module = new CanonicalJsonModule(_numberSerializerProvider.getNumberSerializer());

            JsonFactory factory = JsonFactory.builder() //
                .decorateWith(decorator)
                .build();
            JsonMapper.Builder builder = JsonMapper.builder(factory) //
                .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS) //
                .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY) //
                .enable(JsonNodeFeature.WRITE_PROPERTIES_SORTED) //
                .addModule(module);
            
            if (_enablePrettyPrinting) {
                builder = builder //
                    .enable(SerializationFeature.INDENT_OUTPUT) //
                    .enable(StreamWriteFeature.WRITE_BIGDECIMAL_AS_PLAIN) //
                    .defaultPrettyPrinter(CANONICAL_PRETTY_PRINTER) //
                ;
            }
            
            return builder.build();
        }
    }
    
    public static CanonicalJsonMapper.Builder builder() {
        return new Builder();
    }
}
