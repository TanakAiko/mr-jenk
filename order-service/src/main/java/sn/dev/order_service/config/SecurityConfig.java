package sn.dev.order_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.http.HttpMethod;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final RSAKeysConfig rsaKeysConfig;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // CART endpoints (current user)
                        .requestMatchers(HttpMethod.GET, "/api/cart").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/cart/items").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/cart/items/{productId}").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/cart/items/{productId}").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/cart").authenticated()

                        // ORDER endpoints for client
                        .requestMatchers(HttpMethod.POST, "/api/orders/checkout").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/orders").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/orders/{orderId}").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/orders/{orderId}/cancel").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/orders/{orderId}/redo-to-cart").authenticated()

                        // ORDER endpoints for seller
                        .requestMatchers(HttpMethod.GET, "/api/orders/seller").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/orders/{orderId}/items/{itemId}/status").authenticated()

                        .anyRequest().permitAll())
                .httpBasic(Customizer.withDefaults())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .build();
    }

    @Bean
    JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withPublicKey(rsaKeysConfig.publicKey()).build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthoritiesClaimName("authorities");
        grantedAuthoritiesConverter.setAuthorityPrefix("");

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        return jwtAuthenticationConverter;
    }
}
