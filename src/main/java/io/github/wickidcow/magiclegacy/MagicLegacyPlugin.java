package io.github.wickidcow.magiclegacy;

import java.io.File;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class MagicLegacyPlugin extends JavaPlugin {

    private PackDeploymentResult deploymentResult;
    private MagicLegacyDiagnostics diagnostics;

    @Override
    public void onLoad() {
        deploymentResult = new EmbeddedPackDeployer(this).deploy();
    }

    @Override
    public void onEnable() {
        diagnostics = new MagicLegacyDiagnostics(this);
        getServer().getPluginManager().registerEvents(new MagicGunListener(), this);
        getServer().getPluginManager().registerEvents(new ChristmasSnowballListener(), this);
        getServer().getPluginManager().registerEvents(new ExperienceItemListener(), this);
        getServer().getPluginManager().registerEvents(new UnbreakableRuneListener(), this);
        getServer().getPluginManager().registerEvents(new MagicWeaponListener(), this);
        getServer().getPluginManager().registerEvents(new RandomFoodListener(), this);

        PluginCommand command = getCommand("magiclegacy");
        if (command != null) {
            MagicLegacyCommand handler = new MagicLegacyCommand(this);
            command.setExecutor(handler);
            command.setTabCompleter(handler);
        }

        getServer().getScheduler().runTask(this, () -> {
            diagnostics.logStartupReport();
            if (getServer().getPluginManager().getPlugin("RykenSlimefunCustomizer") == null) {
                getLogger().warning(
                    "RykenSlimefunCustomizer is not installed. Magic Legacy 2.0 currently uses "
                        + "the managed RSC runtime while scripted systems are migrated to native Java."
                );
            }
        });
    }

    File pluginFile() {
        return getFile();
    }

    PackDeploymentResult deploymentResult() {
        return deploymentResult;
    }

    MagicLegacyDiagnostics diagnostics() {
        return diagnostics;
    }

    PackDeploymentResult redeploy() {
        deploymentResult = new EmbeddedPackDeployer(this).deploy();
        diagnostics = new MagicLegacyDiagnostics(this);
        return deploymentResult;
    }
}
