package io.github.wickidcow.magiclegacy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.RayTraceResult;

final class TeleportItemListener implements Listener {

    private static final String PAPER_ID = "MAGIC_TP_PAPER";
    private static final String STICK_ID = "MAGIC_TP_STICK";
    private static final String RANDOM_ID = "MAGIC_RANDOM_TP_PAPER";

    private static final long PAPER_COOLDOWN_MS = 10_000L;
    private static final long STICK_COOLDOWN_MS = 1_000L;
    private static final long RANDOM_COOLDOWN_MS = 10_000L;

    private static final Pattern WORLD_PATTERN = Pattern.compile(".*世界：(.+)");
    private static final Pattern X_PATTERN = Pattern.compile(".*X：([\\d.-]+)");
    private static final Pattern Y_PATTERN = Pattern.compile(".*Y：([\\d.-]+)");
    private static final Pattern Z_PATTERN = Pattern.compile(".*Z：([\\d.-]+)");
    private static final Pattern YAW_PATTERN = Pattern.compile(".*水平方向：([\\d.-]+)");
    private static final Pattern PITCH_PATTERN = Pattern.compile(".*垂直方向：([\\d.-]+)");

    private final NamespacedKey creatorKey;
    private final NamespacedKey worldKey;
    private final NamespacedKey xKey;
    private final NamespacedKey yKey;
    private final NamespacedKey zKey;
    private final NamespacedKey yawKey;
    private final NamespacedKey pitchKey;
    private final Map<UUID, Long> paperCooldowns = new HashMap<>();
    private final Map<UUID, Long> stickCooldowns = new HashMap<>();
    private final Map<UUID, Long> randomCooldowns = new HashMap<>();

    TeleportItemListener(JavaPlugin plugin) {
        creatorKey = new NamespacedKey(plugin, "tp_creator");
        worldKey = new NamespacedKey(plugin, "tp_world");
        xKey = new NamespacedKey(plugin, "tp_x");
        yKey = new NamespacedKey(plugin, "tp_y");
        zKey = new NamespacedKey(plugin, "tp_z");
        yawKey = new NamespacedKey(plugin, "tp_yaw");
        pitchKey = new NamespacedKey(plugin, "tp_pitch");
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || !isRightClick(event.getAction())) {
            return;
        }

        ItemStack item = event.getItem();
        String id = SlimefunItemIdentity.idOf(item);
        if (!PAPER_ID.equals(id) && !STICK_ID.equals(id) && !RANDOM_ID.equals(id)) {
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();

        if (RANDOM_ID.equals(id)) {
            randomTeleport(player);
            return;
        }

        if (player.isSneaking()) {
            bind(player, item);
            return;
        }

        BoundLocation bound = readBound(item);
        if (bound == null) {
            player.sendMessage(ChatColor.AQUA + "This teleport item is not bound yet. Sneak-right-click to bind it.");
            return;
        }

        Location destination = bound.resolve();
        if (destination == null) {
            player.sendMessage(ChatColor.RED + "The bound world '" + bound.worldName() + "' is not loaded.");
            return;
        }

        if (PAPER_ID.equals(id)) {
            if (!ready(player, paperCooldowns, PAPER_COOLDOWN_MS, "Teleport Paper")) {
                return;
            }
            teleportPlayer(player, destination);
        } else {
            if (!ready(player, stickCooldowns, STICK_COOLDOWN_MS, "Teleport Stick")) {
                return;
            }
            teleportTarget(player, destination);
        }
    }

    private void bind(Player player, ItemStack item) {
        Location location = player.getLocation();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            player.sendMessage(ChatColor.RED + "This teleport item has no writable item data.");
            return;
        }

        PersistentDataContainer data = meta.getPersistentDataContainer();
        data.set(creatorKey, PersistentDataType.STRING, player.getName());
        data.set(worldKey, PersistentDataType.STRING, location.getWorld().getName());
        data.set(xKey, PersistentDataType.DOUBLE, location.getX());
        data.set(yKey, PersistentDataType.DOUBLE, location.getY());
        data.set(zKey, PersistentDataType.DOUBLE, location.getZ());
        data.set(yawKey, PersistentDataType.DOUBLE, (double) location.getYaw());
        data.set(pitchKey, PersistentDataType.DOUBLE, (double) location.getPitch());

        meta.setLore(List.of(
            ChatColor.DARK_GRAY + "Magic Legacy",
            ChatColor.GRAY + "Bound by: " + ChatColor.WHITE + player.getName(),
            ChatColor.GRAY + "World: " + ChatColor.WHITE + location.getWorld().getName(),
            ChatColor.GRAY + String.format("X: %.2f  Y: %.2f  Z: %.2f", location.getX(), location.getY(), location.getZ()),
            ChatColor.GRAY + String.format("Yaw: %.2f  Pitch: %.2f", location.getYaw(), location.getPitch()),
            ChatColor.GRAY + "Sneak-right-click to rebind."
        ));
        item.setItemMeta(meta);

        player.sendMessage(
            ChatColor.AQUA + "Bound this teleport item to "
                + ChatColor.WHITE + location.getWorld().getName()
                + ChatColor.GRAY + String.format(" (%.2f, %.2f, %.2f)", location.getX(), location.getY(), location.getZ())
                + ChatColor.GRAY + "."
        );
    }

    private BoundLocation readBound(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }

        PersistentDataContainer data = meta.getPersistentDataContainer();
        String world = data.get(worldKey, PersistentDataType.STRING);
        Double x = data.get(xKey, PersistentDataType.DOUBLE);
        Double y = data.get(yKey, PersistentDataType.DOUBLE);
        Double z = data.get(zKey, PersistentDataType.DOUBLE);
        Double yaw = data.get(yawKey, PersistentDataType.DOUBLE);
        Double pitch = data.get(pitchKey, PersistentDataType.DOUBLE);
        if (world != null && x != null && y != null && z != null && yaw != null && pitch != null) {
            return new BoundLocation(world, x, y, z, yaw.floatValue(), pitch.floatValue());
        }

        return readLegacyLore(meta.getLore());
    }

    private static BoundLocation readLegacyLore(List<String> lore) {
        if (lore == null || lore.size() < 12) {
            return null;
        }

        String world = matchText(WORLD_PATTERN, lore.get(6));
        Double x = matchDouble(X_PATTERN, lore.get(7));
        Double y = matchDouble(Y_PATTERN, lore.get(8));
        Double z = matchDouble(Z_PATTERN, lore.get(9));
        Double yaw = matchDouble(YAW_PATTERN, lore.get(10));
        Double pitch = matchDouble(PITCH_PATTERN, lore.get(11));
        if (world == null || x == null || y == null || z == null || yaw == null || pitch == null) {
            return null;
        }

        return new BoundLocation(world, x, y, z, yaw.floatValue(), pitch.floatValue());
    }

    private static void teleportPlayer(Player player, Location destination) {
        if (!player.teleport(destination)) {
            player.sendMessage(ChatColor.RED + "Teleport failed.");
            return;
        }

        playTeleportEffects(destination);
        player.sendMessage(
            ChatColor.AQUA + "Teleported to " + ChatColor.WHITE + destination.getWorld().getName()
                + ChatColor.GRAY + String.format(" (%.2f, %.2f, %.2f)", destination.getX(), destination.getY(), destination.getZ())
                + ChatColor.GRAY + "."
        );
    }

    private static void teleportTarget(Player player, Location destination) {
        RayTraceResult result = player.getWorld().rayTraceEntities(
            player.getEyeLocation(),
            player.getEyeLocation().getDirection(),
            10.0,
            0.25,
            entity -> entity != player
        );

        drawBeam(player);
        Entity entity = result == null ? null : result.getHitEntity();
        if (entity == null) {
            player.sendMessage(ChatColor.AQUA + "Aim at an entity within 10 blocks.");
            return;
        }

        if (!entity.teleport(destination)) {
            player.sendMessage(ChatColor.RED + "The targeted entity could not be teleported.");
            return;
        }

        playTeleportEffects(destination);
        player.sendMessage(
            ChatColor.AQUA + "Teleported " + ChatColor.WHITE + entity.getName()
                + ChatColor.AQUA + " to the bound location."
        );
    }

    private void randomTeleport(Player player) {
        if (!ready(player, randomCooldowns, RANDOM_COOLDOWN_MS, "Random Teleport Paper")) {
            return;
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        Location current = player.getLocation();
        World world = current.getWorld();

        double x = current.getX() + random.nextDouble(-250.0, 250.0);
        double z = current.getZ() + random.nextDouble(-250.0, 250.0);
        double y = current.getY() + random.nextDouble(-25.0, 25.0);
        y = Math.max(world.getMinHeight() + 1.0, Math.min(world.getMaxHeight() - 1.0, y));
        float yaw = (float) random.nextDouble(-180.0, 180.0);
        float pitch = (float) random.nextDouble(-90.0, 90.0);

        Location destination = new Location(world, x, y, z, yaw, pitch);
        if (!player.teleport(destination)) {
            player.sendMessage(ChatColor.RED + "Random teleport failed.");
            return;
        }

        playTeleportEffects(destination);
        player.sendMessage(
            ChatColor.AQUA + "Random teleport: "
                + ChatColor.WHITE + String.format("%.2f, %.2f, %.2f", x, y, z)
                + ChatColor.GRAY + "."
        );
    }

    private static boolean ready(Player player, Map<UUID, Long> cooldowns, long cooldownMs, String itemName) {
        long now = System.currentTimeMillis();
        long last = cooldowns.getOrDefault(player.getUniqueId(), 0L);
        long remaining = cooldownMs - (now - last);
        if (remaining > 0L) {
            player.sendMessage(
                ChatColor.AQUA + itemName + ChatColor.GRAY + " is recharging for "
                    + ChatColor.WHITE + String.format("%.1f", remaining / 1000.0) + "s" + ChatColor.GRAY + "."
            );
            return false;
        }
        cooldowns.put(player.getUniqueId(), now);
        return true;
    }

    private static void drawBeam(Player player) {
        Location point = player.getEyeLocation().clone();
        var step = point.getDirection().normalize().multiply(0.5);
        for (int i = 0; i < 20; i++) {
            point.add(step);
            player.getWorld().spawnParticle(Particle.PORTAL, point, 1, 0, 0, 0, 0);
        }
    }

    private static void playTeleportEffects(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        world.playSound(location, Sound.BLOCK_PORTAL_TRAVEL, 1.0F, 1.0F);
        world.spawnParticle(Particle.PORTAL, location, 96, 0.8, 1.0, 0.8, 0.1);
    }

    private static String matchText(Pattern pattern, String raw) {
        Matcher matcher = pattern.matcher(ChatColor.stripColor(raw == null ? "" : raw));
        return matcher.matches() ? matcher.group(1) : null;
    }

    private static Double matchDouble(Pattern pattern, String raw) {
        String value = matchText(pattern, raw);
        if (value == null) {
            return null;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static boolean isRightClick(Action action) {
        return action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;
    }

    private record BoundLocation(String worldName, double x, double y, double z, float yaw, float pitch) {
        Location resolve() {
            World world = Bukkit.getWorld(worldName);
            return world == null ? null : new Location(world, x, y, z, yaw, pitch);
        }
    }
}
