package com.fasterxml.jackson.databind.cfg;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.io.CharacterEscapes;
import com.fasterxml.jackson.core.io.SerializedString;
import com.fasterxml.jackson.core.util.Instantiatable;

/**
 * Helper class used for containing settings specifically related
 * to (re)configuring {@link JsonGenerator} constructed for
 * writing output.
 */
public final class GeneratorSettings
    implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    protected final static GeneratorSettings EMPTY = new GeneratorSettings(null, null, null, null);

    /**
     * Also need to use a null marker for root value separator
     */
    protected final static SerializedString NULL_ROOT_VALUE_SEPARATOR = new SerializedString("");

    public final static GeneratorSettings empty = new GeneratorSettings(null, null, null,
            null);

    /**
     * To allow for dynamic enabling/disabling of pretty printing,
     * pretty printer can be optionally configured for writer
     * as well
     */
    public final PrettyPrinter prettyPrinter;

    /**
     * When using data format that uses a schema, schema is passed
     * to generator.
     */
    public final FormatSchema schema;

    /**
     * Caller may want to specify character escaping details, either as
     * defaults, or on call-by-call basis.
     */
    public final CharacterEscapes characterEscapes;

    /**
     * Caller may want to override so-called "root value separator",
     * String added (verbatim, with no quoting or escaping) between
     * values in root context. Default value is a single space character,
     * but this is often changed to linefeed.
     */
    public final SerializableString rootValueSeparator;

    public GeneratorSettings(PrettyPrinter pp, FormatSchema sch,
            CharacterEscapes esc, SerializableString rootSep) {
        prettyPrinter = pp;
        schema = sch;
        characterEscapes = esc;
        rootValueSeparator = rootSep;
    }

    public static GeneratorSettings empty() {
        return EMPTY;
    }

    public GeneratorSettings with(PrettyPrinter pp) {
        return (pp == prettyPrinter) ? this
                : new GeneratorSettings(pp, schema, characterEscapes, rootValueSeparator);
    }

    public GeneratorSettings with(FormatSchema sch) {
        return (schema == sch) ? this
                : new GeneratorSettings(prettyPrinter, sch, characterEscapes, rootValueSeparator);
    }

    public GeneratorSettings with(CharacterEscapes esc) {
        return (characterEscapes == esc) ? this
                : new GeneratorSettings(prettyPrinter, schema, esc, rootValueSeparator);
    }

    public GeneratorSettings withRootValueSeparator(String sep) {
        if (sep == null) {
            if (rootValueSeparator == NULL_ROOT_VALUE_SEPARATOR) {
                return this;
            }
            return new GeneratorSettings(prettyPrinter, schema, characterEscapes, NULL_ROOT_VALUE_SEPARATOR);
        }
        if (sep.equals(_rootValueSeparatorAsString())) {
            return this;
        }
        return new GeneratorSettings(prettyPrinter, schema, characterEscapes,
                new SerializedString(sep));
    }

    public GeneratorSettings withRootValueSeparator(SerializableString sep) {
        if (sep == null) {
            if (rootValueSeparator == null) {
                return this;
            }
            return new GeneratorSettings(prettyPrinter, schema, characterEscapes, null);
        }
        if (sep.equals(rootValueSeparator)) {
            return this;
        }
        return new GeneratorSettings(prettyPrinter, schema, characterEscapes, sep);
    }

    private final String _rootValueSeparatorAsString() {
        return (rootValueSeparator == null) ? null : rootValueSeparator.getValue();
    }

    /*
    /**********************************************************
    /* ObjectWriteContext support methods
    /**********************************************************
     */

    public FormatSchema getSchema() {
        return schema;
    }

    public CharacterEscapes getCharacterEscapes() {
        return characterEscapes;
    }

    public PrettyPrinter getPrettyPrinter() {
        PrettyPrinter pp = prettyPrinter;
        if (pp != null) {
            if (pp instanceof Instantiatable<?>) {
                pp = (PrettyPrinter) ((Instantiatable<?>) pp).createInstance();
            }
            return pp;
        }
        return null;
    }

    public SerializableString getRootValueSeparator(SerializableString defaultSep) {
        if (rootValueSeparator == null) {
            return defaultSep;
        }
        if (rootValueSeparator == NULL_ROOT_VALUE_SEPARATOR) {
            return null;
        }
        return rootValueSeparator;
    }
}