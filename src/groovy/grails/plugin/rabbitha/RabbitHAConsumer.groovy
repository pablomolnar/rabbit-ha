package grails.plugin.rabbitha

import com.rabbitmq.client.QueueingConsumer
import org.apache.log4j.Logger

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

/**
 * @author: Pablo Molnar
 * @since: Mar 10, 2012
 */
abstract class RabbitHAConsumer {
    static final Logger log = Logger.getLogger(this)

    ExecutorService service
    List<RabbitHAConsumerWorker> consumers = []

    int getConcurrency() { 1 }
    abstract void onDelivery(QueueingConsumer.Delivery delivery)
    abstract String getQueueName()

    @PostConstruct
    def start() {
        println "Starting $concurrency consumers for $queueName"
        log.info "Starting $concurrency consumers for $queueName"
        service = Executors.newFixedThreadPool(concurrency)
        concurrency.times {
            def consumer = new RabbitHAConsumerWorker(this)
            consumers << consumer
            service.submit(consumer)
        }
    }

    @PreDestroy
    def destroy() {
        log.info "Shutdown consumers"
        consumers.each {
            it.close()
        }

        // Shutdown pool and wait threads are released
        service.shutdown
        sleep(10)

        if(!service.isTerminated()) {
            log.info "Consumers of queue $queueName are still alive. Force shutdown..."
            service.shutdownNow()
            sleep(10)

            log.info "Check consumers are terminated: " + service.isTerminated()
        }
    }
}