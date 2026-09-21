package io.github.wickidcow.magiclegacy;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

final class MagicLegacyDiagnostics {

    private static final Map<String, String> RUNTIME_PLUGINS = new LinkedHashMap<>();
    private static final Map<String, List<String>> REGISTRY_CHECKS = new LinkedHashMap<>();

    static {
        RUNTIME_PLUGINS.put("RykenSlimefunCustomizer", "RSC runtime bridge");
        RUNTIME_PLUGINS.put("InfinityExpansion2", "InfinityExpansion2 integration");
        RUNTIME_PLUGINS.put("DynaTech", "DynaTech integration");
        RUNTIME_PLUGINS.put("GeneticChickengineering", "Genetic Chickengineering integration");
        RUNTIME_PLUGINS.put("Supreme", "Supreme integration");
        RUNTIME_PLUGINS.put("FoxyMachines", "FoxyMachines integration");
        RUNTIME_PLUGINS.put("Networks", "Networks optional integration");
        RUNTIME_PLUGINS.put("FNAmplifications", "FNAmplifications optional integration");
        RUNTIME_PLUGINS.put("magicexpansion", "Magic Expansion crossover (optional)");

        REGISTRY_CHECKS.put("Magic Legacy", List.of(
            "MAGIC_VERSION",
            "MAGIC_AUTHOR",
            "MAGIC_COSMIC_DUST_1"
        ));
        REGISTRY_CHECKS.put("InfinityExpansion2", List.of(
            "IE_INFINITY_INGOT",
            "IE_MOB_DATA_CARD_VEX"
        ));
        REGISTRY_CHECKS.put("DynaTech", List.of(
            "DYNATECH_VEX_GEM",
            "DYNATECH_BEE",
            "DYNATECH_GROWTH_CHAMBER_MARK_2"
        ));
        REGISTRY_CHECKS.put("Networks", List.of(
            "NTW_CELL",
            "NTW_BRIDGE"
        ));
        REGISTRY_CHECKS.put("Networks Expansion", List.of(
            "NTW_EXPANSION_ARMOR_FORGE_BLUEPRINT",
            "NTW_EXPANSION_SMELTERY_BLUEPRINT",
            "NTW_EXPANSION_EXPANSION_WORKBENCH_BLUEPRINT",
            "NTW_EXPANSION_QUANTUM_WORKBENCH_BLUEPRINT",
            "NTW_EXPANSION_ANCIENT_ALTAR_BLUEPRINT",
            "NTW_EXPANSION_ADVANCED_AUTO_CRAFTING_WITHHOLDING"
        ));
        REGISTRY_CHECKS.put("FNAmplifications", List.of(
            "FN_MACHINERY_COMPONENT_PART",
            "FN_JUKEBOX_III"
        ));
        REGISTRY_CHECKS.put("Magic Expansion", List.of(
            "RSC_MAGIC_MINER",
            "RSC_MAGIC_REDSTONE",
            "RSC_MAGIC_COSMIC_DUST",
            "RSC_MAGIC_SOUL"
        ));
    }

    private final MagicLegacyPlugin plugin;

    MagicLegacyDiagnostics(MagicLegacyPlugin plugin) {
        this.plugin = plugin;
    }

    List<String> statusLines() {
        List<String> lines = new ArrayList<>();
        PackDeploymentResult deployment = plugin.deploymentResult();

        lines.add("Magic Legacy " + plugin.getDescription().getVersion());
        lines.add("Managed runtime: " + (deployment != null && deployment.success() ? "deployed" : "FAILED"));
        lines.add("Native Java migrations: MAGIC_GUN_1, MAGIC_CHRISTMAS_SNOWBALL, MAGIC_EXP_COLLECTOR, MAGIC_EXP_BOTTLE, MAGIC_UNBREAKABLE_RUNE, MAGIC_INFINITE_STICK, MAGIC_INFINITY_BLADE_1, MAGIC_FOODS_RANDOMFOOD, MAGIC_SOUND, MAGIC_STICK_JIGUANG_1, MAGIC_ZHENFA_FIRE_1, MAGIC_NEW_PLANECUP, MAGIC_BANNER_LIANHUN, MAGIC_BANNER_SOUL, MAGIC_POWER_BANK_ALPHA, MAGIC_POWER_BANK_BETA, mob catcher family, captured-mob spawn family, artificial-mob spawn family, all 132 Magic spawners, MAGIC_FLOWER_MIX_1, MAGIC_GEOMINER, MAGIC_INFINITY_MIX_1, teleport item family, enchant upgrade book family, attribute upgrade book family, Genshin blind box, MAGIC_MUSIC, MAGIC_DUST_PICKAXE, archaeology block family, MAGIC_TEST_WHITE_WOOL, MAGIC_POWER_MIX_1, MAGIC_PLAYER_ATTACT, MAGIC_PLANT_1, snow-food family");
        lines.add(
            "Slimefun item identity bridge: "
                + (SlimefunItemIdentity.available() ? "available" : "unavailable (" + SlimefunItemIdentity.error() + ")")
        );
        lines.add(
            "Slimefun block storage bridge: "
                + (SlimefunBlockStorageBridge.available()
                    ? "available"
                    : "unavailable (" + SlimefunBlockStorageBridge.error() + ")")
        );

        if (deployment != null) {
            lines.add("Runtime path: " + deployment.target());
            lines.add("Embedded files: " + deployment.extractedFiles());
            if (deployment.backup() != null) {
                lines.add("Previous unmanaged pack backup: " + deployment.backup());
            }
        }

        PluginManager manager = plugin.getServer().getPluginManager();
        for (Map.Entry<String, String> entry : RUNTIME_PLUGINS.entrySet()) {
            Plugin found = findPluginIgnoreCase(manager, entry.getKey());
            boolean optional = entry.getValue().contains("(optional)");
            String state;
            if (found == null) {
                boolean jarPresent = pluginJarPresent(entry.getKey());
                if (jarPresent) {
                    state = optional
                        ? "JAR present but plugin not loaded (optional; check earlier startup errors)"
                        : "JAR present but plugin not loaded; check earlier startup errors";
                } else {
                    state = optional ? "not installed (optional)" : "missing";
                }
            } else {
                state = found.isEnabled()
                    ? "enabled (" + found.getName() + " " + found.getDescription().getVersion() + ")"
                    : "installed, not enabled (" + found.getName() + ")";
            }
            lines.add(entry.getValue() + ": " + state);
        }

        return lines;
    }

    List<String> doctorLines() {
        List<String> lines = new ArrayList<>(statusLines());
        PackDeploymentResult deployment = plugin.deploymentResult();

        if (deployment != null) {
            lines.add(
                "Managed marker: "
                    + (Files.isRegularFile(deployment.target().resolve(".magiclegacy-managed")) ? "present" : "missing")
            );
            lines.add(
                "info.yml: "
                    + (Files.isRegularFile(deployment.target().resolve("info.yml")) ? "present" : "missing")
            );
        }

        Plugin networksPlugin = findPluginIgnoreCase(plugin.getServer().getPluginManager(), "Networks");
        if (networksPlugin instanceof org.bukkit.plugin.java.JavaPlugin networksJavaPlugin) {
            boolean expansionEnabled = networksJavaPlugin.getConfig()
                .getBoolean("features.networks-expansion.enabled", true);
            lines.add(
                "Networks Expansion content: "
                    + (expansionEnabled ? "enabled" : "DISABLED in plugins/Networks/config.yml")
            );
        }

        RegistryAccessor registry = RegistryAccessor.create();
        if (!registry.available()) {
            lines.add("Slimefun registry check: unavailable (" + registry.error() + ")");
            return lines;
        }

        for (Map.Entry<String, List<String>> group : REGISTRY_CHECKS.entrySet()) {
            int present = 0;
            List<String> missing = new ArrayList<>();
            for (String id : group.getValue()) {
                if (registry.hasItem(id)) {
                    present++;
                } else {
                    missing.add(id);
                }
            }

            boolean optionalGroup = "Magic Expansion".equals(group.getKey())
                || "Networks Expansion".equals(group.getKey());
            if (optionalGroup && present == 0) {
                String suffix = "Networks Expansion".equals(group.getKey())
                    ? "not installed or disabled (optional; Magic uses stable base-Networks recipes)"
                    : "not installed (optional)";
                lines.add(group.getKey() + " registry: " + suffix);
            } else {
                lines.add(
                    group.getKey() + " registry: " + present + "/" + group.getValue().size()
                        + (missing.isEmpty() ? " present" : " present; missing " + String.join(", ", missing))
                );
            }
        }

        return lines;
    }

    void logStartupReport() {
        for (String line : doctorLines()) {
            plugin.getLogger().info("[doctor] " + line);
        }
    }

    private Plugin findPluginIgnoreCase(PluginManager manager, String name) {
        Plugin direct = manager.getPlugin(name);
        if (direct != null) {
            return direct;
        }
        for (Plugin candidate : manager.getPlugins()) {
            if (candidate.getName().equalsIgnoreCase(name)) {
                return candidate;
            }
        }
        return null;
    }

    private boolean pluginJarPresent(String pluginName) {
        File pluginsDirectory = plugin.getDataFolder().getParentFile();
        File[] files = pluginsDirectory.listFiles();
        if (files == null) {
            return false;
        }

        String normalized = pluginName.replace("-", "").replace("_", "").toLowerCase();
        for (File file : files) {
            if (!file.isFile() || !file.getName().toLowerCase().endsWith(".jar")) {
                continue;
            }
            String candidate = file.getName()
                .replace("-", "")
                .replace("_", "")
                .toLowerCase();
            if (candidate.contains(normalized)) {
                return true;
            }
        }
        return false;
    }

    private record RegistryAccessor(Method getById, String error) {

        static RegistryAccessor create() {
            try {
                Class<?> type = Class.forName("io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem");
                Method method = type.getMethod("getById", String.class);
                return new RegistryAccessor(method, null);
            } catch (ReflectiveOperationException ex) {
                return new RegistryAccessor(null, ex.getClass().getSimpleName() + ": " + ex.getMessage());
            }
        }

        boolean available() {
            return getById != null;
        }

        boolean hasItem(String id) {
            if (getById == null) {
                return false;
            }
            try {
                return getById.invoke(null, id) != null;
            } catch (ReflectiveOperationException ex) {
                return false;
            }
        }
    }
}
