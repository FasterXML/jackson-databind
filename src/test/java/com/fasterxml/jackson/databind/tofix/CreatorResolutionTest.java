package com.fasterxml.jackson.databind.tofix;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.cfg.ConstructorDetector;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.assertj.core.api.Assertions.assertThat;

class CreatorResolutionTest extends DatabindTestUtil {

    //Failing test for jackson-databind#4785
    @Test
    void shouldUseCreator() throws Exception {

        CreatorResolutionTest.HostPort value = newJsonMapper()
                .setConstructorDetector(ConstructorDetector.USE_PROPERTIES_BASED)
                .readValue(
                        a2q("'localhost:9090'"),
                        CreatorResolutionTest.HostPort.class);
        assertThat(value).isNotNull()
                         .satisfies(actual ->
                                    {
                                        assertThat(actual.getHostname()).isEqualTo("localhost");
                                        assertThat(actual.getPort()).isEqualTo(9090);
                                    });
    }

    public static class HostPort {
        private final String hostname;
        private final int port;

        public HostPort(String hostname, int port) {
            this.hostname = hostname;
            this.port = port;
        }

        @Override
        public String toString() {
            return "HostPort{" +
                   "hostname='" + hostname + '\'' +
                   ", port=" + port +
                   '}';
        }

        @JsonCreator
        public static HostPort parse(@Nonnull String hostAndPort) {
            final String[] parts = hostAndPort.split(":");
            return new HostPort(parts[0], Integer.parseInt(parts[1]));
        }

        public String getHostname() {
            return hostname;
        }

        public int getPort() {
            return port;
        }
    }
}
