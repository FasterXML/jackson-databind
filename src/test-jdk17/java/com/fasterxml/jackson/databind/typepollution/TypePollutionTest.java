package com.fasterxml.jackson.databind.typepollution;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import io.micronaut.test.typepollution.FocusListener;
import io.micronaut.test.typepollution.ThresholdFocusListener;
import io.micronaut.test.typepollution.TypePollutionTransformer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.function.Executable;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

public class TypePollutionTest {
    private static final int THRESHOLD = 1000;
    private ThresholdFocusListener focusListener;

    @BeforeAll
    static void setupAgent() {
        TypePollutionTransformer.install(net.bytebuddy.agent.ByteBuddyAgent.install());
    }

    @BeforeEach
    void setUp() {
        focusListener = new ThresholdFocusListener();
        FocusListener.setFocusListener(focusListener);
    }

    @AfterEach
    void verifyNoTypeThrashing() {
        FocusListener.setFocusListener(null);
        Assertions.assertTrue(focusListener.checkThresholds(THRESHOLD), "Threshold exceeded, check logs.");
    }

    private void doTest(Executable r) throws Throwable {
        for (int i = 0; i < THRESHOLD * 2; i++) {
            r.execute();
        }
    }

    @Test
    @Disabled
    public void sample() throws Throwable {
        // example test. If you enable this, it will fail, and you can see what a type pollution error looks like.

        interface A {
        }

        interface B {
        }

        class Concrete implements A, B {
        }

        Object c = new Concrete();
        AtomicInteger j = new AtomicInteger();
        doTest(() -> {
            if (c instanceof A) {
                j.incrementAndGet();
            }
            if (c instanceof B) {
                j.incrementAndGet();
            }
        });
        System.out.println(j);
    }

    @Test
    public void deserializeRecordWithList() throws Throwable {
        ObjectMapper mapper = JsonMapper.builder().build();
        ListRecord input = new ListRecord(List.of("foo"));
        String json = mapper.writeValueAsString(input);
        doTest(() -> Assertions.assertEquals(input, mapper.readValue(json, ListRecord.class)));
    }

    record ListRecord(List<String> list) {
    }

    @Test
    public void deserializeRecordWithSet() throws Throwable {
        ObjectMapper mapper = JsonMapper.builder().build();
        SetRecord input = new SetRecord(Set.of("foo"));
        String json = mapper.writeValueAsString(input);
        doTest(() -> Assertions.assertEquals(input, mapper.readValue(json, SetRecord.class)));
    }

    record SetRecord(Set<String> list) {
    }
}
