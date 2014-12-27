package org.dustinl.cucumber
/**
 * Created by dustinl on 12/25/14.
 */
class FeatureRunnerTest extends GroovyTestCase {
    void testGetPluginsArgument() {
        def feature = 'feature'
        def glue = 'org.dustin'
        def plugins = [new Plugin('pretty'), new Plugin('junit:target/bbb/junit.xml')]
        FeatureRunner runner = new FeatureRunner(feature: feature, glue: glue, plugins: plugins)
        assert runner.getPluginsArgument() == ['--plugin', 'pretty', '--plugin', 'junit:target/bbb/feature-junit.xml']
    }

    void testGetArguments() {
        def feature = 'feature'
        def glue = 'org.dustin'
        def plugins = [new Plugin('pretty'), new Plugin('junit:target/ccc/junit.xml')]
        FeatureRunner runner = new FeatureRunner(feature: feature, glue: glue, plugins: plugins)
        assert runner.getArguments() == ['--glue', glue, '--plugin', 'pretty', '--plugin',
                                         'junit:target/ccc/feature-junit.xml', 'classpath:feature'] as String[]
    }

    void testGetFullPluginFileNameWithDir() {
        assert FeatureRunner.getFullPluginFileName('ffff', 'target/ddd', 'ttt.json') == 'target/ddd/ffff-ttt.json'
    }

    void testGetFullPluginFileNameWithoutDir() {
        assert FeatureRunner.getFullPluginFileName('ffff', null, 'ttt.json') == 'ffff-ttt.json'
    }
}
