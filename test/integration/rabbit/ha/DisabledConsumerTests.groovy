package rabbit.ha

import static org.junit.Assert.*
import org.junit.*

class DisabledConsumerTests {

    def disabledConsumer

    @Before
    void setUp() {
        // Setup logic here
    }

    @After
    void tearDown() {
        // Tear down logic here
    }

    @Test
    void "Disabled consumer using a flag in the class should never start"() {
        assert disabledConsumer.disabled == true
        assert disabledConsumer.workers.isEmpty()
    }
}
