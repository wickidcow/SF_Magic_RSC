package io.github.wickidcow.magiclegacy;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

final class MagicWeaponListener implements Listener {

    private static final String INFINITE_STICK_ID = "MAGIC_INFINITE_STICK";
    private static final String INFINITY_BLADE_ID = "MAGIC_INFINITY_BLADE_1";
    private static final double LIFESTEAL_RATIO = 0.01;

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) {
            return;
        }

        ItemStack weapon = attacker.getInventory().getItemInMainHand();

        if (SlimefunItemIdentity.is(weapon, INFINITE_STICK_ID)) {
            useInfiniteStick(event, attacker, weapon);
            return;
        }

        if (SlimefunItemIdentity.is(weapon, INFINITY_BLADE_ID)) {
            applyInfinityBladeLifesteal(event, attacker);
        }
    }

    private static void useInfiniteStick(EntityDamageByEntityEvent event, Player attacker, ItemStack stick) {
        if (!(event.getEntity() instanceof Player target)) {
            return;
        }

        // Preserve the original PvP-only instant-kill behavior while respecting cancelled damage events.
        event.setDamage(0.0);
        consumeOne(attacker, stick);
        target.setHealth(0.0);
    }

    private static void applyInfinityBladeLifesteal(EntityDamageByEntityEvent event, Player attacker) {
        AttributeInstance maxHealthAttribute = attacker.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealthAttribute == null) {
            return;
        }

        double heal = event.getDamage() * LIFESTEAL_RATIO;
        if (heal <= 0.0) {
            return;
        }

        double healed = Math.min(maxHealthAttribute.getValue(), attacker.getHealth() + heal);
        if (healed > attacker.getHealth()) {
            attacker.setHealth(healed);
        }
    }

    private static void consumeOne(Player player, ItemStack stack) {
        if (stack.getAmount() > 1) {
            stack.setAmount(stack.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }
    }
}
