package io.gitlab.arturbosch.detekt.core.reporting

import io.gitlab.arturbosch.detekt.api.ConsoleReport
import io.gitlab.arturbosch.detekt.test.TestConfig
import io.gitlab.arturbosch.detekt.test.TestDetektion
import io.gitlab.arturbosch.detekt.test.TestSetupContext
import io.gitlab.arturbosch.detekt.test.createIssue
import org.assertj.core.api.Assertions.assertThat

internal object AutoCorrectableIssueAssert {

    fun isReportNull(report: ConsoleReport) {
        val config = TestConfig("excludeCorrectable" to "true")
        report.init(TestSetupContext(config))
        val correctableCodeSmell = createIssue(autoCorrectEnabled = true)
        val detektionWithCorrectableSmell = TestDetektion(correctableCodeSmell)
        val result = report.render(detektionWithCorrectableSmell)
        assertThat(result).isNull()
    }
}
