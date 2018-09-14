Here are people who have contributed to the development of Jackson JSON processor
databind core component, version 2.x
(version numbers in brackets indicate release in which the problem was fixed)

(note: for older credits, check out release notes for 1.x versions)

Tatu Saloranta, tatu.saloranta@iki.fi: author

Pascal Glinas:
  * Contributed fixes to 'MappingIterator' handling (Pull#58 and Pull#59)
   (2.1.0)
  * Reported #220: ContainerNode missing 'createNumber(BigInteger)'
   (2.2.2)

Joern Huxhorn: (huxi@github)
  * Suggested [JACKSON-636]: Add 'SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS' to allow
    forced sorting of Maps during serialization
   (2.0.0)
  * Reported #479: NPE on trying to deserialize a `String[]` that contains null
   (2.4.1)
  * Reported #1411: MapSerializer._orderEntries should check for null keys
   (2.7.9)

James Roper:
 * Requested [JACKSON-732]: Allow 'AnnotationIntrospector.findContentDeserializer()'
    (and similar) to return instance, not just Class<?> for instance
  (2.0.0)
 * Suggested [JACKSON-800]: Adding a method for letting modules register
    DeserializationProblemHandlers
  (2.0.0)

Casey Lucas:
 * Reported [JACKSON-798]: Problem with external type id, creators
  (2.0.0)

Tammo van Lessen:
 * Reported [JACKSON-811]: Problems with @JsonIdentityInfo, abstract types
  (2.0.0)
 * Reported [JACKSON-814]: Parsing RFC822/RFC1123 dates failes on non-US locales
  (2.0.0)

Raymond Myers:
 * Suggested [JACKSON-810]: Deserialization Feature: Allow unknown Enum values via
    'DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL'
  (2.0.0)

Ryan Gardner:
 * Contributed #5 -- Add support for maps with java.util.Locale keys
    to the set of StdKeyDeserializers
  (2.0.1)

Razvan Dragut:
 * Suggested [JACKSON-850]: Allow use of zero-arg factory methods as "default creator"
  (2.1.0)

Duncan Atkinson:
 * Reported [JACKSON-851]: State corruption with ObjectWriter, DefaultPrettyPrinter
  (2.1.0)

Mark Wolfe:
 * Suggested #45: Add `@JsonNaming()` for per-class naming strategy overrides
  (2.1.0)

Dmitry Katsubo:
 * Contributed patch for #65: Add getters to `ObjectMapper`, DeserializationContext,
   DeserializationFactory.
  (2.1.0)

Francis Galiegue:
 * Reported #93 (and suggested fix): bug in `ObjectMapper.setAll(...)'
  implementation
  (2.1.1)
 * Reported #433: `ObjectMapper`'s `.valueToTree()` wraps `JsonSerializable` objects
  into a POJONode
  (2.3.3)
 * Contributed #434: Ensure that DecimalNodes with mathematically equal values are equal
  (2.4.0)

kelaneren@github:
 * Reported #157, contributed unit test: NPE when registering same module twice.
  (2.1.4)

Eric Tschetter (cheddar@github):
  * Reported issues #166, #167, #170 (regressions from 1.9.x to 2.x)
   (2.1.4)

Thierry D (thierryd@github)
  * Reported #214: Problem with LICENSE, NOTICE, Android packaging
   (2.2.2)

Luke G-H (lukegh@github)
  * Reported #223: Duplicated nulls with @JsonFormat(shape=Shape.ARRAY)
   (2.2.2)

Karl Moore (karldmoore@github)
  * Reported #217: JsonProcessingExceptions not all wrapped as expected
   (2.2.2)

David Phillips:
  * Requested #308: Improve serialization and deserialization speed of `java.util.UUID`
   (2.3.0)

Seth Pellegrino (jivesoft):
  * Contributed #317: Fix `JsonNode` support for nulls bound to	`ObjectNode`, `ArrayNode`
   (2.3.0)

Florian Schoppmann (fschopp@github)
  * Reported #357: StackOverflowError with contentConverter that returns array type
   (2.7.0)
  * Reported #358: `IterableSerializer` ignoring	annotated content serializer
   (2.3.1)
  * Reported #359: Converted object not using explicitly annotated serializer
   (2.4.0)

Martin Traverso:
  * Reported #406: Cannot use external type id + @JsonTypeIdResolver
   (2.3.2)

Matthew Morrissette:
  * Contributed #381: Allow inlining/unwrapping of value from single-component JSON array
   (2.4.0)

Will Palmeri: (wpalmeri@github)
  * Contributed #407: Make array and Collection serializers use configured value null handler
   (2.4.0)

Cemalettin Koc: (cemo@github)
  * Reported #353: Problems with polymorphic types, `JsonNode` (related to #88)
   (2.4.0)

Ben Fagin: (UnquietCode@github)
  * Suggested #442: Make `@JsonUnwrapped` indicate property inclusion
   (2.4.0)
  * Contributed #81/#455: Allow use of @JsonUnwrapped with typed (@JsonTypeInfo) classes,
    provided that (new) feature `SerializationFeature.FAIL_ON_UNWRAPPED_TYPE_IDENTIFIERS`
    is disabled
   (2.4.0)

Chris Cleveland:
  * Suggested #463: Add 'JsonNode.asText(String defaultValue)`
   (2.4.0)

Benson Margulies:
  * Reported #467: Unwanted POJO's embedded in tree via serialization to tree
   (2.4.0)
  * Reported #601: ClassCastException for a custom serializer for enum key in `EnumMap`
   (2.4.4)
  * Contributed 944: Failure to use custom deserializer for key deserializer
   (2.6.3)
  * Reported #1120: String value omitted from weirdStringException
   (2.6.6)
  * Reported, fixed #1235: `java.nio.file.Path` support incomplete
   (2.8.0)
  * Reported #1270: Generic type returned from type id resolver seems to be ignored
   (2.8.0)

Steve Sanbeg: (sanbeg@github)
  * Contributed #482: Make date parsing error behavior consistent with JDK
   (2.4.1)

Ian Barfield: (tea-dragon@github)
  * Reported #580: delegate deserializers choke on a (single) abstract/polymorphic parameter
   (2.4.4)
  * Reported #844: Using JsonCreator still causes invalid path references in JsonMappingException
   (2.5.5)

Eugene Lukash
  * Reported #592: Wrong `TokenBuffer` delegate deserialization using `@JsonCreator`
   (2.4.4)

Fernando Otero (zeitos@github)
  * Contributed fix for #610: Problem with forward reference in hierarchies
   (2.4.4)

Lovro Pandžić (lpandzic@github)
  * Reported #421: @JsonCreator not used in case of multiple creators with parameter names
   (2.5.0)

Adam Stroud (adstro@github)
  * Contributed	#576: Add fluent API for adding mixins
   (2.5.0)

David Fleeman (fleebytes@github)
  * Contributed #528 implementation: Add support for `JsonType.As.EXISTING_PROPERTY`
   (2.5.0)

Aurélien Leboulanger (herau@github)
  * Contributed improvement for #597: Improve error messaging for cases	where JSON Creator
    returns null (which is illegal)
   (2.5.0)

Michael Spiegel (mspiegel@githib)
  * Contributed #636: `ClassNotFoundException` for classes not (yet) needed during serialization
   (2.5.0)

Michael Ressler (mressler@github)
  * Contributed #566: Add support for case-insensitive deserialization
   (`MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES`)
   (2.5.0)

Konstantin Labun (kulabun@github)
  * Reported #647: Deserialization fails when @JsonUnwrapped property contains an object with same property name
   (2.5.0)

Christopher Smith (chrylis@github)
  * Reported #594: `@JsonValue` on enum not used when enum value is a Map key
   (2.5.0)

Alexandre Santana Campelo (alexqi200@github):
  * Contributed #671: Adding `java.util.Currency` deserialization support for maps
   (2.5.1)

Zoltan Farkas (zolyfarkas@github)
  * Reported #674: Spring CGLIB proxies not handled as intended
   (2.5.1)

Ludevik@github:
  * Reported #682: Class<?>-valued Map keys not serialized properly
   (2.5.1)

Antibrumm@github:
  * Reported #691: Jackson 2.5.0. NullSerializer for MapProperty failing
   (2.5.2)
  * Reported #984: JsonStreamContexts are not build the same way for write.. and convert methods
   (2.6.4)

Shumpei Akai (flexfrank@github)
  * Reported #703: Multiple calls to ObjectMapper#canSerialize(Object.class) returns different values
   (2.5.2)

Francisco A. Lozano (flozano@github)
  * Contributed fix for #703 (see above)
   (2.5.2)

Dylan Scott (dylanscott@github)
  * Reported #738: #738: @JsonTypeInfo non-deterministically ignored in 2.5.1 (concurrency
    issue)
   (2.5.2)

Alain Gilbert (agilbert314@github)
  * Reporter, contributed #766: Fix Infinite recursion (StackOverflowError) when
    serializing a SOAP object
   (2.5.3)

Alexey Gavrilov (Alexey1Gavrilov@github)
  * Reported, contributed fix for #761: Builder deserializer: in-compatible type exception
    when return type is super type
   (2.5.3)

Dmitry Spikhalskiy (Spikhalskiy@github)
  * Reported #731, suggested the way to fix it: XmlAdapter result marshaling error in
    case of ValueType=Object
   (2.5.3)
  * Reported #1456: `TypeFactory` type resolution broken in 2.7 for generic types
   when using `constructType` with context
   (2.7.9 / 2.8.6)

John Meyer (jpmeyer@github)
  * Reported, contributed fix for #745: EnumDeserializer.deserializerForCreator() fails
    when used to deserialize a Map key
   (2.5.3)

Andrew Duckett (andrewduckett@github)
  * Reported #771: Annotation bundles ignored when added to Mixin
   (2.5.4)

Charles Allen:
  * Contributed #785: Add handlings for classes which are available in
    `Thread.currentThread().getContextClassLoader()`
   (2.5.4)

Andrew Goodale (newyankeecodeshop@github)
  * Contributed #816: Allow date-only ISO strings to have no time zone
   (2.5.4)

Kamil Benedykciński (Kamil-Benedykcinski@github)
  * Contributed #801: Using `@JsonCreator` cause generating invalid path reference
   in `JsonMappingException`
   (2.5.4)

Chi Kim (chikim79@github)
  * Reported #878: serializeWithType on BeanSerializer does not setCurrentValue
   (2.5.5 / 2.6.1)

Charles Allen (drcrallen@github):
  * Reported #696: Copy constructor does not preserve `_injectableValues`
   (2.6.0)

Chris Pimlott (pimlottc@github):
  * Suggested #348: ObjectMapper.valueToTree does not work with @JsonRawValue
   (2.6.0)

Laird Nelson (ljnelson@github)
  * Suggested #688: Provide a means for an ObjectMapper to discover mixin annotation
    classes on demand
   (2.6.0)
  * Reported #1088: NPE possibility in SimpleMixinResolver
   (2.6.6)

Derk Norton (derknorton@github)
  * Suggested #689: Add `ObjectMapper.setDefaultPrettyPrinter(PrettyPrinter)`
   (2.6.0)

Michal Letynski (mletynski@github)
  * Suggested #296: Serialization of transient fields with public getters (add
    MapperFeature.PROPAGATE_TRANSIENT_MARKER)
   (2.6.0)

Jeff Schnitzer (stickfigure@github)
  * Suggested #504: Add `DeserializationFeature.USE_LONG_FOR_INTS`
   (2.6.0)

Jerry Yang (islanderman@github)
  * Contributed #820: Add new method for `ObjectReader`, to bind from JSON Pointer position
   (2.6.0)

Lars Pfannenschmidt (larsp@github)
  * Contributed #826: Replaced synchronized HashMap with ConcurrentHashMap in
   TypeDeserializerBase._findDeserializer
   (2.6.0)

Stephen A. Goss (thezerobit@github)
  * Contributed #828: Respect DeserializationFeatures.WRAP_EXCEPTIONS in CollectionDeserializer
   (2.6.0)

Andy Wilkinson (wilkinsona@github)
  * Reported #889: Configuring an ObjectMapper's DateFormat changes time zone
   (2.6.1)

lufe66@github:
  * Reported 894: When using withFactory on ObjectMapper, the created Factory has a TypeParser
    which still has the original Factory
   (2.6.2)

Daniel Walker (dsw2127@github)
  * Reported, contributed fix for #913: `ObjectMapper.copy()` does not preserve
   `MappingJsonFactory` features
   (2.6.2)

Sadayuki Furuhashi (frsyuki@github)
  * Reported #941: Deserialization from "{}" to ObjectNode field causes
    "out of END_OBJECT token" error
   (2.6.3)

David Haraburda (dharaburda@github)
  * Contributed #918: Add `MapperFeature.ALLOW_EXPLICIT_PROPERTY_RENAMING`
   (2.7.0)

Sergio Mira (Sergio-Mira@github)
  * Contributed #940: Add missing `hashCode()` implementations for `JsonNode` types that did not have them
   (2.6.3)

Andreas Pieber (anpieber@github)
  * Reported #939: Regression: DateConversionError in 2.6.x	
   (2.6.3)

Jesse Wilson (swankjesse@github)
  * Contributed #948: Support leap seconds, any number of millisecond digits for ISO-8601 Dates.
   (2.6.3)
  * Contributed #949: Report the offending substring when number parsing fails
   (2.6.3)

Warren Bloomer (stormboy@github)
  * Reported #942: Handle null type id for polymorphic values that use external type id
   (2.6.3)

Ievgen Pianov (pyanoveugen@github)
  * Reported #989: Deserialization from "{}" to java.lang.Object causes "out of END_OBJECT token" error
   (2.6.3)

Jayson Minard (apatrida@github)
  * Reported #1005: Synthetic constructors confusing Jackson data binding
   (2.6.4)
  * Reported #1438: `ACCEPT_CASE_INSENSITIVE_PROPERTIES` is not respected for creator properties
   (2.8.5)

David Bakin (david-bakin@github)
  * Reported #1013: `@JsonUnwrapped` is not treated as assuming `@JsonProperty("")`
   (2.6.4)
  * Suggested #1011: Change ObjectWriter::withAttributes() to take a Map with some kind of wildcard types
   (2.7.0)

Dmitry Romantsov (DmRomantsov@github)
  * Reported #1036: Problem with case-insensitive deserialization
   (2.6.4)

Daniel Norberg (danielnorberg@github)
  * Contributed #1099: Fix custom comparator container node traversal
   (2.6.6)

Miles Kaufmann (milesk-amzn@github)
  * Reported #432: `StdValueInstantiator` unwraps exceptions, losing context
   (2.7.0)

Thomas Mortagne (tmortagne@github)
  * Suggested #857: Add support for java.beans.Transient
   (2.7.0)

Jonas Konrad (yawkat@github)
  * Suggested #905: Add support for `@ConstructorProperties`
   (2.7.0)

Jirka Kremser (Jiri-Kremser@github)
  * Suggested #924: SequenceWriter.writeAll() could accept Iterable
   (2.7.0)

Daniel Mischler (danielmischler@github)
  * Requested #963: Add PropertyNameStrategy `KEBAB_CASE`
   (2.7.0)

Shumpei Akai (flexfrank@github)
  * Reported #978: ObjectMapper#canSerialize(Object.class) returns false even though
   FAIL_ON_EMPTY_BEANS is disabled
   (2.7.0)

Hugo Wood (hgwood@github)
  * Contributed #1010: Support for array delegator
   (2.7.0)

Julian Hyde (julianhyde@github)
  * Reported #1083: Field in base class is not recognized, when using `@JsonType.defaultImpl`
   (2.7.1)

Thibault Kruse (tkruse@github)
  * Reported #1102: Handling of deprecated `SimpleType.construct()` too minimalistic
   (2.7.1)

Aleks Seovic (aseovic@github)
  * Reported #1109: @JsonFormat is ignored by the DateSerializer unless either a custom pattern
    or a timezone are specified
   (2.7.1)

Timur Shakurov (saladinkzn@github)
  * Reported #1134: Jackson 2.7 doesn't work with jdk6 due to use of `Collections.emptyIterator()`
   (2.7.2)

Jiri Mikulasek (pirkogdc@github)
  * Reported #1124: JsonAnyGetter ignores JsonSerialize(contentUsing=...)
   (2.7.2)

Xavi Torrens (xavitorrens@github)
  * Reported #1150: Problem with Object id handling, explicit `null` token
   (2.7.3)

Yoann Rodière (fenrhil@github)
  * Reported #1154: @JsonFormat.pattern on dates is now ignored if shape is not
    explicitely provided
   (2.7.3)

Mark Woon (markwoon@github)
  * Reported #1178: `@JsonSerialize(contentAs=superType)` behavior disallowed in 2.7
   (2.7.4)
  * Reported #1231: `@JsonSerialize(as=superType)` behavior disallowed in 2.7.4
   (2.7.5)
  * Suggested #507: Support for default `@JsonView` for a class
   (2.9.0)

Tom Mack (tommack@github)
  * Reported #1208: treeToValue doesn't handle POJONodes that contain exactly
    the requested value type
   (2.7.4)

William Headrick (headw01@github)
   * Reported#1223: `BasicClassIntrospector.forSerialization(...).findProperties` should
    respect MapperFeature.AUTO_DETECT_GETTERS/SETTERS?
   (2.7.5)

Nick Babcock (nickbabcock)
  * Reported #1225: `JsonMappingException` should override getProcessor()
   (2.7.5)
  * Suggested #1356: Differentiate between input and code exceptions on deserialization
   (2.9.0)

Andrew Joseph (apjoseph@github)
  * Reported #1248: `Annotated` returns raw type in place of Generic Type in 2.7.x
   (2.7.5)

Erich Schubert (kno10@github)
  * Reported #1260: `NullPointerException` in `JsonNodeDeserializer`, provided fix
   (2.7.5)

Brian Pontarelli (voidmain@github)
  * Reported #1301: Problem with `JavaType.toString()` for recursive (self-referential) types
   (2.7.6)

Max Drobotov (fizmax@github)
  * Reported, contributed fix for #1332: `ArrayIndexOutOfBoundException` for enum by index deser
   (2.7.7)

Stuart Douglas (stuartwdouglas@github)
  * Reported #1363: The static field ClassUtil.sCached can cause a class loader leak
   (2.7.8)

Josh Caplan (jecaplan@github)
  * Reported, suggested fix for #1368: Problem serializing `JsonMappingException` due to addition
    of non-ignored `processor` property (added in 2.7)
   (2.7.8)

Diego de Estrada (diegode@github)
  * Contributed fix for #1367: No Object Id found for an instance when using `@ConstructorProperties`
   (2.7.9)

Kevin Hogeland (khogeland@github)
  * Reported #1501: `ArrayIndexOutOfBoundsException` on non-static inner class constructor
   (2.7.9)

Artur Jonkisz (ajonkisz@github)
  * Reported #960: `@JsonCreator` not working on a factory with no arguments for ae enum type
   (2.8.0)

Mikhail Kokho (mkokho@github)
  * Contributed impl for #990: Allow failing on `null` values for creator (add
  `DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES`)
   (2.8.0)

Aleksandr Oksenenko (oleksandr-oksenenko@github)
  * Reported #999: External property is not deserialized
   (2.8.0)

Lokesh Kumar (LokeshN@github)
  * Contributed impl for #1082: Can not use static Creator factory methods for `Enum`s,
    with JsonCreator.Mode.PROPERTIES
   (2.8.0)
  * Reported #1217: `@JsonIgnoreProperties` on Pojo fields not working for deserialization
   (2.8.0)

Ross Goldberg
  * Reported #1165, provided fix for: `CoreXMLDeserializers` does not handle
    time-only `XMLGregorianCalendar`s
   (2.8.0)

Maarten Billemont (lhunath@github)
  * Suggested #1184: Allow overriding of `transient` with explicit inclusion with `@JsonProperty`
   (2.8.0)

Vladimir Kulev (lightoze@github)
  * Reported #1028: Ignore USE_BIG_DECIMAL_FOR_FLOATS for NaN/Infinity
   (2.8.0)

Ari Fogel (arifogel@github)
  * Reported #1261, contributed fix for: `@JsonIdentityInfo` deserialization fails with
    combination of forward references, `@JsonCreator`
   (2.8.0)

Andriy Plokhotnyuk (plokhotnyuk@github)
  * Requested #1277: Add caching of resolved generic types for `TypeFactory`
   (2.8.0)

Arek Gabiga (arekgabiga@github)
  * Reported #1297: Deserialization of generic type with Map.class
   (2.8.1)

Chris Jester-Young (cky@github)
  * Contributed #1335: Unconditionally call `TypeIdResolver.getDescForKnownTypeIds`
   (2.8.2)

Andrew Snare (asnare@github)
  * Reported #1315: Binding numeric values can BigDecimal lose precision
   (2.8.2)

Gili Tzabari (cowwoc@github)
  * Reported #1351: `@JsonInclude(NON_DEFAULT)` doesn't omit null fields
   (2.8.3)

Oleg Zhukov (OlegZhukov@github)
  * Reported #1384: `@JsonDeserialize(keyUsing = ...)` does not work correctly
   together with `DefaultTyping.NON_FINAL`
   (2.8.4)

Pavel Popov (tolkonepiu@github)
  * Contributed fix #1389: Problem with handling of multi-argument creator with Enums
   (2.8.4)

Josh Gruenberg (joshng@github)
  * Reported #1403: Reference-chain hints use incorrect class-name for inner classes
   (2.8.4)

Kevin Donnelly (kpdonn@github)
  * Reported #1432: Off by 1 bug in PropertyValueBuffer
   (2.8.5)

Nathanial Ofiesh (ofiesh@github)
  * Reported #1441: Failure with custom Enum key deserializer, polymorphic types
   (2.8.5)

Frédéric Camblor (fcamblor@github)
  * Reported #1451: Type parameter not passed by `ObjectWriter` if serializer pre-fetch disabled
   (2.8.6)

Stephan Schroevers (Stephan202@github)
  * Reported #1505: @JsonEnumDefaultValue should take precedence over FAIL_ON_NUMBERS_FOR_ENUMS
   (2.8.7)

Alex Panchenko (panchenko@github)
  * Reported #1543: JsonFormat.Shape.NUMBER_INT does not work when defined on enum type in 2.8
   (2.8.8)

Joshua Jones
  * Reported #1573, contributed fix: Missing properties when deserializing using a builder class
   with a non-default constructor and a mutator annotated with `@JsonUnwrapped`
   (2.8.8)

Ivo Studens (istudens@redhat.com)
  * Contributed #1585: Invoke ServiceLoader.load() inside of a privileged block
    when loading modules using `ObjectMapper.findModules()`
   (2.8.9)

Javy Luo (AnywnYu@github)
  * Reported #1595: `JsonIgnoreProperties.allowSetters` is not working in Jackson 2.8
   (2.8.9)

Marco Catania (catanm@github.com)
  * Contributed #1597: Escape JSONP breaking characters
   (2.8.9)

Andrew Joseph (apjoseph@github)
  * Reported #1629 `FromStringDeserializer` ignores registered `DeserializationProblemHandler`
    for `java.util.UUID`
   (2.8.9)

Joe Littlejohn (joelittlejohn@github)
  * Contributed #1642: Support `READ_UNKNOWN_ENUM_VALUES_AS_NULL` with `@JsonCreator`
   (2.8.9)

Slobodan Pejic (slobo-showbie@github)
  * Reported #1647, contributed fix: Missing properties from base class when recursive
    types are involved
   (2.8.9)

Bertrand Renuart (brenuart@github)
  * Reported #1648: `DateTimeSerializerBase` ignores configured date format when creating contextual
   (2.8.9)
  * Reported #1651: `StdDateFormat` fails to parse 'zulu' date when TimeZone other than UTC
   (2.8.9)
  * Suggested #1745: StdDateFormat: accept and truncate millis larger than 3 digits
   (2.9.1)
  * Contributed #1749: StdDateFormat: performance improvement of '_format(..)' method
   (2.9.1)
  * Contributed #1759: Reuse `Calendar` instance during parsing by `StdDateFormat`
   (2.9.1)

Kevin Gallardo (newkek@github)
  * Reported #1658: Infinite recursion when deserializing a class extending a Map,
    with a recursive value type
   (2.8.10)
  * Reported #1729: Integer bounds verification when calling `TokenBuffer.getIntValue()`
   (2.9.4)

Lukas Euler
  * Reported #1735: Missing type checks when using polymorphic type ids

Guixiong Wu (吴桂雄)
  * Reported #2032: Blacklist another serialization gadget (ibatis)
   (2.8.11.2)

svarzee@github
  * Reported #2109, suggested fix: Canonical string for reference type is built incorrectly
   (2.8.11.3 / 2.9.7)

Connor Kuhn (ckuhn@github)
  * Contributed #1341: FAIL_ON_MISSING_EXTERNAL_TYPE_ID_PROPERTY
   (2.9.0)

Jan Lolling (jlolling@github)
  * Contributed #1319: Add `ObjectNode.put(String, BigInteger)`
   (2.9.0)

Michael R Fairhurst (MichaelRFairhurst@github)
  * Reported #1035: `@JsonAnySetter` assumes key of `String`, does not consider declared type.
   (2.9.0)

Fabrizio Cucci (fabriziocucci@github)
  * Reported #1406: `ObjectMapper.readTree()` methods do not return `null` on end-of-input
   (2.9.0)

Emiliano Clariá (emilianogc@github)
  * Contributed #1434: Explicitly pass null on invoke calls with no arguments
   (2.9.0)

Ana Eliza Barbosa (AnaEliza@github)
  * Contributed #1520: Case insensitive enum deserialization feature.
   (2.9.0)

Lyor Goldstein (lgoldstein@github)
  * Reported #1544: `EnumMapDeserializer` assumes a pure `EnumMap` and does not support
    derived classes
   (2.9.0)

Harleen Sahni (harleensahni@github)
  * Reported #403: Make FAIL_ON_NULL_FOR_PRIMITIVES apply to primitive arrays and other
    types that wrap primitives
   (2.9.0)

Jared Jacobs (2is10@github)
  * Requested #1605: Allow serialization of `InetAddress` as simple numeric host address
   (2.9.0)

Patrick Gunia (pgunia@github)
  * Reported #1440: Wrong `JsonStreamContext` in `DeserializationProblemHandler` when reading
  `TokenBuffer` content
   (2.9.0)

Carsten Wickner (CarstenWickner@github)
  * Contributed #1522: Global `@JsonInclude(Include.NON_NULL)` for all properties with a specific type
   (2.9.0)

Chris Plummer (strmer15@github)
  * Reported #1637: `ObjectReader.at()` with `JsonPointer` stops after first collection
   (2.9.0)

Christian Basler (Dissem@github)
  * Reported #1688: Deserialization fails for `java.nio.file.Path` implementations when
    default typing enabled
   (2.9.0)

Tim Bartley (tbartley@github)
  * Reported, suggested fix for #1705: Non-generic interface method hides type resolution info
    from generic base class
   (2.9.1)

Luís Cleto (luiscleto@github)
  * Suggested 1768: Improve `TypeFactory.constructFromCanonical()` to work with
   `java.lang.reflect.Type.getTypeName()` format
   (2.9.2)

Vincent Demay (vdemay@github)
  * Reported #1793: `java.lang.NullPointerException` in `ObjectArraySerializer.acceptJsonFormatVisitor()`
    for array value with `@JsonValue`
   (2.9.2)

Peter Jurkovic (peterjurkovic@github)
  * Reported #1823: ClassNameIdResolver doesn't handle resolve Collections$SingletonMap,
    Collections$SingletonSet
   (2.9.3)

alinakovalenko@github:
  * Reported #1844: Map "deep" merge only adds new items, but not override existing values
   (2.9.3)

Pier-Luc Whissell (pwhissell@github):
  * Reported #1673: Serialising generic value classes via Reference Types (like Optional) fails
    to include type information
   (2.9.4)

Alexander Skvortcov (askvortcov@github)
  * Reported #1853: Deserialise from Object (using Creator methods) returns field name
    instead of value
   (2.9.4)

Joe Schafer (jschaf@github)
  * Reported #1906: Add string format specifier for error message in `PropertyValueBuffer`
   (2.9.4)
  * Reported #1907: Remove `getClass()` from `_valueType` argument for error reporting
   (2.9.4)

Deblock Thomas (deblockt@github)
  * Reported, contributed fix for #1912: `BeanDeserializerModifier.updateBuilder()` does not
    work to set custom  deserializer on a property (since 2.9.0)
 (contributed by Deblock T)

lilei@venusgroup.com.cn:
  * Reported #1931: Two more `c3p0` gadgets to exploit default typing issue
   (2.9.5)

Aniruddha Maru (maroux@github)
  * Reported #1940: `Float` values with integer value beyond `int` lose precision if
    bound to `long`
   (2.9.5)

Timur Shakurov (saladinkzn@github)
  * Reported #1947: `MapperFeature.AUTO_DETECT_XXX` do not work if all disabled
   (2.9.5)

roeltje25@github
  * Reported #1978: Using @JsonUnwrapped annotation in builderdeserializer hangs in
    infinite loop
   (2.9.5)

Freddy Boucher (freddyboucher@github)
  * Reported #1990: MixIn `@JsonProperty` for `Object.hashCode()` is ignored
   (2.9.6)

Ondrej Zizka (OndraZizk@github)
  * Reported #1999: "Duplicate property" issue should mention which class it complains about
   (2.9.6)

Jakub Skierbiszewski (jskierbi@github)
  * Reported, contributed fix for #2001: Deserialization issue with `@JsonIgnore` and
    `@JsonCreator` + `@JsonProperty` for same property name
   (2.9.6)

Carter Kozak (cakofony@github)
  * Reported #2016: Delegating JsonCreator disregards JsonDeserialize info
   (2.9.6)

Reinhard Prechtl (dnno@github)
  * Reported #2034: Serialization problem with type specialization of nested generic types
   (2.9.6)

Chetan Narsude (243826@github)
  * Reported #2038: JDK Serializing and using Deserialized `ObjectMapper` loses linkage
    back from `JsonParser.getCodec()`
   (2.9.6)

Petar Tahchiev (ptahchiev@github)
  * Reported #2060: `UnwrappingBeanPropertyWriter` incorrectly assumes the found
    serializer is of type `UnwrappingBeanSerializer`
   (2.9.6)

Brandon Krieger (bkrieger@github)
  * Reported #2064: Cannot set custom format for `SqlDateSerializer` globally
   (2.9.7)

Thibaut Robert (trobert@github)
  * Requested #2059: Remove `final` modifier for `TypeFactory`
   (2.10.0)

Christopher Smith (chrylis@github)
  * Suggested #2115: Support naive deserialization of `Serializable` values as "untyped",
    same as `java.lang.Object`		     
   (2.10.0)

Édouard Mercier (edouardmercier@github)
  * Requested #2116: Make NumberSerializers.Base public and its inherited classes not final
   (2.9.6)

Semyon Levin (remal@github)
  * Contributed #2120: `NioPathDeserializer` improvement
