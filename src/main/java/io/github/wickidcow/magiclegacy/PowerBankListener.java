package io.github.wickidcow.magiclegacy;

import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

final class PowerBankListener implements Listener {

    private static final String DESCRIPTION_ID = "MAGIC_POWER_BANK_DESCRIPTION";
    private static final String ALPHA_ID = "MAGIC_POWER_BANK_ALPHA";
    private static final String BETA_ID = "MAGIC_POWER_BANK_BETA";

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || !isRightClick(event.getAction())) {
            return;
        }

        ItemStack bank = event.getItem();
        String id = SlimefunItemIdentity.idOf(bank);
        if (id == null) {
            return;
        }

        if (DESCRIPTION_ID.equals(id)) {
            event.setCancelled(true);
            showDescription(event.getPlayer());
            return;
        }

        if (!ALPHA_ID.equals(id) && !BETA_ID.equals(id)) {
            return;
        }

        event.setCancelled(true);
        chargeOffHand(event.getPlayer(), bank, ALPHA_ID.equals(id));
    }

    private static void chargeOffHand(Player player, ItemStack bank, boolean alpha) {
        ItemStack target = player.getInventory().getItemInOffHand();
        if (target == null || target.getType() == Material.AIR) {
            player.sendMessage(ChatColor.AQUA + "Hold one chargeable Slimefun item in your off hand.");
            return;
        }

        if (target.getAmount() != 1) {
            player.sendMessage(ChatColor.AQUA + "The off-hand item must be a single item.");
            return;
        }

        Float targetMax = SlimefunItemIdentity.getMaxCharge(target);
        Float targetCharge = SlimefunItemIdentity.getCharge(target);
        if (targetMax == null || targetCharge == null || targetMax <= 0.0F) {
            player.sendMessage(ChatColor.AQUA + "That off-hand item cannot store Slimefun energy.");
            return;
        }

        float needed = Math.max(0.0F, targetMax - targetCharge);
        if (needed <= 0.0F) {
            player.sendMessage(ChatColor.AQUA + "That item is already fully charged.");
            return;
        }

        Float bankCharge = SlimefunItemIdentity.getCharge(bank);
        Float bankMax = SlimefunItemIdentity.getMaxCharge(bank);
        if (bankCharge == null || bankMax == null || bankMax <= 0.0F) {
            player.sendMessage(ChatColor.RED + "This Magic Power Bank could not read its stored energy.");
            return;
        }

        float cost;
        if (alpha) {
            float threshold = bankMax * 0.5F;
            if (bankCharge < threshold) {
                player.sendMessage(
                    ChatColor.AQUA + "Power Bank Alpha requires at least "
                        + formatEnergy(threshold) + " J before it can discharge."
                );
                return;
            }

            int upperExclusive = Math.max(1, Math.min(500_000, (int) Math.floor(bankCharge) + 1));
            cost = ThreadLocalRandom.current().nextInt(upperExclusive);
        } else {
            cost = needed * 2.0F;
            if (bankCharge < cost) {
                player.sendMessage(
                    ChatColor.AQUA + "Power Bank Beta needs " + formatEnergy(cost)
                        + " J to finish charging that item."
                );
                return;
            }
        }

        if (cost > 0.0F && !SlimefunItemIdentity.removeCharge(bank, cost)) {
            player.sendMessage(ChatColor.RED + "The Power Bank could not spend its stored energy.");
            return;
        }

        if (!SlimefunItemIdentity.setCharge(target, targetMax)) {
            if (cost > 0.0F) {
                SlimefunItemIdentity.setCharge(bank, Math.min(bankMax, bankCharge));
            }
            player.sendMessage(ChatColor.RED + "The target item could not be charged.");
            return;
        }

        player.sendMessage(
            ChatColor.AQUA + (alpha ? "Power Bank Alpha" : "Power Bank Beta")
                + ChatColor.GRAY + " fully charged the off-hand item for "
                + ChatColor.WHITE + formatEnergy(cost) + " J" + ChatColor.GRAY + "."
        );
    }

    private static void showDescription(Player player) {
        player.sendMessage(ChatColor.LIGHT_PURPLE + "Magic Power Banks");
        player.sendMessage(
            ChatColor.GRAY + "Alpha: requires at least half charge, then fully charges one off-hand "
                + "Slimefun item for a randomized cost up to 499,999 J."
        );
        player.sendMessage(
            ChatColor.GRAY + "Beta: fully charges one off-hand Slimefun item at a fixed 2 J bank cost "
                + "for every 1 J transferred."
        );
    }

    private static boolean isRightClick(Action action) {
        return action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;
    }

    private static String formatEnergy(float value) {
        return String.format("%,.0f", value);
    }
}
