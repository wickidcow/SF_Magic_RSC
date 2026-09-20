package io.github.wickidcow.magiclegacy;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
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

final class MagicGunListener implements Listener {

    private static final String ITEM_ID = "MAGIC_GUN_1";
    private static final double RANGE = 20.0;
    private static final double DAMAGE = 5.0;
    private static final int COOLDOWN_TICKS = 8;
    private static final double BEAM_STEP = 0.5;
    private static final double RAY_SIZE = 0.15;

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack stack = event.getItem();
        if (!SlimefunItemIdentity.is(stack, ITEM_ID)) {
            return;
        }

        Player player = event.getPlayer();
        Material cooldownMaterial = stack.getType();
        if (player.hasCooldown(cooldownMaterial)) {
            return;
        }

        // RSC's ItemUseHandler cancelled the underlying use event after invoking the script.
        event.setCancelled(true);
        player.setCooldown(cooldownMaterial, COOLDOWN_TICKS);

        World world = player.getWorld();
        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection().normalize();
        Location start = eye.clone().add(direction.clone().multiply(0.5));

        RayTraceResult result = world.rayTrace(
            start,
            direction,
            RANGE,
            FluidCollisionMode.NEVER,
            true,
            RAY_SIZE,
            null
        );

        double beamLength = RANGE;
        if (result != null && result.getHitPosition() != null) {
            beamLength = start.toVector().distance(result.getHitPosition());
        }

        drawBeam(world, start, direction, beamLength);
        world.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 0.45F, 1.65F);

        if (result == null) {
            return;
        }

        Entity hit = result.getHitEntity();
        if (hit instanceof LivingEntity living && hit != player) {
            living.damage(DAMAGE, player);
        }
    }

    private static void drawBeam(World world, Location start, Vector direction, double length) {
        for (double distance = 0.0; distance <= length; distance += BEAM_STEP) {
            Location point = start.clone().add(direction.clone().multiply(distance));
            world.spawnParticle(Particle.END_ROD, point, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }
}
