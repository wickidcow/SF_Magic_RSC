package io.github.wickidcow.magiclegacy;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

final class BannerLianhunListener implements Listener {

    private static final String ITEM_ID = "MAGIC_BANNER_LIANHUN";
    private static final int MAX_SOULS = 1_888_888;
    private static final int MINIMUM_SOULS = 101;

    private static final double PULSE_DAMAGE_MULTIPLIER = 0.003;
    private static final double BURST_DAMAGE_MULTIPLIER = 8.0;
    private static final double PULSE_RADIUS = 10.0;
    private static final double PULSE_HEIGHT = 10.0;
    private static final double BURST_RADIUS = 30.0;
    private static final double BURST_HEIGHT = 30.0;
    private static final double PULSE_KNOCKBACK = 2.0;
    private static final double BURST_KNOCKBACK = 15.0;

    private static final Pattern LEGACY_SOUL_LINE =
        Pattern.compile("(?i)(?:stored\\s+souls?|current)[^0-9]*(\\d+)");

    private final NamespacedKey soulsKey;

    BannerLianhunListener(MagicLegacyPlugin plugin) {
        soulsKey = new NamespacedKey(plugin, "banner_lianhun_souls");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWeaponHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity living)) {
            return;
        }

        ItemStack banner = player.getInventory().getItemInMainHand();
        if (!SlimefunItemIdentity.is(banner, ITEM_ID) || banner.getAmount() != 1) {
            return;
        }

        if (living.getHealth() - event.getFinalDamage() > 0.0) {
            return;
        }

        int souls = Math.min(MAX_SOULS, readSouls(banner) + 1);
        writeSouls(banner, souls);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack banner = event.getItem();
        if (!SlimefunItemIdentity.is(banner, ITEM_ID)) {
            return;
        }

        event.setCancelled(true);

        Player player = event.getPlayer();
        int souls = readSouls(banner);
        if (souls < MINIMUM_SOULS) {
            player.sendMessage("§cThe Magic Soul Banner needs at least 101 stored souls.");
            return;
        }

        if (player.isSneaking()) {
            useBurst(player, banner, souls);
        } else {
            usePulse(player, banner, souls);
        }
    }

    private void usePulse(Player player, ItemStack banner, int souls) {
        int consumed = ThreadLocalRandom.current().nextInt(7);
        int remaining = Math.max(0, souls - consumed);
        double damage = souls * PULSE_DAMAGE_MULTIPLIER;

        writeSouls(banner, remaining);
        applyEffect(player, "night_vision", 100, 4);
        applyEffect(player, "speed", 100, 4);
        applyEffect(player, "strength", 600, 4);

        drawCylinder(player.getWorld(), player.getLocation(), PULSE_RADIUS, PULSE_HEIGHT,
            Particle.SOUL_FIRE_FLAME, 5, 24, 1);
        damageNearby(player, PULSE_RADIUS, PULSE_HEIGHT, damage, PULSE_KNOCKBACK, "entity.allay.death");

        player.sendMessage(
            "§aSoul Pulse: §f" + formatDamage(damage) + " damage §7with §f" + consumed
                + " soul" + (consumed == 1 ? "" : "s") + " §7consumed. §f" + remaining + " §7remain."
        );
    }

    private void useBurst(Player player, ItemStack banner, int souls) {
        int consumed = (int) Math.ceil(souls / 3.0);
        int remaining = Math.max(0, souls - consumed);
        double damage = souls * BURST_DAMAGE_MULTIPLIER;

        writeSouls(banner, remaining);
        applyEffect(player, "blindness", 100, 255);
        applyEffect(player, "slowness", 100, 255);
        applyEffect(player, "weakness", 600, 255);

        drawCylinder(player.getWorld(), player.getLocation(), BURST_RADIUS, BURST_HEIGHT,
            Particle.SOUL, 7, 36, 2);
        damageNearby(player, BURST_RADIUS, BURST_HEIGHT, damage, BURST_KNOCKBACK,
            "entity.zombie_villager.cure");

        player.sendMessage(
            "§cSoul Burst: §f" + formatDamage(damage) + " damage §7with §f" + consumed
                + " souls §7consumed. §f" + remaining + " §7remain."
        );
    }

    private void damageNearby(
        Player player,
        double radius,
        double height,
        double damage,
        double knockback,
        String soundKey
    ) {
        World world = player.getWorld();
        Location center = player.getLocation();
        playSound(world, center, soundKey);

        for (org.bukkit.entity.Entity entity : world.getNearbyEntities(center, radius, height, radius)) {
            if (!(entity instanceof LivingEntity living) || entity.equals(player)) {
                continue;
            }

            Vector direction = living.getLocation().toVector().subtract(center.toVector());
            if (direction.lengthSquared() < 1.0E-6) {
                direction = new Vector(0.0, 0.25, 0.0);
            } else {
                direction.normalize();
            }

            living.setVelocity(direction.multiply(knockback));
            living.damage(damage, player);
        }
    }

    private static void drawCylinder(
        World world,
        Location center,
        double radius,
        double height,
        Particle particle,
        int verticalRings,
        int pointsPerRing,
        int countPerPoint
    ) {
        double minY = center.getY() - height / 2.0;
        double verticalStep = verticalRings <= 1 ? 0.0 : height / (verticalRings - 1);

        for (int yIndex = 0; yIndex < verticalRings; yIndex++) {
            double y = minY + verticalStep * yIndex;
            for (int point = 0; point < pointsPerRing; point++) {
                double angle = Math.PI * 2.0 * point / pointsPerRing;
                double x = center.getX() + radius * Math.cos(angle);
                double z = center.getZ() + radius * Math.sin(angle);
                world.spawnParticle(particle, new Location(world, x, y, z), countPerPoint);
            }
        }
    }

    private void applyEffect(Player player, String key, int durationTicks, int amplifier) {
        PotionEffectType type = Registry.EFFECT.get(NamespacedKey.minecraft(key));
        if (type != null) {
            player.addPotionEffect(new PotionEffect(type, durationTicks, amplifier, true, true, true));
        }
    }

    private static void playSound(World world, Location location, String key) {
        Sound sound = Registry.SOUND_EVENT.get(NamespacedKey.minecraft(key));
        if (sound != null) {
            world.playSound(location, sound, 1.0F, 1.0F);
        }
    }

    private int readSouls(ItemStack banner) {
        ItemMeta meta = banner.getItemMeta();
        if (meta == null) {
            return 0;
        }

        PersistentDataContainer data = meta.getPersistentDataContainer();
        Integer stored = data.get(soulsKey, PersistentDataType.INTEGER);
        if (stored != null) {
            return Math.max(0, Math.min(MAX_SOULS, stored));
        }

        int legacy = readLegacyLore(meta);
        data.set(soulsKey, PersistentDataType.INTEGER, legacy);
        banner.setItemMeta(meta);
        return legacy;
    }

    private int readLegacyLore(ItemMeta meta) {
        List<String> lore = meta.getLore();
        if (lore == null) {
            return 0;
        }

        for (String line : lore) {
            String plain = ChatColor.stripColor(line);
            if (plain == null) {
                continue;
            }

            Matcher matcher = LEGACY_SOUL_LINE.matcher(plain);
            if (matcher.find()) {
                try {
                    return Math.max(0, Math.min(MAX_SOULS, Integer.parseInt(matcher.group(1))));
                } catch (NumberFormatException ignored) {
                    return 0;
                }
            }
        }

        return 0;
    }

    private void writeSouls(ItemStack banner, int souls) {
        ItemMeta meta = banner.getItemMeta();
        if (meta == null) {
            return;
        }

        int safeSouls = Math.max(0, Math.min(MAX_SOULS, souls));
        meta.getPersistentDataContainer().set(soulsKey, PersistentDataType.INTEGER, safeSouls);
        meta.setDisplayName("§dMagic Soul Banner");
        meta.setLore(List.of(
            "§8Magic Legacy",
            "§7Stored souls: §f" + safeSouls,
            "§7Kills with this banner capture §f1 soul§7.",
            "§7Right-click: §f10-block Soul Pulse§7.",
            "§7Pulse damage: §f0.003 × stored souls§7; costs §f0-6 souls§7.",
            "§7Sneak + right-click: §f30-block Soul Burst§7.",
            "§7Burst damage: §f8 × stored souls§7; costs §f1/3 of stored souls§7.",
            "§7Abilities require more than §f100 souls§7."
        ));
        banner.setItemMeta(meta);
    }

    private static String formatDamage(double damage) {
        if (damage >= 1000.0) {
            return String.format(Locale.US, "%,.0f", damage);
        }
        return String.format(Locale.US, "%.2f", damage);
    }
}
