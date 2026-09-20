package io.github.wickidcow.magiclegacy;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

final class MagicArchaeologyBlockListener implements Listener {

    private static final String NO_DROP_ID = "MAGIC_TEST_WHITE_WOOL";
    private static final String SAND_ID = "MAGIC_KAOGU_SAND_1";
    private static final String GRAVEL_ID = "MAGIC_KAOGU_GRAVEL_1";

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

    private static final List<Material> SAND_RARE = List.of(
        Material.ANGLER_POTTERY_SHERD,
        Material.ARCHER_POTTERY_SHERD,
        Material.ARMS_UP_POTTERY_SHERD,
        Material.BREWER_POTTERY_SHERD,
        Material.MINER_POTTERY_SHERD,
        Material.PRIZE_POTTERY_SHERD,
        Material.SHELTER_POTTERY_SHERD,
        Material.SKULL_POTTERY_SHERD,
        Material.SNORT_POTTERY_SHERD,
        Material.SNIFFER_EGG
    );

    private static final List<Material> SAND_COMMON = List.of(
        Material.GOLD_NUGGET,
        Material.EMERALD,
        Material.WHEAT,
        Material.WOODEN_HOE,
        Material.COAL,
        Material.IRON_AXE,
        Material.SUSPICIOUS_STEW,
        Material.BRICK,
        Material.STICK,
        Material.DIAMOND,
        Material.GUNPOWDER,
        Material.TNT
    );

    private static final List<Material> GRAVEL_RARE = List.of(
        Material.PLENTY_POTTERY_SHERD,
        Material.BLADE_POTTERY_SHERD,
        Material.MOURNER_POTTERY_SHERD,
        Material.EXPLORER_POTTERY_SHERD,
        Material.SNORT_POTTERY_SHERD,
        Material.SHELTER_POTTERY_SHERD,
        Material.ANGLER_POTTERY_SHERD,
        Material.HOWL_POTTERY_SHERD,
        Material.BURN_POTTERY_SHERD,
        Material.RAISER_ARMOR_TRIM_SMITHING_TEMPLATE,
        Material.SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE,
        Material.HOST_ARMOR_TRIM_SMITHING_TEMPLATE,
        Material.WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE,
        Material.DANGER_POTTERY_SHERD,
        Material.HEARTBREAK_POTTERY_SHERD,
        Material.FRIEND_POTTERY_SHERD,
        Material.SHEAF_POTTERY_SHERD,
        Material.HEART_POTTERY_SHERD,
        Material.MUSIC_DISC_RELIC,
        Material.SNIFFER_EGG
    );

    private static final List<Material> GRAVEL_COMMON = List.of(
        Material.IRON_AXE,
        Material.EMERALD,
        Material.WHEAT,
        Material.WOODEN_HOE,
        Material.COAL,
        Material.GOLD_NUGGET,
        Material.EMERALD,
        Material.WHEAT,
        Material.WOODEN_HOE,
        Material.CLAY,
        Material.RED_TERRACOTTA,
        Material.YELLOW_DYE,
        Material.BLUE_DYE,
        Material.LIGHT_BLUE_DYE,
        Material.WHITE_DYE,
        Material.ORANGE_DYE,
        Material.GREEN_CANDLE,
        Material.RED_CANDLE,
        Material.PURPLE_CANDLE,
        Material.BROWN_CANDLE,
        Material.MAGENTA_STAINED_GLASS_PANE,
        Material.PINK_STAINED_GLASS_PANE,
        Material.BLUE_STAINED_GLASS_PANE,
        Material.LIGHT_BLUE_STAINED_GLASS_PANE,
        Material.RED_STAINED_GLASS_PANE,
        Material.YELLOW_STAINED_GLASS_PANE,
        Material.PURPLE_STAINED_GLASS_PANE,
        Material.SPRUCE_SIGN,
        Material.OAK_SIGN,
        Material.GOLD_NUGGET,
        Material.COAL,
        Material.WHEAT_SEEDS,
        Material.BEETROOT_SEEDS,
        Material.DEAD_BUSH,
        Material.FLOWER_POT,
        Material.STRING,
        Material.LEAD
    );

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        String id = SlimefunBlockStorageBridge.idAt(block.getLocation());
        if (id == null) {
            return;
        }

        if (NO_DROP_ID.equals(id)) {
            event.setDropItems(false);
            event.getPlayer().sendMessage(ChatColor.AQUA + "Magic Test White Wool was removed without a block drop.");
            return;
        }

        if (SAND_ID.equals(id)) {
            event.setDropItems(false);
            dropArchaeologyLoot(event.getPlayer(), block, SAND_RARE, SAND_COMMON, "Magic Archaeology Sand");
        } else if (GRAVEL_ID.equals(id)) {
            event.setDropItems(false);
            dropArchaeologyLoot(event.getPlayer(), block, GRAVEL_RARE, GRAVEL_COMMON, "Magic Archaeology Gravel");
        }
    }

    private static void dropArchaeologyLoot(
        Player player,
        Block block,
        List<Material> rare,
        List<Material> common,
        String sourceName
    ) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        List<Material> pool = random.nextDouble() < 0.125 ? rare : common;
        Material selected = pool.get(random.nextInt(pool.size()));
        ItemStack vanillaReward = new ItemStack(selected, 1);

        Location drop = block.getLocation().add(0.5, 0.5, 0.5);
        block.getWorld().dropItemNaturally(drop, vanillaReward);

        ItemStack dust = randomSlimefunDust(random);
        int dustAmount = random.nextInt(1, 4);
        if (dust != null) {
            dust.setAmount(dustAmount);
            block.getWorld().dropItemNaturally(drop, dust);
        }

        StringBuilder message = new StringBuilder()
            .append(ChatColor.AQUA).append(sourceName).append(": ")
            .append(ChatColor.WHITE).append(pretty(selected));
        if (dust != null) {
            message.append(ChatColor.GRAY).append(" + ")
                .append(ChatColor.WHITE).append(displayName(dust))
                .append(" x").append(dustAmount);
        } else {
            message.append(ChatColor.YELLOW).append(" (Slimefun dust registry unavailable)");
        }
        player.sendMessage(message.toString());
    }

    private static ItemStack randomSlimefunDust(ThreadLocalRandom random) {
        int start = random.nextInt(DUST_IDS.size());
        for (int offset = 0; offset < DUST_IDS.size(); offset++) {
            String id = DUST_IDS.get((start + offset) % DUST_IDS.size());
            ItemStack item = SlimefunItemIdentity.copyById(id);
            if (item != null) {
                return item;
            }
        }
        return null;
    }

    private static String displayName(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            String display = ChatColor.stripColor(meta.getDisplayName());
            if (display != null && !display.isBlank()) {
                return display;
            }
        }
        return pretty(item.getType());
    }

    private static String pretty(Material material) {
        String raw = material.getKey().getKey().replace('_', ' ');
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
}
