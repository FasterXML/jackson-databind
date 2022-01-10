package com.fasterxml.jackson.databind.jsontype;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import com.fasterxml.jackson.databind.*;

/**
 * Originally test for [databind#1964], wherein slightly incompatible type hierarchy,
 * where `Map` key is downcast from `String` to `Object` (via use of "raw"
 * types to force compiler to ignore incompatibility) causes exception
 * during serialization. Although ideally code would not force round peg
 * through square hole, it makes sense to add specific exception to allow
 * such downcast just for Map key types (for now at least).
 */
@SuppressWarnings("serial")
public class SubTypeResolutionTest extends BaseMapTest
{
    // [databind#1964]
    static class AccessModel {
        private Map<String, Collection<String>> repositoryPrivileges;

        public AccessModel() {
            repositoryPrivileges = new HashMap<>();
        }

        // 19-Apr-2018, tatu; this would prevent issues
//        @JsonSerialize(typing = JsonSerialize.Typing.STATIC)
        public Map<String, Collection<String>> getRepositoryPrivileges() {
            return repositoryPrivileges;
        }

        public void setRepositoryPrivileges(Map<String, Collection<String>> repositoryPrivileges) {
            this.repositoryPrivileges = repositoryPrivileges;
        }
    }
    static class CustomMap<T> extends LinkedHashMap<Object, T> { }

    // [databind#2034]: specialization from `Object` to other types probably should
    // just be allowed (at least in serialization case)
    interface Dummy {
      List<String> getStrings();
    }

    static class MetaModel<M, B> extends AbstractMetaValue<M, M, B> {

        @JsonProperty
        protected final Map<String, AbstractMetaValue<M, ?, B>> attributes = new HashMap<>();

        public <V> ListMetaAttribute<M, V, B> describeList(final String attributeName) {
          final ListMetaAttribute<M, V, B> metaAttribute = new ListMetaAttribute<>();
          attributes.put(attributeName, metaAttribute);
          return metaAttribute;
        }
      }

    static abstract class AbstractMetaValue<M, V, B> {
        public int getBogus() { return 3; }
    }

    static class ListMetaAttribute<M, V, B> extends MetaAttribute<M, List<V>, B> {
        public ListMetaAttribute() { }
    }

    static class MetaAttribute<M, V, B> extends AbstractMetaValue<M, V, B> {
        public MetaAttribute() { }
      }

    // [databind#2632]: fail to specialize type-erased
    @SuppressWarnings("rawtypes")
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
    @JsonSubTypes(value = {
                    @JsonSubTypes.Type(value = Either.Left.class, name = "left"),
                    @JsonSubTypes.Type(value = Either.Right.class, name = "right")
    })
    static class Either<L, R> {
        static class Left<T> extends Either {   }
        static class Right<T> extends Either {  }
    }

    static class Foo {
        @SuppressWarnings("unchecked")
        public Either<String, String> getEither() {
            return new Either.Right<String>();
        }
    }

    /*
    /**********************************************************************
    /* Unit tests
    /**********************************************************************
     */

    final ObjectMapper MAPPER = newJsonMapper();

    // [databind#1964]
    public void testTypeCompatibility1964() throws Exception
    {
        // Important! Must use raw type since assignment requires effectively
        // casting due incompatible type parameters.
        @SuppressWarnings({ "unchecked", "rawtypes" })
        Map<String, Collection<String>> repoPrivilegesMap = new CustomMap();
        String key = "/storages/storage0/releases";
        Collection<String> values = new HashSet<>();
        values.add("ARTIFACTS_RESOLVE");
        repoPrivilegesMap.put(key, values);

        AccessModel accessModel = new AccessModel();
        accessModel.setRepositoryPrivileges(repoPrivilegesMap);

        String jsonStr = MAPPER.writeValueAsString(accessModel);
        // ... could/should verify more, perhaps, but for now let it be.
        assertNotNull(jsonStr);
    }

    // [databind#2034]
    public void testTypeSpecialization2034() throws Exception
    {
        MetaModel<Dummy, Dummy> metaModel = new MetaModel<>();
        metaModel.describeList("a1");
        String jsonStr = MAPPER.writeValueAsString(metaModel);
        // ... could/should verify more, perhaps, but for now let it be.
        assertNotNull(jsonStr);
    }

    // [databind#2632]: fail to specialize type-erased
    public void testSpecializeIncompatibleRawType() throws Exception
    {
        String json = MAPPER.writeValueAsString(new Foo());
        assertNotNull(json);
    }
}
