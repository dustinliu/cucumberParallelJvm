

import org.apache.commons.io.FileUtils

/**
 * Created by dustinl on 12/25/14.
 */
class PluginTest extends GroovyTestCase {
    void testPluginWithoutFile() {
        Plugin plugin = new Plugin('pretty')
        assert plugin.type == 'pretty'
        assert plugin.dir == null
        assert plugin.file ==  null
    }

    void testPluginWithFile() {
        File target = new File('target/aaa')
        try {
            FileUtils.deleteDirectory(target)
            Plugin plugin = new Plugin('junit:target/aaa/unit.xml')
            assert target.exists()
            assert target.isDirectory()
        } finally {
            FileUtils.deleteDirectory(target)
        }
    }
}
