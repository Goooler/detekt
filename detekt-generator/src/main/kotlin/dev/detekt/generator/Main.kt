@file:JvmName("Main")

package dev.detekt.generator

import com.github.ajalt.clikt.core.PrintHelpMessage
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val options = GeneratorArgs()
    try {
        options.parse(args.toList())
    } catch (e: PrintHelpMessage) {
        println(e.command.getFormattedHelp())
        exitProcess(0)
    }

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
