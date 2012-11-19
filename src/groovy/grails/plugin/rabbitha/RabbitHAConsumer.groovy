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

    int getConcurrency() { 1 }
    int getPrefetchCount() { 100 }

    abstract void onDelivery(QueueingConsumer.Delivery delivery)
    abstract String getQueueName()
	
	def launchWorker(def address, int n) {
		def worker = new RabbitHAConsumerWorker(this, address, n, prefetchCount)
		workers << worker
		service.execute(worker)
	}

    def start() {
        log.info "Starting $concurrency consumer workers for $queueName"
		
		def config = ApplicationHolder.application.config.rabbitmq.connectionfactory
		if(!config) throw new IllegalArgumentException("Is supposed that connection factory settings were already validated...")

        def manyWorkers = config.addresses.any { it instanceof List }
        def workersNumber = concurrency
        if(manyWorkers)
            workersNumber *= config.addresses.size()

        service = Executors.newFixedThreadPool(workersNumber)
        concurrency.times {
			if(manyWorkers) {
                int n = 0;
				config.addresses.each{
					launchWorker(it, n)
                    n++
				}
			} else {
				launchWorker(config.addresses, 0)
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