package com.example.talaria.backend.config;

import com.example.talaria.backend.ws.AuthHandshakeInterceptor;
import com.example.talaria.backend.ws.SubtitleWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;

import java.net.*;
import java.util.*;
import java.util.stream.Collectors;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final SubtitleWebSocketHandler handler;
    private final AuthHandshakeInterceptor auth;

    public WebSocketConfig(SubtitleWebSocketHandler handler, AuthHandshakeInterceptor auth) {
        this.handler = handler;
        this.auth = auth;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        String[] patterns = buildAllowedOriginPatterns();

        registry.addHandler(handler, "/ws/subtitles")
                .addInterceptors(auth)
                // ⬇ setAllowedOrigins(...) 대신 패턴 기반 메서드 사용
                .setAllowedOriginPatterns(patterns);
    }

    /** 실행 시점에 개발PC의 사설 IPv4들을 찾아서 http/https 오리진 패턴을 생성 */
    private String[] buildAllowedOriginPatterns() {
        Set<String> pat = new LinkedHashSet<>();
        // 기본 로컬 호스트
        pat.add("http://localhost:*");
        pat.add("https://localhost:*");
        pat.add("http://127.0.0.1:*");
        pat.add("https://127.0.0.1:*");

        try {
            Enumeration<NetworkInterface> nics = NetworkInterface.getNetworkInterfaces();
            while (nics.hasMoreElements()) {
                NetworkInterface nic = nics.nextElement();
                if (!nic.isUp() || nic.isLoopback()) continue;

                Enumeration<InetAddress> addrs = nic.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (addr instanceof Inet4Address && addr.isSiteLocalAddress()) {
                        String ip = addr.getHostAddress();
                        pat.add("http://" + ip + ":*");
                        pat.add("https://" + ip + ":*");
                    }
                }
            }
        } catch (SocketException ignore) { /* 개발 편의: 실패해도 기본 패턴만 사용 */ }

        pat.add("http://192.168.*:*");
        pat.add("https://192.168.*:*");
        pat.add("http://10.*:*");
        pat.add("https://10.*:*");

        return pat.toArray(String[]::new);
    }
}
