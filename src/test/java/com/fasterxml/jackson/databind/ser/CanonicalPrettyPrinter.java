package com.fasterxml.jackson.databind.ser;

import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.Separators;

public class CanonicalPrettyPrinter extends DefaultPrettyPrinter {
    private static final long serialVersionUID = 1L;
    private static final DefaultIndenter STABLE_INDENTEER = new DefaultIndenter("    ", "\n");

    public static final PrettyPrinter INSTANCE = new CanonicalPrettyPrinter()
        .withObjectIndenter(STABLE_INDENTEER);    

    @Override
    public DefaultPrettyPrinter withSeparators(Separators separators) {
        _separators = separators;
        // TODO it would be great if it was possible to configure this without
        // overriding
        _objectFieldValueSeparatorWithSpaces = separators.getObjectFieldValueSeparator() + " ";
        return this;
    }
}
