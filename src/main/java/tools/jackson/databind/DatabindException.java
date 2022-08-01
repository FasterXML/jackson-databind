package tools.jackson.databind;

import java.io.Closeable;
import java.io.Serializable;
import java.util.*;

import tools.jackson.core.*;
import tools.jackson.core.exc.WrappedIOException;
import tools.jackson.databind.util.ClassUtil;

/**
 * Exception used to signal fatal problems with mapping of
 * content, distinct from low-level I/O problems (signaled using
 * simple {@link WrappedIOException}s) or data encoding/decoding
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
    /* Helper class (to move down to JacksonException shortly!)
    /**********************************************************************
     */

    /**
     * Simple bean class used to contain references. References
     * can be added to indicate execution/reference path that
     * lead to the problem that caused this exception to be
     * thrown.
     */
    public static class Reference implements Serializable
    {
        private static final long serialVersionUID = 3L;

        protected transient Object _from;

        /**
         * Name of property (for POJO) or key (for Maps) that is part
         * of the reference. May be null for Collection types (which
         * generally have {@link #_index} defined), or when resolving
         * Map classes without (yet) having an instance to operate on.
         */
        protected String _propertyName;

        /**
         * Index within a {@link Collection} instance that contained
         * the reference; used if index is relevant and available.
         * If either not applicable, or not available, -1 is used to
         * denote "not known" (or not relevant).
         */
        protected int _index = -1;

        /**
         * Lazily-constructed description of this instance; needed mostly to
         * allow JDK serialization to work in case where {@link #_from} is
         * non-serializable (and has to be dropped) but we still want to pass
         * actual description along.
         */
        protected String _desc;

        /**
         * Default constructor for deserialization purposes
         */
        protected Reference() { }

        public Reference(Object from) { _from = from; }

        public Reference(Object from, String propertyName) {
            _from = from;
            if (propertyName == null) {
                throw new NullPointerException("Cannot pass null 'propertyName'");
            }
            _propertyName = propertyName;
        }

        public Reference(Object from, int index) {
            _from = from;
            _index = index;
        }

        // Setters to let Jackson deserialize instances, but not to be called from outside
        void setPropertyName(String n) { _propertyName = n; }
        void setIndex(int ix) { _index = ix; }
        void setDescription(String d) { _desc = d; }

        /**
         * Object through which reference was resolved. Can be either
         * actual instance (usually the case for serialization), or
         * Class (usually the case for deserialization).
         *<p>
         * Note that this the accessor is not a getter on purpose as we cannot
         * (in general) serialize/deserialize this reference
         */
        public Object from() { return _from; }

        public String getPropertyName() { return _propertyName; }
        public int getIndex() { return _index; }

        public String getDescription()
        {
            if (_desc == null) {
                StringBuilder sb = new StringBuilder();

                if (_from == null) { // can this ever occur?
                    sb.append("UNKNOWN");
                } else {
                    Class<?> cls = (_from instanceof Class<?>) ? (Class<?>)_from : _from.getClass();
                    // Hmmh. Although Class.getName() is mostly ok, it does look
                    // butt-ugly for arrays.
                    // 06-Oct-2016, tatu: as per [databind#1403], `getSimpleName()` not so good
                    //   as it drops enclosing class. So let's try bit different approach
                    int arrays = 0;
                    while (cls.isArray()) {
                        cls = cls.getComponentType();
                        ++arrays;
                    }
                    sb.append(cls.getName());
                    while (--arrays >= 0) {
                        sb.append("[]");
                    }
                }
                sb.append('[');
                if (_propertyName != null) {
                    sb.append('"');
                    sb.append(_propertyName);
                    sb.append('"');
                } else if (_index >= 0) {
                    sb.append(_index);
                } else {
                    sb.append('?');
                }
                sb.append(']');
                _desc = sb.toString();
            }
            return _desc;
        }

        @Override
        public String toString() {
            return getDescription();
        }

        /**
         * May need some cleaning here, given that `from` may or may not be serializable.
         */
        Object writeReplace() {
            // as per [databind#1195], need to ensure description is not null, since
            // `_from` is transient
            getDescription();
            return this;
        }
    }

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
