package com.janitovff.play.ebean.gradle.tasks

import java.net.URL
import java.nio.file.Files

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

import com.avaje.ebean.enhance.agent.ClassPathClassBytesReader
import com.avaje.ebean.enhance.agent.InputStreamTransform
import com.avaje.ebean.enhance.agent.Transformer

import org.objectweb.asm.ClassReader

import com.janitovff.play.ebean.gradle.internal.ClassNameExtractor

class EnhanceEbeanEntitiesTask extends DefaultTask {
    @InputFiles
    FileCollection inputFiles

    @OutputDirectory
    File outputDirectory

    @TaskAction
    void enhance(IncrementalTaskInputs inputs) {
        if (!inputs.incremental)
            project.delete(outputDirectory.listFiles())

        inputs.outOfDate { change ->
            this.enhancePath(change.file)
        }

        inputs.removed { change ->
            this.removeOutputFileOf(change.file)
        }
    }

    void enhancePath(File file) {
        if (file.isFile())
            enhanceFile(file)
        else if (file.isDirectory())
            enhanceDirectory(file)
    }

    private void enhanceDirectory(File path) {
        for (File childPath : path.listFiles())
            enhancePath(childPath)
    }

    private void enhanceFile(File file) {
        System.err.println("Enhancing: " + file)
        try {
            String className = getClassName(file)
            byte[] transformedBytes = enhanceClass(file, className)

            if (transformedBytes != null)
                writeBackEnhancedData(className, transformedBytes)
            else
                copyUnenhancedFile(file, className)
        } catch (IOException cause) {
            throw new RuntimeException("Failed to enhance file: " + file, cause)
        }
    }

    private byte[] enhanceClass(File classFile, String className)
            throws IOException {
        def classLoader = getClass().getClassLoader()
        def classStream = new FileInputStream(classFile)
        def classReader = new ClassPathClassBytesReader(new URL[0])
        def transformer = new Transformer(new URL[0], "debug=9")
        def transform = new InputStreamTransform(transformer, classLoader)

        try {
            return transform.transform(className, classStream)
        } finally {
            classStream.close()
        }
    }

    private String getClassName(File classFile) throws IOException {
        final int NO_FLAGS = 0

        ClassNameExtractor extractor = new ClassNameExtractor()

        classFile.withInputStream { inputStream ->
            ClassReader classReader = new ClassReader(inputStream)

            classReader.accept(extractor, NO_FLAGS)
        }

        return extractor.getClassName()
    }

    private void writeBackEnhancedData(String className, byte[] data)
            throws IOException {
        File outputFile = getOutputFilePathFor(className)

        createFileIfItDoesntExist(outputFile)

        outputFile.withOutputStream { outputStream -> outputStream << data }
    }

    private void copyUnenhancedFile(File sourceFile, String className)
            throws IOException {
        File destinationFile = getOutputFilePathFor(className)

        Files.copy(sourceFile.toPath(), destinationFile.toPath())
    }

    private File getOutputFilePathFor(String className)
            throws IOException {
        String classFile = className.replaceAll("\\.", "/") + ".class"

        return new File(outputDirectory, classFile)
    }

    private void createFileIfItDoesntExist(File file) {
        createParentDirectoriesIfTheyDontExist(file.getParentFile())

        file.createNewFile()
    }

    private void createParentDirectoriesIfTheyDontExist(File directory) {
        if (directory != null && !directory.exists())
            directory.mkdirs()
    }

    private void removeOutputFileOf(File inputFile) {
        String className = getClassName(inputFile)
        File outputFile = getOutputFilePathFor(className)

        outputFile.delete()

        removeParentDirectoriesIfEmpty(outputFile.getParentFile())
    }

    private void removeParentDirectoriesIfEmpty(File directory) {
        if (directory == null || !directory.isEmpty())
            return;

        directory.delete()

        removeParentDirectoriesIfEmpty(directory.getParentFile())
    }
}
