package com.fasterxml.jackson.databind;

import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;

/**
 * Enumeration used with <code>ObjectMapper.enableDefaultTyping()</code>
 * to specify what kind of types (classes) default typing should
 * be used for. It will only be used if no explicit type information
 * is found, but this enumeration further limits subset of those types.
 *<p>
 * Since 2.4 there are special exceptions for JSON Tree model
 * types (sub-types of {@link TreeNode}: default typing is never
 * applied to them.
 * Since 2.8 additional checks are made to avoid attempts at default
 * typing primitive-valued properties.
 *<p>
 * NOTE: use of Default Typing can be a potential security risk if incoming
 * content comes from untrusted sources, and it is recommended that this
 * is either not done, or, if enabled, use <code>ObjectMapper.setDefaultTyping()</code>
 * passing a custom {@link TypeResolverBuilder} implementation that white-lists
 * legal types to use.
 */
public enum DefaultTyping {
    /**
     * This value means that only properties that have
     * {@link java.lang.Object} as declared type (including
     * generic types without explicit type) will use default
     * typing.
     */
    JAVA_LANG_OBJECT,
    
    /**
     * Value that means that default typing will be used for
     * properties with declared type of {@link java.lang.Object}
     * or an abstract type (abstract class or interface).
     * Note that this does <b>not</b> include array types.
     * This does NOT apply to {@link TreeNode} and its subtypes.
     */
    OBJECT_AND_NON_CONCRETE,

    /**
     * Value that means that default typing will be used for
     * all types covered by {@link #OBJECT_AND_NON_CONCRETE}
     * plus all array types for them.
     * This does NOT apply to {@link TreeNode} and its subtypes.
     */
    NON_CONCRETE_AND_ARRAYS,
    
    /**
     * Value that means that default typing will be used for
     * all non-final types, with exception of small number of
     * "natural" types (String, Boolean, Integer, Double), which
     * can be correctly inferred from JSON; as well as for
     * all arrays of non-final types.
     * This does NOT apply to {@link TreeNode} and its subtypes.
     */
    NON_FINAL
}