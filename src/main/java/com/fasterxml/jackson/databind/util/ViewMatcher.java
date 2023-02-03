package com.fasterxml.jackson.databind.util;

/**
 * Helper class used for checking whether a property is visible
 * in the active view
 */
public class ViewMatcher implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    protected final static ViewMatcher EMPTY = new ViewMatcher();

    public boolean isVisibleForView(Class<?> activeView) { return false; }

    public static ViewMatcher construct(Class<?>[] views)
    {
        if (views == null) {
            return EMPTY;
        }
        switch (views.length) {
        case 0:
            return EMPTY;
        case 1:
            return new Single(views[0]);
        }
        return new Multi(views);
    }

    /*
    /**********************************************************
    /* Concrete sub-classes
    /**********************************************************
     */

    private final static class Single extends ViewMatcher
    {
        private static final long serialVersionUID = 1L;

        private final Class<?> _view;
        public Single(Class<?> v) { _view = v; }
        @Override
        public boolean isVisibleForView(Class<?> activeView) {
            return (activeView == _view) || _view.isAssignableFrom(activeView);
        }
    }

    private final static class Multi extends ViewMatcher
        implements java.io.Serializable
    {
        private static final long serialVersionUID = 1L;

        private final Class<?>[] _views;

        public Multi(Class<?>[] v) { _views = v; }

        @Override
        public boolean isVisibleForView(Class<?> activeView)
        {
            for (int i = 0, len = _views.length; i < len; ++i) {
                Class<?> view = _views[i];
                if ((activeView == view) || view.isAssignableFrom(activeView)) {
                    return true;
                }
            }
            return false;
        }
    }
}
