package rabbit.ha

import com.rabbitmq.client.QueueingConsumer
import grails.plugin.rabbitha.RabbitHAConsumer

class DisabledConsumer extends RabbitHAConsumer {
    String queueName = 'dummy_queue'
    boolean disabled = true

    void onDelivery(QueueingConsumer.Delivery delivery) {
        assert false, "This line should never be executed..."
    }
}
