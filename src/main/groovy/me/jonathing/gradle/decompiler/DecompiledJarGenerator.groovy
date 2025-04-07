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
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault

import javax.inject.Inject

/**
 * Generates a decompiled JAR from the input artifact JAR using the given decompiler.
 * <p>The decompiler used is a parameter set by the extension By default it is the latest release of VineFlower at the
 * time of publication.</p>
 *
 * @see DecompiledJarGenerator.Parameters#getDecompilerArtifact()
 * @see DecompilerExtension#decompiler
 * @see <a href="https://vineflower.org/">VineFlower</a>
 * @apiNote This artifact transformer does not actually transform any inputs, but rather uses the input to generate a sources artifact that is then placed next to it prefixed with {@code '-sources'}.
 */
@CompileStatic
@DisableCachingByDefault(because = 'Not yet implemented')
abstract class DecompiledJarGenerator implements TransformAction<Parameters> {
    /**
     * The parameters used by the decompiled jar generator.
     *
     * @see DecompiledJarGenerator
     */
    interface Parameters extends TransformParameters {
        /**
         * The name of the decompiler to use. This is the maven coordinate of the decompiler set from the extension.
         *
         * @return The name of the decompiler (as a property)
         * @see #getDecompilerArtifact()
         */
        abstract @Input Property<String> getDecompilerName()

        /**
         * The decompiler to use. From the extension, this is set by using a detached configuration containing the
         * requested decompiler.
         *
         * @return The decompiler JAR file (as a property)
         * @see #getDecompilerName()
         * @see <a href="https://docs.gradle.org/current/kotlin-dsl/gradle/org.gradle.api.artifacts/-configuration-container/detached-configuration.html">ConfigurationContainer.detachedConfiguration(Dependency...)</a>
         */
        abstract @InputFile RegularFileProperty getDecompilerArtifact()

        /**
         * Whether the decompiler should add a comment banner to the decompiled sources. This is set from the extension.
         *
         * @return Whether to add a comment banner (as a property)
         * @see DecompilerExtension#useCommentBanner
         */
        abstract @Input @Optional Property<Boolean> getUseCommentBanner()

        /**
         * Whether the decompiler should strip non-class files from the decompiled sources. This is set from the
         * extension.
         *
         * @return Whether to strip non-class files (as a property)
         * @see DecompilerExtension#strip
         */
        abstract @Input @Optional Property<Boolean> getStrip()
    }

    private static final Logger LOGGER = Logging.getLogger DecompiledJarGenerator

    /** @see <a href="https://docs.gradle.org/current/userguide/service_injection.html#objectfactory">ObjectFactory Service Injection</a> */
    @Inject abstract ObjectFactory getObjects()
    /** @see <a href="https://docs.gradle.org/current/userguide/service_injection.html#execoperations">ExecOperations Service Injection</a> */
    @Inject abstract ExecOperations getExecOperations()

    /** The artifact to generate the decompiled sources JAR for. */
    abstract @InputArtifact Provider<FileSystemLocation> getInputArtifact()

    /**
     * Generates the decompiled JAR from the input artifact using the given decompiler. This does not transform the
     * input artifact.
     *
     * @param outputs The outputs of the transform action
     */
    @Override
    void transform(TransformOutputs outputs) {
        var input = this.inputArtifact.get().asFile
        outputs.file input

        var sources = this.objects.fileProperty().value(
            this.objects.directoryProperty().fileValue(input.parentFile) // get the parent directory of the input artifact
                .file input.name.replace('.jar', '-sources.jar') // get the sources JAR file location
        )

        // If we already have a sources JAR, skip and immediately return. Currently, this is our only form of caching.
        // TODO [GradleDecompiler] Implement Gradle-compliant caching
        if (sources.get().asFile.exists()) return

        LOGGER.lifecycle 'Decompiling {}', input.name

        var result = this.execOperations.javaexec { exec ->
            // Silences the decompiler output
            exec.standardOutput = new ByteArrayOutputStream()
            exec.errorOutput = new ByteArrayOutputStream()

            exec.classpath = this.objects.fileCollection().from this.parameters.decompilerArtifact.get()
            exec.args = new ArrayList<String>().tap { args ->
                if (this.parameters.useCommentBanner.getOrElse false)
                    args.add "--banner=/*\n * Decompiled by GradleDecompiler using ${this.parameters.decompilerName.get()}\n * https://github.com/Jonathing/GradleDecompiler\n */\n".toString()

                if (this.parameters.strip.getOrElse false)
                    args.add '--skip-extra-files=true'

                args.add '--file'
                args.add input.absolutePath
                args.add sources.locationOnly.get().asFile.absolutePath
            }
        }

        if (result.exitValue != 0)
            LOGGER.error 'ERROR: Failed to decompile {}', input.name
    }
}
