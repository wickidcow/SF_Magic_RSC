package io.github.wickidcow.magiclegacy;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;
import org.bukkit.ChatColor;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

final class MobCatcherListener implements Listener {

    private static final String ALL_CATCH_ID = "MAGIC_ALL_CATCH";
    private static final float ATTEMPT_COST = 50.0F;
    private static final float CAPTURE_COST = 250.0F;
    private static final double RANGE = 5.0;

    private static final Map<String, CaptureSpec> BY_CATCHER = new LinkedHashMap<>();
    private static final Map<EntityType, CaptureSpec> BY_ENTITY = new LinkedHashMap<>();

    static {
        register("MAGIC_BEE_CATCH", EntityType.BEE, "MAGIC_EGG_BEE");
        register("MAGIC_PIG_CATCH", EntityType.PIG, "MAGIC_PIG_1");
        register("MAGIC_SHEEP_CATCH", EntityType.SHEEP, "MAGIC_SHEEP_1");
        register("MAGIC_CHICKEN_CATCH", EntityType.CHICKEN, "MAGIC_CHICKEN_1");
        register("MAGIC_COW_CATCH", EntityType.COW, "MAGIC_COW_1");
        register("MAGIC_OCELOT_CATCH", EntityType.OCELOT, "MAGIC_OCELOT_1");
        register("MAGIC_CAT_CATCH", EntityType.CAT, "MAGIC_CAT_1");
        register("MAGIC_DONKEY_CATCH", EntityType.DONKEY, "MAGIC_DONKEY_1");
        register("MAGIC_FOX_CATCH", EntityType.FOX, "MAGIC_FOX_1");
        register("MAGIC_FROG_CATCH", EntityType.FROG, "MAGIC_FROG_1");
        register("MAGIC_GOAT_CATCH", EntityType.GOAT, "MAGIC_GOAT_1");
        register("MAGIC_HOGLIN_CATCH", EntityType.HOGLIN, "MAGIC_HOGLIN_1");
        register("MAGIC_HORSE_CATCH", EntityType.HORSE, "MAGIC_HORSE_1");
        register("MAGIC_LLAMA_CATCH", EntityType.LLAMA, "MAGIC_LLAMA_1");
        register("MAGIC_TRADER_LLAMA_CATCH", EntityType.TRADER_LLAMA, "MAGIC_TRADER_LLAMA_1");
        register("MAGIC_MOOSHROOM_CATCH", EntityType.MUSHROOM_COW, "MAGIC_MOOSHROOM_1");
        register("MAGIC_MULE_CATCH", EntityType.MULE, "MAGIC_MULE_1");
        register("MAGIC_PANDA_CATCH", EntityType.PANDA, "MAGIC_PANDA_1");
        register("MAGIC_RABBIT_CATCH", EntityType.RABBIT, "MAGIC_RABBIT_1");
        register("MAGIC_STRIDER_CATCH", EntityType.STRIDER, "MAGIC_STRIDER_1");
        register("MAGIC_TURTLE_CATCH", EntityType.TURTLE, "MAGIC_TURTLE_1");
        register("MAGIC_WOLF_CATCH", EntityType.WOLF, "MAGIC_WOLF_1");
        register("MAGIC_ALLAY_CATCH", EntityType.ALLAY, "MAGIC_ALLAY_1");
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || !isRightClick(event.getAction())) {
            return;
        }

        ItemStack catcher = event.getItem();
        String catcherId = SlimefunItemIdentity.idOf(catcher);
        CaptureSpec requested = BY_CATCHER.get(catcherId);
        boolean allCatch = ALL_CATCH_ID.equals(catcherId);
        if (requested == null && !allCatch) {
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();

        Float charge = SlimefunItemIdentity.getCharge(catcher);
        if (charge == null) {
            player.sendMessage(ChatColor.RED + "This Magic catcher could not read its stored energy.");
            return;
        }
        if (charge < CAPTURE_COST) {
            player.sendMessage(ChatColor.AQUA + "The catcher needs at least 250 J before it can be used.");
            return;
        }

        if (!SlimefunItemIdentity.removeCharge(catcher, ATTEMPT_COST)) {
            player.sendMessage(ChatColor.RED + "The catcher could not spend its activation energy.");
            return;
        }

        RayTraceResult result = trace(player);
        drawBeam(player);
        if (result == null || !(result.getHitEntity() instanceof LivingEntity target)) {
            player.sendMessage(ChatColor.AQUA + "Aim at a supported mob within 5 blocks.");
            return;
        }

        CaptureSpec actual = BY_ENTITY.get(target.getType());
        if (actual == null || (!allCatch && actual != requested)) {
            player.sendMessage(
                ChatColor.AQUA + (allCatch
                    ? "That mob is not supported by the Magic catcher."
                    : "This catcher only works on " + requested.displayName() + ".")
            );
            return;
        }

        if (target instanceof Ageable ageable && !ageable.isAdult()) {
            player.sendMessage(ChatColor.AQUA + "Baby mobs cannot be captured.");
            return;
        }

        Float remaining = SlimefunItemIdentity.getCharge(catcher);
        if (remaining == null || remaining < CAPTURE_COST) {
            player.sendMessage(
                ChatColor.AQUA + "A successful capture costs 300 J total. Recharge the catcher and try again."
            );
            return;
        }

        ItemStack captured = SlimefunItemIdentity.copyById(actual.outputItemId());
        if (captured == null) {
            player.sendMessage(ChatColor.RED + "The captured-mob item is not registered. The mob was not removed.");
            return;
        }

        if (!SlimefunItemIdentity.removeCharge(catcher, CAPTURE_COST)) {
            player.sendMessage(ChatColor.RED + "The catcher could not spend its capture energy. The mob was not removed.");
            return;
        }

        Location dropLocation = target.getLocation().clone();
        target.remove();
        dropLocation.getWorld().dropItemNaturally(dropLocation, captured);
        player.sendMessage(
            ChatColor.AQUA + "Captured " + actual.displayName() + ChatColor.GRAY + " for "
                + ChatColor.WHITE + "300 J" + ChatColor.GRAY + "."
        );
    }

    private static RayTraceResult trace(Player player) {
        Location start = player.getEyeLocation();
        Vector direction = start.getDirection().normalize();
        Location origin = start.clone().add(direction);
        Predicate<Entity> filter = entity -> entity instanceof LivingEntity && entity != player;
        return player.getWorld().rayTrace(
            origin,
            direction,
            RANGE,
            FluidCollisionMode.ALWAYS,
            true,
            0.25,
            filter
        );
    }

    private static void drawBeam(Player player) {
        World world = player.getWorld();
        Location point = player.getEyeLocation().clone();
        Vector step = point.getDirection().normalize().multiply(0.5);
        for (int i = 0; i < 10; i++) {
            point.add(step);
            world.spawnParticle(Particle.END_ROD, point, 1, 0, 0, 0, 0);
        }
    }

    private static boolean isRightClick(Action action) {
        return action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;
    }

    private static void register(String catcherId, EntityType entityType, String outputItemId) {
        CaptureSpec spec = new CaptureSpec(catcherId, entityType, outputItemId, pretty(entityType));
        BY_CATCHER.put(catcherId, spec);
        BY_ENTITY.put(entityType, spec);
    }

    private static String pretty(EntityType type) {
        String raw = type.getKey().getKey().replace('_', ' ');
        StringBuilder out = new StringBuilder(raw.length());
        boolean upper = true;
        for (char c : raw.toCharArray()) {
            if (c == ' ') {
                out.append(c);
                upper = true;
            } else if (upper) {
                out.append(Character.toUpperCase(c));
                upper = false;
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    private record CaptureSpec(String catcherId, EntityType entityType, String outputItemId, String displayName) {
    }
}
