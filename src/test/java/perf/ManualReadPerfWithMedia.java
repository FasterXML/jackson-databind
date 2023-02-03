package perf;

import com.fasterxml.jackson.annotation.JsonFormat;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.introspect.Annotated;
import tools.jackson.databind.introspect.JacksonAnnotationIntrospector;
import tools.jackson.databind.json.JsonMapper;

public class ManualReadPerfWithMedia extends ObjectReaderTestBase
{
    @Override
    protected int targetSizeMegs() { return 8; }

    public static void main(String[] args) throws Exception
    {
        if (args.length != 0) {
            System.err.println("Usage: java ...");
            System.exit(1);
        }
        MediaItem.Content content = new MediaItem.Content();
        content.setTitle("Performance micro-benchmark, to be run manually");
        content.addPerson("William");
        content.addPerson("Robert");
        content.setWidth(900);
        content.setHeight(120);
        content.setBitrate(256000);
        content.setDuration(3600 * 1000L);
        content.setCopyright("none");
        content.setPlayer(MediaItem.Player.FLASH);
        content.setUri("http://whatever.biz");

        MediaItem input = new MediaItem(content);
        input.addPhoto(new MediaItem.Photo("http://a.com", "title1", 200, 100, MediaItem.Size.LARGE));
        input.addPhoto(new MediaItem.Photo("http://b.org", "title2", 640, 480, MediaItem.Size.SMALL));

        ObjectMapper m1 = JsonMapper.builder()
                .annotationIntrospector(new NoFormatIntrospector())
                .build();
        ObjectMapper m2 = new JsonMapper();
        new ManualReadPerfWithRecord().testFromBytes(m1, "JSON-as-Object", input, MediaItem.class,
                m2, "JSON-as-Array", input, MediaItem.class);
    }

    final static class NoFormatIntrospector extends JacksonAnnotationIntrospector
    {
        private static final long serialVersionUID = 1L;
        @Override
        public JsonFormat.Value findFormat(MapperConfig<?> config, Annotated a) { return null; }
    }
}
