package com.example.talaria.backend.ws;

import com.example.talaria.backend.service.SessionService;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Component
public class AuthHandshakeInterceptor implements HandshakeInterceptor {

    private final SessionService sessionService;

    public AuthHandshakeInterceptor(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest req, ServerHttpResponse res,
                                   WebSocketHandler handler, Map<String, Object> attrs) {
        var q = UriComponentsBuilder.fromUri(req.getURI()).build().getQueryParams();
        String sid = q.getFirst("sid");
        String token = q.getFirst("token");
        if (sid == null || token == null || !sessionService.validate(sid, token)) {
            return false;
        }
        attrs.put("sid", sid);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest req, ServerHttpResponse res,
                               WebSocketHandler handler, Exception ex) {}
}
