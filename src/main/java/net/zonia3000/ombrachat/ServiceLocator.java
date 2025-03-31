package net.zonia3000.ombrachat;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides a centralized registry for managing and retrieving shared services
 * or objects within the application.
 */
public class ServiceLocator {

    private static final Logger logger = LoggerFactory.getLogger(ServiceLocator.class);

    private static final Map<Class<?>, Object> services = new HashMap<>();

    public static <T> void registerService(Class<T> serviceClass, T service) {
        logger.debug("Registering service {}", serviceClass.getSimpleName());
        services.put(serviceClass, service);
    }

    public static <T> T getService(Class<T> serviceClass) {
        return serviceClass.cast(services.get(serviceClass));
    }
}
