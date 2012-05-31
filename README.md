## RabbitMQ-HA: An alternative RabbitMQ plugin for Grails.

This is an alternative to grails-rabbitmq official plugin. The main difference is this plugin talk directly to the RabbitMQ java driver and the spring-amq libraries are not involved.

### Highlights
+ Client high-availability is guaranteed doing a reconnection logic when a cluster node is down.
+ Consumer concurrency and prefetch count are configurable
+ Create with ease new consumers using grails create-consumer


### Usage
You should add the connection settings in Config.groovy

e.g.:

    rabbitmq {
        connectionfactory {
            username = "guest"
            password = "guest"
            virtualHost = "/"
            addresses = ['hostname_1','hostname_2', ...]

        }
    }


Then create a new consumer use the new command `create-consumer`.

e.g.
`grails create-consumer MyBrandNewConsumer`

The result will be a class like


    class MyBrandNewConsumer extends RabbitHAConsumer {
        static final Logger log = Logger.getLogger(this)

        String queueName = 'items_external_feed'
        int concurrency = 5

        void onDelivery(QueueingConsumer.Delivery delivery) {
    	// process delivery
        }
    }

Every consumers owns an exclusive connection and every worker has a private channel

### Coming goodies
+ Add publisher
+ Add a dashboard with consumers info
+ Runtime add/pause/remove workers

### Coming improvements
+ Remove super class of consumers
+ Remove log4j dependency
+ Improve documentation


### License

(The MIT License)

Copyright (c) 2012 Pablo Molnar @pmolnar;

Permission is hereby granted, free of charge, to any person obtaining
a copy of this software and associated documentation files (the
'Software'), to deal in the Software without restriction, including
without limitation the rights to use, copy, modify, merge, publish,
distribute, sublicense, and/or sell copies of the Software, and to
permit persons to whom the Software is furnished to do so, subject to
the following conditions:

The above copyright notice and this permission notice shall be
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED 'AS IS', WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.