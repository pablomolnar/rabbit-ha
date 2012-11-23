package grails.plugin.rabbitha

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

import javax.annotation.PreDestroy

import org.apache.log4j.Logger
import org.codehaus.groovy.grails.commons.ApplicationHolder

import com.rabbitmq.client.QueueingConsumer

/**
 * @author: Pablo Molnar
 * @since: Mar 10, 2012
 */
abstract class RabbitHAConsumer {
    static final Logger log = Logger.getLogger(this)

    ExecutorService service
    List<RabbitHAConsumerWorker> workers = []
    boolean started

    abstract void onDelivery(QueueingConsumer.Delivery delivery)
    abstract String getQueueName()

    // Default settings
    boolean getDeclareQueue() { false }
    int getConcurrency() { 1 }
    int getPrefetchCount() { 100 }

    def start() {
        log.info "Starting $concurrency consumer workers for $queueName"
		
		def config = ApplicationHolder.application.config.rabbitmq.connectionfactory
		if(!config) throw new IllegalArgumentException("Is supposed that connection factory settings were already validated...")

        // Normal behaviour there is only one RabbitMQ cluster, but is allowed multiple RabbitMQ clusters per consumer.
        int clusters = config.clusters

        service = Executors.newFixedThreadPool(concurrency * clusters)

        concurrency.times {
            clusters.times { clusterIdx ->
                def worker = new RabbitHAConsumerWorker(this, clusterIdx)
                workers << worker
                service.execute(worker)
            }
        }

        started = true
    }

    @PreDestroy
    def destroy() {
        if(!started) return

        log.info "Shutdown consumer of $queueName"
        // TODO: Refactor close & release resources...
        workers.each {
            it.running = false
        }

        workers.each {
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