#!/usr/bin/env groovy
package org.dustinl.cucumber

@Grab(group = 'info.cukes', module = 'cucumber-groovy', version = '1.2.0')
import cucumber.runtime.ClassFinder
import cucumber.runtime.Runtime
import cucumber.runtime.RuntimeOptions
import cucumber.runtime.io.MultiLoader
import cucumber.runtime.io.ResourceLoader
import cucumber.runtime.io.ResourceLoaderClassFinder
import groovy.transform.ToString
import groovyx.gpars.GParsPool
import org.apache.commons.io.FileUtils
@Grab(group = 'commons-io', module = 'commons-io', version = '1.3.2')
import org.apache.commons.io.FilenameUtils

import java.util.jar.JarFile
import java.util.zip.ZipEntry

@ToString
class Plugin {
    String type
    String dir
    String file

    Plugin(String pString) {
        def (String type, String filename) = pString.split(':').toList()
        this.type = type

        if(!filename)  {
            File tmp = File.createTempFile(type, '.out')
            tmp.deleteOnExit()
            filename = tmp.absolutePath
        }
        this.dir = FilenameUtils.getFullPathNoEndSeparator(filename)
        this.file = FilenameUtils.getName(filename)
        if (this.dir) FileUtils.forceMkdir(new File(dir))
    }
}

class CucumberThreadRunner {
    String jarfile
    String feature
    def plugins
    String glue
    def runtime

    def getPluginsArgument() {
        plugins.collect {
            String plugin
            def featureName = FilenameUtils.getName(feature)
            if (it.file) {
                plugin = "${it.type}:" + (it.dir ? "${it.dir}/${featureName}-${it.file}" : "${featureName}-${it.file}")
            } else {
                plugin = it.type
            }

            ['--plugin', plugin]
        }.flatten()
    }

    def getArguments() {
        (['--glue', glue] + getPluginsArgument() + ["classpath:${feature}"]) as String[]
    }

    def run() {
        def classLoader = Thread.currentThread().getContextClassLoader()
        RuntimeOptions options = new RuntimeOptions(Arrays.asList(getArguments()))
        ResourceLoader resourceLoader = new MultiLoader(classLoader)
        ClassFinder classFinder = new ResourceLoaderClassFinder(resourceLoader, classLoader)
        new Runtime(resourceLoader, classFinder, classLoader, options).run()
    }
}


def cli = new CliBuilder(usage: 'cucumberParallelJvm [options] jarfile')
cli.with {
    p longOpt: 'plugin', 'register cucumber plugin', args: 1, argName: 'plugin', required: true
    g longOpt: 'glue', 'glue location', args: 1, argName: 'glue path', required: true
    d longOpt: 'debug', 'enable debug message'
}

def options = cli.parse(args)
options ?: System.exit(1)

if (!options.arguments()[0]) {
    cli.usage()
    System.exit(1)
}


def jarfile = options.arguments()[0]
Thread.currentThread().getContextClassLoader().addURL(new File(jarfile).toURI().toURL())

def features = new JarFile(jarfile).entries().findAll {
    ZipEntry entry -> entry.name.endsWith('.feature')
}

if (options.debug) {
    features.each { println "feature: $it" }
}


def plugins = options.plugins.collect {
    new Plugin(it as String)
}

if (options.debug) {
    plugins.each { println "plugin: $it" }
}

def runner = features.collect { new CucumberThreadRunner(jarfile: jarfile, glue: options.glue, feature: it.name,
        plugins: plugins) }

GParsPool.withPool(4) { runner.eachParallel { it.run() } }

