package tools.jackson.databind.introspect;

import java.util.*;

import tools.jackson.databind.cfg.MapperConfig;

public class PotentialCreators
{
    /**
     * Property-based Creator found, if any
     */
    public PotentialCreator propertiesBased;

    public final List<PotentialCreator> delegating = new ArrayList<>();

    public PotentialCreators()
    {
    }

    /*
    /**********************************************************************
    /* Accumulating candidates
    /**********************************************************************
     */
    
    // desc -> "explicit", "implicit" etc
    public void addPropertiesBased(MapperConfig<?> config, PotentialCreator ctor, String mode)
    {
        if (propertiesBased != null) {
            throw new IllegalArgumentException(String.format(
                    "Conflicting property-based creators: already had %s creator %s, encountered another: %s",
                    mode, propertiesBased.creator(), ctor.creator()));
        }
        propertiesBased = ctor.introspectParamNames(config);
    }

    public void addDelegating(PotentialCreator ctor)
    {
        delegating.add(ctor);
    }

    /*
    /**********************************************************************
    /* Accessors
    /**********************************************************************
     */

    public boolean hasPropertiesBased() {
        return (propertiesBased != null);
    }

    public boolean hasPropertiesBasedOrDelegating() {
        return (propertiesBased != null) || !delegating.isEmpty();
    }
}
