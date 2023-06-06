package com.fasterxml.jackson.databind.ser;

import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.JsonFactory;

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
    protected JsonGenerator _createGenerator(Writer out, IOContext ioCtxt)
            throws IOException {
        JsonGenerator delegate = super._createGenerator(out, ioCtxt);
        return new CanonicalNumberGenerator(delegate, _serializer);
    }
}
