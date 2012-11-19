package grails.plugin.rabbitha

import java.util.concurrent.ConcurrentHashMap

import org.apache.log4j.Logger
import org.codehaus.groovy.grails.commons.ApplicationHolder

import com.rabbitmq.client.Address
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory

/**
 * @author: Pablo Molnar
 * @since: Mar 10, 2012
 */
class RabbitHAConnectionFactory {
	static final Logger log = Logger.getLogger(this)
	static final $LOCK = new Object[0]
	static final ConcurrentHashMap map = new ConcurrentHashMap()

	static Connection getConnection(String queueName, def addresses, int nQueue) {
		Connection connection

		synchronized ($LOCK) {
			connection = map.get(nQueue + "-" + queueName)
			if (connection == null || !connection.isOpen()) {
				def config = ApplicationHolder.application.config.rabbitmq.connectionfactory
				if(!config) throw new IllegalArgumentException("Is supposed that connection factory settings were already validated...")

				log.info "No connection established for queue $queueName. Create a new connection with $config"

				Collections.shuffle(config.addresses)
				def connectionFactory = new ConnectionFactory(username: config.username, password: config.password, virtualHost: config.virtualHost)
				def address = Address.parseAddresses(addresses.join(','))

				connection = connectionFactory.newConnection(address)
				log.info "Succesfully connected to $address for queue $queueName"

				map[queueName] = connection
			}
		}

		return connection
	}
}

