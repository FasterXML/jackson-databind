package tools.jackson.databind.cfg;

/**
 * Default implementation of {@link CacheProvider}.
 * 
 * @since 2.16
 */
public class DefaultCacheProvider
    implements CacheProvider
{
    private static final long serialVersionUID = 1L;

    public static DefaultCacheProvider defaultInstance() {
        return new DefaultCacheProvider();
    }
    
    // To implement!

}
