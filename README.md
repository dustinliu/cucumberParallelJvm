cucumberParallelJvm
===================

cucumberParallelJvm is a groovy script to execute the each cucumber feature by cucumber-jvm in parallel, the report of each feature is written to individual file

Example:
```
cucumberParallelJvm --glue classpath:org.dustinl.cucumber.test --plugin pretty foo.jar
```

the foo.jar must contain the feature files and step definition

