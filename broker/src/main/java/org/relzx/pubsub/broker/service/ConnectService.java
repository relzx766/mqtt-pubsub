package org.relzx.pubsub.broker.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalListener;
import io.netty.channel.Channel;
import io.netty.handler.codec.mqtt.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.relzx.pubsub.broker.properties.BrokerProperties;
import org.relzx.pubsub.broker.properties.factory.BrokerPropertiesFactory;
import org.relzx.pubsub.common.convertor.ClientInfoConvertor;
import org.relzx.pubsub.common.entity.ClientInfo;
import org.relzx.pubsub.common.event.EventType;
import org.relzx.pubsub.common.event.PongEvent;
import org.relzx.pubsub.common.event.SessionInvalidateEvent;
import org.relzx.pubsub.common.exception.ConnectException;
import org.relzx.pubsub.common.listener.Listener;
import org.relzx.pubsub.common.util.EventBusUtil;
import org.relzx.pubsub.common.util.MqttMessageUtil;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ConnectService {
    @Getter
    private static final ConnectService instance = new ConnectService();
    private final BrokerProperties brokerProperties = BrokerPropertiesFactory.getInstance();
    private final Map<Channel, String> channelIdMap = new HashMap<>();
    private Cache<String, ClientInfo> sessionCache;

    private ConnectService() {
        initSessionCache();
        EventBusUtil.registerAsync(new Listener<PongEvent>(EventType.PONG) {
            @Override
            public void onEvent(PongEvent event) {
                onPongEvent(event);
            }
        });
    }

    private void initSessionCache() {
        sessionCache = Caffeine.newBuilder()
                .initialCapacity(brokerProperties.getSessionCacheCapacity())
                .executor(Executors.newVirtualThreadPerTaskExecutor())
                .expireAfterWrite(brokerProperties.getSessionTimeout(), TimeUnit.SECONDS)
                .removalListener((RemovalListener<String, ClientInfo>) (key, value, cause) -> {
                    if (value != null) {
                        Channel channel = value.getChannel();
                        channelIdMap.remove(channel);
                        channel.close();
                        if (cause.wasEvicted()) {
                            EventBusUtil.postAsync(new SessionInvalidateEvent(value, SessionInvalidateEvent.Cause.EXPIRE));
                        } else {
                            EventBusUtil.postAsync(new SessionInvalidateEvent(value, SessionInvalidateEvent.Cause.INVALIDATE));
                        }
                    }
                })
                .build();
    }

    public MqttConnAckMessage connect(MqttConnectMessage message, Channel channel) {
        MqttConnectPayload payload = message.payload();
        boolean cleanSession = message.variableHeader().isCleanSession();
        ClientInfo clientInfo = ClientInfoConvertor.MCM2ClientInfo(message);
        clientInfo.setChannel(channel);
        if (cleanSession) {
            invalidateSession(clientInfo.getClientIdentifier());
        }
        boolean sessionPresent = !cleanSession && sessionCache.getIfPresent(clientInfo.getClientIdentifier()) != null;
        try {
            check(message);
        } catch (ConnectException e) {
            MqttConnectReturnCode returnCode = e.getReturnCode();
            return MqttMessageUtil.connAckMessage(returnCode, sessionPresent);
        }
        String password = new String(payload.passwordInBytes(), StandardCharsets.UTF_8);
        if (brokerProperties.getPassword().equals(password)) {
            clientInfo.setSessionTime(System.currentTimeMillis());
            sessionCache.put(clientInfo.getClientIdentifier(), clientInfo);
            channelIdMap.put(clientInfo.getChannel(), clientInfo.getClientIdentifier());
            log.info("客户端:{} 成功登录", clientInfo.getClientIdentifier());
            return MqttMessageUtil.connAckMessage(MqttConnectReturnCode.CONNECTION_ACCEPTED, sessionPresent);
        } else {
            return MqttMessageUtil.connAckMessage(MqttConnectReturnCode.CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD, sessionPresent);
        }
    }

    public void disconnect(Channel channel) {
        String id = channelIdMap.get(channel);
        sessionCache.invalidate(id);
    }

    private void check(MqttConnectMessage message) throws ConnectException {
        MqttFixedHeader fixedHeader = message.fixedHeader();
        MqttConnectVariableHeader variableHeader = message.variableHeader();
        if (!protocolCheck(variableHeader.version())) {
            throw new ConnectException(MqttConnectReturnCode.CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION);
        }

    }

    private boolean protocolCheck(int version) {
        return brokerProperties.getMqttVersion() == version;
    }

    public ClientInfo getClientInfo(Channel channel) {
        String id = channelIdMap.get(channel);
        return sessionCache.getIfPresent(id);
    }

    public ClientInfo getClientInfo(String id) {
        return sessionCache.getIfPresent(id);
    }

    private void refreshSession(String id, Channel channel) {
        ClientInfo clientInfo = sessionCache.getIfPresent(id);
        if (clientInfo != null) {
            clientInfo.setChannel(channel);
            clientInfo.setSessionTime(System.currentTimeMillis());
            sessionCache.put(id, clientInfo);
            channelIdMap.put(channel, id);
        } else {
            channel.close();
        }
    }

    private void invalidateSession(String id) {
        sessionCache.invalidate(id);
    }

    private void onPongEvent(PongEvent event) {
        Channel channel = event.getChannel();
        String id = channelIdMap.get(channel);
        refreshSession(id, channel);
    }

    public boolean isSessionAlive(Channel channel) {
        return isSessionAlive(getClientInfo(channel));
    }

    public boolean isSessionAlive(ClientInfo clientInfo) {
        return clientInfo != null && clientInfo.getSessionTime() + TimeUnit.SECONDS.toMillis(brokerProperties.getSessionTimeout()) > System.currentTimeMillis();
    }

}
