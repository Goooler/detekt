package dev.detekt.generator

import java.nio.file.Path

data class GeneratorArgs(
    val inputPaths: List<Path>,
    val documentationPath: Path?,
    val configPath: Path?,
    val generateCustomRuleConfig: Boolean,
    val textReplacements: Map<String, String>,
)
