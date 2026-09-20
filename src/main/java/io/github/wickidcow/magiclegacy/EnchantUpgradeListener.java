package io.github.wickidcow.magiclegacy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

final class EnchantUpgradeListener implements Listener {

    private static final String RANDOM_RISKY = "MAGIC_ENCHANT_UP_1";
    private static final String RANDOM_SAFE = "MAGIC_ENCHANT_UP_1_MAX";
    private static final String ALL_RISKY = "MAGIC_ENCHANT_UPUP_1";
    private static final String ALL_SAFE = "MAGIC_ENCHANT_UPUP_1_MAX";

    private static final List<Enchantment> ENCHANTMENTS = loadEnchantments();

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || !isRightClick(event.getAction())) {
            return;
        }

        ItemStack book = event.getItem();
        String id = SlimefunItemIdentity.idOf(book);
        if (!RANDOM_RISKY.equals(id)
            && !RANDOM_SAFE.equals(id)
            && !ALL_RISKY.equals(id)
            && !ALL_SAFE.equals(id)) {
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();
        ItemStack target = player.getInventory().getItemInOffHand();
        if (target == null || target.getType() == Material.AIR) {
            player.sendMessage(ChatColor.AQUA + "Hold the item you want to upgrade in your off hand.");
            return;
        }

        if (RANDOM_RISKY.equals(id) || RANDOM_SAFE.equals(id)) {
            randomUpgrade(player, book, target, RANDOM_RISKY.equals(id));
        } else {
            allUpgrade(player, book, target, ALL_RISKY.equals(id));
        }
    }

    private static void randomUpgrade(Player player, ItemStack book, ItemStack target, boolean risky) {
        if (ENCHANTMENTS.isEmpty()) {
            player.sendMessage(ChatColor.RED + "No enchantments are available from the Bukkit registry.");
            return;
        }

        int maxLevel = highestLevel(target.getEnchantments());
        double chance = risky ? chance(maxLevel, 0.01) : 1.0;

        if (risky && ThreadLocalRandom.current().nextDouble() >= chance) {
            destroyTarget(player);
            consumeOne(player, book);
            player.sendMessage(ChatColor.RED + "The enchant upgrade failed and destroyed the off-hand item.");
            Bukkit.broadcastMessage(
                ChatColor.LIGHT_PURPLE + player.getName()
                    + ChatColor.GRAY + " lost an item to a failed Magic enchant upgrade."
            );
            return;
        }

        Enchantment enchantment = ENCHANTMENTS.get(ThreadLocalRandom.current().nextInt(ENCHANTMENTS.size()));
        int oldLevel = target.getEnchantmentLevel(enchantment);
        int newLevel = oldLevel > 0 ? oldLevel + 1 : 1;
        target.addUnsafeEnchantment(enchantment, newLevel);
        consumeOne(player, book);

        String enchantName = pretty(enchantment);
        player.sendMessage(
            ChatColor.AQUA + (oldLevel > 0 ? "Upgraded " : "Added ")
                + ChatColor.WHITE + enchantName + " " + newLevel
                + ChatColor.AQUA + "."
                + (risky ? ChatColor.GRAY + String.format(" Success chance was %.1f%%.", chance * 100.0) : "")
        );
    }

    private static void allUpgrade(Player player, ItemStack book, ItemStack target, boolean risky) {
        Map<Enchantment, Integer> enchantments = target.getEnchantments();
        if (enchantments.isEmpty()) {
            player.sendMessage(ChatColor.AQUA + "The off-hand item must already have at least one enchantment.");
            return;
        }

        int maxLevel = highestLevel(enchantments);
        double chance = risky ? chance(maxLevel, 0.02) : 1.0;

        if (risky && ThreadLocalRandom.current().nextDouble() >= chance) {
            destroyTarget(player);
            consumeOne(player, book);
            player.sendMessage(ChatColor.RED + "The mass enchant upgrade failed and destroyed the off-hand item.");
            Bukkit.broadcastMessage(
                ChatColor.LIGHT_PURPLE + player.getName()
                    + ChatColor.GRAY + " lost an item to a failed Magic mass enchant upgrade."
            );
            return;
        }

        List<Map.Entry<Enchantment, Integer>> snapshot = new ArrayList<>(enchantments.entrySet());
        for (Map.Entry<Enchantment, Integer> entry : snapshot) {
            target.addUnsafeEnchantment(entry.getKey(), entry.getValue() + 1);
        }
        consumeOne(player, book);

        player.sendMessage(
            ChatColor.AQUA + "Raised all " + snapshot.size() + " enchantment"
                + (snapshot.size() == 1 ? "" : "s") + " by one level."
                + (risky ? ChatColor.GRAY + String.format(" Success chance was %.1f%%.", chance * 100.0) : "")
        );
    }

    private static int highestLevel(Map<Enchantment, Integer> enchantments) {
        int max = 0;
        for (int level : enchantments.values()) {
            max = Math.max(max, level);
        }
        return max;
    }

    private static double chance(int level, double decreasePerTenLevels) {
        int tens = Math.max(0, level / 10);
        return Math.max(0.0, Math.min(0.99, 0.99 - tens * decreasePerTenLevels));
    }

    private static List<Enchantment> loadEnchantments() {
        List<Enchantment> values = new ArrayList<>();
        for (Enchantment enchantment : Registry.ENCHANTMENT) {
            values.add(enchantment);
        }
        return List.copyOf(values);
    }

    private static String pretty(Enchantment enchantment) {
        String raw = enchantment.getKey().getKey().replace('_', ' ');
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

    private static void destroyTarget(Player player) {
        player.getInventory().setItemInOffHand(null);
    }

    private static void consumeOne(Player player, ItemStack stack) {
        if (stack.getAmount() > 1) {
            stack.setAmount(stack.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }
    }

    private static boolean isRightClick(Action action) {
        return action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;
    }
}
