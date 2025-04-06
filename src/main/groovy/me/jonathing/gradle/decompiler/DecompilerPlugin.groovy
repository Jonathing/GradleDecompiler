/*
 * Copyright (c) Jonathan Colmenares
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package me.jonathing.gradle.decompiler

import groovy.transform.CompileStatic
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ProviderFactory

import javax.inject.Inject

@CompileStatic
abstract class DecompilerPlugin implements Plugin<Project> {
    @Inject abstract ObjectFactory getObjects()
    @Inject abstract ProviderFactory getProviders()

    @Override
    void apply(Project project) {
        project.extensions.add(DecompilerExtension.NAME, new DecompilerExtension(project, this.objects, this.providers))
    }
}
