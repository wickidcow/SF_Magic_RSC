package io.github.wickidcow.magiclegacy;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

final class AttributeUpgradeListener implements Listener {

    private static final String RISKY_ID = "MAGIC_ATTRIBUTE_UPUP_1";
    private static final String SAFE_ID = "MAGIC_ATTRIBUTE_UPUP_1_MAX";
    private static final double BASE_SUCCESS = 0.99;

    private final JavaPlugin plugin;
    private final NamespacedKey selectedSlotKey;

    AttributeUpgradeListener(JavaPlugin plugin) {
        this.plugin = plugin;
        this.selectedSlotKey = new NamespacedKey(plugin, "attribute_upgrade_slot");
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || !isRightClick(event.getAction())) {
            return;
        }

        ItemStack book = event.getItem();
        String id = SlimefunItemIdentity.idOf(book);
        if (!RISKY_ID.equals(id) && !SAFE_ID.equals(id)) {
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();

        if (player.isSneaking()) {
            cycleSlot(player, book);
            return;
        }

        upgrade(player, book, RISKY_ID.equals(id));
    }

    private void cycleSlot(Player player, ItemStack book) {
        UpgradeSlot next = selectedSlot(book).next();
        setSelectedSlot(book, next);
        player.sendMessage(
            ChatColor.AQUA + "Attribute upgrade slot: " + ChatColor.WHITE + next.displayName() + ChatColor.GRAY + "."
        );
    }

    private void upgrade(Player player, ItemStack book, boolean risky) {
        ItemStack target = player.getInventory().getItemInOffHand();
        if (target == null || target.getType() == Material.AIR) {
            player.sendMessage(ChatColor.AQUA + "Hold one item to upgrade in your off hand.");
            return;
        }
        if (target.getAmount() != 1) {
            player.sendMessage(ChatColor.AQUA + "The off-hand target must be a single item.");
            return;
        }

        UpgradeSlot slot = selectedSlot(book);
        List<Bonus> bonuses = bonuses(slot);
        if (bonuses.isEmpty()) {
            player.sendMessage(ChatColor.RED + "No attribute bonuses are configured for that slot.");
            return;
        }

        double chance = risky ? successChance(target, slot) : 1.0;
        if (risky && ThreadLocalRandom.current().nextDouble() >= chance) {
            player.getInventory().setItemInOffHand(null);
            consumeOne(player, book);
            player.sendMessage(
                ChatColor.RED + "The attribute upgrade failed and destroyed the off-hand item."
                    + ChatColor.GRAY + String.format(" Success chance was %.1f%%.", chance * 100.0)
            );
            Bukkit.broadcastMessage(
                ChatColor.LIGHT_PURPLE + player.getName()
                    + ChatColor.GRAY + " lost an item to a failed Magic attribute upgrade."
            );
            return;
        }

        double factor = risky
            ? Math.max(0.000001, ThreadLocalRandom.current().nextDouble())
            : 1.0;

        ItemMeta meta = target.getItemMeta();
        if (meta == null) {
            player.sendMessage(ChatColor.RED + "The off-hand item does not support attribute data.");
            return;
        }

        for (Bonus bonus : bonuses) {
            applyMagicModifier(meta, slot, bonus, bonus.amount() * factor);
        }
        target.setItemMeta(meta);
        consumeOne(player, book);

        player.sendMessage(
            ChatColor.AQUA + "Applied " + ChatColor.WHITE + slot.displayName()
                + ChatColor.AQUA + " attribute upgrades"
                + (risky ? ChatColor.GRAY + String.format(" at %.1f%% strength (%.1f%% success chance).", factor * 100.0, chance * 100.0)
                         : ChatColor.GRAY + " at full strength.")
        );
    }

    private double successChance(ItemStack target, UpgradeSlot slot) {
        ItemMeta meta = target.getItemMeta();
        if (meta == null) {
            return BASE_SUCCESS;
        }

        ProgressMetric metric = progressMetric(slot);
        double amount = magicModifierAmount(meta, slot, metric.attribute());
        int penalties = (int) Math.floor(Math.max(0.0, amount) / metric.threshold());
        return Math.max(0.0, BASE_SUCCESS - penalties * 0.01);
    }

    private void applyMagicModifier(ItemMeta meta, UpgradeSlot slot, Bonus bonus, double increment) {
        NamespacedKey key = modifierKey(slot, bonus.attribute());
        double existing = 0.0;

        var modifiers = meta.getAttributeModifiers(bonus.attribute());
        if (modifiers != null) {
            for (AttributeModifier modifier : new ArrayList<>(modifiers)) {
                if (modifier.getKey().equals(key)) {
                    existing += modifier.getAmount();
                    meta.removeAttributeModifier(bonus.attribute(), modifier);
                }
            }
        }

        AttributeModifier updated = new AttributeModifier(
            key,
            existing + increment,
            bonus.operation(),
            slot.group()
        );
        meta.addAttributeModifier(bonus.attribute(), updated);
    }

    private double magicModifierAmount(ItemMeta meta, UpgradeSlot slot, Attribute attribute) {
        NamespacedKey key = modifierKey(slot, attribute);
        double amount = 0.0;
        var modifiers = meta.getAttributeModifiers(attribute);
        if (modifiers == null) {
            return amount;
        }
        for (AttributeModifier modifier : modifiers) {
            if (modifier.getKey().equals(key)) {
                amount += modifier.getAmount();
            }
        }
        return amount;
    }

    private NamespacedKey modifierKey(UpgradeSlot slot, Attribute attribute) {
        return new NamespacedKey(
            plugin,
            "upgrade_" + slot.name().toLowerCase() + "_" + attribute.getKey().getKey()
        );
    }

    private UpgradeSlot selectedSlot(ItemStack book) {
        ItemMeta meta = book.getItemMeta();
        if (meta == null) {
            return UpgradeSlot.HEAD;
        }
        String stored = meta.getPersistentDataContainer().get(selectedSlotKey, PersistentDataType.STRING);
        if (stored == null) {
            return UpgradeSlot.HEAD;
        }
        try {
            return UpgradeSlot.valueOf(stored);
        } catch (IllegalArgumentException ex) {
            return UpgradeSlot.HEAD;
        }
    }

    private void setSelectedSlot(ItemStack book, UpgradeSlot slot) {
        ItemMeta meta = book.getItemMeta();
        if (meta == null) {
            return;
        }
        meta.getPersistentDataContainer().set(selectedSlotKey, PersistentDataType.STRING, slot.name());

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.DARK_GRAY + "Magic Legacy");
        lore.add(ChatColor.GRAY + "Selected slot: " + ChatColor.WHITE + slot.displayName());
        lore.add(ChatColor.GRAY + "Sneak-right-click to cycle the target slot.");
        if (RISKY_ID.equals(SlimefunItemIdentity.idOf(book))) {
            lore.add(ChatColor.GRAY + "Right-click to apply randomized attribute gains.");
            lore.add(ChatColor.GRAY + "Base success: " + ChatColor.WHITE + "99%");
            lore.add(ChatColor.RED + "Failure destroys the off-hand item.");
        } else {
            lore.add(ChatColor.GRAY + "Right-click to apply full attribute gains.");
            lore.add(ChatColor.GRAY + "Success: " + ChatColor.WHITE + "guaranteed");
        }
        lore.add(ChatColor.GRAY + "The book is consumed on an upgrade attempt.");
        meta.setLore(lore);
        book.setItemMeta(meta);
    }

    private static List<Bonus> bonuses(UpgradeSlot slot) {
        return switch (slot) {
            case HEAD -> List.of(
                add(Attribute.MAX_HEALTH, 1.0),
                add(Attribute.ARMOR, 0.2),
                add(Attribute.ARMOR_TOUGHNESS, 0.15),
                add(Attribute.ATTACK_DAMAGE, 0.3)
            );
            case MAIN_HAND -> List.of(
                add(Attribute.ATTACK_DAMAGE, 1.0),
                scalar(Attribute.ATTACK_SPEED, 0.03)
            );
            case OFF_HAND -> List.of(
                add(Attribute.ATTACK_DAMAGE, 1.0),
                scalar(Attribute.MOVEMENT_SPEED, 0.02)
            );
            case CHEST -> List.of(
                add(Attribute.KNOCKBACK_RESISTANCE, 0.01),
                add(Attribute.MAX_HEALTH, 1.5),
                add(Attribute.ARMOR, 0.6),
                add(Attribute.ARMOR_TOUGHNESS, 0.15)
            );
            case LEGS -> List.of(
                add(Attribute.ATTACK_DAMAGE, 0.1),
                add(Attribute.ARMOR, 0.4),
                add(Attribute.ARMOR_TOUGHNESS, 0.15)
            );
            case FEET -> List.of(
                add(Attribute.ARMOR, 0.2),
                add(Attribute.ARMOR_TOUGHNESS, 0.15),
                scalar(Attribute.MOVEMENT_SPEED, 0.02)
            );
        };
    }

    private static ProgressMetric progressMetric(UpgradeSlot slot) {
        return switch (slot) {
            case HEAD -> new ProgressMetric(Attribute.MAX_HEALTH, 2.0);
            case MAIN_HAND -> new ProgressMetric(Attribute.ATTACK_DAMAGE, 10.0);
            case OFF_HAND -> new ProgressMetric(Attribute.ATTACK_DAMAGE, 8.0);
            case CHEST -> new ProgressMetric(Attribute.ARMOR, 3.0);
            case LEGS -> new ProgressMetric(Attribute.ARMOR, 1.2);
            case FEET -> new ProgressMetric(Attribute.ARMOR, 0.6);
        };
    }

    private static Bonus add(Attribute attribute, double amount) {
        return new Bonus(attribute, amount, AttributeModifier.Operation.ADD_NUMBER);
    }

    private static Bonus scalar(Attribute attribute, double amount) {
        return new Bonus(attribute, amount, AttributeModifier.Operation.ADD_SCALAR);
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

    private enum UpgradeSlot {
        HEAD("Head", EquipmentSlotGroup.HEAD),
        CHEST("Chest", EquipmentSlotGroup.CHEST),
        LEGS("Legs", EquipmentSlotGroup.LEGS),
        FEET("Feet", EquipmentSlotGroup.FEET),
        MAIN_HAND("Main Hand", EquipmentSlotGroup.MAINHAND),
        OFF_HAND("Off Hand", EquipmentSlotGroup.OFFHAND);

        private static final UpgradeSlot[] VALUES = values();

        private final String displayName;
        private final EquipmentSlotGroup group;

        UpgradeSlot(String displayName, EquipmentSlotGroup group) {
            this.displayName = displayName;
            this.group = group;
        }

        String displayName() {
            return displayName;
        }

        EquipmentSlotGroup group() {
            return group;
        }

        UpgradeSlot next() {
            return VALUES[(ordinal() + 1) % VALUES.length];
        }
    }

    private record Bonus(Attribute attribute, double amount, AttributeModifier.Operation operation) {
    }

    private record ProgressMetric(Attribute attribute, double threshold) {
    }
}
