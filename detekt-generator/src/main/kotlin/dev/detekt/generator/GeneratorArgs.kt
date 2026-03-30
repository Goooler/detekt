package dev.detekt.generator

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.validate
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

class GeneratorArgs : CliktCommand() {

    val inputPath: List<Path> by option(
        "--input", "-i",
        help = "Input paths to analyze."
    ).convert { value ->
        value.split(',', ';').map { Path(it) }
    }.required()
        .validate { paths ->
            for (path in paths) {
                if (!path.exists()) fail("Input path does not exist: $path")
            }
        }

    val documentationPath: Path? by option(
        "--documentation", "-d",
        help = "Output path for generated documentation."
    ).convert { Path(it) }
        .validate { path ->
            if (path.exists() && !path.isDirectory()) fail("Value passed to --documentation must be a directory.")
        }

    val configPath: Path? by option(
        "--config", "-c",
        help = "Output path for generated detekt config."
    ).convert { Path(it) }
        .validate { path ->
            if (path.exists() && !path.isDirectory()) fail("Value passed to --config must be a directory.")
        }

    val generateCustomRuleConfig: Boolean by option(
        "--generate-custom-rule-config", "-gcrc",
        help = "Generate config for user-defined rules. " +
            "Path to user rules can be specified with --input option"
    ).flag()

    private val rawTextReplacements: List<String> by option(
        "--replace", "-r",
        help = "Any number of key and value pairs that are used to replace placeholders " +
            "during data collection and output generation. Key and value are separated by '='. " +
            "The property may be used multiple times."
    ).multiple()

    val textReplacements: Map<String, String>
        get() = rawTextReplacements.associate { entry ->
            val idx = entry.indexOf('=')
            require(idx >= 0) { "Expected key=value format for --replace option, got: '$entry'" }
            entry.substring(0, idx) to entry.substring(idx + 1)
        }

    override fun run() = Unit
}
