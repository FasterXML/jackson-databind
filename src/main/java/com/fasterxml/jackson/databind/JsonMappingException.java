package com.fasterxml.jackson.databind;

import java.io.Closeable;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;

import com.fasterxml.jackson.annotation.JsonIgnore;

import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.util.ClassUtil;

/**
 * Checked exception used to signal fatal problems with mapping of
 * content, distinct from low-level I/O problems (signaled using
 * simple {@link java.io.IOException}s) or data encoding/decoding
 * problems (signaled with {@link com.fasterxml.jackson.core.exc.StreamReadException},
 * {@link com.fasterxml.jackson.core.exc.StreamWriteException}).
 *<p>
 * One additional feature is the ability to denote relevant path
 * of references (during serialization/deserialization) to help in
 * troubleshooting.
 */
public class JsonMappingException
    extends DatabindException // @since 2.13
{
    private static final long serialVersionUID = 3L;

    /**
     * Let's limit length of reference chain, to limit damage in cases
     * of infinite recursion.
     */
    final static int MAX_REFS_TO_LIST = 1000;

    /*
    /**********************************************************************
    /* Helper classes
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
        private static final long serialVersionUID = 2L; // changes between 2.7 and 2.8

        // transient since 2.8
        protected transient Object _from;

        /**
         * Name of field (for beans) or key (for Maps) that is part
         * of the reference. May be null for Collection types (which
         * generally have {@link #_index} defined), or when resolving
         * Map classes without (yet) having an instance to operate on.
         */
        protected String _fieldName;

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
         *
         * @since 2.8
         */
        protected String _desc;

        /**
         * Default constructor for deserialization/sub-classing purposes
         */
        protected Reference() { }

        public Reference(Object from) { _from = from; }

        public Reference(Object from, String fieldName) {
            _from = from;
            if (fieldName == null) {
                throw new NullPointerException("Cannot pass null fieldName");
            }
            _fieldName = fieldName;
        }

        public Reference(Object from, int index) {
            _from = from;
            _index = index;
        }

        // Setters to let Jackson deserialize instances, but not to be called from outside
        void setFieldName(String n) { _fieldName = n; }
        void setIndex(int ix) { _index = ix; }
        void setDescription(String d) { _desc = d; }

        /**
         * Object through which reference was resolved. Can be either
         * actual instance (usually the case for serialization), or
         * Class (usually the case for deserialization).
         *<p>
         * Note that this value must be `transient` to allow serializability (as
         * often such Object is NOT serializable; or, in case of `Class`, may
         * not available at the point of deserialization). As such will return
         * `null` if instance has been passed using JDK serialization.
         */
        @JsonIgnore
        public Object getFrom() { return _from; }

        public String getFieldName() { return _fieldName; }
        public int getIndex() { return _index; }
        public String getDescription() {
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
                if (_fieldName != null) {
                    sb.append('"');
                    sb.append(_fieldName);
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
         *
         * since 2.8
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
     *
     * @since 2.7
     */
    protected transient Closeable _processor;

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    /**
     * @deprecated Since 2.7 Use variant that takes {@link JsonParser} instead
     */
    @Deprecated // since 2.7
    public JsonMappingException(String msg) { super(msg); }

    /**
     * @deprecated Since 2.7 Use variant that takes {@link JsonParser} instead
     */
    @Deprecated // since 2.7
    public JsonMappingException(String msg, Throwable rootCause) { super(msg, rootCause); }

    /**
     * @deprecated Since 2.7 Use variant that takes {@link JsonParser} instead
     */
    @Deprecated // since 2.7
    public JsonMappingException(String msg, JsonLocation loc) { super(msg, loc); }

    /**
     * @deprecated Since 2.7 Use variant that takes {@link JsonParser} instead
     */
    @Deprecated // since 2.7
    public JsonMappingException(String msg, JsonLocation loc, Throwable rootCause) { super(msg, loc, rootCause); }

    /**
     * @since 2.7
     */
    public JsonMappingException(Closeable processor, String msg) {
        super(msg);
        _processor = processor;
        if (processor instanceof JsonParser) {
            // 17-Aug-2015, tatu: Use of token location makes some sense from databinding,
            //   since actual parsing (current) location is typically only needed for low-level
            //   parsing exceptions.
            _location = ((JsonParser) processor).getTokenLocation();
        }
    }

    /**
     * @since 2.7
     */
    public JsonMappingException(Closeable processor, String msg, Throwable problem) {
        super(msg, problem);
        _processor = processor;
        // 31-Jan-2020: [databind#2482] Retain original location
        if (problem instanceof JacksonException) {
            _location = ((JacksonException) problem).getLocation();
        } else if (processor instanceof JsonParser) {
            _location = ((JsonParser) processor).getTokenLocation();
        }
    }

    /**
     * @since 2.7
     */
    public JsonMappingException(Closeable processor, String msg, JsonLocation loc) {
        super(msg, loc);
        _processor = processor;
    }

    /**
     * @since 2.7
     */
    public static JsonMappingException from(JsonParser p, String msg) {
        return new JsonMappingException(p, msg);
    }

    /**
     * @since 2.7
     */
    public static JsonMappingException from(JsonParser p, String msg, Throwable problem) {
        return new JsonMappingException(p, msg, problem);
    }

    /**
     * @since 2.7
     */
    public static JsonMappingException from(JsonGenerator g, String msg) {
        return new JsonMappingException(g, msg, (Throwable) null);
    }

    /**
     * @since 2.7
     */
    public static JsonMappingException from(JsonGenerator g, String msg, Throwable problem) {
        return new JsonMappingException(g, msg, problem);
    }

    /**
     * @since 2.7
     */
    public static JsonMappingException from(DeserializationContext ctxt, String msg) {
        return new JsonMappingException(_parser(ctxt), msg);
    }

    /**
     * @since 2.7
     */
    public static JsonMappingException from(DeserializationContext ctxt, String msg, Throwable t) {
        return new JsonMappingException(_parser(ctxt), msg, t);
    }

    // @since 2.14
    private static JsonParser _parser(DeserializationContext ctxt) {
        return (ctxt == null) ? null : ctxt.getParser();
    }

    /**
     * @since 2.7
     */
    public static JsonMappingException from(SerializerProvider ctxt, String msg) {
        return new JsonMappingException(_generator(ctxt), msg);
    }

    /**
     * @since 2.7
     */
    public static JsonMappingException from(SerializerProvider ctxt, String msg, Throwable problem) {
        /* 17-Aug-2015, tatu: As per [databind#903] this is bit problematic as
         *   SerializerProvider instance does not currently hold on to generator...
         */
        return new JsonMappingException(_generator(ctxt), msg, problem);
    }

    // @since 2.14
    private static JsonGenerator _generator(SerializerProvider ctxt) {
        return (ctxt == null) ? null : ctxt.getGenerator();
    }

    /**
     * Factory method used when "upgrading" an {@link IOException} into
     * {@link JsonMappingException}: usually only needed to comply with
     * a signature.
     *<p>
     * NOTE: since 2.9 should usually NOT be used on input-side (deserialization)
     *    exceptions; instead use method(s) of <code>InputMismatchException</code>
     *
     * @since 2.1
     */
    public static JsonMappingException fromUnexpectedIOE(IOException src) {
        return new JsonMappingException(null,
                String.format("Unexpected IOException (of type %s): %s",
                        src.getClass().getName(),
                        ClassUtil.exceptionMessage(src)));
    }

    /**
     * Method that can be called to either create a new JsonMappingException
     * (if underlying exception is not a JsonMappingException), or augment
     * given exception with given path/reference information.
     *
     * This version of method is called when the reference is through a
     * non-indexed object, such as a Map or POJO/bean.
     */
    public static JsonMappingException wrapWithPath(Throwable src, Object refFrom,
            String refFieldName) {
        return wrapWithPath(src, new Reference(refFrom, refFieldName));
    }

    /**
     * Method that can be called to either create a new JsonMappingException
     * (if underlying exception is not a JsonMappingException), or augment
     * given exception with given path/reference information.
     *
     * This version of method is called when the reference is through an
     * index, which happens with arrays and Collections.
     */
    public static JsonMappingException wrapWithPath(Throwable src, Object refFrom, int index) {
        return wrapWithPath(src, new Reference(refFrom, index));
    }

    /**
     * Method that can be called to either create a new JsonMappingException
     * (if underlying exception is not a JsonMappingException), or augment
     * given exception with given path/reference information.
     */
    @SuppressWarnings("resource")
    public static JsonMappingException wrapWithPath(Throwable src, Reference ref)
    {
        JsonMappingException jme;
        if (src instanceof JsonMappingException) {
            jme = (JsonMappingException) src;
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
                Object proc0 = ((JacksonException) src).getProcessor();
                if (proc0 instanceof Closeable) {
                    proc = (Closeable) proc0;
                }
            }
            jme = new JsonMappingException(proc, msg, src);
        }
        jme.prependPath(ref);
        return jme;
    }

    /**
     * @since 2.13
     */
    public JsonMappingException withCause(Throwable cause) {
        initCause(cause);
        return this;
    }

    /*
    /**********************************************************************
    /* Accessors/mutators
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

    /**
     * Method called to prepend a reference information in front of
     * current path
     */
    @Override
    public void prependPath(Object referrer, String fieldName) {
        prependPath(new Reference(referrer, fieldName));
    }

    /**
     * Method called to prepend a reference information in front of
     * current path
     */
    @Override
    public void prependPath(Object referrer, int index) {
        prependPath(new Reference(referrer, index));
    }

    public void prependPath(Reference r)
    {
        if (_path == null) {
            _path = new LinkedList<Reference>();
        }
        /* Also: let's not increase without bounds. Could choose either
         * head or tail; tail is easier (no need to ever remove), as
         * well as potentially more useful so let's use it:
         */
        if (_path.size() < MAX_REFS_TO_LIST) {
            _path.addFirst(r);
        }
    }

    /*
    /**********************************************************************
    /* Overridden methods
    /**********************************************************************
     */

    @Override // since 2.8
    @JsonIgnore // as per [databind#1368]
    public Object getProcessor() { return _processor; }

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
