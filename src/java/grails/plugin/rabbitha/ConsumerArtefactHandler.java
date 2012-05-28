package grails.plugin.rabbitha;

import com.rabbitmq.client.QueueingConsumer;
import org.codehaus.groovy.grails.commons.ArtefactHandlerAdapter;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;

/**
 * Created with IntelliJ IDEA.
 * User: pablomolnar
 * Date: 5/28/12
 * Time: 1:24 AM
 * To change this template use File | Settings | File Templates.
 */
public class ConsumerArtefactHandler extends ArtefactHandlerAdapter {

    public static final String TYPE = "Consumer";

    public ConsumerArtefactHandler() {
        super(TYPE, GrailsConsumerClass.class, DefaultGrailsConsumerClass.class, null);
    }

    public boolean isArtefactClass(Class clazz) {
        // class shouldn't be null and should ends with Consumer suffix
        if (clazz == null || !clazz.getName().endsWith(DefaultGrailsConsumerClass.CONSUMER)) return false;

        // and should have onDelivery method defined
        Method method = ReflectionUtils.findMethod(clazz, "onDelivery", new Class[]{QueueingConsumer.Delivery.class});

        return method != null;
    }
}
