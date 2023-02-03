package com.fasterxml.jackson.databind;

import java.io.Closeable;
import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.core.*;

/**
 * Iterator exposed by {@link ObjectMapper} when binding sequence of
 * objects. Extension is done to allow more convenient exposing of
 * {@link IOException} (which basic {@link Iterator} does not expose)
 */
public class MappingIterator<T> implements Iterator<T>, Closeable
{
    protected final static MappingIterator<?> EMPTY_ITERATOR =
        new MappingIterator<Object>(null, null, null, null, false, null);

    /*
    /**********************************************************
    /* State constants
    /**********************************************************
     */

    /**
     * State in which iterator is closed
     */
    protected final static int STATE_CLOSED = 0;

    /**
     * State in which value read failed
     */
    protected final static int STATE_NEED_RESYNC = 1;

    /**
     * State in which no recovery is needed, but "hasNextValue()" needs
     * to be called first
     */
    protected final static int STATE_MAY_HAVE_VALUE = 2;

    /**
     * State in which "hasNextValue()" has been succesfully called
     * and deserializer can be called to fetch value
     */
    protected final static int STATE_HAS_VALUE = 3;

    /*
    /**********************************************************
    /* Configuration
    /**********************************************************
     */

    /**
     * Type to bind individual elements to.
     */
    protected final JavaType _type;

    /**
     * Context for deserialization, needed to pass through to deserializer
     */
    protected final DeserializationContext _context;

    /**
     * Deserializer for individual element values.
     */
    protected final JsonDeserializer<T> _deserializer;

    /**
     * Underlying parser used for reading content to bind. Initialized
     * as not <code>null</code> but set as <code>null</code> when
     * iterator is closed, to denote closing.
     */
    protected final JsonParser _parser;

    /**
     * Context to resynchronize to, in case an exception is encountered
     * but caller wants to try to read more elements.
     */
    protected final JsonStreamContext _seqContext;

    /**
     * If not null, "value to update" instead of creating a new instance
     * for each call.
     */
    protected final T _updatedValue;

    /**
     * Flag that indicates whether input {@link JsonParser} should be closed
     * when we are done or not; generally only called when caller did not
     * pass JsonParser.
     */
    protected final boolean _closeParser;

    /*
    /**********************************************************
    /* Parsing state
    /**********************************************************
     */

    /**
     * State of the iterator
     */
    protected int _state;

    /*
    /**********************************************************
    /* Construction
    /**********************************************************
     */

    /**
     * @param managedParser Whether we "own" the {@link JsonParser} passed or not:
     *   if true, it was created by {@link ObjectReader} and code here needs to
     *   close it; if false, it was passed by calling code and should not be
     *   closed by iterator.
     */
    @SuppressWarnings("unchecked")
    protected MappingIterator(JavaType type, JsonParser p, DeserializationContext ctxt,
            JsonDeserializer<?> deser,
            boolean managedParser, Object valueToUpdate)
    {
        _type = type;
        _parser = p;
        _context = ctxt;
        _deserializer = (JsonDeserializer<T>) deser;
        _closeParser = managedParser;
        if (valueToUpdate == null) {
            _updatedValue = null;
        } else {
            _updatedValue = (T) valueToUpdate;
        }

        /* Ok: one more thing; we may have to skip START_ARRAY, assuming
         * "wrapped" sequence; but this is ONLY done for 'managed' parsers
         * and never if JsonParser was directly passed by caller (if it
         * was, caller must have either positioned it over first token of
         * the first element, or cleared the START_ARRAY token explicitly).
         * Note, however, that we do not try to guess whether this could be
         * an unwrapped sequence of arrays/Lists: we just assume it is wrapped;
         * and if not, caller needs to hand us JsonParser instead, pointing to
         * the first token of the first element.
         */
        if (p == null) { // can this occur?
            _seqContext = null;
            _state = STATE_CLOSED;
        } else {
            JsonStreamContext sctxt = p.getParsingContext();
            if (managedParser && p.isExpectedStartArrayToken()) {
                // If pointing to START_ARRAY, context should be that ARRAY
                p.clearCurrentToken();
            } else {
                // regardless, recovery context should be whatever context we have now,
                // with sole exception of pointing to a start marker, in which case it's
                // the parent
                JsonToken t = p.currentToken();
                if ((t == JsonToken.START_OBJECT) || (t == JsonToken.START_ARRAY)) {
                    sctxt = sctxt.getParent();
                }
            }
            _seqContext = sctxt;
            _state = STATE_MAY_HAVE_VALUE;
        }
    }

    /**
     * Method for getting an "empty" iterator instance: one that never
     * has more values; may be freely shared.
     *
     * @since 2.10 Existed earlier but {@code public} since 2.10
     */
    @SuppressWarnings("unchecked")
    public static <T> MappingIterator<T> emptyIterator() {
        return (MappingIterator<T>) EMPTY_ITERATOR;
    }

    /*
    /**********************************************************
    /* Basic iterator impl
    /**********************************************************
     */

    @Override
    public boolean hasNext()
    {
        try {
            return hasNextValue();
        } catch (JsonMappingException e) {
            return (Boolean) _handleMappingException(e);
        } catch (IOException e) {
            return (Boolean) _handleIOException(e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public T next()
    {
        try {
            return nextValue();
        } catch (JsonMappingException e) {
            return (T) _handleMappingException(e);
        } catch (IOException e) {
            return (T) _handleIOException(e);
        }
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() throws IOException {
        if (_state != STATE_CLOSED) {
            _state = STATE_CLOSED;
            if (_parser != null) {
                _parser.close();
            }
        }
    }

    /*
    /**********************************************************
    /* Extended API, iteration
    /**********************************************************
     */

    /**
     * Equivalent of {@link #next} but one that may throw checked
     * exceptions from Jackson due to invalid input.
     */
    public boolean hasNextValue() throws IOException
    {
        switch (_state) {
        case STATE_CLOSED:
            return false;
        case STATE_NEED_RESYNC:
            _resync();
            // fall-through
        case STATE_MAY_HAVE_VALUE:
            if (_parser == null) {
                return false;
            }
            JsonToken t = _parser.currentToken();
            if (t == null) { // un-initialized or cleared; find next
                t = _parser.nextToken();
                // If EOF, no more, or if we hit END_ARRAY (although we don't clear the token).
                if (t == null || t == JsonToken.END_ARRAY) {
                    _state = STATE_CLOSED;
                    if (_closeParser) {
                        _parser.close();
                    }
                    return false;
                }
            }
            _state = STATE_HAS_VALUE;
            return true;
        case STATE_HAS_VALUE:
            // fall through
        }
        return true;
    }

    public T nextValue() throws IOException
    {
        switch (_state) {
        case STATE_CLOSED:
            return _throwNoSuchElement();
        case STATE_NEED_RESYNC: // fall-through, will do re-sync
        case STATE_MAY_HAVE_VALUE:
            if (!hasNextValue()) {
                return _throwNoSuchElement();
            }
            break;
        case STATE_HAS_VALUE:
            break;
        }

        int nextState = STATE_NEED_RESYNC;
        try {
            T value;
            if (_updatedValue == null) {
                value = _deserializer.deserialize(_parser, _context);
            } else{
                _deserializer.deserialize(_parser, _context, _updatedValue);
                value = _updatedValue;
            }
            nextState = STATE_MAY_HAVE_VALUE;
            return value;
        } finally {
            _state = nextState;
            /* 24-Mar-2015, tatu: As per [#733], need to mark token consumed no
             *   matter what, to avoid infinite loop for certain failure cases.
             *   For 2.6 need to improve further.
             */
            _parser.clearCurrentToken();
        }
    }

    /**
     * Convenience method for reading all entries accessible via
     * this iterator; resulting container will be a {@link java.util.ArrayList}.
     *
     * @return List of entries read
     *
     * @since 2.2
     */
    public List<T> readAll() throws IOException {
        return readAll(new ArrayList<T>());
    }

    /**
     * Convenience method for reading all entries accessible via
     * this iterator
     *
     * @return List of entries read (same as passed-in argument)
     *
     * @since 2.2
     */
    public <L extends List<? super T>> L readAll(L resultList) throws IOException
    {
        while (hasNextValue()) {
            resultList.add(nextValue());
        }
        return resultList;
    }

    /**
     * Convenience method for reading all entries accessible via
     * this iterator
     *
     * @since 2.5
     */
    public <C extends Collection<? super T>> C readAll(C results) throws IOException
    {
        while (hasNextValue()) {
            results.add(nextValue());
        }
        return results;
    }

    /*
    /**********************************************************
    /* Extended API, accessors
    /**********************************************************
     */

    /**
     * Accessor for getting underlying parser this iterator uses.
     *
     * @since 2.2
     */
    public JsonParser getParser() {
        return _parser;
    }

    /**
     * Accessor for accessing {@link FormatSchema} that the underlying parser
     * (as per {@link #getParser}) is using, if any; only parser of schema-aware
     * formats use schemas.
     *
     * @since 2.2
     */
    public FormatSchema getParserSchema() {
    	return _parser.getSchema();
    }

    /**
     * Convenience method, functionally equivalent to:
     *<code>
     *   iterator.getParser().getCurrentLocation()
     *</code>
     *
     * @return Location of the input stream of the underlying parser
     *
     * @since 2.2.1
     */
    public JsonLocation getCurrentLocation() {
        return _parser.getCurrentLocation();
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    protected void _resync() throws IOException
    {
        final JsonParser p = _parser;
        // First, a quick check to see if we might have been lucky and no re-sync needed
        if (p.getParsingContext() == _seqContext) {
            return;
        }

        while (true) {
            JsonToken t = p.nextToken();
            if ((t == JsonToken.END_ARRAY) || (t == JsonToken.END_OBJECT)) {
                if (p.getParsingContext() == _seqContext) {
                    p.clearCurrentToken();
                    return;
                }
            } else if ((t == JsonToken.START_ARRAY) || (t == JsonToken.START_OBJECT)) {
                p.skipChildren();
            } else if (t == null) {
                return;
            }
        }
    }

    protected <R> R _throwNoSuchElement() {
        throw new NoSuchElementException();
    }

    protected <R> R _handleMappingException(JsonMappingException e) {
        throw new RuntimeJsonMappingException(e.getMessage(), e);
    }

    protected <R> R _handleIOException(IOException e) {
        throw new RuntimeException(e.getMessage(), e);
    }
}
