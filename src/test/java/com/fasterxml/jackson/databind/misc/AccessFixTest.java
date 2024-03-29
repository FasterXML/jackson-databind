package com.fasterxml.jackson.databind.misc;

import java.io.IOException;
import java.security.Permission;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertNotNull;

// Test(s) to verify that forced access works as expected
public class AccessFixTest extends DatabindTestUtil
{
    static class CauseBlockingSecurityManager
        extends SecurityManager
    {
        @Override
        public void checkPermission(Permission perm) throws SecurityException {
            if ("suppressAccessChecks".equals(perm.getName())) {
                throw new SecurityException("Cannot force permission: "+perm);
            }
        }
    }

    // [databind#877]: avoid forcing access to `cause` field of `Throwable`
    // as it is never actually used (always call `initCause()` instead)
    @Test
    public void testCauseOfThrowableIgnoral() throws Exception
    {
        final SecurityManager origSecMan = System.getSecurityManager();
        ObjectMapper mapper = jsonMapperBuilder()
                .disable(MapperFeature.OVERRIDE_PUBLIC_ACCESS_MODIFIERS)
                .build();
        // 17-Oct-2023, tatu: JDK 21 has hard fail, try to work around:
        boolean setSucceeded = false;
        try {
            System.setSecurityManager(new CauseBlockingSecurityManager());
            setSucceeded = true;
            _testCauseOfThrowableIgnoral(mapper);
        } catch (UnsupportedOperationException e) {
            // JDK 21+ fail?
            verifyException(e, "Security Manager is deprecated");
        } finally {
            if (setSucceeded) {
                System.setSecurityManager(origSecMan);
            }
        }
    }

    private void _testCauseOfThrowableIgnoral(ObjectMapper mapper) throws Exception
    {
        IOException e = mapper.readValue("{}", IOException.class);
        assertNotNull(e);
    }
}
