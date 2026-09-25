package com.worksure.config;

import com.corundumstudio.socketio.AuthorizationResult;
import com.corundumstudio.socketio.HandshakeData;
import com.corundumstudio.socketio.SocketIOServer;
import com.worksure.security.JwtService;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SocketIOConfig {

    @Bean(destroyMethod = "stop")
    public SocketIOServer socketIOServer(
            JwtService jwtService,
            @Value("${app.socketio.port:9092}") int port,
            @Value("${app.frontend-url}") String frontendUrl
    ) {
        com.corundumstudio.socketio.Configuration config = new com.corundumstudio.socketio.Configuration();
        config.setHostname("0.0.0.0");
        config.setPort(port);
        config.setOrigin(frontendUrl);
        config.setRandomSession(true);
        config.setAuthorizationListener(data -> {
            String token = tokenFromHandshake(data);
            if (token == null) {
                return AuthorizationResult.FAILED_AUTHORIZATION;
            }
            try {
                jwtService.parse(token);
                return AuthorizationResult.SUCCESSFUL_AUTHORIZATION;
            } catch (Exception e) {
                return AuthorizationResult.FAILED_AUTHORIZATION;
            }
        });

        SocketIOServer server = new SocketIOServer(config);
        server.addConnectListener(client -> {
            String token = tokenFromHandshake(client.getHandshakeData());
            if (token == null) {
                client.disconnect();
                return;
            }
            try {
                Claims claims = jwtService.parse(token);
                String userId = claims.getSubject();
                client.set("userId", userId);
                client.joinRoom("user:" + userId);
            } catch (Exception e) {
                client.disconnect();
            }
        });
        server.addEventListener("join:booking", Object.class, (client, data, ackSender) -> {
            if (data != null) {
                client.joinRoom("booking:" + data);
            }
        });
        server.addEventListener("leave:booking", Object.class, (client, data, ackSender) -> {
            if (data != null) {
                client.leaveRoom("booking:" + data);
            }
        });
        server.start();
        System.out.println("WorkSure Socket.IO listening on port " + port);
        return server;
    }

    private static String tokenFromHandshake(HandshakeData data) {
        String token = data.getSingleUrlParam("token");
        if (token != null && !token.isBlank()) {
            return token;
        }
        String auth = data.getHttpHeaders().get("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            return auth.substring(7);
        }
        return null;
    }
}
