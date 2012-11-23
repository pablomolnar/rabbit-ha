package grails.plugin.rabbitha

import org.apache.log4j.Logger
import org.codehaus.groovy.grails.commons.ApplicationHolder

import com.rabbitmq.client.AlreadyClosedException
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.QueueingConsumer
import com.rabbitmq.client.ShutdownSignalException

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
    int retry = 0

    RabbitHAConsumer rabbitHAConsumer
    def queueName
    int clusterIdx
    int prefetchCount

    Connection connection
    Channel channel
    def consumer

    RabbitHAConsumerWorker(RabbitHAConsumer rabbitHAConsumer, int clusterIdx = 0) {
        this.rabbitHAConsumer = rabbitHAConsumer
        this.clusterIdx = clusterIdx
        this.queueName = rabbitHAConsumer.queueName
        this.prefetchCount = rabbitHAConsumer.prefetchCount
    }

    void connect() {
        connection = RabbitHAConnectionFactory.getConnection(queueName, clusterIdx)
        log.info("Connection to $connection")

        channel = connection.createChannel()
        log.info("Channel $channel")

        if(rabbitHAConsumer.declareQueue) {
            channel.queueDeclare(queueName, true, false, false, null)
        }

        consumer = new QueueingConsumer(channel)
        channel.basicConsume(queueName, false, consumer)
        channel.basicQos(prefetchCount)
    }

    void run() {
        def config = ApplicationHolder.application.config.rabbitmq
        def startDelay = config.startDelay ?: 5 // 5 Seconds default delay

        log.info "Delay startup to $startDelay seconds"
        sleep(startDelay * 1000)

        log.info "Worker started"

        while (running) {
            try {
                if (reconnect) {
                    connect()

                    reconnect = false
                    retry = 0
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
                retry = 0

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

                int maxRetries = config.reconnection?.maxRetries
                int maxWaitTime = config.reconnection?.maxWaitTime ?: 60

                if (maxRetries && retry > maxRetries) throw new RuntimeException("Reconnection failed $retry times. Abort mission", e)

                reconnect = true

                int waitTime = retry++
                if(waitTime > maxWaitTime) waitTime = maxWaitTime

                log.error("Exception catched! Reconnection attempt #$retry in $waitTime seconds...", e)
                sleep(waitTime * 1000)
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
