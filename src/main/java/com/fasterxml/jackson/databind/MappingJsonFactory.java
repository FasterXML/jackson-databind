package com.fasterxml.jackson.databind;

import java.io.IOException;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.format.InputAccessor;
import com.fasterxml.jackson.core.format.MatchStrength;

/**
 * Sub-class of {@link JsonFactory} that will create a proper
 * {@link ObjectCodec} to allow seam-less conversions between
 * JSON content and Java objects (POJOs).
 * The only addition to regular {@link JsonFactory} currently
 * is that {@link ObjectMapper} is constructed and passed as
 * the codec to use.
 */
public class MappingJsonFactory
    extends JsonFactory
{
    private static final long serialVersionUID = -1; // since 2.7

    public MappingJsonFactory()
    {
        this(null);
    }

    public MappingJsonFactory(ObjectMapper mapper)
    {
        super(mapper);
        if (mapper == null) {
            setCodec(new ObjectMapper(this));
        }
    }

    public MappingJsonFactory(JsonFactory src, ObjectMapper mapper)
    {
        super(src, mapper);
        if (mapper == null) {
            setCodec(new ObjectMapper(this));
        }
    }

    /**
     * We'll override the method to return more specific type; co-variance
     * helps here
     */
    @Override
    public final ObjectMapper getCodec() { return (ObjectMapper) _objectCodec; }

    // @since 2.1
    @Override
    public JsonFactory copy()
    {
        _checkInvalidCopy(MappingJsonFactory.class);
        // note: as with base class, must NOT copy mapper reference
        return new MappingJsonFactory(this, null);
    }

    /*
    /**********************************************************
    /* Format detection functionality (since 1.8)
    /**********************************************************
     */

    /**
     * Sub-classes need to override this method
     */
    @Override
    public String getFormatName()
    {
        /* since non-JSON factories typically should not extend this class,
         * let's just always return JSON as name.
         */
        return FORMAT_NAME_JSON;
    }

    /**
     * Sub-classes need to override this method
     */
    @Override
    public MatchStrength hasFormat(InputAccessor acc) throws IOException
    {
        if (getClass() == MappingJsonFactory.class) {
            return hasJSONFormat(acc);
        }
        return null;
    }
}
