package dev.detekt.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.PrintHelpMessage
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.core.parse
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import dev.detekt.tooling.api.AnalysisMode
import dev.detekt.tooling.api.spec.RulesSpec
import dev.detekt.tooling.api.spec.RulesSpec.FailurePolicy.FailOnSeverity
import dev.detekt.tooling.api.spec.RulesSpec.FailurePolicy.NeverFail
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.LanguageVersion
import java.net.URL
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.isRegularFile
import kotlin.io.path.notExists
import com.github.ajalt.clikt.parameters.arguments.multiple as multipleArguments
import com.github.ajalt.clikt.parameters.options.multiple as multipleOptions

@Suppress("ThrowsCount")
fun parseArguments(args: Array<out String>): ParsedCliArguments {
    val parser = DetektCliCommand()
    val usageText by lazy { parser.getFormattedHelp().orEmpty() }

    try {
        parser.parse(args.map { it })
    } catch (@Suppress("SwallowedException") ex: PrintHelpMessage) {
        throw HelpRequest(usageText)
    } catch (@Suppress("SwallowedException") ex: UsageError) {
        throw HandledArgumentViolation(ex.message, usageText)
    }

    return try {
        parser.toParsedArgs().apply { validate() }
    } catch (ex: HandledArgumentViolation) {
        throw ex
    } catch (@Suppress("SwallowedException") ex: CliArgumentValidationException) {
        throw HandledArgumentViolation(ex.message, usageText)
    }
}

data class ParsedCliArguments(
    val inputPaths: List<Path>,
    val analysisMode: AnalysisMode,
    val includes: String?,
    val excludes: String?,
    val config: List<Path>,
    val configResource: List<URL>,
    val generateConfig: Path?,
    val plugins: List<Path>,
    val parallel: Boolean,
    val baseline: Path?,
    val createBaseline: Boolean,
    val reportPaths: List<ReportPath>,
    val failOnSeverity: FailureSeverity,
    val basePath: Path,
    val disableDefaultRuleSets: Boolean,
    val buildUponDefaultConfig: Boolean,
    val allRules: Boolean,
    val autoCorrect: Boolean,
    val debug: Boolean,
    val runRule: String?,
    val classpath: List<Path>,
    val apiVersion: ApiVersion?,
    val languageVersion: LanguageVersion?,
    val jvmTarget: JvmTarget,
    val jdkHome: Path?,
    val showVersion: Boolean,
    val freeCompilerArgs: List<String>,
) {
    val failurePolicy: RulesSpec.FailurePolicy
        get() {
            return when (val minSeverity = failOnSeverity) {
                FailureSeverity.Never -> NeverFail

                FailureSeverity.Error,
                FailureSeverity.Warning,
                FailureSeverity.Info,
                -> FailOnSeverity(minSeverity.toSeverity())
            }
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

    fun toParsedArgs(): ParsedCliArguments {
        val parsedInput = input.flatMap(::splitOnCommaOrSemicolon)
        val inputPaths = if (parsedInput.isEmpty()) {
            listOf(Path(System.getProperty("user.dir")))
        } else {
            parsedInput.map(::parsePath)
        }
        validateExistingPaths("--input", inputPaths)

        val parsedConfig = config.flatMap(::splitOnCommaOrSemicolon).map(::parsePath)
        validateExistingPaths("--config", parsedConfig)

        val parsedResources = configResource
            .flatMap(::splitOnCommaOrSemicolon)
            .map(::parseClasspathResource)

        val parsedGenerateConfig = generateConfig?.let(::parsePath)

        val parsedPlugins = plugins.flatMap(::splitOnCommaOrSemicolon).map(::parsePath)
        validateExistingPaths("--plugins", parsedPlugins)

        val parsedBasePath = basePath?.let(::parsePath) ?: Path(System.getProperty("user.dir"))
        validateDirectory("--base-path", parsedBasePath)

        val parsedClasspath = classpath.flatMap(::splitOnSystemPathSeparator).map(::parsePath)
        validateExistingPaths("--classpath", parsedClasspath)

        val parsedJdkHome = jdkHome?.let(::parsePath)
        parsedJdkHome?.let { validateDirectory("--jdk-home", it) }

        return ParsedCliArguments(
            inputPaths = inputPaths,
            analysisMode = analysisMode.toAnalysisMode(),
            includes = includes,
            excludes = excludes,
            config = parsedConfig,
            configResource = parsedResources,
            generateConfig = parsedGenerateConfig,
            plugins = parsedPlugins,
            parallel = parallel,
            baseline = baseline?.let(::parsePath),
            createBaseline = createBaseline,
            reportPaths = reportPaths.map(::parseReportPath),
            failOnSeverity = failOnSeverity?.let(::parseFailureSeverity) ?: FailureSeverity.Error,
            basePath = parsedBasePath,
            disableDefaultRuleSets = disableDefaultRuleSets,
            buildUponDefaultConfig = buildUponDefaultConfig,
            allRules = allRules,
            autoCorrect = autoCorrect,
            debug = debug,
            runRule = runRule,
            classpath = parsedClasspath,
            apiVersion = apiVersion?.let(::parseApiVersion),
            languageVersion = languageVersion?.let(::parseLanguageVersion),
            jvmTarget = jvmTarget?.let(::parseJvmTarget) ?: JvmTarget.DEFAULT,
            jdkHome = parsedJdkHome,
            showVersion = showVersion,
            freeCompilerArgs = freeCompilerArgs,
        )
    }
}

private fun ParsedCliArguments.validate() {
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
