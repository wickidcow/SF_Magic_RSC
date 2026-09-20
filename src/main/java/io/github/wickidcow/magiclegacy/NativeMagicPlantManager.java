package io.github.wickidcow.magiclegacy;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitTask;

final class NativeMagicPlantManager implements Listener {

    private static final String MACHINE_ID = "MAGIC_PLANT_1";
    private static final String REWARD_ID = "INFINITE_INGOT";
    private static final int WARMUP_TICKS = 2;
    private static final long GROWTH_BASE_MILLIS = 5_000L;
    private static final double REWARD_CHANCE = 0.10;

    private final MagicLegacyPlugin plugin;
    private final Map<Location, PlantState> plants = new HashMap<>();
    private final Map<Location, Boolean> pendingBreaks = new HashMap<>();
    private BukkitTask task;

    NativeMagicPlantManager(MagicLegacyPlugin plugin) {
        this.plugin = plugin;
    }

    void start() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        registerChunkDataLoadListener();
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 10L, 10L);
    }

    void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        plants.clear();
        pendingBreaks.clear();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Location location = blockLocation(event.getBlockPlaced().getLocation());
        plugin.getServer().getScheduler().runTask(plugin, () -> discover(location));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBreakCapture(BlockBreakEvent event) {
        Location location = blockLocation(event.getBlock().getLocation());
        if (!MACHINE_ID.equals(SlimefunBlockStorageBridge.idAt(location))) {
            return;
        }
        PlantState state = plants.get(location);
        pendingBreaks.put(location, state != null && state.mature());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBreakFinalize(BlockBreakEvent event) {
        Location location = blockLocation(event.getBlock().getLocation());
        Boolean mature = pendingBreaks.remove(location);
        if (mature == null) {
            return;
        }

        plants.remove(location);
        if (event.isCancelled() || !mature) {
            return;
        }

        World world = location.getWorld();
        if (world == null || ThreadLocalRandom.current().nextDouble() >= REWARD_CHANCE) {
            return;
        }

        ItemStack reward = SlimefunItemIdentity.copyById(REWARD_ID);
        if (reward != null) {
            plugin.getServer().getScheduler().runTask(
                plugin,
                () -> world.dropItemNaturally(location.clone().add(0.5, 0.5, 0.5), reward)
            );
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkUnload(ChunkUnloadEvent event) {
        Chunk chunk = event.getChunk();
        removeChunk(plants, chunk);
        pendingBreaks.keySet().removeIf(location -> sameChunk(location, chunk));
    }

    private void registerChunkDataLoadListener() {
        try {
            Class<?> raw = Class.forName(
                "com.xzavier0722.mc.plugin.slimefun4.storage.event.SlimefunChunkDataLoadEvent"
            );
            if (!Event.class.isAssignableFrom(raw)) {
                throw new IllegalStateException("SlimefunChunkDataLoadEvent is not a Bukkit Event");
            }

            @SuppressWarnings("unchecked")
            Class<? extends Event> eventClass = (Class<? extends Event>) raw;
            PluginManager manager = plugin.getServer().getPluginManager();
            manager.registerEvent(
                eventClass,
                this,
                EventPriority.MONITOR,
                (listener, event) -> onChunkDataLoad(event),
                plugin,
                true
            );
        } catch (ReflectiveOperationException | RuntimeException ex) {
            plugin.getLogger().warning(
                "Could not register Magic Plant chunk discovery: "
                    + ex.getClass().getSimpleName() + ": " + ex.getMessage()
            );
        }
    }

    private void onChunkDataLoad(Event event) {
        try {
            Object chunkData = event.getClass().getMethod("getChunkData").invoke(event);
            Method getAll = chunkData.getClass().getMethod("getAllBlockData");
            Object value = getAll.invoke(chunkData);
            if (!(value instanceof Iterable<?> blocks)) {
                return;
            }

            for (Object blockData : blocks) {
                Method getId = blockData.getClass().getMethod("getSfId");
                Method getLocation = blockData.getClass().getMethod("getLocation");
                Object idValue = getId.invoke(blockData);
                Object locationValue = getLocation.invoke(blockData);
                if (MACHINE_ID.equals(idValue) && locationValue instanceof Location location) {
                    plants.put(blockLocation(location), PlantState.initial());
                }
            }
        } catch (ReflectiveOperationException ex) {
            plugin.getLogger().warning(
                "Could not read Slimefun chunk data for Magic Plant: "
                    + ex.getClass().getSimpleName() + ": " + ex.getMessage()
            );
        }
    }

    private void discover(Location location) {
        if (MACHINE_ID.equals(SlimefunBlockStorageBridge.idAt(location))) {
            plants.put(location, PlantState.initial());
        } else {
            plants.remove(location);
        }
    }

    private void tick() {
        if (plants.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        Iterator<Map.Entry<Location, PlantState>> it = plants.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Location, PlantState> entry = it.next();
            Location location = entry.getKey();
            PlantState state = entry.getValue();

            if (!valid(location)) {
                it.remove();
                continue;
            }

            if (state.warmupTicks() < WARMUP_TICKS) {
                entry.setValue(new PlantState(state.warmupTicks() + 1, state.startedMillis(), false));
                continue;
            }

            if (state.startedMillis() == 0L) {
                entry.setValue(new PlantState(state.warmupTicks(), now, false));
                continue;
            }

            long elapsed = now - state.startedMillis();
            if (elapsed < GROWTH_BASE_MILLIS / 10L) {
                setDeadBush(location);
                continue;
            }

            int age = growthAge(elapsed);
            if (age > 0) {
                setWheatAge(location, age);
                if (age >= 7 && !state.mature()) {
                    entry.setValue(new PlantState(state.warmupTicks(), state.startedMillis(), true));
                }
            }
        }
    }

    private static int growthAge(long elapsed) {
        if (elapsed < GROWTH_BASE_MILLIS / 6L) {
            return 1;
        }
        if (elapsed < GROWTH_BASE_MILLIS / 3L) {
            return 2;
        }
        if (elapsed < GROWTH_BASE_MILLIS / 2L) {
            return 3;
        }
        if (elapsed < (GROWTH_BASE_MILLIS * 2L) / 3L) {
            return 4;
        }
        if (elapsed < (GROWTH_BASE_MILLIS * 5L) / 6L) {
            return 5;
        }
        if (elapsed < GROWTH_BASE_MILLIS) {
            return 6;
        }
        return 7;
    }

    private static void setDeadBush(Location location) {
        Block block = location.getBlock();
        if (block.getType() != Material.DEAD_BUSH) {
            block.setType(Material.DEAD_BUSH, false);
        }
    }

    private static void setWheatAge(Location location, int requestedAge) {
        Block block = location.getBlock();
        if (block.getType() != Material.WHEAT) {
            block.setType(Material.WHEAT, false);
        }

        if (block.getBlockData() instanceof Ageable ageable) {
            int age = Math.max(0, Math.min(requestedAge, ageable.getMaximumAge()));
            if (ageable.getAge() != age) {
                ageable.setAge(age);
                block.setBlockData(ageable, false);
            }
        }
    }

    private static boolean valid(Location location) {
        return location.isWorldLoaded()
            && location.getChunk().isLoaded()
            && MACHINE_ID.equals(SlimefunBlockStorageBridge.idAt(location));
    }

    private static void removeChunk(Map<Location, PlantState> map, Chunk chunk) {
        map.keySet().removeIf(location -> sameChunk(location, chunk));
    }

    private static boolean sameChunk(Location location, Chunk chunk) {
        return location.getWorld() == chunk.getWorld()
            && (location.getBlockX() >> 4) == chunk.getX()
            && (location.getBlockZ() >> 4) == chunk.getZ();
    }

    private static Location blockLocation(Location location) {
        return new Location(
            location.getWorld(),
            location.getBlockX(),
            location.getBlockY(),
            location.getBlockZ()
        );
    }

    private record PlantState(int warmupTicks, long startedMillis, boolean mature) {
        static PlantState initial() {
            return new PlantState(0, 0L, false);
        }
    }
}
