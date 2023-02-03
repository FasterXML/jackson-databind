package com.fasterxml.jackson.databind.util;

/**
 * Helper class used to encapsulate details of name mangling, transforming
 * of names using different strategies (prefixes, suffixes).
 * Default implementation is "no-operation" (aka identity transformation).
 */
public abstract class NameTransformer
{
    /**
     * Singleton "no-operation" transformer which simply returns given
     * name as is. Used commonly as placeholder or marker.
     */
    public final static NameTransformer NOP = new NopTransformer();

    protected final static class NopTransformer
        extends NameTransformer
        implements java.io.Serializable
    {
        private static final long serialVersionUID = 1L;

        @Override
        public String transform(String name) {
            return name;
        }
        @Override
        public String reverse(String transformed) {
            // identity transformation is always reversible:
            return transformed;
        }
    }

    protected NameTransformer() { }

    /**
     * Factory method for constructing a simple transformer based on
     * prefix and/or suffix.
     */
    public static NameTransformer simpleTransformer(final String prefix, final String suffix)
    {
        boolean hasPrefix = (prefix != null) && !prefix.isEmpty();
        boolean hasSuffix = (suffix != null) && !suffix.isEmpty();

        if (hasPrefix) {
            if (hasSuffix) {
                return new NameTransformer() {
                    @Override
                    public String transform(String name) { return prefix + name + suffix; }
                    @Override
                    public String reverse(String transformed) {
                        if (transformed.startsWith(prefix)) {
                            String str = transformed.substring(prefix.length());
                            if (str.endsWith(suffix)) {
                                return str.substring(0, str.length() - suffix.length());
                            }
                        }
                        return null;
                    }
                    @Override
                    public String toString() { return "[PreAndSuffixTransformer('"+prefix+"','"+suffix+"')]"; }
                };
            }
            return new NameTransformer() {
                @Override
                public String transform(String name) { return prefix + name; }
                @Override
                public String reverse(String transformed) {
                    if (transformed.startsWith(prefix)) {
                        return transformed.substring(prefix.length());
                    }
                    return null;
                }
                @Override
                public String toString() { return "[PrefixTransformer('"+prefix+"')]"; }
            };
        }
        if (hasSuffix) {
            return new NameTransformer() {
                @Override
                public String transform(String name) { return name + suffix; }
                @Override
                public String reverse(String transformed) {
                    if (transformed.endsWith(suffix)) {
                        return transformed.substring(0, transformed.length() - suffix.length());
                    }
                    return null;
                }
                @Override
                public String toString() { return "[SuffixTransformer('"+suffix+"')]"; }
            };
        }
        return NOP;
    }

    /**
     * Method that constructs transformer that applies given transformers
     * as a sequence; essentially combines separate transform operations
     * into one logical transformation.
     */
    public static NameTransformer chainedTransformer(NameTransformer t1, NameTransformer t2) {
        return new Chained(t1, t2);
    }

    /**
     * Method called when (forward) transformation is needed.
     */
    public abstract String transform(String name);

    /**
     * Method called when reversal of transformation is needed; should return
     * null if this is not possible, that is, given name cannot have been
     * result of calling {@link #transform} of this object.
     */
    public abstract String reverse(String transformed);

    public static class Chained extends NameTransformer
        implements java.io.Serializable
    {
        private static final long serialVersionUID = 1L;

        protected final NameTransformer _t1, _t2;

        public Chained(NameTransformer t1, NameTransformer t2) {
            _t1 = t1;
            _t2 = t2;
        }

        @Override
        public String transform(String name) {
            return _t1.transform(_t2.transform(name));
        }

        @Override
        public String reverse(String transformed) {
            transformed = _t1.reverse(transformed);
            if (transformed != null) {
                transformed = _t2.reverse(transformed);
            }
            return transformed;
        }

        @Override
        public String toString() { return "[ChainedTransformer("+_t1+", "+_t2+")]"; }
    }
}
