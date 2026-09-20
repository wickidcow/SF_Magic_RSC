package io.github.wickidcow.magiclegacy;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitTask;

final class NativePlayerAttackManager implements Listener {

    private static final String MACHINE_ID = "MAGIC_PLAYER_ATTACT";
    private static final int WARMUP_TICKS = 15;
    private static final long BASE_DRAIN = 64L;
    private static final long ACTIVATION_COST = 4_096L;
    private static final long ACTIVATION_INTERVAL_MILLIS = 7_500L;
    private static final double PLAYER_RADIUS = 50.0;
    private static final float EXPLOSION_POWER = 4.0F;

    private final MagicLegacyPlugin plugin;
    private final Map<Location, MachineState> machines = new HashMap<>();
    private BukkitTask task;

    NativePlayerAttackManager(MagicLegacyPlugin plugin) {
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
        machines.clear();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Location location = blockLocation(event.getBlockPlaced().getLocation());
        plugin.getServer().getScheduler().runTask(plugin, () -> discover(location));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        machines.remove(blockLocation(event.getBlock().getLocation()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkUnload(ChunkUnloadEvent event) {
        Chunk chunk = event.getChunk();
        Iterator<Location> it = machines.keySet().iterator();
        while (it.hasNext()) {
            Location location = it.next();
            if (location.getWorld() == chunk.getWorld()
                && (location.getBlockX() >> 4) == chunk.getX()
                && (location.getBlockZ() >> 4) == chunk.getZ()) {
                it.remove();
            }
        }
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
                "Could not register Magic Player Attack chunk discovery: "
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
                    machines.put(blockLocation(location), MachineState.initial());
                }
            }
        } catch (ReflectiveOperationException ex) {
            plugin.getLogger().warning(
                "Could not read Slimefun chunk data for Magic Player Attack: "
                    + ex.getClass().getSimpleName() + ": " + ex.getMessage()
            );
        }
    }

    private void discover(Location location) {
        if (MACHINE_ID.equals(SlimefunBlockStorageBridge.idAt(location))) {
            machines.put(location, MachineState.initial());
        } else {
            machines.remove(location);
        }
    }

    private void tick() {
        if (machines.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        Iterator<Map.Entry<Location, MachineState>> it = machines.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Location, MachineState> entry = it.next();
            Location location = entry.getKey();
            MachineState state = entry.getValue();

            if (!valid(location)) {
                it.remove();
                continue;
            }

            if (state.warmupTicks() < WARMUP_TICKS) {
                entry.setValue(new MachineState(state.warmupTicks() + 1, state.lastActivationMillis()));
                continue;
            }

            long charge = SlimefunBlockStorageBridge.chargeAt(location);
            if (charge < BASE_DRAIN) {
                continue;
            }

            long afterBaseDrain = charge - BASE_DRAIN;
            if (!SlimefunBlockStorageBridge.setCharge(location, afterBaseDrain)) {
                continue;
            }

            if (now - state.lastActivationMillis() <= ACTIVATION_INTERVAL_MILLIS) {
                continue;
            }

            if (afterBaseDrain < ACTIVATION_COST) {
                entry.setValue(new MachineState(state.warmupTicks(), now));
                continue;
            }

            Player source = randomNearbyPlayer(location);
            if (source == null) {
                entry.setValue(new MachineState(state.warmupTicks(), now));
                continue;
            }

            long afterActivation = afterBaseDrain - ACTIVATION_COST;
            if (!SlimefunBlockStorageBridge.setCharge(location, afterActivation)) {
                continue;
            }

            World world = location.getWorld();
            if (world == null) {
                SlimefunBlockStorageBridge.setCharge(location, afterBaseDrain);
                continue;
            }

            try {
                world.createExplosion(location, EXPLOSION_POWER, false, false, source);
                entry.setValue(new MachineState(state.warmupTicks(), now));
            } catch (RuntimeException ex) {
                SlimefunBlockStorageBridge.setCharge(location, afterBaseDrain);
                plugin.getLogger().fine(
                    "Could not activate Magic Player Attack at "
                        + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ()
                        + ": " + ex.getMessage()
                );
            }
        }
    }

    private Player randomNearbyPlayer(Location center) {
        World world = center.getWorld();
        if (world == null) {
            return null;
        }

        double radiusSquared = PLAYER_RADIUS * PLAYER_RADIUS;
        List<Player> nearby = new ArrayList<>();
        for (Player player : world.getPlayers()) {
            if (!player.isOnline() || player.isDead()) {
                continue;
            }
            if (player.getLocation().distanceSquared(center) <= radiusSquared) {
                nearby.add(player);
            }
        }

        if (nearby.isEmpty()) {
            return null;
        }
        return nearby.get(ThreadLocalRandom.current().nextInt(nearby.size()));
    }

    private static boolean valid(Location location) {
        return location.isWorldLoaded()
            && location.getChunk().isLoaded()
            && MACHINE_ID.equals(SlimefunBlockStorageBridge.idAt(location));
    }

    private static Location blockLocation(Location location) {
        return new Location(
            location.getWorld(),
            location.getBlockX(),
            location.getBlockY(),
            location.getBlockZ()
        );
    }

    private record MachineState(int warmupTicks, long lastActivationMillis) {
        static MachineState initial() {
            return new MachineState(0, 0L);
        }
    }
}
