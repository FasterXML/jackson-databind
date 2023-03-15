package com.fasterxml.jackson.databind;

/**
 * Defines how the string representation of an enum is converted into an external property name for mapping
 * during deserialization.
 *
 * @since 2.15
 */
public interface EnumNamingStrategy {

    /**
     * Translates the given <code>enumName</code> into an external property name according to
     * the implementation of this {@link EnumNamingStrategy}.
     *
     * @param enumName the name of the enum value to translate
     * @return the external property name that corresponds to the given <code>enumName</code>
     * according to the implementation of this {@link EnumNamingStrategy}.
     *
     * @since 2.15
     */
    public String convertEnumToExternalName(String enumName);

}
