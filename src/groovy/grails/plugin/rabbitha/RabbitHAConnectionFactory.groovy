package grails.plugin.rabbitha

import com.rabbitmq.client.Address
import com.rabbitmq.client.Connection

import java.util.concurrent.ConcurrentHashMap
import org.apache.log4j.Logger

/**
 * @author: Pablo Molnar
 * @since: Mar 10, 2012
 */
class RabbitHAConnectionFactory {
    static final Logger log = Logger.getLogger(this)
    static final $LOCK = new Object[0]

    static final ConcurrentHashMap map = new ConcurrentHashMap()
    static Map config = [username: 'admin', password: 'admin', virtualHost: 'items', addresses: ['i-00000007-asm', 'i-00000009-asm', 'i-0000001a-zsm', 'i-000000ed-wsm', 'i-00000140-csm', 'i-00000316-bsm']]


    static Connection getConnection(String queueName) {
        Connection connection

        synchronized ($LOCK) {
            connection = map.get(queueName)
            if (connection == null || !connection.isOpen()) {
                Collections.shuffle(config.addresses)
                def connectionFactory = new RabbitHAConnectionFactory(username: config.username, password: config.password, virtualHost: config.virtualHost, requestedHeartbeat: 3000, connectionTimeout: 5000)
                def addresses = Address.parseAddresses(config.addresses.join(','))
                connection = connectionFactory.newConnection(addresses)
                log.info("Succesfully connected to $connection.address for queue $queueName")

                map[queueName] = connection
            }
        }

        return connection
    }
}

