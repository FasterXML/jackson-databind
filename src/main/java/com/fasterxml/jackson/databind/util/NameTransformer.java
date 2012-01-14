package com.fasterxml.jackson.databind.util;

/**
 * Helper class used to encapsulate details of name mangling, transforming
 * of names using different strategies (prefixes, suffixes).
 * Default implementation is "no-operation" (aka identity transformation).
 */
public class NameTransformer
{
    /**
     * Singleton "no-operation" transformer which simply returns given
     * name as is. Used commonly as placeholder or marker.
     */
    public final static NameTransformer NOP = new NameTransformer();

    protected NameTransformer() { }

    /**
     * Factory method for constructing a simple transformer based on
     * prefix and/or suffix.
     */
    public static NameTransformer simpleTransformer(final String prefix, final String suffix)
    {
        boolean hasPrefix = (prefix != null) && (prefix.length() > 0);
        boolean hasSuffix = (suffix != null) && (suffix.length() > 0);

        if (hasPrefix) {
            if (hasSuffix) {
                return new NameTransformer() {
                    @Override
                    public String transform(String name) { return prefix + name + suffix; }
                };
            }
            return new NameTransformer() {
                @Override
                public String transform(String name) { return prefix + name; }
            };
        }
        if (hasSuffix) {
            return new NameTransformer() {
                @Override
                public String transform(String name) { return name + suffix; }
            };
        }
        return NOP;
    }
    
    /**
     * Method called when transformation is needed
     */
    public String transform(String name) { return name; }
}
