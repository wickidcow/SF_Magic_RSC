package io.github.wickidcow.magiclegacy;

import java.lang.reflect.Method;
import org.bukkit.Location;

final class SlimefunBlockStorageBridge {

    private static final Method CHECK_ID;
    private static final Method GET_INFO;
    private static final Method ADD_INFO;
    private static final String ERROR;

    static {
        Method checkId = null;
        Method getInfo = null;
        Method addInfo = null;
        String error = null;
        try {
            Class<?> storage = Class.forName("me.mrCookieSlime.Slimefun.api.BlockStorage");
            checkId = storage.getMethod("checkID", Location.class);
            getInfo = storage.getMethod("getLocationInfo", Location.class, String.class);
            addInfo = storage.getMethod("addBlockInfo", Location.class, String.class, String.class);
        } catch (ReflectiveOperationException ex) {
            error = ex.getClass().getSimpleName() + ": " + ex.getMessage();
        }
        CHECK_ID = checkId;
        GET_INFO = getInfo;
        ADD_INFO = addInfo;
        ERROR = error;
    }

    private SlimefunBlockStorageBridge() {
    }

    static boolean available() {
        return CHECK_ID != null && GET_INFO != null && ADD_INFO != null;
    }

    static String error() {
        return ERROR == null ? "unknown" : ERROR;
    }

    static String idAt(Location location) {
        if (!available() || location == null) {
            return null;
        }
        try {
            Object value = CHECK_ID.invoke(null, blockLocation(location));
            return value instanceof String id ? id : null;
        } catch (ReflectiveOperationException ex) {
            return null;
        }
    }

    static long chargeAt(Location location) {
        if (!available() || location == null) {
            return 0L;
        }
        try {
            Object value = GET_INFO.invoke(null, blockLocation(location), "energy-charge");
            if (!(value instanceof String raw) || raw.isBlank()) {
                return 0L;
            }
            return Math.max(0L, Long.parseLong(raw));
        } catch (ReflectiveOperationException | NumberFormatException ex) {
            return 0L;
        }
    }

    static boolean setCharge(Location location, long charge) {
        if (!available() || location == null || charge < 0L) {
            return false;
        }
        try {
            ADD_INFO.invoke(null, blockLocation(location), "energy-charge", Long.toString(charge));
            return true;
        } catch (ReflectiveOperationException ex) {
            return false;
        }
    }

    private static Location blockLocation(Location location) {
        return new Location(
            location.getWorld(),
            location.getBlockX(),
            location.getBlockY(),
            location.getBlockZ()
        );
    }
}
