package tools.jackson.databind.util;

import tools.jackson.core.util.Named;
import tools.jackson.databind.PropertyName;

/**
 * Extension over {@link Named} to expose full name; most relevant
 * for formats like XML that use namespacing.
 *
 * @since 3.0
 */
public interface FullyNamed extends Named
{
    PropertyName getFullName();

    default boolean hasName(PropertyName name)  {
        return getFullName().equals(name);
    }
}
