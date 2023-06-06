package tools.jackson.databind.ser;

import tools.jackson.core.StreamWriteFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

public class CanonicalJsonMapper { // TODO It would be great if we could extend JsonMapper but the return type of builder() is incompatible

    public static class Builder { // TODO Can't extend MapperBuilder<JsonMapper, Builder> because that needs JsonFactory as ctor arg and we only have this later
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
            CanonicalJsonFactory jsonFactory = new CanonicalJsonFactory(_numberSerializerProvider.getValueToString());
            CanonicalJsonModule module = new CanonicalJsonModule(_numberSerializerProvider.getNumberSerializer());

            JsonMapper.Builder builder = JsonMapper.builder(jsonFactory)
                .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS) //
                .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY) //
                .addModule(module);
            
            if (_enablePrettyPrinting) {
                builder = builder //
                    .enable(SerializationFeature.INDENT_OUTPUT) //
                    .enable(StreamWriteFeature.WRITE_BIGDECIMAL_AS_PLAIN) //
                    .defaultPrettyPrinter(CanonicalPrettyPrinter.INSTANCE) //
                ;
            }
            
            return builder.build();
        }
    }
    
    public static CanonicalJsonMapper.Builder builder() {
        return new Builder();
    }
}
