package tools.jackson.databind.deser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.annotation.ObjectIdGenerator;
import com.fasterxml.jackson.annotation.ObjectIdResolver;

import tools.jackson.core.JacksonException;
import tools.jackson.core.TokenStreamLocation;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;

/**
 * Simple value container for containing information about single Object Id
 * during deserialization
 */
public class ReadableObjectId
{
    /**
     * @since 2.8 (with this name, formerly `public Object item`)
     */
    protected Object _item;

    /**
     * Flag set when {@link #tryReplaceBoundItem} is called, indicating that
     * {@link #_item} has been replaced and should not be refreshed from resolver.
     *
     * @since 3.2
     */
    protected boolean _itemReplaced;

    protected final ObjectIdGenerator.IdKey _key;

    protected LinkedList<Referring> _referringProperties;

    /**
     * Referring properties that have already been resolved via {@link #bindItem}.
     * Kept alive so that {@link #notifyReferringsOfRebind} can propagate
     * builder-to-built-object replacements (e.g., after {@code finishBuild}).
     *
     * @since 3.2
     */
    protected List<Referring> _resolvedReferringProperties;

    protected ObjectIdResolver _resolver;

    public ReadableObjectId(ObjectIdGenerator.IdKey key) {
        _key = key;
    }

    public void setResolver(ObjectIdResolver resolver) {
        _resolver = resolver;
    }

    public ObjectIdGenerator.IdKey getKey() {
        return _key;
    }

    public void appendReferring(Referring currentReferring) {
        if (_referringProperties == null) {
            _referringProperties = new LinkedList<>();
        }
        _referringProperties.add(currentReferring);
    }

    /**
     * Method called to assign actual POJO to which ObjectId refers to: will
     * also handle referring properties, if any, by assigning POJO.
     */
    public void bindItem(DeserializationContext ctxt, Object ob) throws JacksonException
    {
        _resolver.bindItem(_key, ob);
        _item = ob;
        Object id = _key.key;
        if (_referringProperties != null) {
            Iterator<Referring> it = _referringProperties.iterator();
            _referringProperties = null;
            while (it.hasNext()) {
                Referring ref = it.next();
                ref.handleResolvedForwardReference(ctxt, id, ob);
                // Keep for potential rebind after Builder finishBuild
                if (_resolvedReferringProperties == null) {
                    _resolvedReferringProperties = new ArrayList<>();
                }
                _resolvedReferringProperties.add(ref);
            }
        }
    }

    /**
     * Method called to try to replace the bound item after a delegating
     * {@code @JsonCreator} has converted the intermediate delegate object
     * into the final bean.
     * Unlike {@link #bindItem}, this method does not go through the
     * {@link ObjectIdResolver} (which would reject re-binding) but instead
     * directly replaces the tracked item.
     *
     * @param delegate The intermediate delegate object to match against current binding
     * @param newItem The final bean to replace the delegate with
     *
     * @return {@code true} if this entry was bound to {@code delegate} and was
     *   replaced; {@code false} if not bound to {@code delegate}
     *
     * @since 3.2
     */
    public boolean tryReplaceBoundItem(Object delegate, Object newItem) {
        if (resolve() == delegate) {
            _item = newItem;
            _itemReplaced = true;
            return true;
        }
        return false;
    }

    /**
     * Method called after {@link #tryReplaceBoundItem} to notify previously-resolved
     * {@link Referring} instances that the bound item has been replaced (e.g.,
     * builder → built object). Collection-like Referring implementations should
     * override {@link Referring#handleItemRebind} to swap the old item for the new one.
     *
     * @since 3.2
     */
    public void notifyReferringsOfRebind(Object oldItem, Object newItem)
            throws JacksonException
    {
        if (_resolvedReferringProperties != null) {
            for (Referring ref : _resolvedReferringProperties) {
                ref.handleItemRebind(oldItem, newItem);
            }
        }
    }

    public Object resolve(){
        // [databind#1706] If item was replaced (e.g., after delegating creator),
        // return the replacement instead of refreshing from resolver
        // (which still has stale reference to the intermediate delegate object)
        if (_itemReplaced) {
            return _item;
        }
        return (_item = _resolver.resolveId(_key));
    }

    public boolean hasReferringProperties() {
        return (_referringProperties != null) && !_referringProperties.isEmpty();
    }

    public Iterator<Referring> referringProperties() {
        if (_referringProperties == null) {
            return Collections.<Referring> emptyList().iterator();
        }
        return _referringProperties.iterator();
    }

    /**
     * Method called by {@link DeserializationContext} at the end of deserialization
     * if this Object Id was not resolved during normal processing. Call is made to
     * allow custom implementations to use alternative resolution strategies; currently
     * the only way to make use of this functionality is by sub-classing
     * {@link ReadableObjectId} and overriding this method.
     *<p>
     * Default implementation simply returns <code>false</code> to indicate that resolution
     * attempt did not succeed.
     *
     * @return True, if resolution succeeded (and no error needs to be reported); false to
     *   indicate resolution did not succeed.
     *
     * @since 2.6
     */
    public boolean tryToResolveUnresolved(DeserializationContext ctxt)
    {
        return false;
    }

    /**
     * Allow access to the resolver in case anybody wants to use it directly, for
     * examples from
     * {@link tools.jackson.databind.deser.DeserializationContextExt#tryToResolveUnresolvedObjectId}.
     *
     * @return The registered resolver
     *
     * @since 2.7
     */
    public ObjectIdResolver getResolver() {
        return _resolver;
    }

    @Override
    public String toString() {
        return String.valueOf(_key);
    }

    /*
    /**********************************************************
    /* Helper classes
    /**********************************************************
     */

    public static abstract class Referring {
        private final UnresolvedForwardReference _reference;
        private final Class<?> _beanType;

        public Referring(UnresolvedForwardReference ref, Class<?> beanType) {
            _reference = ref;
            _beanType = beanType;
        }

        public Referring(UnresolvedForwardReference ref, JavaType beanType) {
            _reference = ref;
            _beanType = beanType.getRawClass();
        }

        public TokenStreamLocation getLocation() { return _reference.getLocation(); }
        public Class<?> getBeanType() { return _beanType; }

        public abstract void handleResolvedForwardReference(DeserializationContext ctxt,
                Object id, Object value)
            throws JacksonException;

        public boolean hasId(Object id) {
            return id.equals(_reference.getUnresolvedId());
        }

        /**
         * Method for checking if this forward reference was registered against
         * the given container instance (the object that will receive the resolved
         * value). Used for detecting issues with Builder-based deserialization
         * where the container (builder) is discarded after building.
         *
         * @param obj The object to check against
         * @return {@code true} if this referring was registered against {@code obj}
         *
         * @since 3.2
         */
        /**
         * Called when the resolved item has been rebound (e.g., builder → built object).
         * Implementations that hold resolved values in mutable containers (collections,
         * arrays, maps) should replace the old item with the new one. Default no-op
         * since scalar property references are set on the POJO directly and captured
         * via constructor before build.
         *
         * @param oldItem The previous item (e.g., the builder)
         * @param newItem The replacement item (e.g., the built object)
         *
         * @since 3.2
         */
        public void handleItemRebind(Object oldItem, Object newItem)
                throws JacksonException
        {
            // no-op by default
        }

        public boolean refersTo(Object obj) {
            return false;
        }
    }
}
