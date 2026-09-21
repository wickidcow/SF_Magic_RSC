package io.github.wickidcow.magiclegacy;

import java.io.File;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class MagicLegacyPlugin extends JavaPlugin {

    private PackDeploymentResult deploymentResult;
    private MagicLegacyDiagnostics diagnostics;
    private NativeSpawnerManager nativeSpawnerManager;
    private NativePlainMachineManager nativePlainMachineManager;
    private NativePowerMixManager nativePowerMixManager;
    private NativePlayerAttackManager nativePlayerAttackManager;
    private NativeMagicPlantManager nativeMagicPlantManager;
    private NativeCustomMachineManager nativeCustomMachineManager;
    private NativeInfinityProcessorManager nativeInfinityProcessorManager;

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
        getServer().getPluginManager().registerEvents(new MagicSoundListener(), this);
        getServer().getPluginManager().registerEvents(new MagicLaserStickListener(), this);
        getServer().getPluginManager().registerEvents(new FlameFormationListener(), this);
        getServer().getPluginManager().registerEvents(new SnowFoodListener(), this);
        getServer().getPluginManager().registerEvents(new PlanecupListener(), this);
        getServer().getPluginManager().registerEvents(new BannerLianhunListener(this), this);
        getServer().getPluginManager().registerEvents(new PowerBankListener(), this);
        getServer().getPluginManager().registerEvents(new MobCatcherListener(), this);
        getServer().getPluginManager().registerEvents(new MagicMobSpawnListener(), this);
        getServer().getPluginManager().registerEvents(new TeleportItemListener(this), this);
        getServer().getPluginManager().registerEvents(new EnchantUpgradeListener(), this);
        getServer().getPluginManager().registerEvents(new AttributeUpgradeListener(this), this);
        getServer().getPluginManager().registerEvents(new GenshinBlindBoxListener(), this);
        getServer().getPluginManager().registerEvents(new MagicMusicListener(), this);
        getServer().getPluginManager().registerEvents(new MagicDustPickaxeListener(), this);
        getServer().getPluginManager().registerEvents(new MagicArchaeologyBlockListener(), this);

        getServer().getPluginManager().registerEvents(new StorageSkinFixListener(), this);\n        getServer().getPluginManager().registerEvents(new StorageCompatibilityListener(this), this);\n        getServer().getPluginManager().registerEvents(new MagicFishingListener(), this);\n        nativeSpawnerManager = new NativeSpawnerManager(this);
        nativeSpawnerManager.start();
        nativePlainMachineManager = new NativePlainMachineManager(this);
        nativePlainMachineManager.start();
        nativePowerMixManager = new NativePowerMixManager(this);
        nativePowerMixManager.start();
        nativePlayerAttackManager = new NativePlayerAttackManager(this);
        nativePlayerAttackManager.start();
        nativeMagicPlantManager = new NativeMagicPlantManager(this);
        nativeMagicPlantManager.start();
        nativeCustomMachineManager = new NativeCustomMachineManager(this);
        nativeCustomMachineManager.start();
        nativeInfinityProcessorManager = new NativeInfinityProcessorManager(this);
        nativeInfinityProcessorManager.start();

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

    @Override
    public void onDisable() {
        if (nativeSpawnerManager != null) {
            nativeSpawnerManager.stop();
            nativeSpawnerManager = null;
        }
        if (nativePlainMachineManager != null) {
            nativePlainMachineManager.stop();
            nativePlainMachineManager = null;
        }
        if (nativePowerMixManager != null) {
            nativePowerMixManager.stop();
            nativePowerMixManager = null;
        }
        if (nativePlayerAttackManager != null) {
            nativePlayerAttackManager.stop();
            nativePlayerAttackManager = null;
        }
        if (nativeMagicPlantManager != null) {
            nativeMagicPlantManager.stop();
            nativeMagicPlantManager = null;
        }
        if (nativeCustomMachineManager != null) {
            nativeCustomMachineManager.stop();
            nativeCustomMachineManager = null;
        }
        if (nativeInfinityProcessorManager != null) {
            nativeInfinityProcessorManager.stop();
            nativeInfinityProcessorManager = null;
        }
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
