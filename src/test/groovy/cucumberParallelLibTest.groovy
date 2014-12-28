/**
 * Created by dustinl on 12/28/14.
 */
class cucumberParallelLibTest extends GroovyTestCase {
    void testGetFeaturesWithDir() {
        def lib = new cucumberParallelLib()
        def features = lib.getFeatures('src')
        assert features.sort {a, b -> a.compareTo(b)} == ['plus.feature', 'minus.feature', 'divide.feature']
                .sort {a, b -> a.compareTo(b)}
    }
}
