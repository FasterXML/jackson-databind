package com.fasterxml.jackson.databind.introspect;

import java.util.*;

import com.fasterxml.jackson.databind.cfg.MapperConfig;

public class PotentialCreators
{
    public final List<PotentialCreator> constructors;
    
    public final List<PotentialCreator> factories;

    /**
     * Property-based Creator found, if any
     */
    public PotentialCreator propertiesBased;

    public AnnotatedWithParams defaultCreator;

    public final List<PotentialCreator> delegating = new ArrayList<>();

    public PotentialCreators(List<PotentialCreator> constructors,
            List<PotentialCreator> factories)
    {
        this.constructors = constructors;
        this.factories = factories;
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
                    mode, propertiesBased.creator, ctor.creator));
        }
        propertiesBased = ctor.introspectParamNames(config);
    }

    public void addDelegating(PotentialCreator ctor)
    {
        delegating.add(ctor);
    }
    
    public void addDefault(AnnotatedWithParams ctor)
    {
        defaultCreator = ctor;
    }

    /*
    /**********************************************************************
    /* Accessors
    /**********************************************************************
     */

    public boolean hasParametersBasedOrDelegating() {
        return (propertiesBased != null) || !delegating.isEmpty();
    }
}
