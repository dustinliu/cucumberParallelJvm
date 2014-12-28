#!/usr/bin/env groovy
package org.dustinl.cucumber
import groovy.grape.Grape

Grape.grab([group:'info.cukes', module: 'cucumber-groovy', version: '1.2.0'])
Grape.grab([group:'org.apache.commons', module: 'commons-io', version: '1.3.2'])

GroovyShell shell = new GroovyShell(this.getClass().getClassLoader(), binding)
shell.evaluate(new File('/Users/dustinl/src/cucumberParallelJvm/src/main/groovy/org/dustinl/cucumber/parallelLib.groovy'))
