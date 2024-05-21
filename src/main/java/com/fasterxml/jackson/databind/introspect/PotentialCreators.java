package com.fasterxml.jackson.databind.introspect;

import java.util.*;

import com.fasterxml.jackson.databind.cfg.MapperConfig;

public class PotentialCreators
{
    /**
     * Property-based Creator found, if any
     */
    public PotentialCreator propertiesBased;

    private List<PotentialCreator> delegating;

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
        if (delegating == null) {
            delegating = new ArrayList<>();
        }
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
        return (propertiesBased != null) || (delegating != null && !delegating.isEmpty());
    }

    public List<PotentialCreator> getDelegating() {
        return (delegating == null) ? Collections.emptyList() : delegating;
    }
}
