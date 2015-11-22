package com.fasterxml.jackson.databind;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.BDDAssertions.then;

public class DefaultModuleLoaderTest {

    @Before
    public void setUp() throws Exception {
        Assume.assumeTrue(JavaVersion.EIGHT.isAvailable());
    }

    @Test
    public void shouldLoadDefaultModules() throws Exception {

        // given
        DefaultModuleLoader givenDefaultModuleLoader = new DefaultModuleLoader();

        // when
        List<Module> actual = givenDefaultModuleLoader.getAvailableDefaultModules();

        then(actual)
                .extracting("moduleName")
                .containsExactly("jackson-core", "jackson-datatype-jsr310", "Jdk8Module");
    }
}