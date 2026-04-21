package tools.jackson.databind.introspect;

import java.util.*;

import tools.jackson.databind.cfg.MapperConfig;

public class PotentialCreators
{
    /**
     * Property-based Creator found, if any
     */
    public PotentialCreator propertiesBased;

    /**
     * Whether {@link #propertiesBased} was registered via
     * {@link #setExplicitPropertiesBased} (i.e. from an explicit
     * {@code @JsonCreator}-style annotation), as opposed to an implicit
     * or primary fallback.
     */
    private boolean propertiesBasedExplicit;

    private List<PotentialCreator> explicitDelegating;

    private List<PotentialCreator> implicitDelegatingConstructors;
    private List<PotentialCreator> implicitDelegatingFactories;

    public PotentialCreators() { }

    /*
    /**********************************************************************
    /* Accumulating candidates
    /**********************************************************************
     */
    
    // {@code mode} is used only for diagnostic messages (e.g. "implicit",
    // "primary"); for creators from an explicit {@code @JsonCreator} annotation
    // use {@link #setExplicitPropertiesBased} instead.
    public void setPropertiesBased(MapperConfig<?> config, PotentialCreator ctor, String mode)
    {
        _setPropertiesBased(config, ctor, mode, false);
    }

    /**
     * Variant of {@link #setPropertiesBased} for creators coming from explicit
     * {@code @JsonCreator}-style annotations; records that the registered
     * creator is {@code explicit} so later fallback logic can defer to it.
     */
    public void setExplicitPropertiesBased(MapperConfig<?> config, PotentialCreator ctor)
    {
        _setPropertiesBased(config, ctor, "explicit", true);
    }

    private void _setPropertiesBased(MapperConfig<?> config, PotentialCreator ctor,
            String mode, boolean explicit)
    {
        if (propertiesBased != null) {
            throw new IllegalArgumentException(String.format(
                    "Conflicting property-based creators: already had %s creator %s, encountered another: %s",
                    mode, propertiesBased.creator(), ctor.creator()));
        }
        propertiesBased = ctor.introspectParamNames(config);
        propertiesBasedExplicit = explicit;
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

    // @since 2.18.1
    public boolean hasDelegating() {
        return (explicitDelegating != null) && !explicitDelegating.isEmpty();
    }
    
    public boolean hasPropertiesBased() {
        return (propertiesBased != null);
    }

    public boolean hasExplicitPropertiesBased() {
        return propertiesBasedExplicit;
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
