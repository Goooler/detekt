@file:JvmName("Main")

package dev.detekt.generator

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.PrintHelpMessage
import com.github.ajalt.clikt.core.parse
import com.github.ajalt.clikt.parameters.options.associate
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import java.nio.file.Path
import kotlin.system.exitProcess
import kotlin.io.path.Path as pathOf
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

fun main(args: Array<String>) {
    val parser = GeneratorCliktCommand()
    try {
        parser.parse(args)
    } catch (ex: PrintHelpMessage) {
        println(parser.getFormattedHelp().orEmpty())
        exitProcess(0)
    }
    val options = parser.toOptions()
    options.validate()

    val generator = Generator(
        inputPaths = options.inputPaths,
        textReplacements = options.textReplacements,
        documentationPath = options.documentationPath,
        configPath = options.configPath,
    )
    if (options.generateCustomRuleConfig) {
        generator.executeCustomRuleConfig()
    } else {
        generator.execute()
    }
}

private class GeneratorCliktCommand : CliktCommand(name = "detekt-generator") {
    override val printHelpOnEmptyArgs: Boolean = false
    private val input by option("--input", "-i").multiple(required = true)
    private val documentation by option("--documentation", "-d")
    private val config by option("--config", "-c")
    private val generateCustomRuleConfig by option("--generate-custom-rule-config", "-gcrc").flag(default = false)
    private val replacements by option("--replace", "-r").associate()

    override fun run() = Unit

    fun toOptions(): GeneratorOptions =
        GeneratorOptions(
            inputPaths = input
            .flatMap { it.split(',', ';') }
            .filter { it.isNotBlank() }
            .map(::pathOf),
            documentationPath = documentation?.let(::pathOf),
            configPath = config?.let(::pathOf),
            generateCustomRuleConfig = generateCustomRuleConfig,
            textReplacements = replacements,
        )
}

private data class GeneratorOptions(
    val inputPaths: List<Path>,
    val documentationPath: Path?,
    val configPath: Path?,
    val generateCustomRuleConfig: Boolean,
    val textReplacements: Map<String, String>,
)

private fun GeneratorOptions.validate() {
    inputPaths.forEach {
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
