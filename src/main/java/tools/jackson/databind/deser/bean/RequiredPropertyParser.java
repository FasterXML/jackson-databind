package tools.jackson.databind.deser.bean;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.SerializableString;
import tools.jackson.core.sym.PropertyNameMatcher;
import tools.jackson.core.util.JsonParserDelegate;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.SettableBeanProperty;
import tools.jackson.databind.deser.impl.ExternalTypeHandler;
import tools.jackson.databind.deser.impl.ObjectIdValueProperty;

/**
 * Tracks the input for a single bean or builder. A property may be buffered,
 * or its value may be skipped by a null provider, so tracking assignments
 * would miss some input properties.
 */
final class RequiredPropertyParser extends JsonParserDelegate
{
    private final PropertyNameMatcher _matcher;
    private final SettableBeanProperty[] _properties;
    private final SettableBeanProperty _idProperty;
    private final Set<SettableBeanProperty> _missing;
    private final JsonToken _endToken;
    private ExternalTypeHandler _externalTypes;
    private int _nested;
    private int _arrayIndex;
    private boolean _endOfInput;

    private RequiredPropertyParser(JsonParser p, PropertyNameMatcher matcher,
            SettableBeanProperty[] properties, SettableBeanProperty idProperty,
            ExternalTypeHandler externalTypes,
            Set<SettableBeanProperty> missing, String consumedTypeId)
    {
        super(p);
        _matcher = matcher;
        _properties = properties;
        _idProperty = idProperty;
        _externalTypes = externalTypes;
        _missing = missing;
        _endToken = (matcher == null) ? JsonToken.END_ARRAY : JsonToken.END_OBJECT;
        if (consumedTypeId != null) {
            _markPresent(matcher.matchName(consumedTypeId));
        }
        if (matcher != null && p.currentToken() != JsonToken.START_OBJECT) {
            _observe(p.currentToken());
        }
    }

    static boolean isRequired(SettableBeanProperty prop, Set<String> defaultRequiredNames)
    {
        return prop != null && !prop.isCreatorProperty() && !prop.isInjectionOnly()
                && (prop.isRequired() || (prop.getMetadata().getRequired() == null
                        && defaultRequiredNames.contains(prop.getName())));
    }

    static RequiredPropertyParser start(JsonParser p, BeanPropertyMap properties,
            SettableBeanProperty idProperty, ExternalTypeHandler externalTypes,
            Set<SettableBeanProperty> required, Class<?> activeView, String consumedTypeId)
    {
        JsonToken token = p.currentToken();
        if (token != JsonToken.START_OBJECT && token != JsonToken.PROPERTY_NAME
                && token != JsonToken.END_OBJECT) {
            return null;
        }
        Set<SettableBeanProperty> missing = null;
        for (SettableBeanProperty prop : required) {
            if (_visible(prop, activeView)) {
                if (missing == null) {
                    missing = new LinkedHashSet<>();
                }
                missing.add(prop);
            }
        }
        return missing == null ? null
                : new RequiredPropertyParser(p, properties.getNameMatcher(), properties.getNameMatcherProperties(),
                        idProperty, externalTypes, missing, consumedTypeId);
    }

    static RequiredPropertyParser startArray(JsonParser p, SettableBeanProperty[] ordered,
            SettableBeanProperty idProperty, Set<SettableBeanProperty> required, Class<?> activeView)
    {
        if (required.isEmpty()) {
            return null;
        }
        Set<SettableBeanProperty> missing = null;
        for (SettableBeanProperty matched : ordered) {
            SettableBeanProperty prop = inputProperty(matched, idProperty);
            if (prop != null && required.contains(prop) && _visible(prop, activeView)) {
                if (missing == null) {
                    missing = new LinkedHashSet<>();
                }
                missing.add(prop);
            }
        }
        return missing == null ? null
                : new RequiredPropertyParser(p, null, ordered, idProperty, null, missing, null);
    }

    private static boolean _visible(SettableBeanProperty prop, Class<?> view) {
        return view == null || prop.visibleInView(view);
    }

    static SettableBeanProperty inputProperty(SettableBeanProperty matched,
            SettableBeanProperty idProperty)
    {
        // Object-id binding uses a synthetic required marker. For a property-based
        // id, use the actual property to determine what the caller declared.
        return (matched instanceof ObjectIdValueProperty) ? idProperty : matched;
    }

    Object read(DeserializationContext ctxt, Supplier<Object> bind, Class<?> builderType)
    {
        return ctxt.withParser(this, () -> {
            Object result = bind.get();
            if (result != null && (builderType == null || builderType.isInstance(result))) {
                _checkForMissing(ctxt);
            }
            return result;
        });
    }

    Consumer<SettableBeanProperty> takeExternalPropertyObserver(ExternalTypeHandler handler) {
        if (_externalTypes != handler) {
            return null;
        }
        // Only this object's handler may report values to this tracker.
        _externalTypes = null;
        return _missing::remove;
    }

    private void _checkForMissing(DeserializationContext ctxt)
    {
        if (_endOfInput && !_missing.isEmpty()) {
            SettableBeanProperty prop = _missing.iterator().next();
            ctxt.reportInputMismatch(prop, "Missing required property '%s'", prop.getName());
        }
    }

    private void _markPresent(int index) {
        if (index >= 0 && index < _properties.length) {
            _missing.remove(inputProperty(_properties[index], _idProperty));
        }
    }

    private void _observe(JsonToken token)
    {
        if (token == null || _missing.isEmpty() || _endOfInput) {
            return;
        }
        if (_matcher == null && _nested == 0 && (token.isScalarValue() || token.isStructStart())) {
            _markPresent(_arrayIndex++);
        }
        switch (token) {
        case PROPERTY_NAME:
            if (_nested == 0 && _matcher != null) {
                _markPresent(delegate.currentNameMatch(_matcher));
            }
            break;
        case START_OBJECT:
        case START_ARRAY:
            ++_nested;
            break;
        case END_OBJECT:
        case END_ARRAY:
            if (_nested > 0) {
                --_nested;
            } else {
                _endOfInput = (token == _endToken);
            }
            break;
        default:
        }
    }

    @Override
    public JsonToken nextToken() {
        JsonToken token = delegate.nextToken();
        _observe(token);
        return token;
    }

    @Override
    public JsonToken nextValue() {
        JsonToken token = nextToken();
        return (token == JsonToken.PROPERTY_NAME) ? nextToken() : token;
    }

    @Override
    public String nextName() {
        String name = delegate.nextName();
        _observe(delegate.currentToken());
        return name;
    }

    @Override
    public boolean nextName(SerializableString name) {
        boolean match = delegate.nextName(name);
        _observe(delegate.currentToken());
        return match;
    }

    @Override
    public int nextNameMatch(PropertyNameMatcher matcher) {
        int match = delegate.nextNameMatch(matcher);
        _observe(delegate.currentToken());
        return match;
    }

    @Override
    public JsonParser skipChildren() {
        JsonToken token = delegate.currentToken();
        delegate.skipChildren();
        if (token == JsonToken.START_OBJECT || token == JsonToken.START_ARRAY) {
            _observe(delegate.currentToken());
        }
        return this;
    }
}
