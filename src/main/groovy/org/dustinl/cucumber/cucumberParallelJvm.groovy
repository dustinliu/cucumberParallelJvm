#!/usr/bin/env groovy
package org.dustinl.cucumber

@Grab(group = 'info.cukes', module = 'cucumber-groovy', version = '1.2.0')
import cucumber.runtime.ClassFinder
import cucumber.runtime.Runtime
import cucumber.runtime.RuntimeOptions
import cucumber.runtime.io.MultiLoader
import cucumber.runtime.io.ResourceLoader
import cucumber.runtime.io.ResourceLoaderClassFinder
import groovy.grape.Grape
import groovy.transform.ToString
import groovyx.gpars.GParsExecutorsPool
import org.apache.commons.io.FileUtils
@Grab(group = 'commons-io', module = 'commons-io', version = '1.3.2')
import org.apache.commons.io.FilenameUtils

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.jar.JarFile
import java.util.zip.ZipEntry

def cli = new CliBuilder(usage: 'cucumberParallelJvm [options] jarfile')
cli.with {
    p longOpt: 'plugin', 'register cucumber plugin', args: 1, argName: 'plugin', required: true
    g longOpt: 'glue', 'glue location', args: 1, argName: 'glue path', required: true
    d longOpt: 'debug', 'enable debug message'
    f longOpt: 'fork', 'number of process to fork', args: 1, argName: 'number of fork'
    t longOpt: 'thread', 'number of thread', args: 1, argName: 'number of thread'
}

def options = cli.parse(args)
options ?: System.exit(1)

if (!options.arguments()[0]) {
    cli.usage()
    System.exit(1)
}


int fork = 0
int thread = 0
if (options.fork) fork = Integer.parseInt(options.fork)
if (options.thread) thread = Integer.parseInt(options.thread)


if( !fork  && !thread) fork = 1

if (fork && thread) {
    println 'info: both fork and thread defined, use fork mode'
    thread = 0
}

if(options.debug) { println "${fork} fork, ${thread} thread"}

def jarfile = options.arguments()[0]
Thread.currentThread().getContextClassLoader().addURL(new File(jarfile).toURI().toURL())

def classpath = getClassPath(jarfile, options)

def features = new JarFile(jarfile).entries().findAll { ZipEntry entry -> entry.name.endsWith('.feature') }
if (options.debug) {
    features.each { println "feature: $it" }
}

def plugins = options.plugins.collect { new Plugin(it as String) }
if (options.debug) {
    plugins.each { println "plugin: $it" }
}

def runners
int n
if (fork) {
    n = fork
    if (options.debug) println "classpath string: $classpath"
    runners = features.collect { new ProcessFeatureRunner(classpath: classpath, glue: options.glue, feature: it.name,
            plugins: plugins) }
} else {
    n = thread
    runners = features.collect {
        new ThreadFeatureRunner(glue: options.glue, feature: it.name, plugins: plugins) }
}

def startTime = System.currentTimeMillis()
if(options.debug) { println "$n parallel runners"}

//withPool(n) {
//    runners.collectParallel {it.run()}.each { it.eachLine {println it} }
//}

ExecutorService pool = Executors.newFixedThreadPool(1)
//GParsExecutorsPool.withExistingPool(pool) {
GParsExecutorsPool.withPool {
    runners.collectParallel {it.run()}.each { it.eachLine {println it} }
}

int elapse = System.currentTimeMillis() - startTime
println "test run: ${milli2Time(elapse)}"

//mergeReport(features, plugins)

//====================================================

def getClassPath(String jarfile, OptionAccessor options) {
    Grape.grab(group: 'info.cukes', module: 'cucumber-groovy', version: '1.2.0')
    def cp = Grape.resolve(*[[:], [group: 'info.cukes', module: 'cucumber-groovy', version: '1.2.0']]).findAll()
    cp.addAll Grape.resolve(*[[:], [group: 'org.codehaus.groovy', module: 'groovy-all', version: '2.3.3']]).findAll()
    cp << new File(jarfile).toURI()
    if (options.debug) cp.each { println "classpath: ${new File(it).path}" }
    cp.collect { new File(it).path }.join(':')
}

@ToString
class Plugin {
    String type
    String dir
    String file

    Plugin(String pString) {
        def (String type, String filename) = pString.split(':').toList()
        this.type = type

        this.dir = FilenameUtils.getFullPathNoEndSeparator(filename)
        this.file = FilenameUtils.getName(filename)
        if (this.dir) FileUtils.forceMkdir(new File(dir))
    }
}

class FeatureRunner {
    String feature
    String glue
    def plugins

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
}

class ThreadFeatureRunner extends FeatureRunner {
    def run() {
        def classLoader = Thread.currentThread().getContextClassLoader()
        RuntimeOptions options = new RuntimeOptions(Arrays.asList(getArguments()))
        ResourceLoader resourceLoader = new MultiLoader(classLoader)
        ClassFinder classFinder = new ResourceLoaderClassFinder(resourceLoader, classLoader)
        new Runtime(resourceLoader, classFinder, classLoader, options).run()
        new ByteArrayInputStream( " ".getBytes() )
    }
}


class ProcessFeatureRunner extends FeatureRunner {
    File out
    String classpath

    def run() {
        this.out = File.createTempFile(FilenameUtils.getName(feature), '.out')
        out.deleteOnExit()
        String cmd = "java -cp ${classpath} cucumber.api.cli.Main ${getArguments().join(' ')}"
        new ProcessBuilder(cmd.split()).redirectErrorStream(true).start().getInputStream()
    }
}


def milli2Time (int elapse) {
    String.format("%dh%dm%d.%ds",
            TimeUnit.MILLISECONDS.toHours(elapse),
            TimeUnit.MILLISECONDS.toMinutes(elapse) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(elapse)),
            TimeUnit.MILLISECONDS.toSeconds(elapse) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(elapse)),
            TimeUnit.MILLISECONDS.toMillis(elapse) - TimeUnit.SECONDS.toMillis(TimeUnit.MILLISECONDS.toSeconds(elapse)),
    )
}

