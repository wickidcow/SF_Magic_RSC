package io.github.wickidcow.magiclegacy;

import java.lang.reflect.Method;
import org.bukkit.inventory.ItemStack;

final class SlimefunItemIdentity {

    private static final Method GET_BY_ITEM;
    private static final Method GET_BY_ID;
    private static final Method GET_ID;
    private static final Method GET_ITEM;
    private static final String ERROR;

    static {
        Method getByItem = null;
        Method getById = null;
        Method getId = null;
        Method getItem = null;
        String error = null;
        try {
            Class<?> type = Class.forName("io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem");
            getByItem = type.getMethod("getByItem", ItemStack.class);
            getById = type.getMethod("getById", String.class);
            getId = type.getMethod("getId");
            getItem = type.getMethod("getItem");
        } catch (ReflectiveOperationException ex) {
            error = ex.getClass().getSimpleName() + ": " + ex.getMessage();
        }
        GET_BY_ITEM = getByItem;
        GET_BY_ID = getById;
        GET_ID = getId;
        GET_ITEM = getItem;
        ERROR = error;
    }

    private SlimefunItemIdentity() {
    }

    static boolean available() {
        return GET_BY_ITEM != null && GET_BY_ID != null && GET_ID != null && GET_ITEM != null;
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

    static ItemStack copyById(String id) {
        if (!available()) {
            return null;
        }

        try {
            Object slimefunItem = GET_BY_ID.invoke(null, id);
            if (slimefunItem == null) {
                return null;
            }

            Object item = GET_ITEM.invoke(slimefunItem);
            if (item instanceof ItemStack stack) {
                return stack.clone();
            }
        } catch (ReflectiveOperationException ex) {
            return null;
        }

        return null;
    }
}
