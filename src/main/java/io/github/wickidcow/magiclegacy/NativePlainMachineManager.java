package io.github.wickidcow.magiclegacy;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
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

final class NativePlainMachineManager implements Listener {

    private static final String NEARBY_SOUND_ID = "MAGIC_B_SOUND_MACHINE_1";
    private static final String GLOBAL_SOUND_ID = "MAGIC_B_SOUND_MACHINE_2";
    private static final String BLACK_ROLE_ID = "MAGIC_BLACK_ROLE";
    private static final long BLACK_ROLE_DRAIN = 2_147_483_647L;

    private final MagicLegacyPlugin plugin;
    private final Set<Location> nearbySound = new HashSet<>();
    private final Set<Location> globalSound = new HashSet<>();
    private final Set<Location> blackRole = new HashSet<>();
    private final List<Sound> sounds = new ArrayList<>();

    private BukkitTask task;

    NativePlainMachineManager(MagicLegacyPlugin plugin) {
        this.plugin = plugin;
    }

    void start() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        registerChunkDataLoadListener();

        for (Sound sound : Registry.SOUND_EVENT) {
            sounds.add(sound);
        }

        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 10L, 10L);
    }

    void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        nearbySound.clear();
        globalSound.clear();
        blackRole.clear();
        sounds.clear();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Location location = blockLocation(event.getBlockPlaced().getLocation());
        plugin.getServer().getScheduler().runTask(plugin, () -> discover(location));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        remove(blockLocation(event.getBlock().getLocation()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkUnload(ChunkUnloadEvent event) {
        Chunk chunk = event.getChunk();
        removeChunk(nearbySound, chunk);
        removeChunk(globalSound, chunk);
        removeChunk(blackRole, chunk);
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
                "Could not register native plain-machine chunk discovery: "
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
                if (idValue instanceof String id && locationValue instanceof Location location) {
                    track(blockLocation(location), id);
                }
            }
        } catch (ReflectiveOperationException ex) {
            plugin.getLogger().warning(
                "Could not read Slimefun chunk data for native plain machines: "
                    + ex.getClass().getSimpleName() + ": " + ex.getMessage()
            );
        }
    }

    private void discover(Location location) {
        String id = SlimefunBlockStorageBridge.idAt(location);
        if (id == null) {
            remove(location);
            return;
        }
        track(location, id);
    }

    private void track(Location location, String id) {
        remove(location);
        switch (id) {
            case NEARBY_SOUND_ID -> nearbySound.add(location);
            case GLOBAL_SOUND_ID -> globalSound.add(location);
            case BLACK_ROLE_ID -> blackRole.add(location);
            default -> {
            }
        }
    }

    private void tick() {
        tickNearbySound();
        tickGlobalSound();
        tickBlackRole();
    }

    private void tickNearbySound() {
        if (sounds.isEmpty()) {
            return;
        }

        Iterator<Location> it = nearbySound.iterator();
        while (it.hasNext()) {
            Location location = it.next();
            if (!valid(location, NEARBY_SOUND_ID)) {
                it.remove();
                continue;
            }

            World world = location.getWorld();
            if (world == null) {
                continue;
            }

            for (Player player : world.getPlayers()) {
                if (player.getLocation().distanceSquared(location) > 48.0 * 48.0) {
                    continue;
                }
                Sound sound = sounds.get(ThreadLocalRandom.current().nextInt(sounds.size()));
                player.playSound(location, sound, 1.0F, 1.0F);
            }
        }
    }

    private void tickGlobalSound() {
        if (sounds.isEmpty() || globalSound.isEmpty()) {
            return;
        }

        Iterator<Location> it = globalSound.iterator();
        boolean hasValidMachine = false;
        while (it.hasNext()) {
            Location location = it.next();
            if (!valid(location, GLOBAL_SOUND_ID)) {
                it.remove();
            } else {
                hasValidMachine = true;
            }
        }

        if (!hasValidMachine) {
            return;
        }

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            Sound sound = sounds.get(ThreadLocalRandom.current().nextInt(sounds.size()));
            player.playSound(player.getLocation(), sound, 1.0F, 1.0F);
        }
    }

    private void tickBlackRole() {
        Iterator<Location> it = blackRole.iterator();
        while (it.hasNext()) {
            Location location = it.next();
            if (!valid(location, BLACK_ROLE_ID)) {
                it.remove();
                continue;
            }

            long charge = SlimefunBlockStorageBridge.chargeAt(location);
            if (charge <= 0L) {
                continue;
            }

            long remaining = Math.max(0L, charge - BLACK_ROLE_DRAIN);
            SlimefunBlockStorageBridge.setCharge(location, remaining);
        }
    }

    private boolean valid(Location location, String expectedId) {
        if (!location.isWorldLoaded() || !location.getChunk().isLoaded()) {
            return false;
        }
        return expectedId.equals(SlimefunBlockStorageBridge.idAt(location));
    }

    private void remove(Location location) {
        nearbySound.remove(location);
        globalSound.remove(location);
        blackRole.remove(location);
    }

    private static void removeChunk(Set<Location> locations, Chunk chunk) {
        locations.removeIf(location ->
            location.getWorld() == chunk.getWorld()
                && (location.getBlockX() >> 4) == chunk.getX()
                && (location.getBlockZ() >> 4) == chunk.getZ()
        );
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
