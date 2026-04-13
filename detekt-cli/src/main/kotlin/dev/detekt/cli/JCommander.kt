package dev.detekt.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.PrintHelpMessage
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.core.parse
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple as multipleArguments
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple as multipleOptions
import com.github.ajalt.clikt.parameters.options.option
import kotlin.io.path.isRegularFile
import kotlin.io.path.notExists

fun parseArguments(args: Array<out String>): CliArgs {
    val parser = DetektCliCommand()
    val usageText by lazy { parser.getFormattedHelp().orEmpty() }

    try {
        parser.parse(args.map { it })
    } catch (ex: PrintHelpMessage) {
        throw HelpRequest(usageText)
    } catch (@Suppress("SwallowedException") ex: UsageError) {
        throw HandledArgumentViolation(ex.message, usageText)
    }

    return try {
        parser.toCliArgs().apply { validate() }
    } catch (ex: HandledArgumentViolation) {
        throw ex
    } catch (ex: CliArgumentValidationException) {
        throw HandledArgumentViolation(ex.message, usageText)
    }
}

private class DetektCliCommand : CliktCommand(name = "detekt") {
    override val printHelpOnEmptyArgs: Boolean = false
    override val treatUnknownOptionsAsArgs: Boolean = true

    private val input by option("--input", "-i").multipleOptions()
    private val analysisMode by option("--analysis-mode").default("light")
    private val includes by option("--includes", "-in")
    private val excludes by option("--excludes", "-ex")
    private val config by option("--config", "-c").multipleOptions()
    private val configResource by option("--config-resource", "-cr").multipleOptions()
    private val generateConfig by option("--generate-config", "-gc")
    private val plugins by option("--plugins", "-p").multipleOptions()
    private val parallel by option("--parallel").flag(default = false)
    private val baseline by option("--baseline", "-b")
    private val createBaseline by option("--create-baseline", "-cb").flag(default = false)
    private val reportPaths by option("--report", "-r").multipleOptions()
    private val failOnSeverity by option("--fail-on-severity")
    private val basePath by option("--base-path", "-bp")
    private val disableDefaultRuleSets by option("--disable-default-rulesets", "-dd").flag(default = false)
    private val buildUponDefaultConfig by option("--build-upon-default-config").flag(default = false)
    private val allRules by option("--all-rules").flag(default = false)
    private val autoCorrect by option("--auto-correct", "-ac").flag(default = false)
    private val debug by option("--debug").flag(default = false)
    private val runRule by option("--run-rule")
    private val classpath by option("--classpath", "-cp").multipleOptions()
    private val apiVersion by option("--api-version")
    private val languageVersion by option("--language-version")
    private val jvmTarget by option("--jvm-target")
    private val jdkHome by option("--jdk-home")
    private val showVersion by option("--version").flag(default = false)
    private val freeCompilerArgs by argument().multipleArguments(required = false)

    override fun run() = Unit

    fun toCliArgs(): CliArgs {
        val cliArgs = CliArgs()

        val parsedInput = input.flatMap(::splitOnCommaOrSemicolon)
        cliArgs.inputPaths = if (parsedInput.isEmpty()) cliArgs.inputPaths else parsedInput.map(::parsePath)
        validateExistingPaths("--input", cliArgs.inputPaths)

        cliArgs.analysisMode = analysisMode.toAnalysisMode()
        cliArgs.includes = includes
        cliArgs.excludes = excludes

        cliArgs.config = config.flatMap(::splitOnCommaOrSemicolon).map(::parsePath)
        validateExistingPaths("--config", cliArgs.config)

        cliArgs.configResource = configResource
            .flatMap(::splitOnCommaOrSemicolon)
            .map(::parseClasspathResource)

        cliArgs.generateConfig = generateConfig?.let(::parsePath)

        cliArgs.plugins = plugins.flatMap(::splitOnCommaOrSemicolon).map(::parsePath)
        validateExistingPaths("--plugins", cliArgs.plugins)

        cliArgs.parallel = parallel
        cliArgs.baseline = baseline?.let(::parsePath)
        cliArgs.createBaseline = createBaseline
        cliArgs.reportPaths = reportPaths.map(::parseReportPath)
        cliArgs.failOnSeverity = failOnSeverity?.let(::parseFailureSeverity) ?: FailureSeverity.Error

        cliArgs.basePath = basePath?.let(::parsePath) ?: cliArgs.basePath
        validateDirectory("--base-path", cliArgs.basePath)

        cliArgs.disableDefaultRuleSets = disableDefaultRuleSets
        cliArgs.buildUponDefaultConfig = buildUponDefaultConfig
        cliArgs.allRules = allRules
        cliArgs.autoCorrect = autoCorrect
        cliArgs.debug = debug
        cliArgs.runRule = runRule

        cliArgs.classpath = classpath.flatMap(::splitOnSystemPathSeparator).map(::parsePath)
        validateExistingPaths("--classpath", cliArgs.classpath)

        cliArgs.apiVersion = apiVersion?.let(::parseApiVersion)
        cliArgs.languageVersion = languageVersion?.let(::parseLanguageVersion)
        cliArgs.jvmTarget = jvmTarget?.let(::parseJvmTarget) ?: cliArgs.jvmTarget
        cliArgs.jdkHome = jdkHome?.let(::parsePath)
        cliArgs.jdkHome?.let { validateDirectory("--jdk-home", it) }
        cliArgs.showVersion = showVersion
        cliArgs.freeCompilerArgs = freeCompilerArgs

        return cliArgs
    }
}

private fun CliArgs.validate() {
    var violation: String? = null
    val baseline = baseline

    if (createBaseline && baseline == null) {
        violation = "Creating a baseline.xml requires the --baseline parameter to specify a path."
    }

    if (!createBaseline && baseline != null) {
        if (baseline.notExists()) {
            violation = "The file specified by --baseline should exist '$baseline'."
        } else if (!baseline.isRegularFile()) {
            violation = "The path specified by --baseline should be a file '$baseline'."
        }
    }

    if (violation != null) {
        val usageText = DetektCliCommand().getFormattedHelp().orEmpty()
        throw HandledArgumentViolation(violation, usageText)
    }
}

private fun String.toAnalysisMode(): dev.detekt.tooling.api.AnalysisMode =
    when (this) {
        "light" -> dev.detekt.tooling.api.AnalysisMode.light
        "full" -> dev.detekt.tooling.api.AnalysisMode.full
        else -> throw CliArgumentValidationException("Invalid value for --analysis-mode: $this")
    }
