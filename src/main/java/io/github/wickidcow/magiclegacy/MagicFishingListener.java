package io.github.wickidcow.magiclegacy;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

final class MagicFishingListener implements Listener {

    private static final String ROD_ID = "MAGIC_ROD_ZMZ_WWS";
    private static final String CORE_BAIT_ID = "MAGIC_ROD_ZMZ_WWS_YE_HSJ";
    private static final String WILDCARD_BAIT_ID = "MAGIC_ROD_ZMZ_WWS_YE_YWD";

    private static final Set<String> RESTRICTED_KEYWORDS = Set.of(
        "伪物",
        "矩阵",
        "创造者",
        "创世",
        "腐竹",
        "不可控空生成器",
        "50重压缩原石生成器",
        "过载加速器",
        "刷怪笼",
        "压缩基岩",
        "贪婪矩阵-iii",
        "至尊",
        "熵",
        "壳",
        "螺旋体",
        "奇点",
        "终焉",
        "魔法糖",
        "修罗",
        "压缩",
        "matrix",
        "creator",
        "genesis",
        "spawner",
        "compressed bedrock",
        "overclock",
        "supreme",
        "entropy",
        "singularity",
        "magic sugar"
    );

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
            return;
        }

        Player player = event.getPlayer();
        if (!SlimefunItemIdentity.is(player.getInventory().getItemInMainHand(), ROD_ID)) {
            return;
        }

        Entity caught = event.getCaught();
        if (caught == null) {
            return;
        }

        PlayerInventory inventory = player.getInventory();
        ItemStack offHand = inventory.getItemInOffHand();

        RewardSelection selection;
        if (SlimefunItemIdentity.is(offHand, CORE_BAIT_ID)) {
            ItemStack reward = chooseCoreReward();
            if (reward == null) {
                player.sendMessage(ChatColor.RED + "Magic fishing could not find an eligible Slimefun reward.");
                return;
            }
            selection = new RewardSelection(reward, () -> consumeOffHand(inventory, offHand));
        } else {
            int wildcardSlot = findWildcardBaitSlot(inventory);
            if (wildcardSlot < 0) {
                return;
            }

            ItemStack reward = chooseWildcardReward();
            if (reward == null) {
                player.sendMessage(ChatColor.RED + "Magic fishing could not find an eligible Slimefun reward.");
                return;
            }
            selection = new RewardSelection(reward, () -> consumeSlot(inventory, wildcardSlot));
        }

        selection.consume().run();
        caught.remove();
        event.setExpToDrop(0);

        ItemStack reward = selection.reward();
        reward.setAmount(1);

        org.bukkit.entity.Item dropped = player.getWorld().dropItem(event.getHook().getLocation(), reward);
        dropped.setPickupDelay(2);

        var direction = player.getEyeLocation().toVector().subtract(dropped.getLocation().toVector());
        if (direction.lengthSquared() > 0.0001) {
            dropped.setVelocity(direction.normalize().multiply(1.7));
        }

        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);
        player.sendMessage(
            ChatColor.AQUA + "Magic fishing reward: "
                + ChatColor.WHITE + displayName(reward)
                + ChatColor.AQUA + " x1"
        );
    }

    private static ItemStack chooseCoreReward() {
        List<SlimefunItem> core = coreItems();
        return copyRandom(core);
    }

    private static ItemStack chooseWildcardReward() {
        List<SlimefunItem> all = enabledItems();
        if (all.isEmpty()) {
            return null;
        }

        List<SlimefunItem> core = coreItems(all);
        List<SlimefunItem> pool =
            ThreadLocalRandom.current().nextInt(100) < 10 || core.isEmpty() ? all : core;

        ItemStack chosen = copyRandom(pool);
        if (chosen == null) {
            return null;
        }

        if (isRestricted(chosen) && ThreadLocalRandom.current().nextInt(100) < 99) {
            ItemStack fallback = SlimefunItemIdentity.copyById(WILDCARD_BAIT_ID);
            return fallback == null ? chosen : fallback;
        }

        return chosen;
    }

    private static List<SlimefunItem> enabledItems() {
        return new ArrayList<>(Slimefun.getRegistry().getEnabledSlimefunItems());
    }

    private static List<SlimefunItem> coreItems() {
        return coreItems(enabledItems());
    }

    private static List<SlimefunItem> coreItems(List<SlimefunItem> all) {
        List<SlimefunItem> core = new ArrayList<>();
        for (SlimefunItem item : all) {
            try {
                if ("Slimefun".equalsIgnoreCase(item.getAddon().getName())) {
                    core.add(item);
                }
            } catch (RuntimeException ignored) {
                // An incompletely registered addon item must never break fishing.
            }
        }
        return core;
    }

    private static ItemStack copyRandom(List<SlimefunItem> items) {
        if (items.isEmpty()) {
            return null;
        }

        SlimefunItem item = items.get(ThreadLocalRandom.current().nextInt(items.size()));
        ItemStack stack = item.getItem().clone();
        stack.setAmount(1);
        return stack;
    }

    private static int findWildcardBaitSlot(PlayerInventory inventory) {
        ItemStack[] contents = inventory.getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            if (SlimefunItemIdentity.is(contents[slot], WILDCARD_BAIT_ID)) {
                return slot;
            }
        }
        return -1;
    }

    private static void consumeOffHand(PlayerInventory inventory, ItemStack stack) {
        if (stack.getAmount() > 1) {
            stack.setAmount(stack.getAmount() - 1);
        } else {
            inventory.setItemInOffHand(null);
        }
    }

    private static void consumeSlot(PlayerInventory inventory, int slot) {
        ItemStack stack = inventory.getItem(slot);
        if (stack == null) {
            return;
        }

        if (stack.getAmount() > 1) {
            stack.setAmount(stack.getAmount() - 1);
        } else {
            inventory.setItem(slot, null);
        }
    }

    private static boolean isRestricted(ItemStack stack) {
        String plain = displayName(stack).toLowerCase(Locale.ROOT);
        for (String keyword : RESTRICTED_KEYWORDS) {
            if (plain.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private static String displayName(ItemStack stack) {
        ItemMeta meta = stack.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            String stripped = ChatColor.stripColor(meta.getDisplayName());
            if (stripped != null && !stripped.isBlank()) {
                return stripped;
            }
        }

        return stack.getType().getKey().getKey().replace('_', ' ');
    }

    private record RewardSelection(ItemStack reward, Runnable consume) {
    }
}
