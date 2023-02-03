package com.fasterxml.jackson.databind.deser;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.format.*;
import com.fasterxml.jackson.core.io.MergedStream;

import com.fasterxml.jackson.databind.*;

/**
 * Alternative to {@link DataFormatDetector} that needs to be used when
 * using data-binding.
 *
 * @since 2.1
 */
public class DataFormatReaders
{
    /**
     * By default we will look ahead at most 64 bytes; in most cases,
     * much less (4 bytes or so) is needed, but we will allow bit more
     * leniency to support data formats that need more complex heuristics.
     */
    public final static int DEFAULT_MAX_INPUT_LOOKAHEAD = 64;

    /**
     * Ordered list of readers which both represent data formats to
     * detect (in precedence order, starting with highest) and contain
     * factories used for actual detection.
     */
    protected final ObjectReader[] _readers;

    /**
     * Strength of match we consider to be good enough to be used
     * without checking any other formats.
     * Default value is {@link MatchStrength#SOLID_MATCH},
     */
    protected final MatchStrength _optimalMatch;

    /**
     * Strength of minimal match we accept as the answer, unless
     * better matches are found.
     * Default value is {@link MatchStrength#WEAK_MATCH},
     */
    protected final MatchStrength _minimalMatch;

    /**
     * Maximum number of leading bytes of the input that we can read
     * to determine data format.
     *<p>
     * Default value is {@link #DEFAULT_MAX_INPUT_LOOKAHEAD}.
     */
    protected final int _maxInputLookahead;

    /*
    /**********************************************************
    /* Construction
    /**********************************************************
     */

    public DataFormatReaders(ObjectReader... detectors) {
        this(detectors, MatchStrength.SOLID_MATCH, MatchStrength.WEAK_MATCH,
            DEFAULT_MAX_INPUT_LOOKAHEAD);
    }

    public DataFormatReaders(Collection<ObjectReader> detectors) {
        this(detectors.toArray(new ObjectReader[detectors.size()]));
    }

    private DataFormatReaders(ObjectReader[] detectors,
            MatchStrength optMatch, MatchStrength minMatch,
            int maxInputLookahead)
    {
        _readers = detectors;
        _optimalMatch = optMatch;
        _minimalMatch = minMatch;
        _maxInputLookahead = maxInputLookahead;
    }

    /*
    /**********************************************************
    /* Fluent factories for changing match settings
    /**********************************************************
     */

    public DataFormatReaders withOptimalMatch(MatchStrength optMatch) {
        if (optMatch == _optimalMatch) {
            return this;
        }
        return new DataFormatReaders(_readers, optMatch, _minimalMatch, _maxInputLookahead);
    }

    public DataFormatReaders withMinimalMatch(MatchStrength minMatch) {
        if (minMatch == _minimalMatch) {
            return this;
        }
        return new DataFormatReaders(_readers, _optimalMatch, minMatch, _maxInputLookahead);
    }

    public DataFormatReaders with(ObjectReader[] readers) {
        return new DataFormatReaders(readers, _optimalMatch, _minimalMatch, _maxInputLookahead);
    }

    public DataFormatReaders withMaxInputLookahead(int lookaheadBytes)
    {
        if (lookaheadBytes == _maxInputLookahead) {
            return this;
        }
        return new DataFormatReaders(_readers, _optimalMatch, _minimalMatch, lookaheadBytes);
    }

    /*
    /**********************************************************
    /* Fluent factories for changing underlying readers
    /**********************************************************
     */

    public DataFormatReaders with(DeserializationConfig config)
    {
        final int len = _readers.length;
        ObjectReader[] r = new ObjectReader[len];
        for (int i = 0; i < len; ++i) {
            r[i] = _readers[i].with(config);
        }
        return new DataFormatReaders(r, _optimalMatch, _minimalMatch, _maxInputLookahead);
    }

    public DataFormatReaders withType(JavaType type)
    {
        final int len = _readers.length;
        ObjectReader[] r = new ObjectReader[len];
        for (int i = 0; i < len; ++i) {
            r[i] = _readers[i].forType(type);
        }
        return new DataFormatReaders(r, _optimalMatch, _minimalMatch, _maxInputLookahead);
    }

    /*
    /**********************************************************
    /* Public API
    /**********************************************************
     */

    /**
     * Method to call to find format that content (accessible via given
     * {@link InputStream}) given has, as per configuration of this detector
     * instance.
     *
     * @return Matcher object which contains result; never null, even in cases
     *    where no match (with specified minimal match strength) is found.
     */
    public Match findFormat(InputStream in) throws IOException
    {
        return _findFormat(new AccessorForReader(in, new byte[_maxInputLookahead]));
    }

    /**
     * Method to call to find format that given content (full document)
     * has, as per configuration of this detector instance.
     *
     * @return Matcher object which contains result; never null, even in cases
     *    where no match (with specified minimal match strength) is found.
     */
    public Match findFormat(byte[] fullInputData) throws IOException
    {
        return _findFormat(new AccessorForReader(fullInputData));
    }

    /**
     * Method to call to find format that given content (full document)
     * has, as per configuration of this detector instance.
     *
     * @return Matcher object which contains result; never null, even in cases
     *    where no match (with specified minimal match strength) is found.
     *
     * @since 2.1
     */
    public Match findFormat(byte[] fullInputData, int offset, int len) throws IOException
    {
        return _findFormat(new AccessorForReader(fullInputData, offset, len));
    }

    /*
    /**********************************************************
    /* Overrides
    /**********************************************************
     */

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        final int len = _readers.length;
        if (len > 0) {
            sb.append(_readers[0].getFactory().getFormatName());
            for (int i = 1; i < len; ++i) {
                sb.append(", ");
                sb.append(_readers[i].getFactory().getFormatName());
            }
        }
        sb.append(']');
        return sb.toString();
    }

    /*
    /**********************************************************
    /* Internal methods
    /**********************************************************
     */

    private Match _findFormat(AccessorForReader acc) throws IOException
    {
        ObjectReader bestMatch = null;
        MatchStrength bestMatchStrength = null;
        for (ObjectReader f : _readers) {
            acc.reset();
            MatchStrength strength = f.getFactory().hasFormat(acc);
            // if not better than what we have so far (including minimal level limit), skip
            if (strength == null || strength.ordinal() < _minimalMatch.ordinal()) {
                continue;
            }
            // also, needs to better match than before
            if (bestMatch != null) {
                if (bestMatchStrength.ordinal() >= strength.ordinal()) {
                    continue;
                }
            }
            // finally: if it's good enough match, we are done
            bestMatch = f;
            bestMatchStrength = strength;
            if (strength.ordinal() >= _optimalMatch.ordinal()) {
                break;
            }
        }
        return acc.createMatcher(bestMatch, bestMatchStrength);
    }

    /*
    /**********************************************************
    /* Helper classes
    /**********************************************************
     */

    /**
     * We need sub-class here as well, to be able to access efficiently.
     */
    protected static class AccessorForReader extends InputAccessor.Std
    {
        public AccessorForReader(InputStream in, byte[] buffer) {
            super(in, buffer);
        }
        public AccessorForReader(byte[] inputDocument) {
            super(inputDocument);
        }
        public AccessorForReader(byte[] inputDocument, int start, int len) {
            super(inputDocument, start, len);
        }

        public Match createMatcher(ObjectReader match, MatchStrength matchStrength)
        {
            return new Match(_in, _buffer, _bufferedStart, (_bufferedEnd - _bufferedStart),
                    match, matchStrength);
        }
    }

    /**
     * Result class, similar to {@link DataFormatMatcher}
     */
    public static class Match
    {
        protected final InputStream _originalStream;

        /**
         * Content read during format matching process
         */
        protected final byte[] _bufferedData;

        /**
         * Pointer to the first byte in buffer available for reading
         */
        protected final int _bufferedStart;

        /**
         * Number of bytes available in buffer.
         */
        protected final int _bufferedLength;

        /**
         * Factory that produced sufficient match (if any)
         */
        protected final ObjectReader _match;

        /**
         * Strength of match with {@link #_match}
         */
        protected final MatchStrength _matchStrength;

        protected Match(InputStream in, byte[] buffered,
                int bufferedStart, int bufferedLength,
                ObjectReader match, MatchStrength strength)
        {
            _originalStream = in;
            _bufferedData = buffered;
            _bufferedStart = bufferedStart;
            _bufferedLength = bufferedLength;
            _match = match;
            _matchStrength = strength;
        }

        /*
        /**********************************************************
        /* Public API, simple accessors
        /**********************************************************
         */

        /**
         * Accessor to use to see if any formats matched well enough with
         * the input data.
         */
        public boolean hasMatch() { return _match != null; }

        /**
         * Method for accessing strength of the match, if any; if no match,
         * will return {@link MatchStrength#INCONCLUSIVE}.
         */
        public MatchStrength getMatchStrength() {
            return (_matchStrength == null) ? MatchStrength.INCONCLUSIVE : _matchStrength;
        }

        /**
         * Accessor for {@link JsonFactory} that represents format that data matched.
         */
        public ObjectReader getReader() { return _match; }

        /**
         * Accessor for getting brief textual name of matched format if any (null
         * if none). Equivalent to:
         *<pre>
         *   return hasMatch() ? getMatch().getFormatName() : null;
         *</pre>
         */
        public String getMatchedFormatName() {
            return _match.getFactory().getFormatName();
        }

        /*
        /**********************************************************
        /* Public API, factory methods
        /**********************************************************
         */

        /**
         * Convenience method for trying to construct a {@link JsonParser} for
         * parsing content which is assumed to be in detected data format.
         * If no match was found, returns null.
         */
        public JsonParser createParserWithMatch() throws IOException
        {
            if (_match == null) {
                return null;
            }
            JsonFactory jf = _match.getFactory();
            if (_originalStream == null) {
                return jf.createParser(_bufferedData, _bufferedStart, _bufferedLength);
            }
            return jf.createParser(getDataStream());
        }

        /**
         * Method to use for accessing input for which format detection has been done.
         * This <b>must</b> be used instead of using stream passed to detector
         * unless given stream itself can do buffering.
         * Stream will return all content that was read during matching process, as well
         * as remaining contents of the underlying stream.
         */
        public InputStream getDataStream() {
            if (_originalStream == null) {
                return new ByteArrayInputStream(_bufferedData, _bufferedStart, _bufferedLength);
            }
            return new MergedStream(null, _originalStream, _bufferedData, _bufferedStart, _bufferedLength);
        }
    }

}
