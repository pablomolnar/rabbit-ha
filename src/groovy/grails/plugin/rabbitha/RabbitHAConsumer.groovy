package grails.plugin.rabbitha

import com.rabbitmq.client.QueueingConsumer
import org.apache.log4j.Logger

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy
import java.util.concurrent.TimeUnit

/**
 * @author: Pablo Molnar
 * @since: Mar 10, 2012
 */
abstract class RabbitHAConsumer {
    static final Logger log = Logger.getLogger(this)

    ExecutorService service
    List<RabbitHAConsumerWorker> consumers = []

    int getConcurrency() { 1 }
    int getPrefetchCount() { 100 }

    abstract void onDelivery(QueueingConsumer.Delivery delivery)
    abstract String getQueueName()

    def start() {
        log.info "Starting $concurrency consumers for $queueName"
        service = Executors.newFixedThreadPool(concurrency)
        concurrency.times {
            def consumer = new RabbitHAConsumerWorker(this, prefetchCount)
            consumers << consumer
            service.execute(consumer)
        }
    }

    @PreDestroy
    def destroy() {
        log.info "Shutdown consumer of $queueName"
        // TODO: Refactor close & release resources...
        consumers.each {
            it.running = false
        }

        consumers.each {
            it.close()
        }

        // Shutdown pool and wait threads are released
        service.shutdown()
        if(!service.awaitTermination(100, TimeUnit.MILLISECONDS)) {
            log.warn "Consumer workers of queue $queueName are still alive. Force shutdown..."
            service.shutdownNow()

            if(!service.awaitTermination(100, TimeUnit.MILLISECONDS)) {
                log.error "WTF! Some tasks still alive :S"
            }
        }
    }
}