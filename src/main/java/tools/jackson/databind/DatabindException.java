package tools.jackson.databind;

import java.io.Closeable;

import tools.jackson.core.*;
import tools.jackson.core.exc.JacksonIOException;

/**
 * Exception used to signal fatal problems with mapping of
 * content, distinct from low-level I/O problems (signaled using
 * simple {@link JacksonIOException}s) or data encoding/decoding
 * problems (signaled with {@link tools.jackson.core.exc.StreamReadException},
 * {@link tools.jackson.core.exc.StreamWriteException}).
 *<p>
 * One additional feature is the ability to denote relevant path
 * of references (during serialization/deserialization) to help in
 * troubleshooting.
 */
public class DatabindException
    extends JacksonException
{
    private static final long serialVersionUID = 3L;

    /*
    /**********************************************************************
    /* Life-cycle: constructors for local use, sub-classes
    /**********************************************************************
     */

    protected DatabindException(Closeable processor, String msg) {
        super(processor, msg);
    }

    protected DatabindException(Closeable processor, String msg, Throwable problem) {
        super(processor, msg, problem);
    }

    protected DatabindException(Closeable processor, String msg, JsonLocation loc)
    {
        super(msg, loc, null);
    }

    protected DatabindException(String msg, JsonLocation loc, Throwable rootCause) {
        super(msg, loc, rootCause);
    }

    protected DatabindException(String msg) {
        super(msg);
    }

    /*
    /**********************************************************************
    /* Life-cycle: simple factory methods (for actual construction)
    /**********************************************************************
     */

    public static DatabindException from(JsonParser p, String msg) {
        return new DatabindException(p, msg);
    }

    public static DatabindException from(JsonParser p, String msg, Throwable problem) {
        return new DatabindException(p, msg, problem);
    }

    public static DatabindException from(JsonGenerator g, String msg) {
        return new DatabindException(g, msg, (Throwable) null);
    }

    public static DatabindException from(JsonGenerator g, String msg, Throwable problem) {
        return new DatabindException(g, msg, problem);
    }

    public static DatabindException from(DeserializationContext ctxt, String msg) {
        return new DatabindException(_parser(ctxt), msg);
    }

    private static JsonParser _parser(DeserializationContext ctxt) {
        return (ctxt == null) ? null : ctxt.getParser();
    }

    public static DatabindException from(SerializerProvider ctxt, String msg, Throwable problem) {
        // 17-Aug-2015, tatu: As per [databind#903] this is bit problematic as
        //   SerializerProvider instance does not currently hold on to generator...
        return new DatabindException(_generator(ctxt), msg, problem);
    }

    private static JsonGenerator _generator(SerializerProvider ctxt) {
        return (ctxt == null) ? null : ctxt.getGenerator();
    }

    /*
    /**********************************************************************
    /* Overridden standard methods
    /**********************************************************************
     */

    @Override
    public String getLocalizedMessage() {
        return _buildMessage();
    }

    /**
     * Method is overridden so that we can properly inject description
     * of problem path, if such is defined.
     */
    @Override
    public String getMessage() {
        return _buildMessage();
    }

    protected String _buildMessage()
    {
        // First: if we have no path info, let's just use parent's definition as is
        String msg = super.getMessage();
        if (_path == null) {
            return msg;
        }
        StringBuilder sb = (msg == null) ? new StringBuilder() : new StringBuilder(msg);
        /* 18-Feb-2009, tatu: initially there was a linefeed between
         *    message and path reference; but unfortunately many systems
         *   (loggers, junit) seem to assume linefeeds are only added to
         *   separate stack trace.
         */
        sb.append(" (through reference chain: ");
        sb = getPathReference(sb);
        sb.append(')');
        return sb.toString();
    }

    @Override
    public String toString()
    {
        return getClass().getName()+": "+getMessage();
    }
}
