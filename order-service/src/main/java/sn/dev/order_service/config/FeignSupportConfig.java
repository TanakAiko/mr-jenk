package sn.dev.order_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import feign.RequestInterceptor;
import feign.form.spring.SpringFormEncoder;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class FeignSupportConfig {
    @Bean
    public SpringFormEncoder feignFormEncoder() {
        return new SpringFormEncoder();
    }

    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication instanceof JwtAuthenticationToken jwtToken) {
                String tokenValue = jwtToken.getToken().getTokenValue();
                log.debug("Forwarding JWT token: {}", tokenValue.substring(0, Math.min(tokenValue.length(), 20)) + "...");
                requestTemplate.header("Authorization", "Bearer " + tokenValue);
            } else {
                log.warn("No JWT token found in SecurityContext to forward via Feign");
            }
        };
    }
}
