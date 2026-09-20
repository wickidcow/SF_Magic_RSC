package io.github.wickidcow.magiclegacy;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class MagicLegacyCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("status", "doctor", "deploy");
    private final MagicLegacyPlugin plugin;

    MagicLegacyCommand(MagicLegacyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(
        @NotNull CommandSender sender,
        @NotNull Command command,
        @NotNull String label,
        @NotNull String[] args
    ) {
        String sub = args.length == 0 ? "status" : args[0].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "status" -> send(sender, plugin.diagnostics().statusLines());
            case "doctor" -> send(sender, plugin.diagnostics().doctorLines());
            case "deploy" -> {
                if (!sender.hasPermission("magiclegacy.admin")) {
                    sender.sendMessage("You do not have permission to redeploy Magic Legacy.");
                    return true;
                }

                PackDeploymentResult result = plugin.redeploy();
                if (result.success()) {
                    sender.sendMessage(
                        "Magic Legacy runtime redeployed (" + result.extractedFiles()
                            + " files). Perform a normal full server restart before testing it."
                    );
                } else {
                    sender.sendMessage("Magic Legacy deployment failed: " + result.message());
                }
            }
            default -> sender.sendMessage("Usage: /" + label + " <status|doctor|deploy>");
        }

        return true;
    }

    private static void send(CommandSender sender, List<String> lines) {
        sender.sendMessage("---- Magic Legacy ----");
        for (String line : lines) {
            sender.sendMessage(line);
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(
        @NotNull CommandSender sender,
        @NotNull Command command,
        @NotNull String alias,
        @NotNull String[] args
    ) {
        if (args.length != 1) {
            return Collections.emptyList();
        }

        String prefix = args[0].toLowerCase(Locale.ROOT);
        return SUBCOMMANDS.stream().filter(value -> value.startsWith(prefix)).toList();
    }
}
