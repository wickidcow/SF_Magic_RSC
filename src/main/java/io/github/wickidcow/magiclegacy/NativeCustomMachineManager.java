package io.github.wickidcow.magiclegacy;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitTask;

final class NativeCustomMachineManager implements Listener {

    private static final String FLOWER_MACHINE_ID = "MAGIC_FLOWER_MIX_1";
    private static final String FLOWER_BOX_ID = "MAGIC_FLOWER_MIX_BOX_1";
    private static final long FLOWER_COST = 10L;

    private static final String GEOMINER_ID = "MAGIC_GEOMINER";
    private static final String GEOMINER_BOX_ID = "MAGIC_GEOMINER_BOX";
    private static final long GEOMINER_TICK_COST = 1314L;
    private static final long GEOMINER_CRAFT_INTERVAL = 30_000L;
    private static final int GEOMINER_WARMUP_TICKS = 30;
    private static final int[] GEOMINER_OUTPUT_SLOTS = {
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34
    };

    private static final List<FlowerStep> FLOWERS = List.of(
        new FlowerStep(Material.POPPY, 0, Material.BLUE_ORCHID, 1),
        new FlowerStep(Material.BLUE_ORCHID, 1, Material.ALLIUM, 2),
        new FlowerStep(Material.ALLIUM, 2, Material.AZURE_BLUET, 3),
        new FlowerStep(Material.AZURE_BLUET, 3, Material.RED_TULIP, 4),
        new FlowerStep(Material.RED_TULIP, 4, Material.ORANGE_TULIP, 5),
        new FlowerStep(Material.ORANGE_TULIP, 5, Material.WHITE_TULIP, 6),
        new FlowerStep(Material.WHITE_TULIP, 6, Material.PINK_TULIP, 7),
        new FlowerStep(Material.PINK_TULIP, 7, Material.OXEYE_DAISY, 8),
        new FlowerStep(Material.OXEYE_DAISY, 8, Material.SUNFLOWER, 18),
        new FlowerStep(Material.SUNFLOWER, 18, Material.LILAC, 19),
        new FlowerStep(Material.LILAC, 19, Material.ROSE_BUSH, 20),
        new FlowerStep(Material.ROSE_BUSH, 20, Material.PEONY, 21),
        new FlowerStep(Material.PEONY, 21, Material.WITHER_ROSE, 22),
        new FlowerStep(Material.WITHER_ROSE, 22, Material.CORNFLOWER, 23),
        new FlowerStep(Material.CORNFLOWER, 23, Material.LILY_OF_THE_VALLEY, 24),
        new FlowerStep(Material.LILY_OF_THE_VALLEY, 24, Material.DANDELION, 25),
        new FlowerStep(Material.DANDELION, 25, Material.TORCHFLOWER, 26),
        new FlowerStep(Material.TORCHFLOWER, 26, Material.PITCHER_PLANT, 36),
        new FlowerStep(Material.PITCHER_PLANT, 36, Material.PINK_PETALS, 37),
        new FlowerStep(Material.PINK_PETALS, 37, Material.SPORE_BLOSSOM, 38),
        new FlowerStep(Material.SPORE_BLOSSOM, 38, Material.POPPY, 0)
    );

    private static final List<GeoResourceSpec> GEO_RESOURCES = List.of(
        new GeoResourceSpec("magic_redstone", "MAGIC_REDSTONE"),
        new GeoResourceSpec("magic_cosmic_dust", "MAGIC_COSMIC_DUST"),
        new GeoResourceSpec("magic_soul", "MAGIC_SOUL")
    );

    private static final Method BLOCK_STORAGE_GET_INVENTORY;
    private static final Method MENU_GET_ITEM;
    private static final Method MENU_REPLACE_ITEM;
    private static final Method MENU_PUSH_ITEM;
    private static final Method SLIMEFUN_GET_REGISTRY;
    private static final Method SLIMEFUN_GET_GPS_NETWORK;
    private static final String LINK_ERROR;

    static {
        Method getInventory = null;
        Method getItem = null;
        Method replaceItem = null;
        Method pushItem = null;
        Method getRegistry = null;
        Method getGps = null;
        String error = null;

        try {
            Class<?> storage = Class.forName("me.mrCookieSlime.Slimefun.api.BlockStorage");
            Class<?> menu = Class.forName("me.mrCookieSlime.Slimefun.api.inventory.BlockMenu");
            Class<?> slimefun = Class.forName("io.github.thebusybiscuit.slimefun4.implementation.Slimefun");

            getInventory = storage.getMethod("getInventory", Location.class);
            getItem = menu.getMethod("getItemInSlot", int.class);

            try {
                replaceItem = menu.getMethod("replaceExistingItem", int.class, ItemStack.class);
            } catch (NoSuchMethodException ignored) {
                replaceItem = menu.getMethod("addItem", int.class, ItemStack.class);
            }

            for (Method method : menu.getMethods()) {
                if ("pushItem".equals(method.getName()) && method.getParameterCount() == 2) {
                    pushItem = method;
                    break;
                }
            }

            getRegistry = slimefun.getMethod("getRegistry");
            getGps = slimefun.getMethod("getGPSNetwork");
        } catch (ReflectiveOperationException ex) {
            error = ex.getClass().getSimpleName() + ": " + ex.getMessage();
        }

        BLOCK_STORAGE_GET_INVENTORY = getInventory;
        MENU_GET_ITEM = getItem;
        MENU_REPLACE_ITEM = replaceItem;
        MENU_PUSH_ITEM = pushItem;
        SLIMEFUN_GET_REGISTRY = getRegistry;
        SLIMEFUN_GET_GPS_NETWORK = getGps;
        LINK_ERROR = error;
    }

    private final MagicLegacyPlugin plugin;
    private final Map<Location, MachineState> machines = new HashMap<>();
    private BukkitTask task;

    NativeCustomMachineManager(MagicLegacyPlugin plugin) {
        this.plugin = plugin;
    }

    void start() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        registerChunkDataLoadListener();

        if (!available()) {
            plugin.getLogger().warning(
                "Native Magic custom machines are unavailable because a Slimefun runtime API could not be linked: "
                    + error()
            );
            return;
        }

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
        machines.keySet().removeIf(location -> sameChunk(location, chunk));
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
                "Could not register native custom-machine chunk discovery: "
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
                "Could not read Slimefun chunk data for native custom machines: "
                    + ex.getClass().getSimpleName() + ": " + ex.getMessage()
            );
        }
    }

    private void discover(Location location) {
        String id = SlimefunBlockStorageBridge.idAt(location);
        if (id == null) {
            machines.remove(location);
            return;
        }
        track(location, id);
    }

    private void track(Location location, String id) {
        if (FLOWER_MACHINE_ID.equals(id)) {
            machines.putIfAbsent(location, new MachineState(id, 0, 0L));
        } else if (GEOMINER_ID.equals(id)) {
            machines.putIfAbsent(location, new MachineState(id, 0, 0L));
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

            if (!valid(location, state.id())) {
                it.remove();
                continue;
            }

            if (FLOWER_MACHINE_ID.equals(state.id())) {
                tickFlower(location);
                continue;
            }

            MachineState next = tickGeominer(location, state, now);
            entry.setValue(next);
        }
    }

    private void tickFlower(Location machine) {
        Location box = blockLocation(machine.clone().add(0, 1, 0));
        if (!FLOWER_BOX_ID.equals(SlimefunBlockStorageBridge.idAt(box))) {
            return;
        }

        Object menu = getMenu(box);
        if (menu == null) {
            return;
        }

        ItemStack target = getItem(menu, 44);
        Material stopMaterial = target == null ? null : target.getType();

        if (stopMaterial != null) {
            for (FlowerStep step : FLOWERS) {
                ItemStack slot = getItem(menu, step.sourceSlot());
                if (slot != null && slot.getType() == stopMaterial) {
                    return;
                }
            }
        }

        long charge = SlimefunBlockStorageBridge.chargeAt(machine);
        if (charge < FLOWER_COST) {
            return;
        }

        for (FlowerStep step : FLOWERS) {
            ItemStack source = getItem(menu, step.sourceSlot());
            if (source == null || source.getType() != step.source()) {
                continue;
            }

            ItemStack destination = getItem(menu, step.destinationSlot());
            if (destination != null && destination.getType() != Material.AIR) {
                continue;
            }

            if (!SlimefunBlockStorageBridge.setCharge(machine, charge - FLOWER_COST)) {
                return;
            }

            int amount = source.getAmount();
            replaceItem(menu, step.sourceSlot(), new ItemStack(Material.AIR));
            replaceItem(menu, step.destinationSlot(), new ItemStack(step.destination(), amount));
            return;
        }
    }

    private MachineState tickGeominer(Location machine, MachineState state, long now) {
        Location box = blockLocation(machine.clone().add(0, -1, 0));
        if (!GEOMINER_BOX_ID.equals(SlimefunBlockStorageBridge.idAt(box))) {
            return state;
        }

        Object menu = getMenu(box);
        if (menu == null) {
            return state;
        }

        long charge = SlimefunBlockStorageBridge.chargeAt(machine);
        if (charge < GEOMINER_TICK_COST) {
            replaceItem(menu, 13, statusItem(
                Material.BARRIER,
                ChatColor.GREEN + "Information",
                List.of(ChatColor.YELLOW + "Magic Geominer: " + ChatColor.RED + "Stopped")
            ));
            return state;
        }

        if (!SlimefunBlockStorageBridge.setCharge(machine, charge - GEOMINER_TICK_COST)) {
            return state;
        }

        replaceItem(menu, 13, statusItem(
            Material.LIGHT,
            ChatColor.GREEN + "Information",
            List.of(ChatColor.YELLOW + "Magic Geominer: " + ChatColor.GREEN + "Working")
        ));

        if (state.warmupTicks() < GEOMINER_WARMUP_TICKS) {
            return new MachineState(state.id(), state.warmupTicks() + 1, state.lastCraftMillis());
        }

        if (now - state.lastCraftMillis() <= GEOMINER_CRAFT_INTERVAL) {
            return state;
        }

        boolean mined = mineFirstAvailableGeoResource(machine, menu);
        if (!mined) {
            replaceItem(menu, 4, statusItem(
                Material.NETHER_STAR,
                ChatColor.GREEN + "Mining Complete",
                List.of(ChatColor.AQUA + "No supported Magic GEO resources remain here.")
            ));
        }

        return new MachineState(state.id(), state.warmupTicks(), now);
    }

    private boolean mineFirstAvailableGeoResource(Location machine, Object menu) {
        World world = machine.getWorld();
        if (world == null) {
            return false;
        }

        for (GeoResourceSpec spec : GEO_RESOURCES) {
            Object resource = geoResource(spec.key());
            if (resource == null) {
                continue;
            }

            int supplies = getSupplies(resource, world, machine.getBlockX() >> 4, machine.getBlockZ() >> 4);
            if (supplies <= 0) {
                continue;
            }

            ItemStack output = SlimefunItemIdentity.copyById(spec.outputItemId());
            if (output == null) {
                continue;
            }

            if (!pushItem(menu, output, GEOMINER_OUTPUT_SLOTS)) {
                return false;
            }

            if (!setSupplies(resource, world, machine.getBlockX() >> 4, machine.getBlockZ() >> 4, supplies - 1)) {
                return false;
            }

            return true;
        }

        return false;
    }

    private static Object geoResource(String path) {
        try {
            Object registry = SLIMEFUN_GET_REGISTRY.invoke(null);
            Method getGeoResources = registry.getClass().getMethod("getGEOResources");
            Object map = getGeoResources.invoke(registry);
            Method get = map.getClass().getMethod("get", Object.class);
            Object value = get.invoke(map, new NamespacedKey("rykenslimefuncustomizer", path));
            if (value instanceof Optional<?> optional) {
                return optional.orElse(null);
            }
            return value;
        } catch (ReflectiveOperationException ex) {
            return null;
        }
    }

    private static int getSupplies(Object resource, World world, int chunkX, int chunkZ) {
        try {
            Object manager = geoResourceManager();
            if (manager == null) {
                return 0;
            }
            Method method = findMethod(manager.getClass(), "getSupplies", 4);
            if (method == null) {
                return 0;
            }
            Object value = method.invoke(manager, resource, world, chunkX, chunkZ);
            if (value instanceof OptionalInt optional) {
                return optional.orElse(0);
            }
            return value instanceof Number number ? number.intValue() : 0;
        } catch (ReflectiveOperationException ex) {
            return 0;
        }
    }

    private static boolean setSupplies(Object resource, World world, int chunkX, int chunkZ, int amount) {
        try {
            Object manager = geoResourceManager();
            if (manager == null) {
                return false;
            }
            Method method = findMethod(manager.getClass(), "setSupplies", 5);
            if (method == null) {
                return false;
            }
            method.invoke(manager, resource, world, chunkX, chunkZ, amount);
            return true;
        } catch (ReflectiveOperationException ex) {
            return false;
        }
    }

    private static Object geoResourceManager() {
        try {
            Object gps = SLIMEFUN_GET_GPS_NETWORK.invoke(null);
            Method getManager = gps.getClass().getMethod("getResourceManager");
            return getManager.invoke(gps);
        } catch (ReflectiveOperationException ex) {
            return null;
        }
    }

    private static Method findMethod(Class<?> type, String name, int parameterCount) {
        for (Method method : type.getMethods()) {
            if (name.equals(method.getName()) && method.getParameterCount() == parameterCount) {
                return method;
            }
        }
        return null;
    }

    private static Object getMenu(Location location) {
        try {
            return BLOCK_STORAGE_GET_INVENTORY.invoke(null, location);
        } catch (ReflectiveOperationException ex) {
            return null;
        }
    }

    private static ItemStack getItem(Object menu, int slot) {
        try {
            Object value = MENU_GET_ITEM.invoke(menu, slot);
            return value instanceof ItemStack stack ? stack : null;
        } catch (ReflectiveOperationException ex) {
            return null;
        }
    }

    private static void replaceItem(Object menu, int slot, ItemStack item) {
        try {
            MENU_REPLACE_ITEM.invoke(menu, slot, item);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static boolean pushItem(Object menu, ItemStack item, int[] slots) {
        if (MENU_PUSH_ITEM == null) {
            return false;
        }

        try {
            Object result = MENU_PUSH_ITEM.invoke(menu, item, slots);
            if (result == null) {
                return true;
            }
            if (result instanceof ItemStack leftover) {
                return leftover.getAmount() <= 0 || leftover.getType() == Material.AIR;
            }
            return true;
        } catch (ReflectiveOperationException | IllegalArgumentException ex) {
            return false;
        }
    }

    private static ItemStack statusItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static boolean valid(Location location, String expectedId) {
        return location.isWorldLoaded()
            && location.getChunk().isLoaded()
            && expectedId.equals(SlimefunBlockStorageBridge.idAt(location));
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

    private static boolean available() {
        return BLOCK_STORAGE_GET_INVENTORY != null
            && MENU_GET_ITEM != null
            && MENU_REPLACE_ITEM != null
            && MENU_PUSH_ITEM != null
            && SLIMEFUN_GET_REGISTRY != null
            && SLIMEFUN_GET_GPS_NETWORK != null;
    }

    private static String error() {
        return LINK_ERROR == null ? "unknown" : LINK_ERROR;
    }

    private record MachineState(String id, int warmupTicks, long lastCraftMillis) {
    }

    private record FlowerStep(
        Material source,
        int sourceSlot,
        Material destination,
        int destinationSlot
    ) {
    }

    private record GeoResourceSpec(String key, String outputItemId) {
    }
}
