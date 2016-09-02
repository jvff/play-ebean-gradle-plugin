package com.janitovff.play.ebean.gradle.plugins

import org.gradle.api.file.FileCollection
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.jvm.plugins.JvmComponentPlugin
import org.gradle.jvm.JarBinarySpec
import org.gradle.jvm.JvmLibrarySpec
import org.gradle.jvm.tasks.Jar
import org.gradle.language.java.JavaSourceSet
import org.gradle.language.scala.tasks.PlatformScalaCompile
import org.gradle.model.Each
import org.gradle.model.ModelMap
import org.gradle.model.Mutate
import org.gradle.model.Path
import org.gradle.model.RuleSource
import org.gradle.platform.base.ComponentSpec
import org.gradle.play.PlayApplicationSpec
import org.gradle.play.plugins.PlayPluginConfigurations

import com.janitovff.play.ebean.gradle.tasks.EnhanceEbeanEntitiesTask

class PlayEbeanGradlePlugin implements Plugin<Project> {
    public void apply(Project project) {
        project.getPluginManager().apply(JvmComponentPlugin)
    }

    static class Rules extends RuleSource {
        @Mutate
        void createEbeanEntitiesComponent(
                @Path("components") ModelMap<ComponentSpec> components) {
            components.create("ebeanEntities", JvmLibrarySpec)
        }

        @Mutate
        void configureEbeanEntitiesSourceSet(
                @Path("components.ebeanEntities.sources.java")
                        JavaSourceSet sourceSet,
                @Path("components.play") PlayApplicationSpec playComponent,
                PlayPluginConfigurations configurations) {
            JavaSourceSet javaSourceSet = playComponent.getSources().get("java")

            sourceSet.getSource().srcDirs = javaSourceSet.getSource().srcDirs
            sourceSet.getSource().include("models/**/*.java")

            String playJavaDependency = 'com.typesafe.play:play-java_2.11:2.3.9'

            sourceSet.dependencies.module 'org.avaje.ebean:ebean:8.2.3'
            sourceSet.dependencies.module playJavaDependency
        }

        @Mutate
        void ignoreModelsInPlayJavaSourceSet(
                @Each PlayApplicationSpec playComponent) {
            JavaSourceSet javaSources = playComponent.getSources().get("java")

            javaSources.source.exclude("models/**/*.java")
        }

        @Mutate
        void addDependencyToJavaSourceSet(
                @Path("components.play.sources.java") JavaSourceSet sourceSet) {
            sourceSet.dependencies.library('ebeanEntities')
        }

        @Mutate
        void createTaskToEnhanceClasses(ModelMap<Task> tasks,
                PlayPluginConfigurations configurations) {
            tasks.create("enhanceEbeanEntities", EnhanceEbeanEntitiesTask)
        }

        @Mutate
        void configureTaskToEnhanceEntities(
                @Path("tasks.enhanceEbeanEntities")
                        EnhanceEbeanEntitiesTask enhanceTask,
                @Path("tasks.compileEbeanEntitiesJarEbeanEntitiesJava")
                        Task compileTask) {
            FileCollection compiledFiles = compileTask.outputs.getFiles()

            enhanceTask.dependsOn(compileTask)
            enhanceTask.inputFiles = compiledFiles
            enhanceTask.outputDirectory = getEnhancedClassesDir(compiledFiles)
        }

        private File getEnhancedClassesDir(FileCollection inputFiles) {
            for (File file : inputFiles) {
                if (file.toString().contains("classes"))
                    return getEnhancedClassesDirUsing(file.toString())
            }
        }

        private File getEnhancedClassesDirUsing(String path) {
            return new File(path.replaceFirst("classes", "enhancedClasses"))
        }

        @Mutate
        void createTaskToCreateEnhancedJar(ModelMap<Task> tasks) {
            tasks.create("createEnhancedEbeanEntitiesJar", Jar)
        }

        @Mutate
        void configureTaskToCreateEnhancedJar(
                @Path("tasks.createEnhancedEbeanEntitiesJar")
                        Jar enhancedJarTask,
                @Path("tasks.createEbeanEntitiesJar") Jar jarTask,
                @Path("tasks.enhanceEbeanEntities")
                        EnhanceEbeanEntitiesTask enhanceTask) {
            String destinationPath = jarTask.destinationDir.getPath()
                    .replaceAll("ebeanEntities", "enhancedEbeanEntities")
            File destinationDir = new File(destinationPath)

            enhancedJarTask.dependsOn(enhanceTask)
            enhancedJarTask.from(enhanceTask.outputDirectory)
            enhancedJarTask.destinationDir = destinationDir
            enhancedJarTask.archiveName = "enhancedEbeanEntities"
        }

        @Mutate
        void addEnhancedJarDependencyToPlayConfiguration(
                @Path("tasks.compilePlayBinaryScala") Task compileTask,
                @Path("tasks.createEnhancedEbeanEntitiesJar") Jar jarTask,
                PlayPluginConfigurations configurations) {
            compileTask.dependsOn(jarTask)
            configurations.play.addDependency(jarTask.outputs.files)
        }
    }
}
