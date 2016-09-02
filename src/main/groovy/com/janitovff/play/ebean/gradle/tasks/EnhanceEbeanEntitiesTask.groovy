package com.janitovff.play.ebean.gradle.tasks

import java.net.URL

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.TaskAction

import com.avaje.ebean.enhance.agent.ClassPathClassBytesReader
import com.avaje.ebean.enhance.agent.InputStreamTransform
import com.avaje.ebean.enhance.agent.Transformer

import org.objectweb.asm.ClassReader

import com.janitovff.play.ebean.gradle.internal.ClassNameExtractor

class EnhanceEbeanEntitiesTask extends DefaultTask {
    FileCollection inputFiles

    @TaskAction
    void enhance() {
        for (File file : inputFiles) {
            enhancePath(file)
        }
    }

    private void enhancePath(File file) {
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
            byte[] transformedBytes = enhanceClass(file)

            if (transformedBytes != null)
                writeBackEnhancedData(file, transformedBytes)
        } catch (IOException cause) {
            throw new RuntimeException("Failed to enhance file: " + file, cause)
        }
    }

    private void enhanceClass(File classFile) throws IOException {
        def className = getClassName(classFile)
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

    private void writeBackEnhancedData(File file, byte[] data)
            throws IOException {
        file.withOutputStream { outputStream -> outputStream << data }
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
}
