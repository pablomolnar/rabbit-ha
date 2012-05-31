package rabbit.ha



import com.rabbitmq.client.QueueingConsumer
import grails.plugin.rabbitha.RabbitHAConsumer
import org.apache.log4j.Logger

class PabloConsumer extends RabbitHAConsumer {
    static final Logger log = Logger.getLogger(this)

    String queueName = 'items_external_feed'
    int concurrency = 5

    void onDelivery(QueueingConsumer.Delivery delivery) {
        // process delivery
    }
}
