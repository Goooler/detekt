package dev.detekt.generator

import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

class GeneratorArgs {
    var inputPath: List<Path> = emptyList()

    var documentationPath: Path? = null

    var configPath: Path? = null

    var help: Boolean = false

    var generateCustomRuleConfig: Boolean = false

    var textReplacements: Map<String, String> = mutableMapOf()

    fun validate() {
        inputPath.forEach {
            if (!it.exists()) error("Input path does not exist: $it")
        }
        documentationPath?.let {
            if (it.exists() && !it.isDirectory()) {
                error("Value passed to --documentation must be a directory.")
            }
        }
        configPath?.let {
            if (it.exists() && !it.isDirectory()) {
                error("Value passed to --config must be a directory.")
            }
        }
    }
}
