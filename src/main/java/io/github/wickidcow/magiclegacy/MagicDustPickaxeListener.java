package io.github.wickidcow.magiclegacy;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

final class MagicDustPickaxeListener implements Listener {

    private static final String ITEM_ID = "MAGIC_DUST_PICKAXE";
    private static final int GIFT_COST = 250;

    private static final List<String> DUST_IDS = List.of(
        "IRON_DUST",
        "GOLD_DUST",
        "TIN_DUST",
        "COPPER_DUST",
        "SILVER_DUST",
        "LEAD_DUST",
        "ALUMINUM_DUST",
        "ZINC_DUST",
        "MAGNESIUM_DUST"
    );

    private static final List<String> GIFT_IDS = List.of(
        "FILLED_FLASK_OF_KNOWLEDGE",
        "DAMASCUS_STEEL_INGOT",
        "MAGIC_REDSTONE",
        "DAMASCUS_STEEL_INGOT",
        "STEEL_INGOT",
        "NETHER_ICE",
        "NEPTUNIUM",
        "REDSTONE_ALLOY",
        "REINFORCED_ALLOY_INGOT",
        "HARDENED_METAL_INGOT"
    );

    private int usageCount;

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || !isRightClick(event.getAction())) {
            return;
        }

        ItemStack pickaxe = event.getItem();
        if (!SlimefunItemIdentity.is(pickaxe, ITEM_ID)) {
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();
        ItemStack offHand = player.getInventory().getItemInOffHand();

        if (offHand != null && offHand.getType() == Material.DIAMOND) {
            redeemGift(player, pickaxe, offHand);
            return;
        }

        if (offHand == null || offHand.getType() != Material.COBBLESTONE) {
            player.sendMessage(
                ChatColor.AQUA + "Hold cobblestone in your off hand to grind dust, or a diamond to redeem a 250-use gift."
            );
            return;
        }

        if (!damagePickaxe(player, pickaxe, 1)) {
            player.sendMessage(ChatColor.RED + "The Magic Dust Pickaxe broke.");
            return;
        }

        consumeOffHand(player, offHand, 1);
        usageCount++;
        updateDisplay(pickaxe);

        ItemStack reward = randomSlimefunItem(DUST_IDS);
        if (reward == null) {
            player.sendMessage(ChatColor.RED + "No valid Slimefun dust reward is registered.");
            return;
        }

        giveOrDrop(player, reward);
        player.sendMessage(
            ChatColor.AQUA + "Ground 1 cobblestone into "
                + ChatColor.WHITE + displayName(reward)
                + ChatColor.GRAY + ". Progress: "
                + ChatColor.WHITE + usageCount + "/" + GIFT_COST
                + ChatColor.GRAY + "."
        );
    }

    private void redeemGift(Player player, ItemStack pickaxe, ItemStack diamond) {
        if (usageCount < GIFT_COST) {
            player.sendMessage(
                ChatColor.AQUA + "The Magic Dust Pickaxe needs "
                    + ChatColor.WHITE + (GIFT_COST - usageCount)
                    + ChatColor.AQUA + " more grinding uses before a gift can be redeemed."
            );
            return;
        }

        ItemStack reward = randomSlimefunItem(GIFT_IDS);
        if (reward == null) {
            player.sendMessage(ChatColor.RED + "No valid Magic gift reward is registered. Nothing was consumed.");
            return;
        }

        consumeOffHand(player, diamond, 1);
        usageCount -= GIFT_COST;
        repairPickaxe(pickaxe, 30);
        updateDisplay(pickaxe);

        ExperienceOrb orb = player.getWorld().spawn(player.getEyeLocation(), ExperienceOrb.class);
        orb.setCustomName("Magic Dust Pickaxe Reward");
        orb.setCustomNameVisible(false);
        orb.setExperience(100);

        giveOrDrop(player, reward);
        player.sendMessage(
            ChatColor.LIGHT_PURPLE + "Magic Dust gift: "
                + ChatColor.WHITE + displayName(reward)
                + ChatColor.GRAY + ", 100 XP, and 30 durability restored."
        );
    }

    private static ItemStack randomSlimefunItem(List<String> ids) {
        int start = ThreadLocalRandom.current().nextInt(ids.size());
        for (int offset = 0; offset < ids.size(); offset++) {
            String id = ids.get((start + offset) % ids.size());
            ItemStack item = SlimefunItemIdentity.copyById(id);
            if (item != null) {
                item.setAmount(1);
                return item;
            }
        }
        return null;
    }

    private static boolean damagePickaxe(Player player, ItemStack pickaxe, int amount) {
        ItemMeta meta = pickaxe.getItemMeta();
        if (!(meta instanceof Damageable damageable)) {
            return true;
        }

        int max = pickaxe.getType().getMaxDurability();
        int next = damageable.getDamage() + amount;
        if (max > 0 && next >= max) {
            player.getInventory().setItemInMainHand(null);
            return false;
        }

        damageable.setDamage(next);
        pickaxe.setItemMeta(meta);
        return true;
    }

    private static void repairPickaxe(ItemStack pickaxe, int amount) {
        ItemMeta meta = pickaxe.getItemMeta();
        if (!(meta instanceof Damageable damageable)) {
            return;
        }
        damageable.setDamage(Math.max(0, damageable.getDamage() - amount));
        pickaxe.setItemMeta(meta);
    }

    private static void consumeOffHand(Player player, ItemStack stack, int amount) {
        if (stack.getAmount() > amount) {
            stack.setAmount(stack.getAmount() - amount);
        } else {
            player.getInventory().setItemInOffHand(null);
        }
    }

    private static void giveOrDrop(Player player, ItemStack item) {
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(item);
        for (ItemStack leftover : leftovers.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }
    }

    private static String displayName(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            return ChatColor.stripColor(meta.getDisplayName());
        }
        return item.getType().getKey().getKey().replace('_', ' ');
    }

    private void updateDisplay(ItemStack pickaxe) {
        ItemMeta meta = pickaxe.getItemMeta();
        if (meta == null) {
            return;
        }

        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Magic Dust Pickaxe");
        meta.setLore(List.of(
            ChatColor.DARK_GRAY + "Magic Legacy",
            ChatColor.GRAY + "Off-hand cobblestone: grind into a random Slimefun dust.",
            ChatColor.GRAY + "Each grind costs " + ChatColor.WHITE + "1 durability" + ChatColor.GRAY + ".",
            ChatColor.GRAY + "Off-hand diamond at 250 uses: redeem a Magic gift.",
            ChatColor.GRAY + "Gift: " + ChatColor.WHITE + "100 XP + 30 durability repair + random reward",
            ChatColor.GRAY + "Grinding progress: " + ChatColor.WHITE + usageCount + "/" + GIFT_COST
        ));
        pickaxe.setItemMeta(meta);
    }

    private static boolean isRightClick(Action action) {
        return action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;
    }
}
