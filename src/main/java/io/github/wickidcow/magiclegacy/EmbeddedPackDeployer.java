package io.github.wickidcow.magiclegacy;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

final class EmbeddedPackDeployer {

    private static final String PREFIX = "embedded/Magic/";
    private static final String MARKER = ".magiclegacy-managed";
    private static final DateTimeFormatter BACKUP_TIME =
        DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC);

    private final MagicLegacyPlugin plugin;

    EmbeddedPackDeployer(MagicLegacyPlugin plugin) {
        this.plugin = plugin;
    }

    PackDeploymentResult deploy() {
        Path plugins = plugin.getDataFolder().toPath().getParent();
        Path addons = plugins.resolve("RykenSlimefunCustomizer").resolve("addons");
        Path target = addons.resolve("Magic");
        Path staging = addons.resolve(".Magic.magiclegacy-staging");
        Path backup = null;

        try {
            Files.createDirectories(addons);
            deleteTree(staging);
            Files.createDirectories(staging);

            int extracted = extract(staging);
            if (extracted < 10 || !Files.isRegularFile(staging.resolve("info.yml"))) {
                throw new IOException("Embedded Magic runtime was incomplete (" + extracted + " files)");
            }

            Files.writeString(
                staging.resolve(MARKER),
                "managed-by=MagicLegacy\nversion=" + plugin.getDescription().getVersion()
                    + "\ndeployed-at=" + Instant.now() + "\n",
                StandardCharsets.UTF_8
            );

            if (Files.exists(target)) {
                if (Files.exists(target.resolve(MARKER))) {
                    deleteTree(target);
                } else {
                    Path backupRoot = plugin.getDataFolder().toPath().resolve("backups");
                    Files.createDirectories(backupRoot);
                    backup = backupRoot.resolve("Magic-before-plugin-" + BACKUP_TIME.format(Instant.now()));
                    Files.move(target, backup);
                    plugin.getLogger().warning(
                        "Existing unmanaged RSC Magic folder was preserved at " + backup
                    );
                }
            }

            move(staging, target);
            return new PackDeploymentResult(
                true,
                target,
                backup,
                extracted,
                "Managed Magic runtime deployed before RSC startup"
            );
        } catch (Exception ex) {
            plugin.getLogger().severe("Could not deploy embedded Magic runtime: " + ex.getMessage());
            try {
                deleteTree(staging);
            } catch (IOException ignored) {
                // Best effort cleanup only.
            }
            return new PackDeploymentResult(false, target, backup, 0, ex.toString());
        }
    }

    private int extract(Path staging) throws Exception {
        int count = 0;
        try (JarFile jar = new JarFile(plugin.pluginFile())) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (!name.startsWith(PREFIX) || name.equals(PREFIX)) {
                    continue;
                }

                String relative = name.substring(PREFIX.length());
                Path output = staging.resolve(relative).normalize();
                if (!output.startsWith(staging)) {
                    throw new IOException("Unsafe embedded path: " + name);
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(output);
                    continue;
                }

                Files.createDirectories(output.getParent());
                try (InputStream in = jar.getInputStream(entry)) {
                    Files.copy(in, output, StandardCopyOption.REPLACE_EXISTING);
                }
                count++;
            }
        }
        return count;
    }

    private static void move(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(source, target);
        }
    }

    private static void deleteTree(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }
}
