@file:JvmName("Main")

package dev.detekt.generator

import com.github.ajalt.clikt.core.main

fun main(args: Array<String>) {
    val options = GeneratorArgs()
    options.main(args)

    val generator = Generator(
        inputPaths = options.inputPath,
        documentationPath = options.documentationPath,
        configPath = options.configPath,
    )
    if (options.generateCustomRuleConfig) {
        generator.executeCustomRuleConfig()
    } else {
        generator.execute()
    }
}
