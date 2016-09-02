# Play-Ebean Gradle plugin

Gradle plugin to be used with the Play! framework Gradle plugin. Enhanced
versions of classes in the models package are created, and added as a
dependency to the play configuration.

## Usage

Using the new plugin mechanism (requires Gradle 2.1 or later):

    plugins {
        id "com.janitovff.play-ebean" version "0.0.1"
    }

Using the common plugin mechanism:

    buildscript {
        repositories {
            maven {
                url "https://plugins.gradle.org/m2/"
            }
        }

        dependencies {
            classpath "gradle.plugin.com.janitovff:play-ebean-gradle-plugin:0.0.1"
        }
    }

    apply plugin: "com.janitovff.play-ebean"

## Details

A new Jar component is created for the Ebean Entities. The `models` package is
used as the package to be enhanced. All classes in that package are removed
from the default play binary source set, and added to a new source set used to
create the ebeanEntities.jar. The compiled classes in this Jar are then enhanced
and repackaged into a new enhancedEbeanEntities.jar file, which is then added as
a dependency to the play configuration. If you want to add dependecies specific
for compiling the `models` package classes, you can do so as below:

    model {
        components {
            ebeanEntities {
                sources {
                    java {
                        dependencies {
                            module 'com.typesafe.play:play-java_2.11:2.3.9'
                        }
                    }
                }
            }
        }
    }
