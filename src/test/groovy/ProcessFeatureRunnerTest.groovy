

/**
 * Created by dustinl on 12/27/14.
 */
class ProcessFeatureRunnerTest extends GroovyTestCase {
    void testRun() {

    }

    void testGetCommand() {
        def classpath = '.:/tmp/ewfdf:/var/dfdf/fdsfdsfsdf'
        def glue = 'classpath:org.dustinl.cucumber'
        def feature = 'ffffff'
        def plugins = [new Plugin('pretty'), new Plugin('junit:target/bbb/junit.xml')]
        ProcessFeatureRunner runners =
                new ProcessFeatureRunner(classpath: classpath, glue: glue, feature: feature, plugins: plugins)
        assert runners.command == 'java -cp ' + classpath + ' cucumber.api.cli.Main --glue ' + glue + ' --plugin ' +
                "pretty --plugin junit:target/bbb/${feature}-junit.xml " +  feature
    }
}
