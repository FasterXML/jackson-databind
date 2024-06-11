package tools.jackson.databind;

import java.io.Closeable;
import java.util.*;

import tools.jackson.core.*;
import tools.jackson.core.exc.JacksonIOException;
import tools.jackson.databind.util.ClassUtil;

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

    /**
     * Let's limit length of reference chain, to limit damage in cases
     * of infinite recursion.
     */
    final static int MAX_REFS_TO_LIST = 1000;

    /*
    /**********************************************************************
    /* State/configuration
    /**********************************************************************
     */

    /**
     * Path through which problem that triggering throwing of
     * this exception was reached.
     */
    protected LinkedList<Reference> _path;

    /**
     * Underlying processor ({@link JsonParser} or {@link JsonGenerator}),
     * if known.
     *<p>
     * NOTE: typically not serializable hence <code>transient</code>
     */
    protected transient Closeable _processor;

    /*
    /**********************************************************************
    /* Life-cycle: constructors for local use, sub-classes
    /**********************************************************************
     */

    protected DatabindException(Closeable processor, String msg)
    {
        super(msg);
        _processor = processor;
        if (processor instanceof JsonParser) {
            // 17-Aug-2015, tatu: Use of token location makes some sense from databinding,
            //   since actual parsing (current) location is typically only needed for low-level
            //   parsing exceptions.
            _location = ((JsonParser) processor).currentTokenLocation();
        }
    }

    protected DatabindException(Closeable processor, String msg, Throwable problem)
    {
        super(msg, problem);
        _processor = processor;
        // 31-Jan-2020: [databind#2482] Retain original location
        if (problem instanceof JacksonException) {
            _location = ((JacksonException) problem).getLocation();
        } else if (processor instanceof JsonParser) {
            _location = ((JsonParser) processor).currentTokenLocation();
        }
    }

    protected DatabindException(Closeable processor, String msg, JsonLocation loc)
    {
        super(msg, loc, null);
        _processor = processor;
        _location = loc;
    }

    protected DatabindException(String msg, JsonLocation loc, Throwable rootCause) {
        super(msg, loc, rootCause);
    }

    protected DatabindException(String msg) {
        super(msg);
    }

    protected DatabindException(String msg, JsonLocation loc) {
        this(msg, loc, null);
    }

    protected DatabindException(String msg, Throwable rootCause) {
        this(msg, null, rootCause);
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

    public static DatabindException from(DeserializationContext ctxt, String msg, Throwable t) {
        return new DatabindException(_parser(ctxt), msg, t);
    }

    private static JsonParser _parser(DeserializationContext ctxt) {
        return (ctxt == null) ? null : ctxt.getParser();
    }

    public static DatabindException from(SerializerProvider ctxt, String msg) {
        return new DatabindException(_generator(ctxt), msg);
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
    /* Life-cycle: more advanced factory-like methods
    /**********************************************************************
     */

    /**
     * Method that can be called to either create a new DatabindException
     * (if underlying exception is not a DatabindException), or augment
     * given exception with given path/reference information.
     *
     * This version of method is called when the reference is through a
     * non-indexed object, such as a Map or POJO/bean.
     */
    public static DatabindException wrapWithPath(Throwable src, Object refFrom,
            String refPropertyName) {
        return wrapWithPath(src, new Reference(refFrom, refPropertyName));
    }

    /**
     * Method that can be called to either create a new DatabindException
     * (if underlying exception is not a DatabindException), or augment
     * given exception with given path/reference information.
     *
     * This version of method is called when the reference is through an
     * index, which happens with arrays and Collections.
     */
    public static DatabindException wrapWithPath(Throwable src, Object refFrom, int index) {
        return wrapWithPath(src, new Reference(refFrom, index));
    }

    /**
     * Method that can be called to either create a new DatabindException
     * (if underlying exception is not a DatabindException), or augment
     * given exception with given path/reference information.
     */
    @SuppressWarnings("resource")
    public static DatabindException wrapWithPath(Throwable src, Reference ref)
    {
        DatabindException jme;
        if (src instanceof DatabindException) {
            jme = (DatabindException) src;
        } else {
            // [databind#2128]: try to avoid duplication
            String msg = ClassUtil.exceptionMessage(src);
            // Let's use a more meaningful placeholder if all we have is null
            if (msg == null || msg.isEmpty()) {
                msg = "(was "+src.getClass().getName()+")";
            }
            // 17-Aug-2015, tatu: Let's also pass the processor (parser/generator) along
            Closeable proc = null;
            if (src instanceof JacksonException) {
                Object proc0 = ((JacksonException) src).processor();
                if (proc0 instanceof Closeable) {
                    proc = (Closeable) proc0;
                }
            }
            jme = new DatabindException(proc, msg, src);
        }
        jme.prependPath(ref);
        return jme;
    }

    /*
    /**********************************************************************
    /* Life-cycle: information augmentation (cannot use factory style, alas)
    /**********************************************************************
     */

    /**
     * Method called to prepend a reference information in front of
     * current path
     */
    public DatabindException prependPath(Object referrer, String propertyName) {
        return prependPath(new Reference(referrer, propertyName));
    }

    /**
     * Method called to prepend a reference information in front of
     * current path
     */
    public DatabindException prependPath(Object referrer, int index) {
        return prependPath(new Reference(referrer, index));
    }

    public DatabindException prependPath(Reference r)
    {
        if (_path == null) {
            _path = new LinkedList<Reference>();
        }
        // Also: let's not increase without bounds. Could choose either
        // head or tail; tail is easier (no need to ever remove), as
        // well as potentially more useful so let's use it:
        if (_path.size() < MAX_REFS_TO_LIST) {
            _path.addFirst(r);
        }
        return this;
    }

    /*
    /**********************************************************************
    /* Accessors
    /**********************************************************************
     */

    /**
     * Method for accessing full structural path within type hierarchy
     * down to problematic property.
     */
    public List<Reference> getPath()
    {
        if (_path == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(_path);
    }

    /**
     * Method for accessing description of path that lead to the
     * problem that triggered this exception
     */
    public String getPathReference()
    {
        return getPathReference(new StringBuilder()).toString();
    }

    public StringBuilder getPathReference(StringBuilder sb)
    {
        _appendPathDesc(sb);
        return sb;
    }

    /*
    /**********************************************************************
    /* Overridden standard methods
    /**********************************************************************
     */

    @Override
    public Object processor() { return _processor; }

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

    /*
    /**********************************************************************
    /* Internal methods
    /**********************************************************************
     */

    protected void _appendPathDesc(StringBuilder sb)
    {
        if (_path == null) {
            return;
        }
        Iterator<Reference> it = _path.iterator();
        while (it.hasNext()) {
            sb.append(it.next().toString());
            if (it.hasNext()) {
                sb.append("->");
            }
        }
    }
}
