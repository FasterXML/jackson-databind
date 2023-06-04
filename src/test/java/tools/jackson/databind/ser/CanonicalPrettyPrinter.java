package tools.jackson.databind.ser;

import tools.jackson.core.PrettyPrinter;
import tools.jackson.core.util.DefaultIndenter;
import tools.jackson.core.util.DefaultPrettyPrinter;
import tools.jackson.core.util.Separators;

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
        _nameValueSeparatorWithSpaces = separators.getObjectNameValueSeparator() + " ";
        return this;
    }
}
