package tools.jackson.databind.ser;

import java.io.Writer;
import java.math.BigDecimal;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.ObjectWriteContext;
import tools.jackson.core.io.IOContext;
import tools.jackson.core.json.JsonFactory;

/**
 * TODO Fix double numbers. This feels like a very heavy solution plus I can't
 * use the JsonFactory.builder().
 */
public class CanonicalJsonFactory extends JsonFactory {
    private static final long serialVersionUID = 1L;

    private ValueToString<BigDecimal> _serializer;

    public CanonicalJsonFactory(ValueToString<BigDecimal> serializer) {
        this._serializer = serializer;
    }

    @Override
    protected JsonGenerator _createGenerator(ObjectWriteContext writeCtxt, IOContext ioCtxt, Writer out)
            throws JacksonException {
        JsonGenerator delegate = super._createGenerator(writeCtxt, ioCtxt, out);
        return new CanonicalNumberGenerator(delegate, _serializer);
    }
}
