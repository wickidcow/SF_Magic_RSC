package io.github.wickidcow.magiclegacy;

import java.nio.file.Path;

public record PackDeploymentResult(
    boolean success,
    Path target,
    Path backup,
    int extractedFiles,
    String message
) {
}
