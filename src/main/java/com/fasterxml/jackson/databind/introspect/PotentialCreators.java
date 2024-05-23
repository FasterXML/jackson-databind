package com.fasterxml.jackson.databind.introspect;

import java.util.*;

import com.fasterxml.jackson.databind.cfg.MapperConfig;

public class PotentialCreators
{
    /**
     * Property-based Creator found, if any
     */
    public PotentialCreator propertiesBased;

    private List<PotentialCreator> explicitDelegating;

    private List<PotentialCreator> implicitDelegatingConstructors;
    private List<PotentialCreator> implicitDelegatingFactories;

    public PotentialCreators() { }

    /*
    /**********************************************************************
    /* Accumulating candidates
    /**********************************************************************
     */
    
    // desc -> "explicit", "implicit" etc
    public void setPropertiesBased(MapperConfig<?> config, PotentialCreator ctor, String mode)
    {
        if (propertiesBased != null) {
            throw new IllegalArgumentException(String.format(
                    "Conflicting property-based creators: already had %s creator %s, encountered another: %s",
                    mode, propertiesBased.creator(), ctor.creator()));
        }
        propertiesBased = ctor.introspectParamNames(config);
    }

    public void addExplicitDelegating(PotentialCreator ctor)
    {
        if (explicitDelegating == null) {
            explicitDelegating = new ArrayList<>();
        }
        explicitDelegating.add(ctor);
    }

    public void setImplicitDelegating(List<PotentialCreator> implicitConstructors,
            List<PotentialCreator> implicitFactories)
    {
        implicitDelegatingConstructors = implicitConstructors;
        implicitDelegatingFactories = implicitFactories;
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
        return (propertiesBased != null) || (explicitDelegating != null && !explicitDelegating.isEmpty());
    }

    public List<PotentialCreator> getExplicitDelegating() {
        return (explicitDelegating == null) ? Collections.emptyList() : explicitDelegating;
    }

    public List<PotentialCreator> getImplicitDelegatingFactories() {
        return (implicitDelegatingFactories == null) ? Collections.emptyList() : implicitDelegatingFactories;
    }
    
    public List<PotentialCreator> getImplicitDelegatingConstructors() {
        return (implicitDelegatingConstructors == null) ? Collections.emptyList() : implicitDelegatingConstructors;
    }
}
