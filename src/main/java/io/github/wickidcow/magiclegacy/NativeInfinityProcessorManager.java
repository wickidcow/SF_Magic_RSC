package io.github.wickidcow.magiclegacy;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
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

final class NativeInfinityProcessorManager implements Listener {

    private static final String MACHINE_ID = "MAGIC_INFINITY_MIX_1";
    private static final String BOX_ID = "MAGIC_INFINITY_MIX_BOX_1";

    private static final String COBBLE_MODULE_ID = "MAGIC_INFINITY_COBBLE_GEN";
    private static final String DUST_MODULE_ID = "MAGIC_INFINITY_DUST_EXTRACTOR";
    private static final String INGOT_MODULE_ID = "MAGIC_INFINITY_INGOT_FORMER";

    private static final long COBBLE_POWER = 3_200L;
    private static final long DUST_POWER = 28_800L;
    private static final long INGOT_POWER = 28_800L;

    private static final int MAX_ACTIVE_MODULES = 12;
    private static final int[] MODULE_SLOTS = {45, 46, 47, 48, 49, 50, 51};
    private static final int[] OUTPUT_SLOTS = {
        0, 1, 2, 3, 4, 5, 6, 7, 8,
        9, 10, 11, 12, 13, 14, 15, 16, 17,
        18, 19, 20, 21, 22, 23, 24, 25, 26,
        27, 28, 29, 30, 31, 32, 33, 34, 35
    };

    private static final String[] DUST_IDS = {
        "IRON_DUST", "GOLD_DUST", "TIN_DUST", "COPPER_DUST",
        "SILVER_DUST", "LEAD_DUST", "ALUMINUM_DUST", "ZINC_DUST", "MAGNESIUM_DUST"
    };

    private static final String[] INGOT_IDS = {
        "TIN_INGOT", "COPPER_INGOT", "SILVER_INGOT", "LEAD_INGOT",
        "ALUMINUM_INGOT", "ZINC_INGOT", "MAGNESIUM_INGOT"
    };

    private static final Method BLOCK_STORAGE_GET_INVENTORY;
    private static final Method MENU_GET_ITEM;
    private static final Method MENU_REPLACE_ITEM;
    private static final Method MENU_PUSH_ITEM;
    private static final String LINK_ERROR;

    static {
        Method getInventory = null;
        Method getItem = null;
        Method replaceItem = null;
        Method pushItem = null;
        String error = null;

        try {
            Class<?> storage = Class.forName("me.mrCookieSlime.Slimefun.api.BlockStorage");
            Class<?> menu = Class.forName("me.mrCookieSlime.Slimefun.api.inventory.BlockMenu");

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
        } catch (ReflectiveOperationException ex) {
            error = ex.getClass().getSimpleName() + ": " + ex.getMessage();
        }

        BLOCK_STORAGE_GET_INVENTORY = getInventory;
        MENU_GET_ITEM = getItem;
        MENU_REPLACE_ITEM = replaceItem;
        MENU_PUSH_ITEM = pushItem;
        LINK_ERROR = error;
    }

    private final MagicLegacyPlugin plugin;
    private final Set<Location> machines = new HashSet<>();
    private BukkitTask task;

    NativeInfinityProcessorManager(MagicLegacyPlugin plugin) {
        this.plugin = plugin;
    }

    void start() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        registerChunkDataLoadListener();

        if (!available()) {
            plugin.getLogger().warning(
                "Native Magic Infinity Processor is unavailable because the Slimefun BlockMenu API could not be linked: "
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
        machines.removeIf(location -> sameChunk(location, chunk));
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
                "Could not register Magic Infinity Processor chunk discovery: "
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
                "Could not read Slimefun chunk data for Magic Infinity Processor: "
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
        if (machines.isEmpty()) {
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

            ModuleCounts raw = countModules(menu);
            long neededPower = raw.cobble() * COBBLE_POWER
                + raw.dust() * DUST_POWER
                + raw.ingot() * INGOT_POWER;

            long charge = SlimefunBlockStorageBridge.chargeAt(machine);
            if (neededPower <= 0L || charge < neededPower) {
                replaceItem(menu, 53, statusItem(
                    Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE,
                    ChatColor.YELLOW + "Insufficient Power",
                    List.of()
                ));
                continue;
            }

            ModuleCounts production = productionCounts(raw);
            production = capActiveModules(production);

            int units = production.cobble() + production.dust() + production.ingot();
            int emptySlots = countEmptySlots(menu, 0, 35);
            if ((emptySlots / 3) < units) {
                replaceItem(menu, 53, statusItem(
                    Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE,
                    ChatColor.YELLOW + "Output Full",
                    List.of()
                ));
                continue;
            }

            List<String> lore = machineLore(raw, neededPower);

            if (!SlimefunBlockStorageBridge.setCharge(machine, charge - neededPower)) {
                continue;
            }

            emitCobble(menu, production.cobble());
            emitDust(menu, production.dust());
            emitIngots(menu, production.ingot());

            lore.addAll(outputLore(production));
            replaceItem(menu, 53, statusItem(
                Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE,
                ChatColor.YELLOW + "Magic Infinity Processing",
                lore
            ));
        }
    }

    private static ModuleCounts countModules(Object menu) {
        int cobble = 0;
        int dust = 0;
        int ingot = 0;

        for (int slot : MODULE_SLOTS) {
            ItemStack stack = getItem(menu, slot);
            if (stack == null) {
                continue;
            }

            String id = SlimefunItemIdentity.idOf(stack);
            if (COBBLE_MODULE_ID.equals(id)) {
                cobble += stack.getAmount();
            } else if (DUST_MODULE_ID.equals(id)) {
                dust += stack.getAmount();
            } else if (INGOT_MODULE_ID.equals(id)) {
                ingot += stack.getAmount();
            }
        }

        return new ModuleCounts(cobble, dust, ingot);
    }

    private static ModuleCounts productionCounts(ModuleCounts raw) {
        int cobble = raw.cobble();
        int dust = raw.dust();
        int ingot = raw.ingot();

        if (cobble >= dust) {
            cobble -= dust;
        } else {
            dust = cobble;
            cobble = 0;
        }

        if (dust >= ingot) {
            dust -= ingot;
        } else {
            ingot = dust;
            dust = 0;
        }

        return new ModuleCounts(cobble, dust, ingot);
    }

    private static ModuleCounts capActiveModules(ModuleCounts counts) {
        int cobble = Math.min(MAX_ACTIVE_MODULES, counts.cobble());
        int remaining = MAX_ACTIVE_MODULES - cobble;

        int dust = Math.min(remaining, counts.dust());
        remaining -= dust;

        int ingot = Math.min(remaining, counts.ingot());
        return new ModuleCounts(cobble, dust, ingot);
    }

    private static int countEmptySlots(Object menu, int start, int end) {
        int empty = 0;
        for (int slot = start; slot <= end; slot++) {
            ItemStack stack = getItem(menu, slot);
            if (stack == null || stack.getType() == Material.AIR || stack.getAmount() <= 0) {
                empty++;
            }
        }
        return empty;
    }

    private static void emitCobble(Object menu, int units) {
        for (int i = 0; i < units; i++) {
            for (int j = 0; j < 3; j++) {
                pushItem(menu, new ItemStack(Material.COBBLESTONE, 64), OUTPUT_SLOTS);
            }
        }
    }

    private static void emitDust(Object menu, int units) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < units; i++) {
            for (int j = 0; j < 3; j++) {
                String id = DUST_IDS[random.nextInt(DUST_IDS.length)];
                ItemStack output = SlimefunItemIdentity.copyById(id);
                if (output != null) {
                    output.setAmount(64);
                    pushItem(menu, output, OUTPUT_SLOTS);
                }
            }
        }
    }

    private static void emitIngots(Object menu, int units) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < units; i++) {
            for (int j = 0; j < 3; j++) {
                ItemStack output;
                if (random.nextDouble() < (2.0 / 9.0)) {
                    output = new ItemStack(
                        random.nextBoolean() ? Material.IRON_INGOT : Material.GOLD_INGOT,
                        64
                    );
                } else {
                    String id = INGOT_IDS[random.nextInt(INGOT_IDS.length)];
                    output = SlimefunItemIdentity.copyById(id);
                    if (output != null) {
                        output.setAmount(64);
                    }
                }

                if (output != null) {
                    pushItem(menu, output, OUTPUT_SLOTS);
                }
            }
        }
    }

    private static List<String> machineLore(ModuleCounts raw, long neededPower) {
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.AQUA + "Machine Data ================================");

        if (raw.cobble() > 0) {
            lore.add(ChatColor.AQUA + "Cobblestone Generator: "
                + ChatColor.YELLOW + raw.cobble() + ChatColor.AQUA + " installed");
        }
        if (raw.dust() > 0) {
            lore.add(ChatColor.AQUA + "Dust Extractor: "
                + ChatColor.YELLOW + raw.dust() + ChatColor.AQUA + " installed");
        }
        if (raw.ingot() > 0) {
            lore.add(ChatColor.AQUA + "Ingot Former: "
                + ChatColor.YELLOW + raw.ingot() + ChatColor.AQUA + " installed");
        }
        if (neededPower > 0L) {
            lore.add(ChatColor.AQUA + "Current energy use: "
                + ChatColor.YELLOW + (neededPower * 2L) + ChatColor.AQUA + " J/s");
        }

        lore.add(ChatColor.AQUA + "Machine Data ================================");
        return lore;
    }

    private static List<String> outputLore(ModuleCounts production) {
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GREEN + "Output Data ================================");

        if (production.cobble() > 0) {
            lore.add(ChatColor.GREEN + "Cobblestone: "
                + ChatColor.YELLOW + (production.cobble() * 192) + ChatColor.GREEN + " items");
        }
        if (production.dust() > 0) {
            lore.add(ChatColor.GREEN + "Dust: "
                + ChatColor.YELLOW + (production.dust() * 192) + ChatColor.GREEN + " items");
        }
        if (production.ingot() > 0) {
            lore.add(ChatColor.GREEN + "Ingots: "
                + ChatColor.YELLOW + (production.ingot() * 192) + ChatColor.GREEN + " items");
        }

        lore.add(ChatColor.GREEN + "Output Data ================================");
        return lore;
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
            MENU_PUSH_ITEM.invoke(menu, item, slots);
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

    private static boolean valid(Location location) {
        return location.isWorldLoaded()
            && location.getChunk().isLoaded()
            && MACHINE_ID.equals(SlimefunBlockStorageBridge.idAt(location));
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
            && MENU_PUSH_ITEM != null;
    }

    private static String error() {
        return LINK_ERROR == null ? "unknown" : LINK_ERROR;
    }

    private record ModuleCounts(int cobble, int dust, int ingot) {
    }
}
