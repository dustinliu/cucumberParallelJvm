#!/usr/bin/env groovy
package org.dustinl.cucumber
import cucumber.runtime.ClassFinder
import cucumber.runtime.Runtime
import cucumber.runtime.RuntimeOptions
import cucumber.runtime.io.MultiLoader

//@Grab(group = 'info.cukes', module = 'cucumber-groovy', version = '1.2.0')
import cucumber.runtime.io.ResourceLoader
import cucumber.runtime.io.ResourceLoaderClassFinder
import groovy.grape.Grape
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils

import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

//@Grab(group = 'org.apache.commons', module = 'commons-io', version = '1.3.2')
import java.util.concurrent.TimeUnit
import java.util.jar.JarFile
import java.util.zip.ZipEntry

def env = System.getenv()
env.each {key, value -> println("${key}: ${value}")}

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
    println '[info] both fork and thread defined, use fork mode'
    thread = 0
}

if(options.debug) { println "${fork} fork, ${thread} thread"}

def jarfile = options.arguments()[0]
Thread.currentThread().getContextClassLoader().addURL(new File(jarfile).toURI().toURL())

def classpath = getClassPath(jarfile, options.debug)

def features = new JarFile(jarfile).entries().findAll { ZipEntry entry -> entry.name.endsWith('.feature') }
if (options.debug) {
    features.each { println "feature: $it" }
}

def plugins = options.plugins.collect { new Plugin(it) }
if (options.debug) { plugins.each { println "plugin: $it" } }

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

ExecutorService pool = Executors.newFixedThreadPool(n)
runners.each {
    pool.submit(new Runnable() {
        @Override
        void run() {
            it.run()
        }
    })
}

runners.each {it.getOutput().eachLine {println it} }

int elapse = System.currentTimeMillis() - startTime
println "test run: ${milli2Time(elapse)}"

if (runners.every {it.done}) pool.shutdown()

System.exit(runners.every { it.exitValue == 0 } ? 0 :1)

//===============================================
def getClassPath(jarfile, debug) {
    def cp = Grape.resolve(*[[:], [group: 'info.cukes', module: 'cucumber-groovy', version: '1.2.0']]).findAll()
    cp.addAll Grape.resolve(*[[:], [group: 'org.codehaus.groovy', module: 'groovy-all', version: '2.3.3']]).findAll()
    cp << new File(jarfile).toURI()
    if (debug) cp.each { println "classpath: ${new File(it).path}" }
    cp.collect { new File(it).path }.join(':')
}

class Plugin {
    String type
    String dir
    String file

    Plugin(pString) {
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
                plugin = "${it.type}:" + getFullPluginFileName(featureName, it.dir, it.file)
            } else {
                plugin = it.type
            }

            ['--plugin', plugin]
        }.flatten()
    }

    def getArguments() {
        (['--glue', glue] + getPluginsArgument() + ["classpath:${feature}"]) as String[]
    }

    def static getFullPluginFileName(feature, dir, file) {
        dir ? "${dir}/${feature}-${file}" : "${feature}-${file}"
    }
}

class ThreadFeatureRunner extends FeatureRunner {
    Runtime runtime
    boolean done = false
    CountDownLatch startSignal = new CountDownLatch(1)

    void run() {
        def classLoader = Thread.currentThread().getContextClassLoader()
        RuntimeOptions options = new RuntimeOptions(Arrays.asList(getArguments()))
        ResourceLoader resourceLoader = new MultiLoader(classLoader)
        ClassFinder classFinder = new ResourceLoaderClassFinder(resourceLoader, classLoader)
        runtime = new Runtime(resourceLoader, classFinder, classLoader, options)
        runtime.run()
        startSignal.countDown()
        done = true
    }

    def getOutput() {
        startSignal.await()
        new ByteArrayInputStream(" ".getBytes())
    }

    def getExitValue() {
        startSignal.await()
        runtime.exitStatus()
    }
}

class ProcessFeatureRunner extends FeatureRunner {
    String classpath
    Process process
    CountDownLatch startSignal = new CountDownLatch(1)
    boolean done = false

    def run() {
        String cmd = getCommand()
        println getCommand()
        process = new ProcessBuilder(cmd.split()).redirectErrorStream(true).start()
        startSignal.countDown()
        process.waitFor()
        done = true
    }

    def getCommand() {
        "java -cp ${classpath} cucumber.api.cli.Main ${getArguments().join(' ')}"
    }

    def getOutput() {
        startSignal.await()
        process.getInputStream()
    }

    def getExitValue() {
        startSignal.await()
        process.exitValue()
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

