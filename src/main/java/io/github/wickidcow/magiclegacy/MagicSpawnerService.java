package io.github.wickidcow.magiclegacy;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

final class MagicSpawnerService implements Listener {

    private static final String PREFIX = "MAGIC_SPAWNER_";
    private static final long ENERGY_PER_CYCLE = 520L;
    private static final double SPAWN_RADIUS = 5.0;
    private static final double ENTITY_RADIUS = 18.0;
    private static final int MAX_NEARBY_LIVING = 16;
    private static final int MAX_SAME_TYPE = 15;

    private final JavaPlugin plugin;
    private final SlimefunBlockBridge bridge;
    private final Map<BlockKey, SpawnerState> spawners = new HashMap<>();
    private BukkitTask task;

    MagicSpawnerService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.bridge = new SlimefunBlockBridge(plugin);
    }

    void start() {
        bridge.registerChunkLoadListener(this, refs -> {
            for (SlimefunBlockBridge.BlockRef ref : refs) {
                register(ref.id(), ref.location());
            }
        });

        for (SlimefunBlockBridge.BlockRef ref : bridge.loadedBlocks()) {
            register(ref.id(), ref.location());
        }

        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
        plugin.getLogger().info("Native Magic spawner scheduler loaded " + spawners.size() + " placed spawner(s).");
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
        Location location = event.getBlockPlaced().getLocation();
        Bukkit.getScheduler().runTask(plugin, () -> {
            String id = bridge.idAt(location);
            if (id != null) {
                register(id, location);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        spawners.remove(BlockKey.of(event.getBlock().getLocation()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkUnload(ChunkUnloadEvent event) {
        int chunkX = event.getChunk().getX();
        int chunkZ = event.getChunk().getZ();
        World world = event.getWorld();
        spawners.entrySet().removeIf(entry ->
            entry.getKey().worldId.equals(world.getUID().toString())
                && (entry.getKey().x >> 4) == chunkX
                && (entry.getKey().z >> 4) == chunkZ
        );
    }

    private void register(String id, Location location) {
        SpawnerSpec spec = SpawnerSpec.parse(id);
        if (spec == null || location.getWorld() == null) {
            return;
        }

        BlockKey key = BlockKey.of(location);
        spawners.compute(key, (ignored, old) -> {
            long next = old == null ? System.currentTimeMillis() + spec.intervalMillis() : old.nextSpawnAt();
            return new SpawnerState(id, location.clone(), spec, next);
        });
    }

    private void tick() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<BlockKey, SpawnerState>> iterator = spawners.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<BlockKey, SpawnerState> entry = iterator.next();
            SpawnerState state = entry.getValue();
            Location location = state.location();

            if (location.getWorld() == null || !location.isChunkLoaded()) {
                iterator.remove();
                continue;
            }

            if (now < state.nextSpawnAt()) {
                continue;
            }

            String liveId = bridge.idAt(location);
            if (!state.id().equals(liveId)) {
                iterator.remove();
                continue;
            }

            state = state.withNextSpawnAt(now + state.spec().intervalMillis());
            entry.setValue(state);

            spawnCycle(state);
        }
    }

    private void spawnCycle(SpawnerState state) {
        Location origin = state.location();
        World world = origin.getWorld();
        if (world == null) {
            return;
        }

        int living = 0;
        int sameType = 0;
        for (Entity entity : world.getNearbyEntities(origin, ENTITY_RADIUS, ENTITY_RADIUS, ENTITY_RADIUS)) {
            if (entity instanceof LivingEntity && !(entity instanceof Player)) {
                living++;
                if (entity.getType() == state.spec().type()) {
                    sameType++;
                }
            }
        }

        if (living >= MAX_NEARBY_LIVING || sameType >= MAX_SAME_TYPE) {
            return;
        }

        int allowedByLiving = MAX_NEARBY_LIVING - living;
        int allowedByType = MAX_SAME_TYPE - sameType;
        int amount = Math.min(state.spec().batchSize(), Math.min(allowedByLiving, allowedByType));
        if (amount <= 0) {
            return;
        }

        if (bridge.getCharge(state.id(), origin) < ENERGY_PER_CYCLE) {
            return;
        }
        if (!bridge.removeCharge(state.id(), origin, ENERGY_PER_CYCLE)) {
            return;
        }

        for (int i = 0; i < amount; i++) {
            Location spawn = randomSpawnLocation(origin);
            try {
                world.spawnEntity(spawn, state.spec().type());
            } catch (RuntimeException ex) {
                plugin.getLogger().fine(
                    "Magic spawner " + state.id() + " could not spawn " + state.spec().type()
                        + " at " + compact(spawn) + ": " + ex.getMessage()
                );
            }
        }
    }

    private static Location randomSpawnLocation(Location origin) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double x = random.nextDouble(-SPAWN_RADIUS, SPAWN_RADIUS + 0.0001);
        double z = random.nextDouble(-SPAWN_RADIUS, SPAWN_RADIUS + 0.0001);
        double y = random.nextDouble(0.0, 1.0001);
        return origin.clone().add(x + 0.5, y, z + 0.5);
    }

    private static String compact(Location location) {
        return location.getWorld().getName() + " "
            + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
    }

    private record SpawnerState(String id, Location location, SpawnerSpec spec, long nextSpawnAt) {
        SpawnerState withNextSpawnAt(long value) {
            return new SpawnerState(id, location, spec, value);
        }
    }

    private record SpawnerSpec(EntityType type, long intervalMillis, int batchSize) {
        static SpawnerSpec parse(String id) {
            if (id == null || !id.startsWith(PREFIX)) {
                return null;
            }

            int split = id.lastIndexOf('_');
            if (split <= PREFIX.length() || split == id.length() - 1) {
                return null;
            }

            int tier;
            try {
                tier = Integer.parseInt(id.substring(split + 1));
            } catch (NumberFormatException ex) {
                return null;
            }

            String typeName = id.substring(PREFIX.length(), split);
            if ("MUSHROOM_COW".equals(typeName)) {
                typeName = "MOOSHROOM";
            }

            EntityType type = Registry.ENTITY_TYPE.get(NamespacedKey.minecraft(typeName.toLowerCase()));
            if (type == null || !type.isAlive()) {
                return null;
            }

            return switch (tier) {
                case 1 -> new SpawnerSpec(type, 15_000L, 1);
                case 2 -> new SpawnerSpec(type, 10_000L, 3);
                case 3 -> new SpawnerSpec(type, 5_000L, 5);
                default -> null;
            };
        }
    }

    private record BlockKey(String worldId, int x, int y, int z) {
        static BlockKey of(Location location) {
            return new BlockKey(
                location.getWorld().getUID().toString(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ()
            );
        }
    }
}
