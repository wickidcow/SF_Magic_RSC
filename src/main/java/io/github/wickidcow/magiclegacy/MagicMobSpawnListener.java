package io.github.wickidcow.magiclegacy;

import java.util.LinkedHashMap;
import java.util.Map;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

final class MagicMobSpawnListener implements Listener {

    private static final Map<String, SpawnSpec> SPAWNS = new LinkedHashMap<>();

    static {
        animal("MAGIC_EGG_BEE", EntityType.BEE);
        animal("MAGIC_PIG_1", EntityType.PIG);
        animal("MAGIC_SHEEP_1", EntityType.SHEEP);
        animal("MAGIC_CHICKEN_1", EntityType.CHICKEN);
        animal("MAGIC_COW_1", EntityType.COW);
        animal("MAGIC_OCELOT_1", EntityType.OCELOT);
        animal("MAGIC_CAT_1", EntityType.CAT);
        animal("MAGIC_DONKEY_1", EntityType.DONKEY);
        animal("MAGIC_FOX_1", EntityType.FOX);
        animal("MAGIC_FROG_1", EntityType.FROG);
        animal("MAGIC_GOAT_1", EntityType.GOAT);
        animal("MAGIC_HOGLIN_1", EntityType.HOGLIN);
        animal("MAGIC_HORSE_1", EntityType.HORSE);
        animal("MAGIC_LLAMA_1", EntityType.LLAMA);
        animal("MAGIC_TRADER_LLAMA_1", EntityType.TRADER_LLAMA);
        animal("MAGIC_MOOSHROOM_1", EntityType.MOOSHROOM);
        animal("MAGIC_MULE_1", EntityType.MULE);
        animal("MAGIC_PANDA_1", EntityType.PANDA);
        animal("MAGIC_RABBIT_1", EntityType.RABBIT);
        animal("MAGIC_STRIDER_1", EntityType.STRIDER);
        animal("MAGIC_TURTLE_1", EntityType.TURTLE);
        animal("MAGIC_WOLF_1", EntityType.WOLF);

        artificial("MAGIC_EGG_IRON_GOLEM", EntityType.IRON_GOLEM, "Magic Artificial Iron Golem");
        artificial("MAGIC_ARTIFICIAL_WITHER_SKELETON", EntityType.WITHER_SKELETON, "Magic Artificial Wither Skeleton");
        artificial("MAGIC_ARTIFICIAL_SKELETON", EntityType.SKELETON, "Magic Artificial Skeleton");
        artificial("MAGIC_ARTIFICIAL_CREEPER", EntityType.CREEPER, "Magic Artificial Creeper");
        artificial("MAGIC_ARTIFICIAL_ZOMBIE", EntityType.ZOMBIE, "Magic Artificial Zombie");
        artificial("MAGIC_ARTIFICIAL_GIANT", EntityType.GIANT, "Magic Artificial Giant");
        SPAWNS.put("MAGIC_ALLAY_1", new SpawnSpec(EntityType.ALLAY, false, null, 5));
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || !isRightClick(event.getAction())) {
            return;
        }

        ItemStack item = event.getItem();
        SpawnSpec spec = SPAWNS.get(SlimefunItemIdentity.idOf(item));
        if (spec == null) {
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();
        Block target = event.getClickedBlock();
        if (target == null) {
            target = player.getTargetBlockExact(spec.range());
        }
        if (target == null) {
            player.sendMessage(ChatColor.AQUA + "Aim at a block to release this Magic mob.");
            return;
        }

        Location location = target.getLocation().add(0.5, 1.0, 0.5);
        Entity entity;
        try {
            entity = location.getWorld().spawnEntity(location, spec.type());
        } catch (RuntimeException ex) {
            player.sendMessage(ChatColor.RED + "That Magic mob could not be spawned here.");
            return;
        }

        if (spec.spawnAsBaby() && entity instanceof Ageable ageable) {
            ageable.setAge(-24000);
        }

        if (spec.customName() != null && !spec.customName().isBlank()) {
            entity.setCustomName(ChatColor.LIGHT_PURPLE + spec.customName());
            entity.setCustomNameVisible(true);
        }

        consumeOne(player, item);
        player.sendMessage(
            ChatColor.AQUA + "Released " + ChatColor.WHITE + pretty(spec.type()) + ChatColor.GRAY + "."
        );
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

    private static void animal(String itemId, EntityType type) {
        SPAWNS.put(itemId, new SpawnSpec(type, true, null, 1));
    }

    private static void artificial(String itemId, EntityType type, String name) {
        SPAWNS.put(itemId, new SpawnSpec(type, false, name, 5));
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

    private record SpawnSpec(EntityType type, boolean spawnAsBaby, String customName, int range) {
    }
}
