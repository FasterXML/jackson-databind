Project: jackson-databind

------------------------------------------------------------------------
=== Releases === 
------------------------------------------------------------------------

2.10.0 (not yet released)

#2059: Remove `final` modifier for `TypeFactory`
 (requested by Thibaut R)
#2115: Support naive deserialization of `Serializable` values as "untyped", same
  as `java.lang.Object`
 (requested by Christopher S)
#2116: Make NumberSerializers.Base public and its inherited classes not final
 (requested by Édouard M)
#2126: `DeserializationContext.instantiationException()` throws `InvalidDefinitionException`

2.9.7 (not yet released)

#2060: `UnwrappingBeanPropertyWriter` incorrectly assumes the found serializer is
  of type `UnwrappingBeanSerializer`
 (reported by Petar T)
#2064: Cannot set custom format for `SqlDateSerializer` globally
 (reported by Brandon K)
#2079: NPE when visiting StaticListSerializerBase
 (reported by WorldSEnder@github)
#2082: `FactoryBasedEnumDeserializer` should be cachable
#2088: `@JsonUnwrapped` fields are skipped when using `PropertyBasedCreator` if
  they appear after the last creator property
 (reported, fix contributed by 6bangs@github)
#2096: `TreeTraversingParser` does not take base64 variant into account
 (reported by tangiel@github)
#2097: Block more classes from polymorphic deserialization (CVE-2018-14718
  - CVE-2018-14721)
#2109: Canonical string for reference type is built incorrectly
 (reported by svarzee@github)
#2120: `NioPathDeserializer` improvement
 (contributed by Semyon L)
#2128: Location information included twice for some `JsonMappingException`s

2.9.6 (12-Jun-2018)

#955: Add `MapperFeature.USE_BASE_TYPE_AS_DEFAULT_IMPL` to use declared base type
   as `defaultImpl` for polymorphic deserialization
  (contributed by mikeldpl@github)
#1328: External property polymorphic deserialization does not work with enums
#1565: Deserialization failure with Polymorphism using JsonTypeInfo `defaultImpl`,
  subtype as target
#1964: Failed to specialize `Map` type during serialization where key type
  incompatibility overidden via "raw" types
 (reported by ptirador@github)
#1990: MixIn `@JsonProperty` for `Object.hashCode()` is ignored
 (reported by Freddy B)
#1991: Context attributes are not passed/available to custom serializer if object is in POJO
 (reported by dletin@github)
#1998: Removing "type" attribute with Mixin not taken in account if
  using ObjectMapper.copy()
 (reported by SBKila@github)
#1999: "Duplicate property" issue should mention which class it complains about
 (reported by Ondrej Z)
#2001: Deserialization issue with `@JsonIgnore` and `@JsonCreator` + `@JsonProperty`
  for same property name
 (reported, fix contributed by Jakub S)
#2015: `@Jsonsetter with Nulls.SKIP` collides with
  `DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL` when parsing enum
 (reported by ndori@github)
#2016: Delegating JsonCreator disregards JsonDeserialize info
 (reported by Carter K)
#2019: Abstract Type mapping in 2.9 fails when multiple modules are registered
 (reported by asger82@github)
#2021: Delegating JsonCreator disregards `JsonDeserialize.using` annotation
#2023: `JsonFormat.Feature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT` not working
  with `null` coercion with `@JsonSetter`
#2027: Concurrency error causes `IllegalStateException` on `BeanPropertyMap`
 (reported by franboragina@github)
#2032: CVE-2018-11307: Potential information exfiltration with default typing, serialization gadget from MyBatis
 (reported by Guixiong Wu)
#2034: Serialization problem with type specialization of nested generic types
 (reported by Reinhard P)
#2038: JDK Serializing and using Deserialized `ObjectMapper` loses linkage
  back from `JsonParser.getCodec()`
 (reported by Chetan N)
#2051: Implicit constructor property names are not renamed properly with
  `PropertyNamingStrategy`
#2052: CVE-2018-12022: Block polymorphic deserialization of types from Jodd-db library
 (reported by Guixiong Wu)
#2058: CVE-2018-12023: Block polymorphic deserialization of types from Oracle JDBC driver
 (reported by Guixiong Wu)

2.9.5 (26-Mar-2018)

#1911: Allow serialization of `BigDecimal` as String, using
  `@JsonFormat(shape=Shape.String)`, config overrides
 (suggested by cen1@github)
#1912: `BeanDeserializerModifier.updateBuilder()` not work to set custom
  deserializer on a property (since 2.9.0)
 (contributed by Deblock T)
#1931: Two more `c3p0` gadgets to exploit default typing issue
 (reported by lilei@venusgroup.com.cn)
#1932: `EnumMap` cannot deserialize with type inclusion as property
#1940: `Float` values with integer value beyond `int` lose precision if
  bound to `long`
 (reported by Aniruddha M)
#1941: `TypeFactory.constructFromCanonical()` throws NPE for Unparameterized
  generic canonical strings
 (reported by ayushgp@github)
#1947: `MapperFeature.AUTO_DETECT_XXX` do not work if all disabled
 (reported by Timur S)
#1977: Serializing an Iterator with multiple sub-types fails after upgrading to 2.9.x
 (reported by ssivanand@github)
#1978: Using @JsonUnwrapped annotation in builderdeserializer hangs in infinite loop
 (reported by roeltje25@github)

2.9.4 (24-Jan-2018)

#1382: `@JsonProperty(access=READ_ONLY)` unxepected behaviour with `Collections`
 (reported by hexfaker@github)
#1673: Serialising generic value classes via Reference Types (like Optional) fails
  to include type information
 (reported by Pier-Luc W)
#1729: Integer bounds verification when calling `TokenBuffer.getIntValue()`
 (reported by Kevin G)
#1853: Deserialise from Object (using Creator methods) returns field name instead of value
 (reported by Alexander S)
#1854: NPE deserializing collection with `@JsonCreator` and `ACCEPT_CASE_INSENSITIVE_PROPERTIES`
 (reported by rue-jw@github)
#1855: Blacklist for more serialization gadgets (dbcp/tomcat, spring)
#1859: Issue handling unknown/unmapped Enum keys
 (reported by remya11@github)
#1868: Class name handling for JDK unmodifiable Collection types changed
  (reported by Rob W)
#1870: Remove `final` on inherited methods in `BuilderBasedDeserializer` to allow
  overriding by subclasses
  (requested by Ville K)
#1878: `@JsonBackReference` property is always ignored when deserializing since 2.9.0
 (reported by reda-alaoui@github)
#1895: Per-type config override "JsonFormat.Shape.OBJECT" for Map.Entry not working
 (reported by mcortella@github)
#1899: Another two gadgets to exploit default typing issue in jackson-databind
 (reported by OneSourceCat@github)
#1906: Add string format specifier for error message in `PropertyValueBuffer`
 (reported by Joe S)
#1907: Remove `getClass()` from `_valueType` argument for error reporting
 (reported by Joe S)

2.9.3 (09-Dec-2017)

#1604: Nested type arguments doesn't work with polymorphic types
#1794: `StackTraceElementDeserializer` not working if field visibility changed
 (reported by dsingley@github)
#1799: Allow creation of custom sub-types of `NullNode`, `BooleanNode`, `MissingNode`
#1804: `ValueInstantiator.canInstantiate()` ignores `canCreateUsingArrayDelegate()`
 (reported byb henryptung@github)
#1807: Jackson-databind caches plain map deserializer and use it even map has `@JsonDeserializer`
 (reported by lexas2509@github)
#1823: ClassNameIdResolver doesn't handle resolve Collections$SingletonMap & Collections$SingletonSet
 (reported by Peter J)
#1831: `ObjectReader.readValue(JsonNode)` does not work correctly with polymorphic types,
  value to update
 (reported by basmastr@github)
#1835: ValueInjector break from 2.8.x to 2.9.x
 (repoted by kinigitbyday@github)
#1842: `null` String for `Exception`s deserialized as String "null" instead of `null`
 (reported by ZeleniJure@github)
#1843: Include name of unsettable property in exception from `SetterlessProperty.set()`
 (suggested by andreh7@github)
#1844: Map "deep" merge only adds new items, but not override existing values
 (reported by alinakovalenko@github)

2.9.2 (14-Oct-2017)

(possibly) #1756: Deserialization error with custom `AnnotationIntrospector`
 (reported by Daniel N)
#1705: Non-generic interface method hides type resolution info from generic base class
  (reported by Tim B)
 NOTE: was originally reported fixed in 2.9.1 -- turns out it wasn't.
#1767: Allow `DeserializationProblemHandler` to respond to primitive types
 (reported by nhtzr@github)
#1768: Improve `TypeFactory.constructFromCanonical()` to work with
  `java.lang.reflect.Type.getTypeName()' format
 (suggested by Luís C)
#1771: Pass missing argument for string formatting in `ObjectMapper`
 (reported by Nils B)
#1788: `StdDateFormat._parseAsISO8601()` does not parse "fractional" timezone correctly
#1793: `java.lang.NullPointerException` in `ObjectArraySerializer.acceptJsonFormatVisitor()`
  for array value with `@JsonValue`
 (reported by Vincent D)

2.9.1 (07-Sep-2017)

#1725: `NPE` In `TypeFactory. constructParametricType(...)`
 (reported by ctytgat@github)
#1730: InvalidFormatException` for `JsonToken.VALUE_EMBEDDED_OBJECT`
 (reported by zigzago@github)
#1744: StdDateFormat: add option to serialize timezone offset with a colon
 (contributed by Bertrand R)
#1745: StdDateFormat: accept and truncate millis larger than 3 digits
 (suggested by Bertrand R)
#1749: StdDateFormat: performance improvement of '_format(..)' method 
 (contributed by Bertrand R)
#1759: Reuse `Calendar` instance during parsing by `StdDateFormat`
 (contributed by Bertrand R)
- Fix `DelegatingDeserializer` constructor to pass `handledType()` (and
  not type of deserializer being delegated to!)
- Add `Automatic-Module-Name` ("com.fasterxml.jackson.databind") for JDK 9 module system

2.9.0 (30-Jul-2017)

#219: SqlDateSerializer does not obey SerializationConfig.Feature.WRITE_DATES_AS_TIMESTAMPS
 (reported by BrentDouglas@github)
#265: Add descriptive exception for attempts to use `@JsonWrapped` via Creator parameter
#291: @JsonTypeInfo with As.EXTERNAL_PROPERTY doesn't work if external type property
  is referenced more than once
 (reported by Starkom@github)
#357: StackOverflowError with contentConverter that returns array type
 (reported by Florian S)
#383: Recursive `@JsonUnwrapped` (`child` with same type) fail: "No _valueDeserializer assigned"
 (reported by tdavis@github)
#403: Make FAIL_ON_NULL_FOR_PRIMITIVES apply to primitive arrays and other types that wrap primitives
 (reported by Harleen S)
#476: Allow "Serialize as POJO" using `@JsonFormat(shape=Shape.OBJECT)` class annotation
#507: Support for default `@JsonView` for a class
 (suggested by Mark W)
#687: Exception deserializing a collection @JsonIdentityInfo and a property based creator
#865: `JsonFormat.Shape.OBJECT` ignored when class implements `Map.Entry`
#888: Allow specifying custom exclusion comparator via `@JsonInclude`,
  using `JsonInclude.Include.CUSTOM`
#994: `DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS` only works for POJOs, Maps
#1029: Add a way to define property name aliases
#1035: `@JsonAnySetter` assumes key of `String`, does not consider declared type.
 (reported by Michael F)
#1060: Allow use of `@JsonIgnoreProperties` for POJO-valued arrays, `Collection`s
#1106: Add `MapperFeature.ALLOW_COERCION_OF_SCALARS` for enabling/disabling coercions
#1284: Make `StdKeySerializers` use new `JsonGenerator.writeFieldId()` for `int`/`long` keys
#1320: Add `ObjectNode.put(String, BigInteger)`
 (proposed by Jan L)
#1341: `DeserializationFeature.FAIL_ON_MISSING_EXTERNAL_TYPE_ID_PROPERTY`
 (contributed by Connor K)
#1347: Extend `ObjectMapper.configOverrides()` to allow changing visibility rules
#1356: Differentiate between input and code exceptions on deserialization
 (suggested by Nick B)
#1369: Improve `@JsonCreator` detection via `AnnotationIntrospector`
 by passing `MappingConfig`
#1371: Add `MapperFeature.INFER_CREATOR_FROM_CONSTRUCTOR_PROPERTIES` to allow
 disabling use of `@CreatorProperties` as explicit `@JsonCreator` equivalent
#1376: Add ability to disable JsonAnySetter/JsonAnyGetter via mixin
 (suggested by brentryan@github)
#1399: Add support for `@JsonMerge` to allow "deep update"
#1402: Use `@JsonSetter(nulls=...)` to specify handling of `null` values during deserialization
#1406: `ObjectMapper.readTree()` methods do not return `null` on end-of-input
 (reported by Fabrizio C)
#1407: `@JsonFormat.pattern` is ignored for `java.sql.Date` valued properties
 (reported by sangpire@github)
#1415: Creating CollectionType for non generic collection class broken
#1428: Allow `@JsonValue` on a field, not just getter
#1434: Explicitly pass null on invoke calls with no arguments
 (contributed by Emiliano C)
#1433: `ObjectMapper.convertValue()` with null does not consider null conversions
  (`JsonDeserializer.getNullValue()`)
 (contributed by jdmichal@github)
#1440: Wrong `JsonStreamContext` in `DeserializationProblemHandler` when reading
  `TokenBuffer` content
 (reported by Patrick G)
#1444: Change `ObjectMapper.setSerializationInclusion()` to apply to content inclusion too
#1450: `SimpleModule.addKeyDeserializer()' should throw `IllegalArgumentException` if `null`
  reference of `KeyDeserializer` passed
 (suggested by PawelJagus@github)
#1454: Support `@JsonFormat.lenient` for `java.util.Date`, `java.util.Calendar`
#1474: Replace use of `Class.newInstance()` (deprecated in Java 9) with call via Constructor
#1480: Add support for serializing `boolean`/`Boolean` as number (0 or 1)
 (suggested by jwilmoth@github)
#1520: Case insensitive enum deserialization with `MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS`
 (contributed by Ana-Eliza B)
#1522: Global `@JsonInclude(Include.NON_NULL)` for all properties with a specific type
 (contributed by Carsten W)
#1544: EnumMapDeserializer assumes a pure EnumMap and does not support EnumMap derived classes
 (reported by Lyor G)
#1550: Unexpected behavior with `@JsonInclude(JsonInclude.Include.NON_EMPTY)` and
 `java.util.Date` serialization
#1551: `JsonMappingException` with polymorphic type and `JsonIdentityInfo` when basic type is abstract
 (reported by acm073@github)
#1552: Map key converted to byte array is not serialized as base64 string
 (reported by nmatt@github)
#1554: Support deserialization of `Shape.OBJECT` ("as POJO") for `Map`s (and map-like types)
#1556: Add `ObjectMapper.updateValue()` method to update instance with given overrides
 (suggested by syncer@github)
#1583: Add a `DeserializationFeature.FAIL_ON_TRAILING_TOKENS` to force reading of the
  whole input as single value
#1592: Add support for handling primitive/discrepancy problem with type refinements
#1605: Allow serialization of `InetAddress` as simple numeric host address
 (requested by Jared J)
#1616: Extraneous type id mapping added for base type itself
#1619: By-pass annotation introspection for array types
#1637: `ObjectReader.at()` with `JsonPointer` stops after first collection
 (reported by Chris P)
#1653: Convenience overload(s) for ObjectMapper#registerSubtypes
#1655: `@JsonAnyGetter` uses different `bean` parameter in `SimpleBeanPropertyFilter`
 (reported by georgeflugq@github)
#1678: Rewrite `StdDateFormat` ISO-8601 handling functionality
#1684: Rewrite handling of type ids to let `JsonGenerator` handle (more of) details
#1688: Deserialization fails for `java.nio.file.Path` implementations when default typing
  enabled
 (reported by Christian B)
#1690: Prevent use of quoted number (index) for Enum deserialization via
  `MapperFeature.ALLOW_COERCION_OF_SCALARS`
 (requested by magdel@github)

2.8.11.2 (08-Jun-2018)

#1941: `TypeFactory.constructFromCanonical()` throws NPE for Unparameterized
  generic canonical strings
 (reported by ayushgp@github)
#2032: CVE-2018-11307: Potential information exfiltration with default typing, serialization gadget from MyBatis
 (reported by Guixiong Wu)
#2052: CVE-2018-12022: Block polymorphic deserialization of types from Jodd-db library
 (reported by Guixiong Wu)
#2058: CVE-2018-12023: Block polymorphic deserialization of types from Oracle JDBC driver
 (reported by Guixiong Wu)

2.8.11.1 (11-Feb-2018)

#1872: `NullPointerException` in `SubTypeValidator.validateSubType` when
  validating Spring interface
 (reported by Rob W)
#1899: Another two gadgets to exploit default typing issue in jackson-databind
 (reported by OneSourceCat@github)
#1931: Two more `c3p0` gadgets to exploit default typing issue

2.8.11 (24-Dec-2017)

#1604: Nested type arguments doesn't work with polymorphic types
#1680: Blacklist couple more types for deserialization
#1767: Allow `DeserializationProblemHandler` to respond to primitive types
 (reported by nhtzr@github)
#1768: Improve `TypeFactory.constructFromCanonical()` to work with
  `java.lang.reflect.Type.getTypeName()` format
#1804: `ValueInstantiator.canInstantiate()` ignores `canCreateUsingArrayDelegate()`
 (reported by henryptung@github)
#1807: Jackson-databind caches plain map deserializer and use it even map has `@JsonDeserializer`
 (reported by lexas2509@github)
#1855: Blacklist for more serialization gadgets (dbcp/tomcat, spring)

2.8.10 (24-Aug-2017)

#1657: `StdDateFormat` deserializes dates with no tz/offset as UTC instead of
  configured timezone
 (reported by Bertrand R)
#1680: Blacklist couple more types for deserialization
#1658: Infinite recursion when deserializing a class extending a Map,
  with a recursive value type
 (reported by Kevin G)
#1679: `StackOverflowError` in Dynamic `StdKeySerializer`
#1711: Delegating creator fails to work for binary data (`byte[]`) with
 binary formats (CBOR, Smile)
#1735: Missing type checks when using polymorphic type ids
 (reported by Lukas Euler)
#1737: Block more JDK types from polymorphic deserialization

2.8.9 (12-Jun-2017)

#1595: `JsonIgnoreProperties.allowSetters` is not working in Jackson 2.8
 (reported by Javy L)
#1597: Escape JSONP breaking characters
 (contributed by Marco C)
#1629: `FromStringDeserializer` ignores registered `DeserializationProblemHandler`
  for `java.util.UUID`
 (reported by Andrew J)
#1642: Support `READ_UNKNOWN_ENUM_VALUES_AS_NULL` with `@JsonCreator`
 (contributed by Joe L)
#1647: Missing properties from base class when recursive types are involved
 (reported by Slobodan P)
#1648: `DateTimeSerializerBase` ignores configured date format when creating contextual
 (reported by Bertrand R)
#1651: `StdDateFormat` fails to parse 'zulu' date when TimeZone other than UTC
 (reported by Bertrand R)

2.8.8.1 (19-Apr-2017)

#1585: Invoke ServiceLoader.load() inside of a privileged block when loading
  modules using `ObjectMapper.findModules()`
 (contributed by Ivo S)
#1599: Jackson Deserializer security vulnerability
 (reported by ayound@github)
#1607: @JsonIdentityReference not used when setup on class only
 (reported by vboulaye@github)

2.8.8 (05-Apr-2017)

(partial) #994: `DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS` only works for POJOs, Maps
#1345: `@JsonProperty(access = READ_ONLY)` together with generated constructor (Lombok) causes
 exception: "Could not find creator property with name ..."
 (reported by Raniz85@github)
#1533: `AsPropertyTypeDeserializer` ignores `DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT`
#1543: JsonFormat.Shape.NUMBER_INT does not work when defined on enum type in 2.8
 (reported by Alex P)
#1570: `Enum` key for `Map` ignores `SerializationFeature.WRITE_ENUMS_USING_INDEX`
 (reported by SolaKun@github)
#1573: Missing properties when deserializing using a builder class with a non-default
  constructor and a mutator annotated with `@JsonUnwrapped`
 (reported by Joshua J)
#1575: Problem with `@JsonIgnoreProperties` on recursive property (regression in 2.8)
 (reported by anujkumar04@github)
- Minor fix to creation of `PropertyMetadata`, had one path that could lead to NPE

2.8.7 (21-Feb-2017)

#935: `@JsonProperty(access = Access.READ_ONLY)` - unexpected behaviour
#1317: '@JsonIgnore' annotation not working with creator properties, serialization

2.8.6 (12-Jan-2017)

#349: @JsonAnySetter with @JsonUnwrapped: deserialization fails with arrays
 (reported by hdave@github)
#1388: `@JsonIdentityInfo`: id has to be the first key in deserialization when
  deserializing with `@JsonCreator`
 (reported by moodysalem@github)
#1425: `JsonNode.binaryValue()` ignores illegal character if it's the last one
 (reported by binoternary@github)
#1453: `UntypedObjectDeserializer` does not retain `float` type (over `double`)
#1456: `TypeFactory` type resolution broken in 2.7 for generic types
   when using `constructType` with context
#1473: Add explicit deserializer for `StringBuilder` due to Java 9 changes
#1493: `ACCEPT_CASE_INSENSITIVE_PROPERTIES` fails with `@JsonUnwrapped`

2.8.5 (14-Nov-2016)

#1417: Further issues with `@JsonInclude` with `NON_DEFAULT`
#1421: ACCEPT_SINGLE_VALUE_AS_ARRAY partially broken in 2.7.x, 2.8.x
#1429: `StdKeyDeserializer` can erroneously use a static factory method
  with more than one argument
#1432: Off by 1 bug in PropertyValueBuffer
 (reported by Kevin D)
#1438: `ACCEPT_CASE_INSENSITIVE_PROPERTIES` is not respected for creator properties
 (reported by Jayson M)
#1439: NPE when using with filter id, serializing `java.util.Map` types
#1441: Failure with custom Enum key deserializer, polymorphic types
 (reported by Nathanial O)
#1445: Map key deserializerModifiers ignored
 (reported by alfonsobonso@github)
- Improvements to #1411 fix to ensure consistent `null` key handling

2.8.4 (14-Oct-2016)

#466: Jackson ignores Type information when raw return type is BigDecimal or BigInteger 
#1001: Parameter names module gets confused with delegate creator which is a static method
#1324: Boolean parsing with `StdDeserializer` is too slow with huge integer value
 (reported by pavankumar-parankusam@github)
#1383: Problem with `@JsonCreator` with 1-arg factory-method, implicit param names
#1384: `@JsonDeserialize(keyUsing = ...)` does not work correctly together with
  DefaultTyping.NON_FINAL
 (reported by Oleg Z)
#1385: Polymorphic type lost when using `@JsonValue`
 (reported by TomMarkuske@github)
#1389 Problem with handling of multi-argument creator with Enums
 (fix contributed by Pavel P)
#1392: Custom UnmodifiableSetMixin Fails in Jackson 2.7+ but works in Jackson 2.6
 (reported by Rob W)
#1395: Problems deserializing primitive `long` field while using `TypeResolverBuilder`
 (reported by UghZan3@github)
#1403: Reference-chain hints use incorrect class-name for inner classes
 (reported by Josh G)
#1411: MapSerializer._orderEntries should check for null keys
 (reported by Jörn H)

2.8.3 (17-Sep-2016)

#1351: `@JsonInclude(NON_DEFAULT)` doesn't omit null fields
 (reported by Gili T)
#1353: Improve error-handling for `java.net.URL` deserialization
#1361: Change `TokenBuffer` to use new `writeEmbeddedObject()` if possible

2.8.2 (30-Aug-2016)

#1315: Binding numeric values can BigDecimal lose precision
 (reported by Andrew S)
#1327: Class level `@JsonInclude(JsonInclude.Include.NON_EMPTY)` is ignored
 (reported by elruwen@github)
#1335: Unconditionally call `TypeIdResolver.getDescForKnownTypeIds`
 (contributed by Chris J-Y)

2.8.1 (20-Jul-2016)

#1256: `Optional.empty()` not excluded if property declared with type `Object`
#1288: Type id not exposed for `JsonTypeInfo.As.EXTERNAL_PROPERTY` even when `visible` set to `true`
 (reported by libetl@github)
#1289: Optimize construction of `ArrayList`, `LinkedHashMap` instances
#1291: Backward-incompatible behaviour of 2.8: deserializing enum types
   with two static factory methods fail by default
#1297: Deserialization of generic type with Map.class
 (reported by Arek G)
#1302: NPE for `ResolvedRecursiveType` in 2.8.0 due to caching

2.8.0 (04-Jul-2016)

#621: Allow definition of "ignorable types" without annotation (using
  `Mapper.configOverride(type).setIsIgnoredType(true)`
#867: Support `SerializationFeature.WRITE_EMPTY_JSON_ARRAYS ` for `JsonNode`
#903: Add `JsonGenerator` reference to `SerializerProvider`
#931: Add new method in `Deserializers.Base` to support `ReferenceType`
#960: `@JsonCreator` not working on a factory with no arguments for an enum type
 (reported by Artur J)
#990: Allow failing on `null` values for creator (add 
  `DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES`)
 (contributed by mkokho@github)
#999: External property is not deserialized
 (reported by Aleksandr O)
#1017: Add new mapping exception type ('InvalidTypeIdException') for subtype resolution errors
 (suggested by natnan@github)
#1028: Ignore USE_BIG_DECIMAL_FOR_FLOATS for NaN/Infinity
 (reported by Vladimir K, lightoze@github)
#1047: Allow use of `@JsonAnySetter` on a Map-valued field, no need for setter
#1082: Can not use static Creator factory methods for `Enum`s, with JsonCreator.Mode.PROPERTIES
 (contributed by Lokesh K)
#1084: Change `TypeDeserializerBase` to take `JavaType` for `defaultImpl`, NOT `Class`
#1126: Allow deserialization of unknown Enums using a predefined value
 (contributed by Alejandro R)
#1136: Implement `TokenBuffer.writeEmbeddedObject(Object)`
 (suggested by Gregoire C, gcxRun@github)
#1165: CoreXMLDeserializers does not handle time-only XMLGregorianCalendars
 (reported, contributed fix by Ross G)
#1181: Add the ability to specify the initial capacity of the ArrayNode
 (suggested by Matt V, mveitas@github)
#1184: Allow overriding of `transient` with explicit inclusion with `@JsonProperty`
 (suggested by Maarten B)
#1187: Refactor `AtomicReferenceDeserializer` into `ReferenceTypeDeserializer`
#1204: Add a convenience accessor `JavaType.hasContentType()` (true for container or reference type)
#1206: Add "anchor type" member for `ReferenceType`
#1211: Change `JsonValueSerializer` to get `AnnotatedMethod`, not "raw" method
#1217: `@JsonIgnoreProperties` on Pojo fields not working for deserialization
 (reported by Lokesh K)
#1221: Use `Throwable.addSuppressed()` directly and/or via try-with-resources
#1232: Add support for `JsonFormat.Feature.ACCEPT_CASE_INSENSITIVE_PROPERTIES`
#1233: Add support for `JsonFormat.Feature.WRITE_SORTED_MAP_ENTRIES`
#1235: `java.nio.file.Path` support incomplete
 (reported by, fix contributed by Benson M)
#1261: JsonIdentityInfo broken deserialization involving forward references and/or cycles
 (reported by, fix contributed by Ari F)
#1270: Generic type returned from type id resolver seems to be ignored
 (reported by Benson M)
#1277: Add caching of resolved generic types for `TypeFactory`
 (requested by Andriy P)

2.7.10 (not yet released)

#1628: Don't print to error stream about failure to load JDK 7 types
 (reported by Villane@github)

2.7.9 (04-Feb-2017)

#1367: No Object Id found for an instance when using `@ConstructorProperties`
#1505: @JsonEnumDefaultValue should take precedence over FAIL_ON_NUMBERS_FOR_ENUMS
 (suggested by Stephan S)
#1506: Missing `KeyDeserializer` for `CharSequence`
#1513: `MapSerializer._orderEntries()` throws NPE when operating on `ConcurrentHashMap`
 (reported by Sovietaced@github)
- Simplified processing of class annotations (for `AnnotatedClass`) to try to
  solve rare concurrency problems with "root name" annotations.

2.7.8 (26-Sep-2016)

#877: @JsonIgnoreProperties`: ignoring the "cause" property of `Throwable` on GAE
#1359: Improve `JsonNode` deserializer to create `FloatNode` if parser supports
#1362: ObjectReader.readValues()` ignores offset and length when reading an array
 (reported by wastevenson@github)
#1363: The static field ClassUtil.sCached can cause a class loader leak
 (reported by Stuart D)
#1368: Problem serializing `JsonMappingException` due to addition of non-ignored
  `processor` property (added in 2.7)
 (reported, suggesed fix by Josh C)
#1383: Problem with `@JsonCreator` with 1-arg factory-method, implicit param names

2.7.7 (27-Aug-2016)

#1322: EnumMap keys not using enum's `@JsonProperty` values unlike Enum values
 (reported by MichaelChambers@github)
#1332: Fixed ArrayIndexOutOfBoundException for enum by index deser
 (reported by Max D)
#1344: Deserializing locale assumes JDK separator (underscore), does not
  accept RFC specified (hyphen)
 (reported by Jim M)

2.7.6 (23-Jul-2016)

#1215: Problem with type specialization for Maps with `@JsonDeserialize(as=subtype)`
 (reported by brentryan@github)
#1279: Ensure DOM parsing defaults to not expanding external entities
#1288: Type id not exposed for `JsonTypeInfo.As.EXTERNAL_PROPERTY` even when `visible` set to `true`
#1299: Timestamp deserialization error
 (reported by liyuj@github)
#1301: Problem with `JavaType.toString()` for recursive (self-referential) types
 (reported by Brian P)
#1307: `TypeWrappedDeserializer` doesn't delegate the `getNullValue()` method to `_deserializer`
 (reported by vfries@github)

2.7.5 (11-Jun-2016)

#1098: DeserializationFeature.FAIL_ON_INVALID_SUBTYPE does not work with
  `JsonTypeInfo.Id.CLASS`
 (reported by szaccaria@github)
#1223: `BasicClassIntrospector.forSerialization(...).findProperties` should
  respect MapperFeature.AUTO_DETECT_GETTERS/SETTERS?
 (reported by William H)
#1225: `JsonMappingException` should override getProcessor()
 (reported by Nick B)

2.6.7.1 (11-Jul-2017)

#1383: Problem with `@JsonCreator` with 1-arg factory-method, implicit param names
#1599: Backport the extra safety checks for polymorphic deserialization

2.6.7 (05-Jun-2016)

#1194: Incorrect signature for generic type via `JavaType.getGenericSignature
#1228: @JsonAnySetter does not deserialize null to Deserializer's NullValue
 (contributed by Eric S)
#1231: `@JsonSerialize(as=superType)` behavior disallowed in 2.7.4
 (reported by Mark W)
#1248: `Annotated` returns raw type in place of Generic Type in 2.7.x
 (reported by Andrew J, apjoseph@github)
#1253: Problem with context handling for `TokenBuffer`, field name
#1260: `NullPointerException` in `JsonNodeDeserializer`
 (reported by Eric S)

2.7.4 (29-Apr-2016)

#1122: Jackson 2.7 and Lombok: 'Conflicting/ambiguous property name definitions'
#1178: `@JsonSerialize(contentAs=superType)` behavior disallowed in 2.7
#1186: SimpleAbstractTypeResolver breaks generic parameters
 (reported by tobiash@github)
#1189: Converter called twice results in ClassCastException
 (reported by carrino@github)
#1191: Non-matching quotes used in error message for date parsing
#1194: Incorrect signature for generic type via `JavaType.getGenericSignature
#1195: `JsonMappingException` not Serializable due to 2.7 reference to source (parser)
 (reported by mjustin@github)
#1197: `SNAKE_CASE` doesn't work when using Lombok's `@AllArgsConstructor`
#1198: Problem with `@JsonTypeInfo.As.EXTERNAL_PROPERTY`, `defaultImpl`, missing type id, NPE
#1203: `@JsonTypeInfo` does not work correctly for ReferenceTypes like `AtomicReference`
#1208: treeToValue doesn't handle POJONodes that contain exactly the requested value type
  (reported by Tom M)
- Improve handling of custom content (de)serializers for `AtomicReference`

2.7.3 (16-Mar-2016)

#1125: Problem with polymorphic types, losing properties from base type(s)
#1150: Problem with Object id handling, explicit `null` token
 (reported by Xavi T)
#1154: @JsonFormat.pattern on dates is now ignored if shape is not explicitely provided
 (reported by Yoann R)
#1161: `DeserializationFeature.READ_ENUMS_USING_TO_STRING` not dynamically
  changeable with 2.7
 (reported by asa-git@github)
- Minor fixes to `AnnotationIntrospector.findEnumValues()` to correct problems with
  merging of explicit enum value names.

2.7.2 (26-Feb-2016)

#1124: JsonAnyGetter ignores JsonSerialize(contentUsing=...)
 (reported by Jiri M)
#1128: UnrecognizedPropertyException in 2.7.1 for properties that work with version 2.6.5
 (reported by Roleek@github)
#1129: When applying type modifiers, don't ignore container types.
#1130: NPE in `StdDateFormat` hashCode and equals
 (reported by Kazuki S, kazuki43zoo@github)
#1134: Jackson 2.7 doesn't work with jdk6 due to use of `Collections.emptyIterator()`
 (reported by Timur S, saladinkzn@github)

2.7.1-1 (03-Feb-2016)

Special one-off "micro patch" for:

#1115: Problems with deprecated `TypeFactory.constructType(type, ctxt)` methods if `ctxt` is `null`

2.7.1 (02-Feb-2016)

#1079: Add back `TypeFactory.constructType(Type, Class)` as "deprecated" in 2.7.1
#1083: Field in base class is not recognized, when using `@JsonType.defaultImpl`
 (reported by Julian H)
#1095: Prevent coercion of `int` from empty String to `null` if
  `DeserializationFeature .FAIL_ON_NULL_FOR_PRIMITIVES` is `true`
 (reported by yzmyyff@github)
#1102: Handling of deprecated `SimpleType.construct()` too minimalistic
 (reported by Thibault K)
#1109: @JsonFormat is ignored by the DateSerializer unless either a custom pattern
  or a timezone are specified
 (contributed by Aleks S)

2.7.0 (10-Jan-2016)

#76: Problem handling datatypes Recursive type parameters
 (reported by Aram K)
#357: StackOverflowError with contentConverter that returns array type
 (reported by Florian S)
#432: `StdValueInstantiator` unwraps exceptions, losing context
 (reported by Miles K)
#497: Add new JsonInclude.Include feature to exclude maps after exclusion removes all elements
#803: Allow use of `StdDateFormat.setLenient()`
 (suggested by raj-ghodke@github)
#819: Add support for setting `FormatFeature` via `ObjectReader`, `ObjectWriter`
#857: Add support for java.beans.Transient (requires Java 7)
 (suggested by Thomas M)
#898: Add `ObjectMapper.getSerializerProviderInstance()`
#905: Add support for `@ConstructorProperties` (requires Java 7)
 (requested by Jonas K)
#909: Rename PropertyNamingStrategy CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES as SNAKE_CASE,
   PASCAL_CASE_TO_CAMEL_CASE as UPPER_CAMEL_CASE
 (suggested by marcottedan@github)
#915: ObjectMapper default timezone is GMT, should be UTC
 (suggested by Infrag@github)
#918: Add `MapperFeature.ALLOW_EXPLICIT_PROPERTY_RENAMING`
 (contributed by David H)
#924: `SequenceWriter.writeAll()` could accept `Iterable`
 (suggested by Jiri-Kremser@github(
#932: Rewrite ser/deser for `AtomicReference`, based on "optional" ser/desers
#933: Close some gaps to allow using the `tryToResolveUnresolved` flows
#936: Deserialization into List subtype with JsonCreator no longer works
 (reported by adamjoeldavis@github)
#948: Support leap seconds, any number of millisecond digits for ISO-8601 Dates.
 (contributed by Jesse W)
#952: Revert non-empty handling of primitive numbers wrt `NON_EMPTY`; make
  `NON_DEFAULT` use extended criteria
#957: Merge `datatype-jdk7` stuff in (java.nio.file.Path handling)
#959: Schema generation: consider active view, discard non-included properties
#963: Add PropertyNameStrategy `KEBAB_CASE`
 (requested by Daniel M)
#978: ObjectMapper#canSerialize(Object.class) returns false even though FAIL_ON_EMPTY_BEANS is disabled
 (reported by Shumpei A)
#997: Add `MapperFeature.OVERRIDE_PUBLIC_ACCESS_MODIFIERS`
#998: Allow use of `NON_DEFAULT` for POJOs without default constructor
#1000: Add new mapping exception type for enums and UUIDs
 (suggesed by natnan@github)
#1010: Support for array delegator
 (contributed by Hugo W)
#1011: Change ObjectWriter::withAttributes() to take a Map with some kind of wildcard types
 (suggested by David B)
#1043: @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY) does not work on fields
 (reported by fabiolaa@github)
#1044: Add `AnnotationIntrospector.resolveSetterConflict(...)` to allow custom setter conflict resolution
 (suggested by clydebarrow@github)
- Make `JsonValueFormat` (self-)serializable, deserializable, to/from valid external
  value (as per JSON Schema spec)

INCOMPATIBILITIES:

- While unlikely to be problematic, #959 above required an addition of `SerializerProvider`
  argument for `depositSchemaProperty()` method `BeanProperty` and `PropertyWriter` interfaces
- JDK baseline now Java 7 (JDK 1.7), from Java 6/JDK 1.6

2.6.6 (05-Apr-2016)

#1088: NPE possibility in SimpleMixinResolver
 (reported by Laird N)
#1099: Fix custom comparator container node traversal
 (contributed by Daniel N)
#1108: Jackson not continue to parse after DeserializationFeature.FAIL_ON_INVALID_SUBTYPE error
 (reported by jefferyyuan@github)
#1112: Detailed error message from custom key deserializer is discarded
 (contributed by Benson M)
#1120: String value omitted from weirdStringException
 (reported by Benson M)
#1123: Serializing and Deserializing Locale.ROOT
 (reported by hookumsnivy@github)

2.6.5 (19-Jan-2016)

#1052: Don't generate a spurious NullNode after parsing an embedded object
 (reported by philipa@github)
#1061: Problem with Object Id and Type Id as Wrapper Object (regression in 2.5.1)
#1073: Add try-catch around `java.sql` type serializers
 (suggested by claudemt@github)
#1078: ObjectMapper.copy() still does not preserve _registeredModuleTypes
 (reported by ajonkisz@github)

2.6.4 (07-Dec-2015)

#984: JsonStreamContexts are not build the same way for write.. and convert methods
 (reported by Antibrumm@github)
#989: Deserialization from "{}" to java.lang.Object causes "out of END_OBJECT token" error
 (reported by Ievgen P)
#1003: JsonTypeInfo.As.EXTERNAL_PROPERTY does not work with a Delegate
 (reported by alexwen@github)
#1005: Synthetic constructors confusing Jackson data binding
 (reported by Jayson M)
#1013: `@JsonUnwrapped` is not treated as assuming `@JsonProperty("")`
 (reported by David B)
#1036: Problem with case-insensitive deserialization
 (repoted by Dmitry R)
- Fix a minor problem with `@JsonNaming` not recognizing default value

2.6.3 (12-Oct-2015)

#749: `EnumMap` serialization ignores `SerializationFeature.WRITE_ENUMS_USING_TO_STRING`
 (reported by scubasau@github)
#938: Regression: `StackOverflowError` with recursive types that contain `Map.Entry`
 (reported by jloisel@github)
#939: Regression: DateConversionError in 2.6.x 
 (reported by Andreas P, anpieber@github)
#940: Add missing `hashCode()` implementations for `JsonNode` types that did not have them
 (contributed by Sergio M)
#941: Deserialization from "{}" to ObjectNode field causes "out of END_OBJECT token" error
 (reported by Sadayuki F)
#942: Handle null type id for polymorphic values that use external type id
 (reported by Warren B, stormboy@github)
#943: Incorrect serialization of enum map key
 (reported by Benson M)
#944: Failure to use custom deserializer for key deserializer
 (contributed by Benson M)
#949: Report the offending substring when number parsing fails
 (contributed by Jesse W)
#965: BigDecimal values via @JsonTypeInfo/@JsonSubTypes get rounded
 (reported by gmjabs@github)

2.6.2 (14-Sep-2015)

#894: When using withFactory on ObjectMapper, the created Factory has a TypeParser
  which still has the original Factory
 (reported by lufe66@github)
#899: Problem serializing `ObjectReader` (and possibly `ObjectMapper`)
#913: ObjectMapper.copy does not preserve MappingJsonFactory features
 (reported, fixed by Daniel W)
#922: ObjectMapper.copy() does not preserve _registeredModuleTypes
#928: Problem deserializing External Type Id if type id comes before POJO

2.6.1 (09-Aug-2015)

#873: Add missing OSGi import
#881: BeanDeserializerBase having issues with non-CreatorProperty properties.
 (reported by dharaburda@github)
#884: ArrayIndexOutOfBoundException for `BeanPropertyMap` (with ObjectId)
 (reported by alterGauner@github)
#889: Configuring an ObjectMapper's DateFormat changes time zone
 (reported by Andy W, wilkinsona@github)
#890: Exception deserializing a byte[] when the target type comes from an annotation
 (reported by gmjabs@github)

2.6.0 (19-Jul-2015)

#77: Allow injection of 'transient' fields
#95: Allow read-only properties with `@JsonIgnoreProperties(allowGetters=true)`
#222: EXTERNAL_PROPERTY adds property multiple times and in multiple places
 (reported by Rob E, thatsnotright@github)
#296: Serialization of transient fields with public getters (add
    MapperFeature.PROPAGATE_TRANSIENT_MARKER)
 (suggested by Michal L)
#312: Support Type Id mappings where two ids map to same Class
#348: ObjectMapper.valueToTree does not work with @JsonRawValue
 (reported by Chris P, pimlottc@github)
#504: Add `DeserializationFeature.USE_LONG_FOR_INTS`
 (suggested by Jeff S)
#624: Allow setting external `ClassLoader` to use, via `TypeFactory`
#649: Make `BeanDeserializer` use new `parser.nextFieldName()` and `.hasTokenId()` methods
#664: Add `DeserializationFeature.ACCEPT_FLOAT_AS_INT` to prevent coercion of floating point
 numbers int `int`/`long`/`Integer`/`Long`
 (requested by wenzis@github)
#677: Specifying `Enum` value serialization using `@JsonProperty`
 (requested by Allen C, allenchen1154@github)
#679: Add `isEmpty()` implementation for `JsonNode` serializers
#688: Provide a means for an ObjectMapper to discover mixin annotation classes on demand
 (requested by Laird N)
#689: Add `ObjectMapper.setDefaultPrettyPrinter(PrettyPrinter)`
 (requested by derknorton@github)
#696: Copy constructor does not preserve `_injectableValues`
 (reported by Charles A)
#698: Add support for referential types (ReferenceType)
#700: Cannot Change Default Abstract Type Mapper from LinkedHashMap
 (reported by wealdtech@github)
#725: Auto-detect multi-argument constructor with implicit names if it is the only visible creator
#727: Improve `ObjectWriter.forType()` to avoid forcing base type for container types
#734: Add basic error-recovery for `ObjectReader.readValues()`
#737: Add support for writing raw values in TokenBuffer
 (suggested by Guillaume S, gsmet@github)
#740: Ensure proper `null` (as empty) handling for `AtomicReference`
#741: Pass `DeserializationContext' argument for `JsonDeserializer` methods "getNullValue()"
 and "getEmptyValue()"
#743: Add `RawValue` helper type, for piping raw values through `TokenBuffer`
#756: Disabling SerializationFeature.FAIL_ON_EMPTY_BEANS does not affect `canSerialize()`
 (reported by nickwongdev@github)
#762: Add `ObjectWriter.withoutRootName()`, `ObjectReader.withoutRootName()`
#765: `SimpleType.withStaticTyping()` impl incorrect
#769: Fix `JacksonAnnotationIntrospector.findDeserializer` to return `Object` (as per
  `AnnotationIntrospector`); similarly for other `findXxx(De)Serializer(...)` methods
#777: Allow missing build method if its name is empty ("")
 (suggested by galdosd@github)
#781: Support handling of `@JsonProperty.required` for Creator methods
#787: Add `ObjectMapper setFilterProvider(FilterProvider)` to allow chaining
 (suggested by rgoldberg@githin)
#790: Add `JsonNode.equals(Comparator<JsonNode>, JsonNode)` to support
  configurable/external equality comparison
#794: Add `SerializationFeature.WRITE_DATES_WITH_ZONE_ID` to allow inclusion/exclusion of
  timezone id for date/time values (as opposed to timezone offset)
#795: Converter annotation not honored for abstract types
 (reported by myrosia@github)
#797: `JsonNodeFactory` method `numberNode(long)` produces `IntNode` for small numbers
#810: Force value coercion for `java.util.Properties`, so that values are `String`s
#811: Add new option, `JsonInclude.Include.NON_ABSENT` (to support exclusion of
  JDK8/Guava Optionals)
#812: Java 8 breaks Class-value annotation properties, wrt generics: need to work around
#813: Add support for new property of `@JsonProperty.access` to support
  read-only/write-only use cases
#820: Add new method for `ObjectReader`, to bind from JSON Pointer position
 (contributed by Jerry Y, islanderman@github)
#824: Contextual `TimeZone` changes don't take effect wrt `java.util.Date`,
  `java.util.Calendar` serialization
#826: Replaced synchronized HashMap with ConcurrentHashMap in TypeDeserializerBase._findDeserializer
 (contributed by Lars P)
#827: Fix for polymorphic custom map key serializer
 (reported by mjr6140@gitgub)
#828: Respect DeserializationFeatures.WRAP_EXCEPTIONS in CollectionDeserializer
 (contributed by Steve G, thezerobit@github)
#840: Change semantics of `@JsonPropertyOrder(alphabetic)` to only count `true` value
#848: Custom serializer not used if POJO has `@JsonValue`
#849: Possible problem with `NON_EMPTY` exclusion, `int`s, `Strings`
#868: Annotations are lost in the case of duplicate methods
- Remove old cglib compatibility tests; cause problems in Eclipse
- Add `withFilterId()` method in `JsonSerializer` (demote from `BeanSerializer`)

2.5.5 (07-Dec-2015)

#844: Using JsonCreator still causes invalid path references in JsonMappingException
 (reported by Ian B)
#852: Accept scientific number notation for quoted numbers too
#878: serializeWithType on BeanSerializer does not setCurrentValue
 (reported by Chi K, chikim79@github)

2.5.4 (09-Jun-2015)

#676: Deserialization of class with generic collection inside depends on
  how is was deserialized first time
 (reported by lunaticare@github)
#771: Annotation bundles ignored when added to Mixin
 (reported by Andrew D)
#774: NPE from SqlDateSerializer as _useTimestamp is not checked for being null
 (reported by mrowkow@github)
#785: Add handlings for classes which are available in `Thread.currentThread().getContextClassLoader()`
 (contributed by Charles A)
#792: Ensure Constructor Parameter annotations are linked with those of Field, Getter, or Setter
#793: `ObjectMapper.readTree()` does not work with defaultTyping enabled
 (reported by gracefulgopher@github)
#801: Using `@JsonCreator` cause generating invalid path reference in `JsonMappingException`
 (contributed by Kamil B)
#815: Presence of PropertyNamingStrategy Makes Deserialization fail
#816: Allow date-only ISO strings to have no time zone
 (contributed by Andrew G)
- Fix handling of Enums wrt JSON Schema, when 'toString()' used for serialization

2.5.3 (24-Apr-2015)

#731: XmlAdapter result marshaling error in case of ValueType=Object
 (reported, debugged by Dmitry S)
#742: Allow deserialization of `null` Object Id (missing already allowed)
#744: Custom deserializer with parent object update failing
 (reported by migel@github)
#745: EnumDeserializer.deserializerForCreator fails when used to deserialize a Map key
 (contributed by John M)
#761: Builder deserializer: in-compatible type exception when return type is super type
 (contributed by Alexey G)
#766: Fix Infinite recursion (StackOverflowError) when serializing a SOAP object
 (contributed by Alain G)

2.5.2 (29-Mar-2015)

#609: Problem resolving locally declared generic type
 (repoted by Hal H)
#691: NullSerializer for MapProperty failing when using polymorphic handling
 (reported by Antibrumm@github)
#703: Multiple calls to ObjectMapper#canSerialize(Object.class) returns different values
 (reported by flexfrank@github)
#705: JsonAnyGetter doesn't work with JsonSerialize (except with keyUsing)
 (reported by natnan@github)
#728: TypeFactory#_fromVariable returns unknownType() even though it has enough information
  to provide a more specific type
 (reported by jkochaniak@github)
#733: MappingIterator should move past errors or not return hasNext() == true
 (reported by Lorrin N, lorrin@github)
#738: @JsonTypeInfo non-deterministically ignored in 2.5.1 (concurrency issue)
 (reported by Dylan S, dylanscott@github)
- Improvement to handling of custom `ValueInstantiator` for delegating mode; no more NPE
  if `getDelegateCreator()` returns null
- Refactor `TypedKey` into separate util class

2.5.1 (06-Feb-2015)

#667: Problem with bogus conflict between single-arg-String vs `CharSequence` constructor
#669: JSOG usage of @JsonTypeInfo and @JsonIdentityInfo(generator=JSOGGenerator.class) fails
 (reported by ericali78@github)
#671: Adding `java.util.Currency` deserialization support for maps
 (contributed by Alexandre S-C)
#674: Spring CGLIB proxies not handled as intended
 (reported by Zoltan F)
#682: Class<?>-valued Map keys not serialized properly
 (reported by Ludevik@github)
#684: FAIL_ON_NUMBERS_FOR_ENUMS does not fail when integer value is quoted
 (reported by kllp@github)
#696: Copy constructor does not preserve `_injectableValues`
 (reported by Charles A)
- Add a work-around in `ISO8601DateFormat` to allow omission of ':' from timezone
- Bit more work to complete #633

2.5.0 (01-Jan-2015)

#47: Support `@JsonValue` for (Map) key serialization 
#113: Problem deserializing polymorphic types with @JsonCreator
#165: Add `DeserializationContext.getContextualType()` to let deserializer
  known the expected type.
#299: Add `DeserializationFeature.FAIL_ON_UNRESOLVED_OBJECT_IDS` to allow missing
  Object Ids (as global default)
#408: External type id does not allow use of 'visible=true'
#421: @JsonCreator not used in case of multiple creators with parameter names
 (reported by Lovro P, lpandzic@github)
#427: Make array and Collection serializers call `JsonGenerator.writeStartArray(int)`
#521: Keep bundle annotations, prevent problems with recursive annotation types
 (reported by tea-dragon@github)
#527: Add support for `@JsonInclude(content=Include.NON_NULL)` (and others) for Maps
#528: Add support for `JsonType.As.EXISTING_PROPERTY`
 (reported by heapifyman@github; implemented by fleebytes@github)
#539: Problem with post-procesing of "empty bean" serializer; was not calling
  'BeanSerializerModifier.modifySerializer()` for empty beans
 (reported by Fabien R, fabienrenaud@github)
#540: Support deserializing `[]` as null or empty collection when the java type
  is a not an object, `DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT`
 (requested by Fabien R, fabienrenaud@github)
#543: Problem resolving self-referential recursive types
 (reported by ahgittin@github)
#550: Minor optimization: prune introspection of "well-known" JDK types
#552: Improved handling for ISO-8601 (date) format
 (contributed by Jerome G, geronimo-iia@github)
#559: Add `getDateFormat()`, `getPropertyNamingStrategy()` in `ObjectMapper`
#560: @JsonCreator to deserialize BigInteger to Enum
 (requested by gisupp@github)
#565: Add support for handling `Map.Entry`
#566: Add support for case-insensitive deserialization (`MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES`)
 (contributed by Michael R)
#571: Add support in ObjectMapper for custom `ObjectReader`, `ObjectWriter` (sub-classes)
#572: Override default serialization of Enums
 (requested by herau@github)
#576: Add fluent API for adding mixins
 (contributed by Adam S, adstro@github)
#594: `@JsonValue` on enum not used when enum value is a Map key
 (reported by chrylis@github)
#596: Add support for `@JsonProperty.defaultValue`, exposed via `BeanProperty.getMetadata().getDefaultValue()`
#597: Improve error messaging for cases where JSON Creator returns null (which
  is illegal)
 (contributed by Aurelien L)
#599: Add a simple mechanism for avoiding multiple registrations of the same module
#607: Allow (re)config of `JsonParser.Feature`s via `ObjectReader`
#608: Allow (re)config of `JsonGenerator.Feature`s via `ObjectWriter`
#614: Add a mechanism for using `@JsonCreator.mode` for resolving possible ambiguity between
  delegating- and property-based creators
#616: Add `SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS`
#622: Support for non-scalar ObjectId Reference deserialiazation (like JSOG)
#623: Add `StdNodeBasedDeserializer`
#630: Add `KeyDeserializer` for `Class`
#631: Update `current value` of `JsonParser`, `JsonGenerator` from standard serializers,
 deserializers
 (suggested by Antibrumm@github)
#633: Allow returning null value from IdResolver to make type information optional
 (requested by Antibrumm@github)
#634: Add `typeFromId(DatabindContext,String)` in `TypeIdDeserializer`
#636: `ClassNotFoundException` for classes not (yet) needed during serialization
 (contributed by mspiegel@github)
#638: Add annotation-based method(s) for injecting properties during serialization
 (using @JsonAppend, VirtualBeanPropertyWriter)
#647: Deserialization fails when @JsonUnwrapped property contains an object with same property name
 (reported by Konstantin L)
#653: Jackson doesn't follow JavaBean naming convention (added `MapperFeature.USE_STD_BEAN_NAMING`)
#654: Add support for (re)configuring `JsonGenerator.setRootValueSeparator()` via `ObjectWriter`
#655: Add `ObjectWriter.writeValues()` for writing value sequences
#660: `@JsonCreator`-annotated factory method is ignored if constructor exists
- Allow use of `Shape.ARRAY` for Enums, as an alias to 'use index'
- Start using `JsonGenerator.writeStartArray(int)` to help data formats
  that benefit from knowing number of elements in arrays (and would otherwise
  need to buffer values to know length)
- Added new overload for `JsonSerializer.isEmpty()`, to eventually solve #588
- Improve error messaging (related to [jaxb-annotations#38]) to include known subtype ids.

2.4.6 (23-Apr-2015)

#735: (complete fix) @JsonDeserialize on Map with contentUsing custom deserializer overwrites default behavior
 (reported by blackfyre512@github) (regression due to #604)
$744: Custom deserializer with parent object update fails

2.4.5.1 (26-Mar-2015)

Special one-off "micro patch" for:

#706: Add support for `@JsonUnwrapped` via JSON Schema module
#707: Error in getting string representation of an ObjectNode with a float number value
 (reported by @navidqar)
#735: (partial) @JsonDeserialize on Map with contentUsing custom deserializer overwrites default behavior

2.4.5 (13-Jan-2015)

#635: Reduce cachability of `Map` deserializers, to avoid problems with per-property config changes
    (regression due to #604)
#656: `defaultImpl` configuration is ignored for `WRAPPER_OBJECT`
- Solve potential cyclic-resolution problem for `UntypedObjectDeserializer`

2.4.4 (24-Nov-2014)

(jackson-core)#158: Setter confusion on assignable types
 (reported by tsquared2763@github)
#245: Calls to ObjectMapper.addMixInAnnotations() on an instance returned by ObjectMapper.copy()
 don't work
 (reported by Erik D)
#580: delegate deserializers choke on a (single) abstract/polymorphic parameter
 (reported by Ian B, tea-dragon@github)
#590: Binding invalid Currency gives nonsense at end of the message
 (reported by Jerbell@github)
#592: Wrong `TokenBuffer` delegate deserialization using `@JsonCreator`
 (reported by Eugene L)
#601: ClassCastException for a custom serializer for enum key in `EnumMap`
 (reported by Benson M)
#604: `Map` deserializers not being cached, causing performance problems
#610: Fix forward reference in hierarchies
 (contributed by zeito@github)
#619: Off by one error in AnnotatedWithParams
 (reported by stevetodd@github)
- Minor fix to `EnumSerializer` regarding detection "serialize using index"
- Minor fix to number serializers, to call proper callback for schema generation

2.4.3 (02-Oct-2014)

#496: Wrong result with `new TextNode("false").asBoolean(true)`
 (reported by Ivar R, ivarru@github)
#511: DeserializationFeature.FAIL_ON_INVALID_SUBTYPE does not work
 (reported by sbelikov@github)
#523: MapDeserializer and friends do not report the field/key name for mapping exceptions
 (reported by Ian B, tea-dragon@github)
#524: @JsonIdentityReference(alwaysAsId = true) Custom resolver is reset to SimpleObjectIdResolver
 (reported by pkokorev@github)
#541: @JsonProperty in @JsonCreator is conflicting with POJOs getters/attributes
 (reported by fabienrenaud@github)
#543: Problem resolving self-referential generic types
#570: Add Support for Parsing All Compliant ISO-8601 Date Formats
 (requested by pfconrey@github)
- Fixed a problem with `acceptJsonFormatVisitor` with Collection/array types that
  are marked with `@JsonValue`; could cause NPE in JSON Schema generator module.

2.4.2 (14-Aug-2014)

#515: Mixin annotations lost when using a mixin class hierarchy with non-mixin interfaces
 (reported by 'stevebread@github')
- Fixed a problem related to [jackson-dataformat-smile#19].

2.4.1.2 (12-Jul-2014)

Special one-off "micro patch" for:

#503: Concurrency issue inside com.fasterxml.jackson.databind.util.LRUMap.get(Object)
 (reported by fjtc@github)

2.4.1.1 (18-Jun-2014)

Special one-off "micro patch" for:

#491: Temporary work-around for issue #490 (full fix for 2.5 needs to be
  in `jackson-annotations`)
#506: Index is never set for Collection and Array in InvalidFormatException.Reference
 (reported by Fabrice D, fabdouglas@github)
- Fixed a problem related to [jackson-dataformat-smile#19].

2.4.1 (17-Jun-2014)

#479: NPE on trying to deserialize a `String[]` that contains null
 (reported by huxi@github)
#482: Make date parsing error behavior consistent with JDK
 (suggested by Steve S, sanbeg@github)
#489 (partial): TypeFactory cache prevents garbage collection of custom ClassLoader
 (reported by sftwrengnr@github)

2.4.0 (02-Jun-2014)

#81: Allow use of @JsonUnwrapped with typed (@JsonTypeInfo) classes, provided
  that (new) feature `SerializationFeature.FAIL_ON_UNWRAPPED_TYPE_IDENTIFIERS`
  is disabled
 (constributed by Ben F, UnquietCode@github)
#88: Prevent use of type information for `JsonNode` via default typing
 (reported by electricmonk@github)
#149: Allow use of "stringified" indexes for Enum values
 (requested by chenboxiang@github)
#176: Allow use external Object Id resolver (to use with @JsonIdentityInfo etc)
 (implemented by Pascal G)
#193: Conflicting property name definitions
 (reported by Stuart J, sgjohnston@github)
#323: Serialization of the field with deserialization config
 (reported by metanet@github)
#327: Should not consider explicitly differing renames a fail, as long as all are explicit
#335: Allow use of `@JsonPropertyOrder(alphabetic=true)` for Map properties
#351: ObjectId does not properly handle forward references during deserialization
 (contributed by pgelinas)
#352 Add `ObjectMapper.setConfig()` for overriding `SerializationConfig`/`DeserializationConfig`
#353: Problems with polymorphic types, `JsonNode` (related to #88)
 (reported by cemo@github)
#359: Converted object not using explicitly annotated serializer
 (reported by Florian S [fschopp@github])
#369: Incorrect comparison for renaming in `POJOPropertyBuilder`
#375: Add `readValue()`/`readPropertyValue()` methods in `DeserializationContext`
#376: Add support for `@JsonFormat(shape=STRING)` for number serializers
#381: Allow inlining/unwrapping of value from single-component JSON array
 (contributed by yinzara@github)
#390: Change order in which managed/back references are resolved (now back-ref
 first, then forward)
 (requested by zAlbee@github)
#407: Properly use null handlers for value types when serializer Collection
 and array types
 (contributed by Will P)
#425: Add support for using `Void.class` as "no class", instead of `NoClass.class`
#428: `PropertyNamingStrategy` will rename even explicit name from `@JsonProperty`
 (reported by turskip@github)
#435: Performance bottleneck in TypeFactory._fromClass
 (reported by Sean D, sdonovanuk@github)
#434: Ensure that DecimalNodes with mathematically equal values are equal
 (contributed by Francis G)
#435: Performance bottleneck in TypeFactory._fromClass
 (reported by sdonovanuk@github)
#438: Add support for accessing `@JsonProperty(index=N)` annotations
#442: Make `@JsonUnwrapped` indicate property inclusion
 (suggested by Ben F)
#447: ArrayNode#addAll should accept Collection<? extends JsonNode>
 (suggested by alias@github)
#461: Add new standard naming strategy, `PropertyNamingStrategy.LowerCaseStrategy`
#463: Add 'JsonNode.asText(String defaultValue)`
 (suggested by Chris C)
#464: Include `JsonLocation` in more mapping exceptions
 (contributed by Andy C (q3aiml@github))
#465: Make it easier to support serialization of custom subtypes of `Number`
#467: Unwanted POJO's embedded in tree via serialization to tree
 (reported by Benson M)
- Slightly improve `SqlDateSerializer` to support `@JsonFormat`
- Improve handling of native type ids (YAML, CBOR) to use non-native type ids
  as fallback

2.3.5 (13-Jan-2015)

#496: Wrong result for TextNode("false").asBoolean(true)
 (reported by Ivar R, ivarru@github)
#543: Problems resolving self-referential generic types.
#656: defaultImpl configuration is ignored for WRAPPER_OBJECT

2.3.4 (17-Jul-2014)

#459: BeanDeserializerBuilder copy constructor not copying `_injectables`
#462: Annotation-provided Deserializers are not contextualized inside CreatorProperties
 (reported by aarondav@github)

2.3.3 (10-Apr-2014)

#420: Remove 'final' modifier from `BeanDeserializerBase.deserializeWithType`
 (requested by Ghoughpteighbteau@github)
#422: Allow use of "True" and "False" as aliases for booleans when coercing from
  JSON String
#423: Fix `CalendarSerializer` to work with custom format
 (reported by sergeymetallic@github)
#433: `ObjectMapper`'s `.valueToTree()` wraps `JsonSerializable` objects into a POJONode
 (reported by Francis G)
- Fix null-handling for `CollectionSerializer`

2.3.2 (01-Mar-2014)

#378: Fix a problem with custom enum deserializer construction
 (reported by BokoEnos@github)
#379: Fix a problem with (re)naming of Creator properties; needed to make
 Paranamer module work with NamingStrategy.
 (reported by Chris P, cpilsworth@github)
#398: Should deserialize empty (not null) URI from empty String
 (reported by pgieser@github)
#406: @JsonTypeIdResolver not working with external type ids
 (reported by Martin T)
#411: NumberDeserializers throws exception with NaN and +/- Infinity
 (reported by clarkbreyman@github)
#412: ObjectMapper.writerWithType() does not change root name being used
 (repoted by jhalterman@github)
- Added `BeanSerializerBase._serializeObjectId()` needed by modules that
  override standard BeanSerializer; specifically, XML module.

2.3.1 (28-Dec-2013)

#346: Fix problem deserializing `ObjectNode`, with @JsonCreator, empty
  JSON Object
 (reported by gaff78@github)
#358: `IterableSerializer` ignoring annotated content serializer
 (reported by Florian S)
#361: Reduce sync overhead for SerializerCache by using volatile, double-locking
 (contributed by stuartwdouglas@github)
#362: UUID output as Base64 String with ObjectMapper.convertValue()
 (reported by jknack@github)
#367: Make `TypeNameIdResolver` call `TypeResolver` for resolving base type
 (suggested by Ben F)
#370: Fail to add Object Id for POJO with no properties
 (reported by jh3141@github)
- Fix for [jackson-module-afterburner#38]: need to remove @JacksonStdImpl from
  `RawSerializer`, to avoid accidental removal of proper handling.

2.3.0 (13-Nov-2013)

#48: Add support for `InetSocketAddress`
 (contributed by Nick T)
#152: Add support for traversing `JsonNode` with (new!) `JsonPointer` implementation
 (suggested by fge@github)
#208: Accept "fromString()" as an implicit Creator (factory) method (alias for "valueOf()")
 (requested by David P)
#215: Allow registering custom `CharacterEscapes` to use for serialization,
 via `ObjectWriter.with(CharacterEscapes)` (and `ObjectMapper.writer(CharacterEscapes)`)
#227: Allow "generic" Enum serializers, deserializers, via `SimpleModule`
#234: Incorrect type information for deeply nested Maps
 (reported by Andrei P)
#237: Add `DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY` to optionally
  throw `JsonMappingException` on duplicate keys, tree model (`JsonNode`)
#238: Allow existence of overlapping getter, is-getter (choose 'regular' getter)
#239: Support `ByteBuffer`
 (suggested by mckamey@github)
#240: Make sure `@JsonSerialize.include` does not accidentally override
  class inclusion settings
 (requested by thierryhenrio@github)
#253: `DelegatingDeserializer` causes problems for Managed/BackReferences
 (reported by bfelaco@github)
#257: Make `UntypedObjectDeserializer` support overides for `List`, `Map` etc
#268: Add new variant of `ObjectMapper.canSerialize()` that can return `Throwable`
 that caused false to be returned (if any)
#269: Add support for new `@JsonPropertyDescription` via `AnnotationIntrospector`
 as well as `BeanProperty.getMedata().getDescription()`
#270: Add `SerializationFeature.USE_EQUALITY_FOR_OBJECT_ID` to allow use of equality
 (instead of identity) for figuring out when to use Object Id
 (requested by beku8@github)
#271: Support handling of `@JsonUnwrapped` for in-built JSON Schema generation
#277: Make `TokenBuffer` support new native type and object ids
#302: Add `setNamingStrategy` in `Module.SetupContext`
 (suggested by Miguel C)
#305: Add support for accessing `TypeFactory` via `TypeIdResolverBase`
 (not yet via `TypeIdResolver` interface), other configuration
#306: Allow use of `@JsonFilter` for properties, not just classes 
#307: Allow use of `@JsonFilter` for Maps in addition to POJOs
#308: Improve serialization and deserialization speed of `java.util.UUID` by 4x
 (suggested by David P)
#310: Improve `java.util.UUID` serialization with binary codecs, to use "raw" form.
#311: Make sure that "creator properties" are alphabetically ordered too, if
  so requested.
#315: Allow per-property definition of null serializer to use, using
 new `@JsonSerialize(nullsUsing=xxx)` annotation property
#317: Fix `JsonNode` support for nulls bound to `ObjectNode`, `ArrayNode`
 (contributed by Seth P)
#318: Problems with `ObjectMapper.updateValue()`, creator property-backed accessors
#319: Add support for per-call ("contextual") attributes, with defaulting,
 to allow keeping track of state during (de)serialization
#324: Make sure to throw `JsonMappingException` from `EnumDeserializer` creator,
  not `IllegalArgumentException`
 (reported by beverku@github)
#326: Support `@JsonFilter` for "any getter" properties
#334: Make `ArrayNode`, `ObjectNode` non-final again
#337: `AnySetter` does not support polymorphic types
 (reported by askvortsov@github)
#340: AtomicReference not working with polymorphic types
#342: Add `DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES` to make `ObjectMapper`
  throw exception when encountering explicitly ignored properties
 (requested by Ruslan M)
[JACKSON-890]: Support managed/back-references for polymorphic (abstract) types
- Add 'BeanPropertyWriter.isUnwrapping()' for future needs (by Afterburner)
- Add coercions from String "null" (as if null token was parsed) for primitives/Wrappers.
- Add `JsonDeserializer.handledType()`

2.2.4 (10-Jun-2014)

#292: Problems with abstract `Map`s, `Collection`s, polymorphic deserialization
#324: EnumDeserializer should throw JsonMappingException, not IllegalArgumentException
#346: Problems deserializing `ObjectNode` from empty JSON Object, with @JsonCreator

2.2.3 (22-Aug-2013)

#234: Problems with serializing types for deeply nested generic Maps, default typing 
#251: SerializationFeature.WRITE_BIGDECIMAL_AS_PLAIN ignored with JsonNode
  serialization
 (reported by fge@github)
#259: Fix a problem with JSON Schema generation for `@JsonValue`
 (reported by Lior L)
#267: Handle negative, stringified timestamps
 (reported by Drecth@github)
#281: Make `NullNode` use configured null-value serializer
#287: Fix problems with converters, Maps with Object values
 (reported by antubis@github)
#288: Fix problem with serialization converters assigned with annotations
 (reported by cemo@github)

2.2.2 (26-May-2013)

#216: Problems with Android, 1.6-only types
#217: JsonProcessingExceptions not all wrapped as expected
 (reported by karldmoore@github)
#220: ContainerNode missing 'createNumber(BigInteger)'
 (reported by Pascal G)
#223: Duplicated nulls with @JsonFormat(shape=Shape.ARRAY)
 (reported by lukegh@github)
#226: Field mapping fail on deserialization to common referenced object when
  @JsonUnwrapped is used
 (reported by ikvia@github)
#232: Converting bound BigDecimal value to tree fails with WRITE_BIGDECIMAL_AS_PLAIN
 (reported by celkings@github)
- Minor fix to handle primitive types for key deserializer lookups
- Add convenience method `MappingIterator.getCurrentLocation()`
 (suggested by Tomdz@github)

2.2.1 (03-May-2013)

#214: Problem with LICENSE, NOTICE, Android packaging
 (reported by thierryd@github)

2.2.0 (22-Apr-2013)

Fixes:

#23: Fixing typing of root-level collections
#118: JsonTypeInfo.as.EXTERNAL_PROPERTY not working correctly
 with missing type id, scalar types
#130: TimeZone not set for GregorianCalendar, even if configured
#144: MissingNode.isValueNode() should return 'false'
 (reported by 'fge@github')
#146: Creator properties were not being renamed as expected
 (contributed by Christoper C)
#188: Problem with ObjectId serialization, 'alwaysAsId' references

Improvements:

#116: JavaType implements `java.lang.reflect.Type` (as does `TypeReference`)
#147: Defer reporting of problems with missing creator parameters
 (contributed by Christoper C)
#155: Make `ObjectNode` and `ArrayNode` final (other node types already were)
 (requested by fge@github)
#161: Add deserializer for java.util.concurrent.ArrayBlockingQueue
#173: Add 'JsonNode.traverse(ObjectCodec)' for convenience
#181: Improve error reporting for missing '_valueDeserializer'
#194: Add `FloatNode` type in tree model (JsonNode)
 (requested by msteiger@github)
#199: Allow deserializing `Iterable` instances (as basic `Collection`s)
 (requested by electrum@github)
#206: Make 'ObjectMapper.createDeserializationContext()' overridable
 (requested by noter@github)
#207: Add explicit support for `short` datatypes, for tree model
 (contributed by msteiger@github)

New features:

#120: Extend BeanDeserializerModifier to work with non-POJO deserializers
#121: Extend BeanSerializerModifier to work with non-POJO serializers
#124: Add support for serialization converters (@JsonSerializer(converter=...))
#124: Add support for deserialization converters (@JsonDeserializer(converter=...))
#140: Add 'SerializationFeature.WRITE_BIGDECIMAL_AS_PLAIN' to allow forcing
  of non-scientific notation when serializing BigDecimals.
 (suggested by phedny@github)
#148: Add 'DeserializationFeature.FAIL_ON_INVALID_SUBTYPE`, which allows mapping
  entries with missing or invalid type id into null references (instead of failing).
  Also allows use of '@JsonTypeInfo.defaultImpl = NoClass.class' as alternative.
#159: Add more accessors in 'MappingIterator': getParser(), getParserSchema(),
  readAll()
 (suggested by Tom D)
#190: Add 'MapperFeature.ALLOW_FINAL_FIELDS_AS_MUTATORS' (default: true) for
 pruning out final fields (to avoid using as mutators)
 (requested by Eric T)
#195: Add 'MapperFeature.INFER_PROPERTY_MUTATORS' (default: enabled) for finer
  control of what mutators are auto-detected.
 (requested by Dain S)
#198: Add SPI metadata, handling in ObjectMapper (findModules()), for
  automatic registration of auto-detected extension modules
 (suggested by 'beamerblvd@github')
#203: Added new features to support advanced date/time handling:
  - SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS
  - DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS
  - DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE

Other:

#126: Update JDK baseline to 1.6
* API under 'com.fasterxml.jackson.databind.jsonFormatVisitors' changed significantly
  based on experiences with external JSON Schema generator.
* Version information accessed via code-generated access class, instead of reading
  VERSION.txt
* Added 2 methods in Converter interface: getInputType(), getOutputType(),
  to allow programmatic overrides (needed by JAXB annotation module)

2.1.4 (26-Feb-2013)

* [JACKSON-887]: StackOverflow with parameterized sub-class field
 (reported by Alexander M)
* [#130]: TimeZone not set for GregorianCalendar, when deserializing
* [#157]: NPE when registering module twice
* [#162]: JsonNodeFactory: work around an old bug with BigDecimal and zero
 (submitted by fge@github)
* [#166]: Incorrect optimization for `ObjectMapper.convertValue(Class)`
 (reported by Eric T)
* [#167]: Problems with @JsonValue, polymorphic types (regression from 1.x)
 (reported by Eric T)
* [#170]: Problems deserializing `java.io.File` if creator auto-discovery disabled
 (reported by Eric T)
* [#175]: NPE for JsonMappingException, if no path is specified
 (reported by bramp@github)

2.1.3 (19-Jan-2013)

* [Issue#141]: ACCEPT_EMPTY_STRING_AS_NULL_OBJECT not working for enums
* [Issue#142]: Serialization of class containing EnumMap with polymorphic enum
  fails to generate class type data
 (reported by kidavis4@github)

2.1.2 (04-Dec-2012)

* [Issue#106]: NPE in ObjectArraySerializer.createContextual(...)
* [Issue#117]: HandlerInstantiator defaulting not working
 (reported by Alexander B)
* [Issue#118]: Problems with JsonTypeInfo.As.EXTERNAL_PROPERTY, scalar values
 (reported by Adva11@github)
* [Issue#119]: Problems with @JsonValue, JsonTypeInfo.As.EXTERNAL_PROPERTY
 (reported by Adva11@github)
* [Issue#122]: ObjectMapper.copy() was not copying underlying mix-in map
 (reported by rzlo@github)

2.1.1 (11-Nov-2012)

Fixes:

* [JACKSON-875]: Enum values not found if Feature.USE_ANNOTATIONS disabled
 (reported by Laurent P)
* [Issue#93]: ObjectNode.setAll() broken; would not add anything for
  empty ObjectNodes.
 (reported by Francis G)
* Making things implement java.io.Serializable:
  - Issues: #94, #99, #100, #102
    (reported by Sean B)
* [Issue#96]: Problem with JsonTypeInfo.As.EXTERNAL_PROPERTY, defaultImpl
 (reported by Adva11@github)

2.1.0 (08-Oct-2012)

  New minor version for 2.x series. Major improvements in multiple areas,
  including:

  - Dataformat auto-detection
  - More `@JsonFormat.shape` variant to serialize Collections as
    JSON Objects, POJOs as JSON Arrays (csv-like).
  - Much more configuration accessible via ObjectReader, ObjectWriter
  - New mechanism for JSON Schema generation, other uses (in future)

Fixes:

* [JACKSON-830]/[Issue#19]: Change OSGi bundle name to be fully-qualified
* ]JACKSON-847]: Make @JsonIdentityInfo work with property-based creator
* [JACKSON-851]: State corruption with ObjectWriter, DefaultPrettyPrinter
 (reported by Duncan A)
* [Issue#75]: Too aggressive KeySerializer caching
* Minor fix wrt [Issue#11], coercion needed extra checks

Improvements:

* [JACKSON-758]: Remove 'IOException' from throws clauses of "writeValueAsString"
  and "writeValueAsBytes" of ObjectMapper/ObjectWriter
 (suggested by G-T Chen)
* [JACKSON-839]: Allow "upgrade" of integer number types for
  UntypedObjectDeserializer, even with default typing enabled.
* [JACKSON-850]: Allow use of zero-arg factory methods as "default creator"
  (suggested by Razvan D)
* [Issue#9]: Implement 'required' JSON Schema attribute for bean properties
* [Issue#20]: Add new exception type, InvalidFormatException (sub-type of
  JsonMappingException) to indicate data format problems
 (suggested by HolySamosa@github)
* [Issue#30]: ObjectReader and ObjectWriter now try to pre-fetch root
  (de)serializer if possible; minor performance improvement (2% for small POJOs).
* [Issue#33]: Simplified/clarified definition of 'ObjectReader.readValues()';
  minor change in behavior for JSON Array "wrapped" sequences
* [Issue#60]: Add 'JsonNode.hasNonNull(...)' method(s)
 (suggested by Jeff S on mailing list) 
* [Issue#64]: Add new "standard" PropertyNamingStrategy, PascalCaseStrategy
  (PropertyNamingStrategy.PASCAL_CASE_TO_CAMEL_CASE)
 (contributed by Sean B)
* [Issue#65]: Add getters to `ObjectMapper`, DeserializationContext/-Factory.
 (contributed by Dmitry K)
* [Issue#69]: Add `PropertyName` abstraction, new methods in AnnotationIntrospector
* [Issue#80]: Make `DecimalNode` normalize input, to make "1.0" and "1.00"equal
 (reported by fge@github)

New features:

* [Issue#15]: Support data format auto-detection via ObjectReader (added
  'withFormatDetection(...)' fluent factories)
* [Issue#21]: Add 'ObjectNode.set(...)' method (and related) to improve
  chaining, semantic consistency of Tree Model API
 (suggested by fge@Github)
* [Issue#22]: Add 'ObjectMapper.setAnnotationIntrospectors()' which allows
  defining different introspectors for serialization, deserialization
* [Issue#24]: Allow serialization of Enums as JSON Objects
 (suggested by rveloso@github)
* [Issue#28]: Add 'ObjectMapper.copy()', to create non-linked copy of
  mapper, with same configuration settings
* [Issue#29]: Allow serializing, deserializing POJOs as JSON Arrays
  by using `@JsonFormat(shape=Shape.ARRAY)`
* [Issue#40]: Allow serialization of Collections as JSON Objects
  (and deserialization from)
 (suggested by 'rveloso@github')
* [Issue#42]: Allow specifying Base64 variant to use for Base64-encoded data
  using ObjectReader.with(Base64Variant), ObjectWriter.with(Base64Variant).
 (suggested by 'mpfau@github')
* [Issue#45]: Add '@JsonNaming' annotation to define per-class PropertyNamingStrategy
 (suggested by Mark W)
* [Pull#58]: Make 'MappingIterator' implement 'Closable'
 (contributed by Pascal G)
* [Issue#72]: Add 'MapperFeature.USE_WRAPPER_NAME_AS_PROPERTY_NAME' to use
  wrapper name annotations for renaming properties
* [Issue#87]: Add 'StdDelegatingSerializer', 'StdDelegatingDeserializer' to
  simplify writing of two-step handlers
* (issue #4 of jackson-annotations): Add `@JsonIdentityReference(alwaysAsId=true)`
  to force ALL references to an object written as Object Id, even the first one.
* Added 'ObjectReader#withHandler' to allow for reconfiguring deserialization
  problem handler
 (suggested by 'electricmonk')

Other changes:

* New variant of AnnotationIntrospector.getFormat(), to support class
  annotations
* It is now possible to serialize instances of plain old Object, iff
  'FAIL_ON_EMPTY_BEANS' is disabled.
* Trying to remove reference to "JSON" in datatype conversion errors
 (since databinding is format-agnostic)

INCOMPATIBILITIES: (rats!)

* Note that [Issue#33] (see above) is, technically speaking, backwards
  imcompatible change. It is estimated that it should NOT affect most
  users, as changes are to edge cases (and undocumented ones at that).
  However, it can potentially cause problems with upgrade.
* Implementation of `JsonFormatVisitable` resulting in 2 new methods
  being added in `BeanPropertyFilter` interface -- this is unfortunate,
  but was required to support full traversability.

2.0.4 (26-Jun-2012)

* [Issue#6]: element count for PrettyPrinter, endObject wrong
   (reported by "thebluemountain")
* [JACKSON-838]: Utf8StreamParser._reportInvalidToken() skips letters
    from reported token name
   (reported by Lóránt Pintér)
* [JACKSON-841] Data is doubled in SegmentedStringWriter output
   (reported by Scott S)
* [JACKSON-842] ArrayIndexOutOfBoundsException when skipping C-style comments
   (reported by Sebastien R)

2.0.3: no version 2.0.3 released -- only used for extension modules

2.0.2 [14-May-2012]

Fixes:

* [Issue#14]: Annotations were not included from parent classes of
  mix-in classes
 (reported by @guillaup)
* [JACKSON-824]: Combination of JSON Views, ObjectMapper.readerForUpdating()
  was not working
 (reported by Nir S)
(and all fixes from 1.9.7)

Improvements:

* [Issue#11]: Improve ObjectMapper.convertValue()/.treeToValue() to use
  cast if possible

2.0.1 [23-Apr-2012]

Fixes:

* [JACKSON-827] Ensure core packages work on JDK 1.5
 (reported by Pascal g)
* [JACKSON-829] Custom serializers not working for List<String> properties,
  @JsonSerialize(contentUsing)
 (reported by James R)

Improvements:

* [Issue#5]: Add support for maps with java.util.Locale keys to the set of
  StdKeyDeserializers
 (contributed by Ryan G)

2.0.0 [25-Mar-2012]

Fixes:

* [JACKSON-368]: Problems with managed references, abstract types
* [JACKSON-711]: Delegating @JsonCreator did not work with Injectable values
* [JACKSON-798]: Problem with external type id, creators
  (reported by Casey L)
(and all fixes up until and including 1.9.6)

Improvements:

* [JACKSON-546]: Indicate end-of-input with JsonMappingException instead
  of EOFException, when there is no parsing exception
* [JACKSON-664]: Reduce overhead of type resolution by adding caching
  in TypeFactory
* [JACKSON-690]: Pass DeserializationContext through ValueInstantiator
* [JACKSON-695]: Add 'isEmpty(value)' in JsonSerializer to allow
  customizing handling of serialization of empty values
* [JACKSON-710]: 'ObjectMapper.convertValue()' should ignore root value
  wrapping/unwrapping settings
* [JACKSON-730] Split various features (JsonParser, JsonGenerator,
  SerializationConfig, DeserializationConfig) into per-factory
  features (MapperFeature, JsonFactory.Feature) an per
  instance features (existing ones)
* [JACKSON-732]: Allow 'AnnotationIntrospector.findContentDeserializer()'
  (and similar) to return instance, not just Class<?> for instance
 (requested by James R)
* [JACKSON-736]: Add (more) access to array, container and map serializers
* [JACKSON-737]: Allow accessing of "creator properties" for BeanDeserializer
* [JACKSON-748]: Add 'registerSubtypes' to 'Module.setupContext' (and SimpleModule)
* [JACKSON-749]: Make @JsonValue work for Enum deserialization
* [JACKSON-769]: ObjectNode/ArrayNode: change 'put', 'insert', 'add' to return
  'this node' (unless already returning something)
* [JACKSON-770]: Simplify method naming for JsonNode, drop unnecessary 'get' prefix
  from methods like 'getTextValue()' (becomes 'textValue()')
* [JACKSON-777]: Rename 'SerializationConfig.Feature' as 'SerializationFeature',
  'DeserializationConfig.Feature' as 'DeserializationFeature'
* [JACKSON-780]: MissingNode, NullNode should return 'defaultValue' from 'asXxx' methods,
  (not 0 for numbers), as they are not numeric types
* [JACKSON-787]: Allow use of @JsonIgnoreProperties for properties (fields, getters, setters)
* [JACKSON-795]: @JsonValue was not working for Maps, Collections
* [JACKSON-800]: Add 'Module.SetupContext#addDeserializationProblemHandler'
 (suggested by James R)

New features:

* [JACKSON-107]: Add support for Object Identity (to handled cycles, shared refs),
  with @JsonIdentityInfo
* [JACKSON-435]: Allow per-property Date formatting using @JsonFormat.
* [JACKSON-437]: Allow injecting of type id as POJO property, by setting
  new '@JsonTypeInfo.visible' property to true.
* [JACKSON-469]: Support "Builder pattern" for deserialiation; that is, allow
  use of separate Builder object for data binding, creating actual value
* [JACKSON-608]: Allow use of JSON Views for deserialization
* [JACKSON-636]: Add 'SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS' to allow
  forced sorting of Maps during serialization
  (suggested by Joern H)
* [JACKSON-669]: Allow prefix/suffix for @JsonUnwrapped properties
 (requested by Aner P)
* [JACKSON-707]: Add 'JsonNode.deepCopy()', to create safe deep copies
  of ObjectNodes, ArrayNodes.
* [JACKSON-714]: Add general-purpose @JsonFormat annotation
* [JACKSON-718]: Added 'JsonNode.canConvertToInt()', 'JsonNode.canConvertToLong()'
* [JACKSON-747]: Allow changing of 'SerializationFeature' for ObjectWriter,
  'DeserializationFeature' for ObjectReader.
* [JACKSON-752]: Add @JsonInclude (replacement of @JsonSerialize.include)
* [JACKSON-754]: Add @JacksonAnnotationsInside for creating "annotation
  bundles" (also: AnnotationIntrospector.isAnnotationBundle())
* [JACKSON-762]: Allow using @JsonTypeId to specify property to use as
  type id, instead of using separate type id resolver.
* [JACKSON-764]: Allow specifying "root name" to use for root wrapping
  via ObjectReader, ObjectWriter.
* [JACKSON-772]: Add 'JsonNode.withArray()' to use for traversing Array nodes.
* [JACKSON-793]: Add support for configurable Locale, TimeZone to use
  (via SerializationConfig, DeserializationConfig)
* [JACKSON-805]: Add 'SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED'
  to improve interoperability with BadgerFish/Jettison
* [JACKSON-810]: Deserialization Feature: Allow unknown Enum values via
  'DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL'
  (suggested by Raymond R)
* [JACKSON-813]: Add '@JsonSerializableSchema.id' attribute, to indicate
  'id' value to add to generated JSON Schemas.

[entries for versions 1.x and earlier not retained; refer to earlier releases)
