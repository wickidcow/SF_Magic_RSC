package io.github.wickidcow.magiclegacy;

import java.lang.reflect.Method;
import org.bukkit.inventory.ItemStack;

final class SlimefunItemIdentity {

    private static final Method GET_BY_ITEM;
    private static final Method GET_ID;
    private static final String ERROR;

    static {
        Method getByItem = null;
        Method getId = null;
        String error = null;
        try {
            Class<?> type = Class.forName("io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem");
            getByItem = type.getMethod("getByItem", ItemStack.class);
            getId = type.getMethod("getId");
        } catch (ReflectiveOperationException ex) {
            error = ex.getClass().getSimpleName() + ": " + ex.getMessage();
        }
        GET_BY_ITEM = getByItem;
        GET_ID = getId;
        ERROR = error;
    }

    private SlimefunItemIdentity() {
    }

    static boolean available() {
        return GET_BY_ITEM != null && GET_ID != null;
    }

    static String error() {
        return ERROR == null ? "unknown" : ERROR;
    }

    static boolean is(ItemStack stack, String expectedId) {
        if (!available() || stack == null) {
            return false;
        }

        try {
            Object slimefunItem = GET_BY_ITEM.invoke(null, stack);
            if (slimefunItem == null) {
                return false;
            }

            Object id = GET_ID.invoke(slimefunItem);
            return expectedId.equals(id);
        } catch (ReflectiveOperationException ex) {
            return false;
        }
    }
}
