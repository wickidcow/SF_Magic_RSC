package io.github.wickidcow.magiclegacy;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
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

final class NativePowerMixManager implements Listener {

    private static final String MACHINE_ID = "MAGIC_POWER_MIX_1";
    private static final String BOX_ID = "MAGIC_POWER_MIX_BOX_1";
    private static final long CAPACITY = 1_008_888_888L;

    private static final List<GeneratorModule> MODULES = List.of(
        new GeneratorModule("SUPREME_BASIC_VENTUS_GENERATOR", "Basic Wind Generator", 45, 5000, Material.LIGHT_BLUE_CONCRETE),
        new GeneratorModule("INFINITE_PANEL", "Infinite Panel", 46, 120000, Material.LIGHT_BLUE_GLAZED_TERRACOTTA),
        new GeneratorModule("VOID_PANEL", "Void Panel", 47, 6000, Material.LIGHT_GRAY_GLAZED_TERRACOTTA),
        new GeneratorModule("CELESTIAL_PANEL", "Celestial Panel", 48, 1500, Material.YELLOW_GLAZED_TERRACOTTA),
        new GeneratorModule("ADVANCED_PANEL", "Advanced Solar Panel", 49, 300, Material.RED_GLAZED_TERRACOTTA),
        new GeneratorModule("MAGIC_NEWPLAYER_SOLAR_GENERATOR", "Magic New Player Solar Generator", 50, 100, Material.DAYLIGHT_DETECTOR),
        new GeneratorModule("WATER_TURBINE", "Water Turbine", 51, 128, Material.PRISMARINE_WALL),
        new GeneratorModule("SOLAR_GENERATOR_4", "Charged Solar Generator", 52, 256, Material.DAYLIGHT_DETECTOR)
    );

    private static final Method BLOCK_STORAGE_GET_INVENTORY;
    private static final Method MENU_GET_SIZE;
    private static final Method MENU_GET_ITEM;
    private static final Method MENU_REPLACE_ITEM;
    private static final String MENU_LINK_ERROR;

    static {
        Method getInventory = null;
        Method getSize = null;
        Method getItem = null;
        Method replaceItem = null;
        String error = null;
        try {
            Class<?> storage = Class.forName("me.mrCookieSlime.Slimefun.api.BlockStorage");
            Class<?> menu = Class.forName("me.mrCookieSlime.Slimefun.api.inventory.BlockMenu");
            getInventory = storage.getMethod("getInventory", Location.class);
            getSize = menu.getMethod("getSize");
            getItem = menu.getMethod("getItemInSlot", int.class);
            try {
                replaceItem = menu.getMethod("replaceExistingItem", int.class, ItemStack.class);
            } catch (NoSuchMethodException ignored) {
                replaceItem = menu.getMethod("addItem", int.class, ItemStack.class);
            }
        } catch (ReflectiveOperationException ex) {
            error = ex.getClass().getSimpleName() + ": " + ex.getMessage();
        }
        BLOCK_STORAGE_GET_INVENTORY = getInventory;
        MENU_GET_SIZE = getSize;
        MENU_GET_ITEM = getItem;
        MENU_REPLACE_ITEM = replaceItem;
        MENU_LINK_ERROR = error;
    }

    private final MagicLegacyPlugin plugin;
    private final Set<Location> machines = new HashSet<>();
    private BukkitTask task;

    NativePowerMixManager(MagicLegacyPlugin plugin) {
        this.plugin = plugin;
    }

    void start() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        registerChunkDataLoadListener();

        if (!menuAvailable()) {
            plugin.getLogger().warning(
                "Native Magic Power Mix is unavailable because the Slimefun BlockMenu API could not be linked: "
                    + menuError()
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
        Iterator<Location> it = machines.iterator();
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
                "Could not register Magic Power Mix chunk discovery: "
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
                    machines.add(blockLocation(location));
                }
            }
        } catch (ReflectiveOperationException ex) {
            plugin.getLogger().warning(
                "Could not read Slimefun chunk data for Magic Power Mix: "
                    + ex.getClass().getSimpleName() + ": " + ex.getMessage()
            );
        }
    }

    private void discover(Location location) {
        if (MACHINE_ID.equals(SlimefunBlockStorageBridge.idAt(location))) {
            machines.add(location);
        } else {
            machines.remove(location);
        }
    }

    private void tick() {
        if (!menuAvailable() || machines.isEmpty()) {
            return;
        }

        Iterator<Location> it = machines.iterator();
        while (it.hasNext()) {
            Location machine = it.next();
            if (!valid(machine)) {
                it.remove();
                continue;
            }

            Location box = blockLocation(machine.clone().add(0, 1, 0));
            if (!BOX_ID.equals(SlimefunBlockStorageBridge.idAt(box))) {
                continue;
            }

            Object menu = getMenu(box);
            if (menu == null) {
                continue;
            }

            long totalPower = 0L;
            for (GeneratorModule module : MODULES) {
                int count = countItem(menu, module.itemId());
                totalPower += (long) count * module.powerPerSecond();
                replaceMenuItem(
                    menu,
                    module.statusSlot(),
                    count > 0
                        ? statusItem(
                            module.material(),
                            ChatColor.GREEN + "Generating",
                            List.of(
                                ChatColor.GRAY + "Magic Matrix: " + ChatColor.YELLOW + "Generator",
                                ChatColor.GRAY + "Type: " + ChatColor.BLUE + module.displayName(),
                                ChatColor.GRAY + "Generation: " + ChatColor.AQUA
                                    + ((long) count * module.powerPerSecond()) + ChatColor.GRAY + " J/s"
                            )
                        )
                        : statusItem(
                            Material.RED_STAINED_GLASS_PANE,
                            ChatColor.RED + "Not Generating",
                            List.of(ChatColor.GRAY + "Type: " + ChatColor.BLUE + module.displayName())
                        )
                );
            }

            replaceMenuItem(
                menu,
                53,
                totalPower > 0
                    ? statusItem(
                        Material.SOUL_LANTERN,
                        ChatColor.GREEN + "Generating",
                        List.of(
                            ChatColor.GRAY + "Magic Matrix: " + ChatColor.YELLOW + "Generator",
                            ChatColor.GRAY + "Generation: " + ChatColor.AQUA + totalPower + ChatColor.GRAY + " J/s"
                        )
                    )
                    : statusItem(
                        Material.RED_STAINED_GLASS_PANE,
                        ChatColor.RED + "Not Generating",
                        List.of(ChatColor.GRAY + "Magic Matrix: " + ChatColor.YELLOW + "Generator")
                    )
            );

            if (totalPower <= 0L) {
                continue;
            }

            long current = SlimefunBlockStorageBridge.chargeAt(machine);
            long add = totalPower / 2L;
            long next = Math.min(CAPACITY, current + add);
            if (next > current) {
                SlimefunBlockStorageBridge.setCharge(machine, next);
            }
        }
    }

    private static boolean valid(Location location) {
        return location.isWorldLoaded()
            && location.getChunk().isLoaded()
            && MACHINE_ID.equals(SlimefunBlockStorageBridge.idAt(location));
    }

    private static boolean menuAvailable() {
        return BLOCK_STORAGE_GET_INVENTORY != null
            && MENU_GET_SIZE != null
            && MENU_GET_ITEM != null
            && MENU_REPLACE_ITEM != null;
    }

    private static String menuError() {
        return MENU_LINK_ERROR == null ? "unknown" : MENU_LINK_ERROR;
    }

    private static Object getMenu(Location location) {
        try {
            return BLOCK_STORAGE_GET_INVENTORY.invoke(null, location);
        } catch (ReflectiveOperationException ex) {
            return null;
        }
    }

    private static int countItem(Object menu, String itemId) {
        try {
            int size = ((Number) MENU_GET_SIZE.invoke(menu)).intValue();
            int count = 0;
            for (int slot = 0; slot < size; slot++) {
                Object value = MENU_GET_ITEM.invoke(menu, slot);
                if (value instanceof ItemStack stack && itemId.equals(SlimefunItemIdentity.idOf(stack))) {
                    count += stack.getAmount();
                }
            }
            return count;
        } catch (ReflectiveOperationException ex) {
            return 0;
        }
    }

    private static void replaceMenuItem(Object menu, int slot, ItemStack item) {
        try {
            MENU_REPLACE_ITEM.invoke(menu, slot, item);
        } catch (ReflectiveOperationException ignored) {
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

    private static Location blockLocation(Location location) {
        return new Location(
            location.getWorld(),
            location.getBlockX(),
            location.getBlockY(),
            location.getBlockZ()
        );
    }

    private record GeneratorModule(
        String itemId,
        String displayName,
        int statusSlot,
        long powerPerSecond,
        Material material
    ) {
    }
}
