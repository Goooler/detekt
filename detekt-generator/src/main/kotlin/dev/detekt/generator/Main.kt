@file:JvmName("Main")

package dev.detekt.generator

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.PrintHelpMessage
import com.github.ajalt.clikt.core.parse
import com.github.ajalt.clikt.parameters.options.associate
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import kotlin.system.exitProcess
import kotlin.io.path.Path

fun main(args: Array<String>) {
    val parser = GeneratorCliktCommand()
    try {
        parser.parse(args)
    } catch (ex: PrintHelpMessage) {
        println(parser.getFormattedHelp().orEmpty())
        exitProcess(0)
    }
    val options = parser.toGeneratorArgs()
    options.validate()

    val generator = Generator(
        inputPaths = options.inputPath,
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

    fun toGeneratorArgs(): GeneratorArgs {
        val args = GeneratorArgs()
        args.inputPath = input
            .flatMap { it.split(',', ';') }
            .filter { it.isNotBlank() }
            .map(::Path)
        args.documentationPath = documentation?.let(::Path)
        args.configPath = config?.let(::Path)
        args.generateCustomRuleConfig = generateCustomRuleConfig
        args.textReplacements = replacements
        return args
    }
}
