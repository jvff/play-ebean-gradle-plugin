package com.janitovff.play.ebean.gradle.internal

import org.objectweb.asm.ClassVisitor

import static org.objectweb.asm.Opcodes.ASM5

public class ClassNameExtractor extends ClassVisitor {
    private ArrayList<String> classNames

    public ClassNameExtractor() {
        super(ASM5)

        classNames = new ArrayList<String>()
    }

    @Override
    public void visit(int version, int access, String name, String signature,
            String superName, String[] interfaces) {
        classNames.add(name)
    }

    public String getClassName() {
        if (classNames.size() != 1) {
            throw new RuntimeException("Expected a single class, found "
                    + classNames.size())
        }

        return classNames.get(0).replaceAll("/", ".")
    }
}
