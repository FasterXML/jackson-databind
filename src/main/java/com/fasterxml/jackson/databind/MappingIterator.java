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
    
    protected final JavaType _type;

    protected final DeserializationContext _context;
    
    protected final JsonDeserializer<T> _deserializer;

    protected JsonParser _parser;
    
    /**
     * Flag that indicates whether input {@link JsonParser} should be closed
     * when we are done or not; generally only called when caller did not
     * pass JsonParser.
     */
    protected final boolean _closeParser;

    /**
     * Flag that is set when we have determined what {@link #hasNextValue()}
     * should value; reset when {@link #nextValue} is called
     */
    protected boolean _hasNextChecked;
    
    /**
     * If not null, "value to update" instead of creating a new instance
     * for each call.
     */
    protected final T _updatedValue;

    /**
     * @deprecated Since 2.1, to be removed
     */
    @Deprecated
    protected MappingIterator(JavaType type, JsonParser jp, DeserializationContext ctxt,
            JsonDeserializer<?> deser)
    {
        this(type, jp, ctxt, deser, true, null);
    }

    /**
     * @param managedParser Whether we "own" the {@link JsonParser} passed or not:
     *   if true, it was created by {@link ObjectReader} and code here needs to
     *   close it; if false, it was passed by calling code and should not be
     *   closed by iterator.
     */
    @SuppressWarnings("unchecked")
    protected MappingIterator(JavaType type, JsonParser jp, DeserializationContext ctxt,
            JsonDeserializer<?> deser,
            boolean managedParser, Object valueToUpdate)
    {
        _type = type;
        _parser = jp;
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
        if (managedParser && jp != null && jp.getCurrentToken() == JsonToken.START_ARRAY) {
            jp.clearCurrentToken();
        }
    }

    @SuppressWarnings("unchecked")
    protected static <T> MappingIterator<T> emptyIterator() {
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
            throw new RuntimeJsonMappingException(e.getMessage(), e);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public T next()
    {
        try {
            return nextValue();
        } catch (JsonMappingException e) {
            throw new RuntimeJsonMappingException(e.getMessage(), e);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void close() throws IOException{
        if(_parser != null) {
            _parser.close();
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
        if (_parser == null) {
            return false;
        }
        if (!_hasNextChecked) {
            JsonToken t = _parser.getCurrentToken();
            _hasNextChecked = true;
            if (t == null) { // un-initialized or cleared; find next
                t = _parser.nextToken();
                // If EOF, no more, or if we hit END_ARRAY (although we don't clear the token).
                if (t == null || t == JsonToken.END_ARRAY) {
                    JsonParser jp = _parser;
                    _parser = null;
                    if (_closeParser) {
                        jp.close();
                    }
                    return false;
                }
            }
        }
        return true;
    }
    
    public T nextValue() throws IOException
    {
        // caller should always call 'hasNext[Value]' first; but let's ensure:
        if (!_hasNextChecked) {
            if (!hasNextValue()) {
                throw new NoSuchElementException();
            }
        }
        if (_parser == null) {
            throw new NoSuchElementException();
        }
        _hasNextChecked = false;
        T result;
        
        if (_updatedValue == null) {
            result = _deserializer.deserialize(_parser, _context);
        } else{
            _deserializer.deserialize(_parser, _context, _updatedValue);
            result = _updatedValue;
        }
        // Need to consume the token too
        _parser.clearCurrentToken();
        return result;
    }

    /**
     * Convenience method for reading all entries accessible via
     * this iterator
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
    public List<T> readAll(List<T> resultList) throws IOException
    {
        while (hasNextValue()) {
    		    resultList.add(nextValue());
        }
        return resultList;
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
}
