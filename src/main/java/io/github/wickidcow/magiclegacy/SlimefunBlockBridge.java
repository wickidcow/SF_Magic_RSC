package io.github.wickidcow.magiclegacy;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;

final class SlimefunBlockBridge {

    private final Plugin plugin;
    private final Method getDatabaseManager;
    private final Method getBlockDataController;
    private final Method getAllLoadedChunkData;
    private final Method getAllBlockData;
    private final Method getSfId;
    private final Method getLocation;
    private final Method slimefunItemGetById;
    private final Class<?> energyComponentClass;
    private final Method getChargeLong;
    private final Method removeChargeLong;
    private final Class<? extends Event> chunkDataLoadEventClass;
    private final Method getChunkDataFromEvent;

    @SuppressWarnings("unchecked")
    SlimefunBlockBridge(Plugin plugin) {
        this.plugin = plugin;
        try {
            Class<?> slimefunClass = Class.forName("io.github.thebusybiscuit.slimefun4.implementation.Slimefun");
            Class<?> databaseManagerClass = Class.forName("io.github.thebusybiscuit.slimefun4.core.config.SlimefunDatabaseManager");
            Class<?> controllerClass = Class.forName("com.xzavier0722.mc.plugin.slimefun4.storage.controller.BlockDataController");
            Class<?> chunkDataClass = Class.forName("com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunChunkData");
            Class<?> blockDataClass = Class.forName("com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunBlockData");
            Class<?> slimefunItemClass = Class.forName("io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem");
            energyComponentClass = Class.forName("io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetComponent");
            Class<?> eventClass = Class.forName("com.xzavier0722.mc.plugin.slimefun4.storage.event.SlimefunChunkDataLoadEvent");

            getDatabaseManager = slimefunClass.getMethod("getDatabaseManager");
            getBlockDataController = databaseManagerClass.getMethod("getBlockDataController");
            getAllLoadedChunkData = controllerClass.getMethod("getAllLoadedChunkData");
            getAllBlockData = chunkDataClass.getMethod("getAllBlockData");
            getSfId = blockDataClass.getMethod("getSfId");
            getLocation = blockDataClass.getMethod("getLocation");
            slimefunItemGetById = slimefunItemClass.getMethod("getById", String.class);
            getChargeLong = energyComponentClass.getMethod("getChargeLong", Location.class);
            removeChargeLong = energyComponentClass.getMethod("removeCharge", Location.class, long.class);
            chunkDataLoadEventClass = (Class<? extends Event>) eventClass;
            getChunkDataFromEvent = eventClass.getMethod("getChunkData");
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Slimefun block runtime API is unavailable", ex);
        }
    }

    Set<BlockRef> loadedBlocks() {
        Set<BlockRef> result = new HashSet<>();
        try {
            Object databaseManager = getDatabaseManager.invoke(null);
            Object controller = getBlockDataController.invoke(databaseManager);
            Object chunks = getAllLoadedChunkData.invoke(controller);
            if (chunks instanceof Collection<?> collection) {
                for (Object chunkData : collection) {
                    collectChunk(chunkData, result);
                }
            }
        } catch (ReflectiveOperationException ex) {
            plugin.getLogger().warning("Unable to enumerate loaded Slimefun blocks: " + ex.getMessage());
        }
        return result;
    }

    void registerChunkLoadListener(Listener owner, java.util.function.Consumer<Set<BlockRef>> consumer) {
        EventExecutor executor = (ignored, event) -> {
            try {
                Object chunkData = getChunkDataFromEvent.invoke(event);
                Set<BlockRef> refs = new HashSet<>();
                collectChunk(chunkData, refs);
                consumer.accept(refs);
            } catch (ReflectiveOperationException ex) {
                plugin.getLogger().warning("Unable to process Slimefun chunk-data load: " + ex.getMessage());
            }
        };
        Bukkit.getPluginManager().registerEvent(
            chunkDataLoadEventClass,
            owner,
            EventPriority.MONITOR,
            executor,
            plugin,
            true
        );
    }

    String idAt(Location location) {
        for (BlockRef ref : loadedBlocks()) {
            if (sameBlock(ref.location(), location)) {
                return ref.id();
            }
        }
        return null;
    }

    long getCharge(String id, Location location) {
        Object component = component(id);
        if (component == null) {
            return 0L;
        }
        try {
            Object value = getChargeLong.invoke(component, location);
            return value instanceof Number number ? number.longValue() : 0L;
        } catch (ReflectiveOperationException ex) {
            return 0L;
        }
    }

    boolean removeCharge(String id, Location location, long amount) {
        Object component = component(id);
        if (component == null) {
            return false;
        }
        long before = getCharge(id, location);
        if (before < amount) {
            return false;
        }
        try {
            removeChargeLong.invoke(component, location, amount);
            return getCharge(id, location) <= before - amount;
        } catch (ReflectiveOperationException ex) {
            return false;
        }
    }

    private Object component(String id) {
        try {
            Object item = slimefunItemGetById.invoke(null, id);
            return item != null && energyComponentClass.isInstance(item) ? item : null;
        } catch (ReflectiveOperationException ex) {
            return null;
        }
    }

    private void collectChunk(Object chunkData, Set<BlockRef> output) throws ReflectiveOperationException {
        Object blocks = getAllBlockData.invoke(chunkData);
        if (!(blocks instanceof Collection<?> collection)) {
            return;
        }
        for (Object blockData : collection) {
            Object id = getSfId.invoke(blockData);
            Object location = getLocation.invoke(blockData);
            if (id instanceof String value && location instanceof Location loc) {
                output.add(new BlockRef(value, loc.clone()));
            }
        }
    }

    private static boolean sameBlock(Location a, Location b) {
        return a.getWorld() == b.getWorld()
            && a.getBlockX() == b.getBlockX()
            && a.getBlockY() == b.getBlockY()
            && a.getBlockZ() == b.getBlockZ();
    }

    record BlockRef(String id, Location location) {
    }
}
