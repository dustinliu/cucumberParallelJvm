#!/usr/bin/env groovy

import groovy.grape.Grape

Grape.grab([group:'info.cukes', module: 'cucumber-groovy', version: '1.2.0'])
Grape.grab([group:'org.apache.commons', module: 'commons-io', version: '1.3.2'])

def baseDir = System.getenv()['BASEDIR']
File lib = new File(baseDir + '/cucumberParallelLib.groovy')

GroovyShell shell = new GroovyShell(this.getClass().getClassLoader(), binding)
shell.evaluate(lib)
