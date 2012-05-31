package grails.plugin.rabbitha

import com.rabbitmq.client.AlreadyClosedException
import com.rabbitmq.client.QueueingConsumer
import com.rabbitmq.client.ShutdownSignalException
import org.apache.log4j.Logger
import java.util.concurrent.Callable
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection

/**
 * @author: Pablo Molnar
 * @since: 31/1/12
 *
 * RabbitMQ highly available consumer.
 * Implementation of a basic parallel enabled consumer with reconnection logic on errors.
 *
 */
class RabbitHAConsumerWorker implements Runnable {
    static final Logger log = Logger.getLogger(this)

    volatile boolean running = true
    volatile boolean reconnect = true
    int i = 0

    RabbitHAConsumer rabbitHAConsumer
    def queueName

    Connection connection
    Channel channel
    def consumer

    RabbitHAConsumerWorker(RabbitHAConsumer rabbitHAConsumer, int prefetchCount = 100) {
        this.rabbitHAConsumer = rabbitHAConsumer
        this.queueName = rabbitHAConsumer.queueName
    }

    void connect() {
        connection = RabbitHAConnectionFactory.getConnection(queueName)
        log.info("Connection to $connection.address")

        channel = connection.createChannel()
        log.info("Channel $channel")

        consumer = new QueueingConsumer(channel)
        channel.basicConsume(queueName, false, consumer)
        channel.basicQos(10)
    }

    void run() {
        log.info "Worker started"

        while (running) {
            try {
                if (reconnect) {
                    connect()
                    reconnect = false
                }

                QueueingConsumer.Delivery delivery = consumer.nextDelivery()
                log.info "delivery on:" + queueName

                try {
                    rabbitHAConsumer.onDelivery(delivery)
                } catch (e) {
                    channel.basicNack(delivery.envelope.deliveryTag, false, true)
                    log.error "Error processing message ${new String(delivery.body)}", e
                    continue
                }

                channel.basicAck(delivery.envelope.deliveryTag, false)
                i = 0

            } catch (e) {
                if (running == false) {
                    log.debug "Exception was thrown while consumer was closing: ${e.class}"
                    return
                }

                // Only handled exceptions
                if (!(e instanceof ConnectException || e instanceof ShutdownSignalException || e instanceof IOException || e instanceof AlreadyClosedException)) {
                    log.error("Don't reconnect with this exception....", e)
                    throw new RuntimeException("Don't reconnect with this exception....", e)
                }

                if (i > 10) throw new RuntimeException("Reconnection failed $i times. Abort mission", e)

                i++
                reconnect = true
                def delay = i * 1000

                log.error("Exception catched! Reconnection attempt #$i in $i seconds...", e)
                sleep(delay)
            }
        }
    }

    void close() {
        log.info("Closing resources")

        running = false
        try { if(channel.isOpen()) channel.close() } catch(e) {} // close quietly
        try { if(connection.isOpen()) connection.close() } catch(e) {} // close quietly
    }
}
