package tools.jackson.databind.introspect;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonView;

import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectMapperTestAccess;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

// [databind#6227] `findDefaultViews()` set its "resolved" flag BEFORE computing the
// value, so a second thread could observe the flag and return `null` views.
public class BeanDescriptionDefaultViewsRaceTest extends DatabindTestUtil
{
    static class Views { static class Pub { } }

    @JsonView(Views.Pub.class)
    static class ViewedBean {
        public String a = "a";
        public String b = "b";
    }

    /**
     * Introspector that blocks inside the very first {@code findViews()} call, holding
     * open the window between the flag write and the value write.
     */
    static class BlockingIntrospector extends JacksonAnnotationIntrospector
    {
        private static final long serialVersionUID = 1L;

        final CountDownLatch inside = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        final Semaphore firstCall = new Semaphore(1);

        @Override
        public Class<?>[] findViews(MapperConfig<?> config, Annotated a) {
            if (firstCall.tryAcquire()) { // only the first caller blocks
                inside.countDown();
                try {
                    release.await(10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            return super.findViews(config, a);
        }
    }

    @Test
    public void testConcurrentFindDefaultViews() throws Exception
    {
        final BlockingIntrospector intr = new BlockingIntrospector();
        ObjectMapper mapper = jsonMapperBuilder()
                .annotationIntrospector(intr)
                .build();
        final BeanDescription desc = ObjectMapperTestAccess.beanDescriptionForSer(mapper, ViewedBean.class);

        final AtomicReference<Class<?>[]> fromA = new AtomicReference<>();
        final AtomicReference<Class<?>[]> fromB = new AtomicReference<>();

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<?> a = pool.submit(() -> fromA.set(desc.findDefaultViews()));
            // wait until thread A is INSIDE findViews(), i.e. mid-resolution
            assertTrue(intr.inside.await(10, TimeUnit.SECONDS), "thread A never reached findViews()");
            Future<?> b = pool.submit(() -> fromB.set(desc.findDefaultViews()));
            // give B a chance to read the half-resolved state, then let A finish
            Thread.sleep(100L);
            intr.release.countDown();
            a.get(10, TimeUnit.SECONDS);
            b.get(10, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }

        assertNotNull(fromA.get(), "views resolved by first thread should not be null");
        assertNotNull(fromB.get(),
                "second thread saw the 'resolved' flag before the value was assigned, and got null views");
        assertArrayEquals(fromA.get(), fromB.get());
        assertEquals(Views.Pub.class, fromB.get()[0]);
    }
}
