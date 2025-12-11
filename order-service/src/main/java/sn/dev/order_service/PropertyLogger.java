package sn.dev.order_service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PropertyLogger {

    @Value("${spring.application.name:NOT_SET}")
    private String appName;

    @Value("${spring.data.mongodb.uri:NOT_SET}")
    private String mongoUri;

    @PostConstruct
    public void logProperties() {
        System.out.println(">>> order-service appName = " + appName);
        System.out.println(">>> order-service Mongo URI = " + mongoUri);
    }
}
