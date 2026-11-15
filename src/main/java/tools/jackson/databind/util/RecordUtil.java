package tools.jackson.databind.util;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;

import tools.jackson.databind.PropertyName;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.introspect.AnnotatedClass;
import tools.jackson.databind.introspect.AnnotatedConstructor;
import tools.jackson.databind.introspect.AnnotatedWithParams;
import tools.jackson.databind.introspect.PotentialCreator;

/**
 * Helper class for finding so-called canonical constructor
 * of Record types.
 */
public class RecordUtil
{
    public static String[] getRecordFieldNames(Class<?> recordType) {
        final RecordComponent[] components = recordType.getRecordComponents();
        if (components == null) {
            // not a record, or no reflective access on native image
            return null;
        }
        return Arrays.stream(components).map(RecordComponent::getName).toArray(String[]::new);
    }

    public static PotentialCreator findCanonicalRecordConstructor(MapperConfig<?> config,
            AnnotatedClass recordClass,
            List<PotentialCreator> constructors)
    {
        final RecordComponent[] components = recordClass.getRawType().getRecordComponents();
        if (components == null) {
            // not a record, or no reflective access on native image
            return null;
        }

        // And then locate the canonical constructor
        final int argCount = components.length;
        // One special case: zero-arg constructor not included in candidate List
        if (argCount == 0) {
            // Bit hacky but has to do: create new PotentialCreator let caller deal
            AnnotatedConstructor defCtor = recordClass.getDefaultConstructor();
            if (defCtor != null) {
                return new PotentialCreator(defCtor, null);
            }
        }

        for (PotentialCreator ctor : constructors) {
            if (!isCanonicalConstructor(components, ctor.creator())) {
                continue;
            }
            // Found it! One more thing; get implicit Record field names:
            final PropertyName[] implicits = new PropertyName[argCount];
            for (int i = 0; i < argCount; ++i) {
                implicits[i] = PropertyName.construct(components[i].getName());
            }
            return ctor.introspectParamNames(config, implicits);
        }

        throw new IllegalArgumentException("Failed to find the canonical Record constructor of type "
                        +ClassUtil.getTypeDescription(recordClass.getType()));
    }

    /**
     * Helper method for checking whether given Creator is the canonical constructor
     * of Record type with given components: constructor with the same parameter
     * types, in the same order.
     *
     * @since 3.4
     */
    public static boolean isCanonicalConstructor(RecordComponent[] components,
            AnnotatedWithParams creator)
    {
        if (!(creator instanceof AnnotatedConstructor)
                || (creator.getParameterCount() != components.length)) {
            return false;
        }
        for (int i = 0; i < components.length; ++i) {
            if (!creator.getRawParameterType(i).equals(components[i].getType())) {
                return false;
            }
        }
        return true;
    }
}
