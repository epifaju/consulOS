package com.consulos.portal;

public final class PortalSessionHolder {

    private static final ThreadLocal<PortalSession> CURRENT = new ThreadLocal<>();

    private PortalSessionHolder() {
    }

    public static void set(PortalSession session) {
        CURRENT.set(session);
    }

    public static PortalSession get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
