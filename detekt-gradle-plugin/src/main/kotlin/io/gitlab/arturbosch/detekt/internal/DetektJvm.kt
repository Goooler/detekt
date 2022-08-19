package io.gitlab.arturbosch.detekt.internal

import io.gitlab.arturbosch.detekt.DetektPlugin
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import java.io.File

internal class DetektJvm(private val project: Project) {
    fun registerTasks(extension: DetektExtension) {
        (project.extensions.getByName("kotlin") as KotlinJvmProjectExtension).sourceSets.all { sourceSet ->
            project.registerJvmDetektTask(extension, sourceSet)
            project.registerJvmCreateBaselineTask(extension, sourceSet)
        }
    }

    private fun Project.registerJvmDetektTask(extension: DetektExtension, sourceSet: KotlinSourceSet) {
        registerDetektTask(DetektPlugin.DETEKT_TASK_NAME + sourceSet.name.capitalize(), extension) {
            source = sourceSet.kotlin
            classpath.setFrom(sourceSet.kotlin.srcDirs.existingFiles())
            // If a baseline file is configured as input file, it must exist to be configured, otherwise the task fails.
            // We try to find the configured baseline or alternatively a specific variant matching this task.
            extension.baseline?.existingVariantOrBaseFile(sourceSet.name)?.let { baselineFile ->
                baseline.set(layout.file(project.provider { baselineFile }))
            }
            setReportOutputConventions(reports, extension, sourceSet.name)
            description = "EXPERIMENTAL: Run detekt analysis for ${sourceSet.name} classes with type resolution"
        }
    }

    private fun Project.registerJvmCreateBaselineTask(extension: DetektExtension, sourceSet: KotlinSourceSet) {
        registerCreateBaselineTask(DetektPlugin.BASELINE_TASK_NAME + sourceSet.name.capitalize(), extension) {
            source = sourceSet.kotlin
            classpath.setFrom(sourceSet.kotlin.srcDirs.existingFiles())
            val variantBaselineFile = extension.baseline?.addVariantName(sourceSet.name)
            baseline.set(project.layout.file(project.provider { variantBaselineFile }))
            description = "EXPERIMENTAL: Creates detekt baseline for ${sourceSet.name} classes with type resolution"
        }
    }

    private fun Set<File>.existingFiles() = filter { it.exists() }
}
