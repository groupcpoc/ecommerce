package com.ecommerce.apigateway.config;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import java.nio.charset.StandardCharsets;
/**
 * Returns a single, generic response body for every 403 produced by the
 * gateway's authorizeExchange() rules, regardless of which rule denied
 * the request or why. This avoids leaking role/ownership details to the caller.
 */
@Component
public class CustomAccessDeniedHandler implements ServerAccessDeniedHandler {
    private static final String BODY = "{\"error\": \"Access denied.\"}";
    @Override
    public Mono<Void> handle(ServerWebExchange exchange, AccessDeniedException ex) {
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        DataBuffer buffer = exchange.getResponse()
                .bufferFactory()
                .wrap(BODY.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
   