package io.github.wickidcow.magiclegacy;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

final class NativeSpawnerManager implements Listener {

    private static final Pattern SPAWNER_ID = Pattern.compile("^MAGIC_SPAWNER_(.+)_([123])$");
    private static final long ENERGY_COST = 520L;
    private static final int RANGE = 5;

    private final JavaPlugin plugin;
    private final Map<Location, SpawnerState> spawners = new HashMap<>();
    private BukkitTask task;

    NativeSpawnerManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    void start() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!SlimefunBlockStorageBridge.available()) {
                plugin.getLogger().warning(
                    "Native Magic spawners are unavailable because Slimefun block storage could not be linked: "
                        + SlimefunBlockStorageBridge.error()
                );
                return;
            }

            for (World world : plugin.getServer().getWorlds()) {
                for (Chunk chunk : world.getLoadedChunks()) {
                    scanChunk(chunk);
                }
            }
        });

        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        spawners.clear();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (event.getBlockPlaced().getType() != Material.SPAWNER) {
            return;
        }
        Location location = blockLocation(event.getBlockPlaced().getLocation());
        plugin.getServer().getScheduler().runTask(plugin, () -> discover(location));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        spawners.remove(blockLocation(event.getBlock().getLocation()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        plugin.getServer().getScheduler().runTask(plugin, () -> scanChunk(event.getChunk()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkUnload(ChunkUnloadEvent event) {
        Chunk chunk = event.getChunk();
        Iterator<Location> it = spawners.keySet().iterator();
        while (it.hasNext()) {
            Location location = it.next();
            if (location.getWorld() == chunk.getWorld()
                && (location.getBlockX() >> 4) == chunk.getX()
                && (location.getBlockZ() >> 4) == chunk.getZ()) {
                it.remove();
            }
        }
    }

    private void scanChunk(Chunk chunk) {
        if (!chunk.isLoaded()) {
            return;
        }

        for (BlockState state : chunk.getTileEntities()) {
            if (state instanceof CreatureSpawner) {
                discover(state.getLocation());
            }
        }
    }

    private void discover(Location rawLocation) {
        Location location = blockLocation(rawLocation);
        Block block = location.getBlock();
        if (block.getType() != Material.SPAWNER) {
            spawners.remove(location);
            return;
        }

        String id = SlimefunBlockStorageBridge.idAt(location);
        SpawnerSpec spec = parseSpec(id);
        if (spec == null) {
            spawners.remove(location);
            return;
        }

        configureVanillaSpawner(block, spec.type());
        spawners.compute(location, (ignored, previous) -> {
            long now = System.currentTimeMillis();
            if (previous != null && previous.spec().equals(spec)) {
                return previous;
            }
            return new SpawnerState(spec, now + spec.intervalMillis());
        });
    }

    private void tick() {
        if (!SlimefunBlockStorageBridge.available() || spawners.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        Iterator<Map.Entry<Location, SpawnerState>> it = spawners.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Location, SpawnerState> entry = it.next();
            Location location = entry.getKey();
            SpawnerState state = entry.getValue();

            if (!location.isWorldLoaded() || !location.getChunk().isLoaded()) {
                it.remove();
                continue;
            }

            Block block = location.getBlock();
            if (block.getType() != Material.SPAWNER) {
                it.remove();
                continue;
            }

            String currentId = SlimefunBlockStorageBridge.idAt(location);
            SpawnerSpec currentSpec = parseSpec(currentId);
            if (currentSpec == null) {
                it.remove();
                continue;
            }
            if (!currentSpec.equals(state.spec())) {
                configureVanillaSpawner(block, currentSpec.type());
                state = new SpawnerState(currentSpec, now + currentSpec.intervalMillis());
                entry.setValue(state);
            }

            if (now < state.nextSpawnMillis()) {
                continue;
            }

            long charge = SlimefunBlockStorageBridge.chargeAt(location);
            long next = now + state.spec().intervalMillis();
            entry.setValue(new SpawnerState(state.spec(), next));

            if (charge < ENERGY_COST) {
                continue;
            }

            if (!SlimefunBlockStorageBridge.setCharge(location, charge - ENERGY_COST)) {
                continue;
            }

            spawnCycle(location, state.spec());
        }
    }

    private void spawnCycle(Location origin, SpawnerSpec spec) {
        World world = origin.getWorld();
        if (world == null) {
            return;
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < spec.spawnCount(); i++) {
            double x = random.nextDouble(-RANGE, RANGE);
            double y = random.nextDouble(0.0, 1.0);
            double z = random.nextDouble(-RANGE, RANGE);
            Location spawn = origin.clone().add(x, y, z);
            try {
                world.spawnEntity(spawn, spec.type());
            } catch (RuntimeException ex) {
                plugin.getLogger().fine(
                    "Could not spawn " + spec.type() + " for Magic spawner at "
                        + origin.getBlockX() + "," + origin.getBlockY() + "," + origin.getBlockZ()
                        + ": " + ex.getMessage()
                );
            }
        }
    }

    private static SpawnerSpec parseSpec(String id) {
        if (id == null) {
            return null;
        }

        Matcher matcher = SPAWNER_ID.matcher(id);
        if (!matcher.matches()) {
            return null;
        }

        String entityToken = matcher.group(1);
        int tier = Integer.parseInt(matcher.group(2));
        EntityType type;
        try {
            type = "MUSHROOM_COW".equals(entityToken)
                ? EntityType.MOOSHROOM
                : EntityType.valueOf(entityToken);
        } catch (IllegalArgumentException ex) {
            return null;
        }

        return switch (tier) {
            case 1 -> new SpawnerSpec(type, 15_000L, 1);
            case 2 -> new SpawnerSpec(type, 10_000L, 3);
            case 3 -> new SpawnerSpec(type, 5_000L, 5);
            default -> null;
        };
    }

    private static void configureVanillaSpawner(Block block, EntityType type) {
        if (!(block.getState() instanceof CreatureSpawner spawner)) {
            return;
        }

        spawner.setSpawnedType(type);
        spawner.setMinSpawnDelay(800);
        spawner.setMaxSpawnDelay(800);
        spawner.setMaxNearbyEntities(0);
        spawner.setRequiredPlayerRange(18);
        spawner.setSpawnRange(0);
        spawner.setSpawnCount(0);
        spawner.update(false, false);
    }

    private static Location blockLocation(Location location) {
        return new Location(
            location.getWorld(),
            location.getBlockX(),
            location.getBlockY(),
            location.getBlockZ()
        );
    }

    private record SpawnerSpec(EntityType type, long intervalMillis, int spawnCount) {
    }

    private record SpawnerState(SpawnerSpec spec, long nextSpawnMillis) {
    }
}
