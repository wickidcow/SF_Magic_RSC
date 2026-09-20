package io.github.wickidcow.magiclegacy;

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
        RUNTIME_PLUGINS.put("magicexpansion", "Magic Expansion crossover");

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
        lines.add("Native Java migrations: MAGIC_GUN_1, MAGIC_CHRISTMAS_SNOWBALL, MAGIC_EXP_COLLECTOR, MAGIC_EXP_BOTTLE, MAGIC_UNBREAKABLE_RUNE");
        lines.add(
            "Slimefun item identity bridge: "
                + (SlimefunItemIdentity.available() ? "available" : "unavailable (" + SlimefunItemIdentity.error() + ")")
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
            Plugin found = manager.getPlugin(entry.getKey());
            lines.add(
                entry.getValue() + ": "
                    + (found == null ? "missing" : (found.isEnabled() ? "enabled" : "installed, not enabled"))
            );
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

            lines.add(
                group.getKey() + " registry: " + present + "/" + group.getValue().size()
                    + (missing.isEmpty() ? " present" : " present; missing " + String.join(", ", missing))
            );
        }

        return lines;
    }

    void logStartupReport() {
        for (String line : doctorLines()) {
            plugin.getLogger().info("[doctor] " + line);
        }
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
