package io.github.wickidcow.magiclegacy;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

final class SnowFoodListener implements Listener {

    private static final Map<String, EffectSpec> EFFECTS = new LinkedHashMap<>();

    static {
        EFFECTS.put("MAGIC_FOODS_JIAOZI", new EffectSpec("glowing", "Glowing"));
        EFFECTS.put("MAGIC_FOODS_APPLE", new EffectSpec("speed", "Speed"));
        EFFECTS.put("MAGIC_FOODS_A_1", new EffectSpec("fire_resistance", "Fire Resistance"));
        EFFECTS.put("MAGIC_FOODS_A_2", new EffectSpec("health_boost", "Health Boost"));
        EFFECTS.put("MAGIC_FOODS_A_3", new EffectSpec("invisibility", "Invisibility"));
        EFFECTS.put("MAGIC_FOODS_A_4", new EffectSpec("levitation", "Levitation"));
        EFFECTS.put("MAGIC_FOODS_A_5", new EffectSpec("saturation", "Saturation"));
        EFFECTS.put("MAGIC_FOODS_A_6", new EffectSpec("weakness", "Weakness"));
        EFFECTS.put("MAGIC_FOODS_A_7", new EffectSpec("conduit_power", "Conduit Power"));
        EFFECTS.put("MAGIC_FOODS_A_8", new EffectSpec("absorption", "Absorption"));
        EFFECTS.put("MAGIC_FOODS_A_9", new EffectSpec("regeneration", "Regeneration"));
        EFFECTS.put("MAGIC_FOODS_A_10", new EffectSpec("luck", "Luck"));
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        String itemId = SlimefunItemIdentity.idOf(event.getItem());
        EffectSpec spec = EFFECTS.get(itemId);
        if (spec == null) {
            return;
        }

        PotionEffectType type = Registry.EFFECT.get(NamespacedKey.minecraft(spec.key()));
        if (type == null) {
            event.getPlayer().sendMessage(ChatColor.RED + "This Magic food effect is unavailable on this server version.");
            return;
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        int amplifier = random.nextInt(50);
        int level = amplifier + 1;
        int durationSeconds = random.nextInt(1, 101);

        Player player = event.getPlayer();
        player.addPotionEffect(new PotionEffect(type, durationSeconds * 20, amplifier, true, true, true));
        player.sendMessage(
            ChatColor.AQUA + spec.displayName()
                + " " + level
                + ChatColor.GRAY + " for "
                + ChatColor.AQUA + durationSeconds + "s"
        );
    }

    private record EffectSpec(String key, String displayName) {
    }
}
