#!/bin/env groovy
package org.dustinl.cucumber

@Grab(group='info.cukes', module='cucumber-java', version='1.2.0')
import cucumber.runtime.ClassFinder
import cucumber.runtime.Runtime
import cucumber.runtime.RuntimeOptions
import cucumber.runtime.io.MultiLoader
import cucumber.runtime.io.ResourceLoader
import cucumber.runtime.io.ResourceLoaderClassFinder
import groovy.transform.ToString
import groovyx.gpars.GParsPool
@Grab(group='org.apache.commons', module='commons-io', version='1.3.2')
import org.apache.commons.io.FilenameUtils

import java.util.jar.JarFile
import java.util.zip.ZipEntry

@ToString
class Plugin {
    String type
    String dir
    String file
}

class CucumberRunner {
    String jarfile
    String feature
    def plugins
    String glue

    def getPluginsArgument() {
        plugins.collect {
            String plugin
            def featureName = FilenameUtils.getName(feature)
            if (it.file) {
                plugin = "${it.type}:" + (it.dir ? "${it.dir}/${featureName}-${it.file}" :  "${featureName}-${it.file}")
            } else {
                plugin = it.type
            }

            ['--plugin', plugin]
        }.flatten()
    }

    def getArguments() {
        (['--glue', glue] + getPluginsArgument() + ["classpath:${feature}"]) as String[]
    }

    def getRuntime() {
        def classLoader = Thread.currentThread().getContextClassLoader()
//        classLoader.addURL(new File(jarfile).toURI().toURL())
        RuntimeOptions options = new RuntimeOptions(Arrays.asList(getArguments()))
        ResourceLoader resourceLoader = new MultiLoader(classLoader)
        ClassFinder classFinder = new ResourceLoaderClassFinder(resourceLoader, classLoader)
        new Runtime(resourceLoader, classFinder, classLoader, options)
    }
}


def cli = new CliBuilder(usage: 'cucumberParallelJvm [options] jarfile')
cli.with {
    p longOpt: 'plugin', 'register cucumber plugin', args: 1, argName: 'plugin', required: true
    g longOpt: 'glue', 'glue location', args: 1, argName: 'glue path', required: true
    d longOpt: 'debug', 'enable debug message'
}

def options = cli.parse(args)
options?:System.exit(1)

if(!options.arguments()[0]) {
    cli.usage()
    System.exit(1)
}


def jarfile = options.arguments()[0]
this.getClass().classLoader.rootLoader.addURL(new File(jarfile).toURI().toURL())

def features = new JarFile(jarfile).entries().findAll {
    ZipEntry entry -> entry.name.endsWith('.feature')
}

if (options.debug) { features.each { println "feature: [$it]" } }


def plugins  = options.plugins.collect {
    def (type, filename) = it.split(':').toList()
    def dir = FilenameUtils.getFullPathNoEndSeparator(filename)
    def file = FilenameUtils.getName(filename)
    new Plugin(type: type, dir: dir, file: file)
}

if (options.debug) { plugins.each { println it } }

def runtimes = features.collect {
    CucumberRunner runner = new CucumberRunner(jarfile: jarfile, glue: options.glue, feature: it.name, plugins:
            plugins)
    if(options.debug) println runner.getArguments()
    runner.getRuntime()
}

GParsPool.withPool(4) { runtimes.eachParallel { it.run() } }

