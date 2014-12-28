/**
 * Created by dustinl on 12/28/14.
 */
class cucumberParallelLibTest extends GroovyTestCase {
    void testGetFeaturesWithDir() {
        def lib = new cucumberParallelLib()
        def features = lib.getFeatures('src')
        assert features.sort { a, b -> a.compareTo(b) } ==
                ['src/main/resources/features/plus.feature',
                 'src/main/resources/features/minus.feature',
                 'src/main/resources/features/divide.feature']
                        .sort { a, b -> a.compareTo(b) }
    }
}
