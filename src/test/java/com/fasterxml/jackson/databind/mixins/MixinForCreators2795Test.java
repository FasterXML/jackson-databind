package com.fasterxml.jackson.databind.mixins;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

// [databind#2795]: Regression in 2.11.0, no mix-ins for JDK collections
public class MixinForCreators2795Test extends DatabindTestUtil
{
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    static class UnmodifiableCollectionMixin {
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public UnmodifiableCollectionMixin(final Collection<?> collection) { }
    }

    @Test
    public void testMixinWithUnmmodifiableCollection() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .addMixIn(Collections.unmodifiableCollection(Collections.emptyList()).getClass(),
                        UnmodifiableCollectionMixin.class)
                .build();
        mapper.activateDefaultTypingAsProperty(mapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL, "@class");

        final List<String> strings = Arrays.asList("1", "2");
        final Collection<String> unmodifiableCollection = Collections.unmodifiableCollection(strings);
        final byte[] bytes = mapper.writeValueAsBytes(unmodifiableCollection);

        final Collection<?> result = mapper.readValue(bytes, Collection.class);

        assertEquals(2, result.size());
    }
}
