/*
 * Copyright (c) Jonathan Colmenares
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package me.jonathing.gradle.decompiler

import groovy.transform.CompileStatic
import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.InputFile
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault

import javax.inject.Inject

// TODO [GradleDecompiler] Implement Gradle-compliant caching
@CompileStatic
@DisableCachingByDefault(because = 'Not yet implemented')
abstract class DecompiledJarGenerator implements TransformAction<Parameters> {
    interface Parameters extends TransformParameters {
        abstract @InputFile RegularFileProperty getDecompiler()
    }

    private static final Logger LOGGER = Logging.getLogger DecompiledJarGenerator

    @Inject abstract ObjectFactory getObjects()
    @Inject abstract ExecOperations getExecOperations()

    abstract @InputArtifact Provider<FileSystemLocation> getInputArtifact()

    @Override
    void transform(TransformOutputs outputs) {
        var input = this.inputArtifact.get().asFile
        outputs.file input

        var directory = this.objects.directoryProperty().fileValue input.parentFile
        var sources = this.objects.fileProperty().value(
            directory.file input.name.replace('.jar', '-sources.jar')
        )

        if (sources.get().asFile.exists()) return

        LOGGER.lifecycle 'Decompiling {}', input.name

        var result = this.execOperations.javaexec { exec ->
            exec.standardOutput = new ByteArrayOutputStream()
            exec.errorOutput = new ByteArrayOutputStream()
            exec.classpath = this.objects.fileCollection().from this.parameters.decompiler.get()
            exec.args = [
                '--banner=', 'Decompiled with VineFlower using GradleDecompiler',
                '--skip-extra-files', 'true',
                '--file',
                this.inputArtifact.get().asFile.absolutePath,
                sources.locationOnly.get().asFile.absolutePath
            ]
        }

        if (result.exitValue != 0)
            LOGGER.error 'ERROR: Failed to decompile {}', input.name
    }
}
