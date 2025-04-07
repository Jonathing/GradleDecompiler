/*
 * Copyright (c) Jonathan Colmenares
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package me.jonathing.gradle.decompiler

import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ProviderFactory

import javax.inject.Inject

/**
 * The entry point of the decompiler plugin. This is used to pass plugin-specific service injectors to the extension.
 *
 * @see DecompilerExtension
 */
@CompileStatic
abstract class DecompilerPlugin implements Plugin<Project> {
    @PackageScope static final Logger LOGGER = Logging.getLogger DecompilerPlugin

    /** @see <a href="https://docs.gradle.org/current/userguide/service_injection.html#objectfactory">ObjectFactory Service Injection</a> */
    @Inject abstract ObjectFactory getObjects()
    /** @see <a href="https://docs.gradle.org/current/userguide/service_injection.html#providerfactory">ProviderFactory Service Injection</a> */
    @Inject abstract ProviderFactory getProviders()

    /**
     * Applies the decompiler plugin, which creates and registers the decompiler extension.
     *
     * @param project The project to apply the plugin to
     */
    @Override
    void apply(Project project) {
        project.extensions.add(DecompilerExtension.NAME, new DecompilerExtension(project, this.objects, this.providers))
    }
}
