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

    protected DatabindException(Closeable processor, String msg, TokenStreamLocation loc) {
        super(processor, msg, loc, null);
    }

    protected DatabindException(Closeable processor, String msg, TokenStreamLocation loc,
            Throwable rootCause) {
        super(processor, msg, loc, rootCause);
    }
    protected DatabindException(String msg, TokenStreamLocation loc, Throwable rootCause) {
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

    public static DatabindException from(DeserializationContext ctxt, String msg, Throwable problem) {
        return new DatabindException(_parser(ctxt), msg, problem);
    }

    public static DatabindException from(SerializationContext ctxt, String msg) {
        return new DatabindException(_generator(ctxt), msg);
    }

    public static DatabindException from(SerializationContext ctxt, String msg, Throwable problem) {
        return new DatabindException(_generator(ctxt), msg, problem);
    }

    // // "Overrides": methods with name same as ones from JacksonException
    // // (but for static methods no real overriding, static dispatch)

    public static JacksonException wrapWithPath(DeserializationContext ctxt,
            Throwable src, Reference ref) {
        JsonParser p = _parser(ctxt);

        // Copied from JacksonException.wrapWithPath()
        JacksonException jme;
        if (src instanceof JacksonException je) {
            jme = je;
        } else {
            // [databind#2128]: try to avoid duplication
            String msg = _exceptionMessage(src);
            // Let's use a more meaningful placeholder if all we have is null
            if (msg == null || msg.isEmpty()) {
                msg = "(was "+src.getClass().getName()+")";
            }
            TokenStreamLocation loc = (p == null) ? null : p.currentLocation();
            jme = new DatabindException(p, msg, loc, src);
        }
        jme.prependPath(ref);
        return jme;
    }
    
    public static JacksonException wrapWithPath(SerializationContext ctxt,
            Throwable src, Reference ref) {
        JsonGenerator g = _generator(ctxt);

        // Copied from JacksonException.wrapWithPath()
        JacksonException jme;
        if (src instanceof JacksonException je) {
            jme = je;
        } else {
            String msg = _exceptionMessage(src);
            if (msg == null || msg.isEmpty()) {
                msg = "(was "+src.getClass().getName()+")";
            }
            // 2025-04-11, tatu: No location from generator, currently
            TokenStreamLocation loc = null;
            jme = new DatabindException(g, msg, loc, src);
        }
        jme.prependPath(ref);
        return jme;
    }

    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */

    private static JsonParser _parser(DeserializationContext ctxt) {
        return (ctxt == null) ? null : ctxt.getParser();
    }

    private static JsonGenerator _generator(SerializationContext ctxt) {
        return (ctxt == null) ? null : ctxt.getGenerator();
    }
}
