@artifact.package@

import com.rabbitmq.client.QueueingConsumer
import grails.plugin.rabbitha.RabbitHAConsumer
import org.apache.log4j.Logger

class @artifact.name@ extends RabbitHAConsumer {
    static final Logger log = Logger.getLogger(this)

    String queueName = 'enter_queue_name_to_consume'
    int concurrency = 1

    void onDelivery(QueueingConsumer.Delivery delivery) {
        // process delivery
    }
}
