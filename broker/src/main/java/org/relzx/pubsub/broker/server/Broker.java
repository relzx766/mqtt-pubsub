package org.relzx.pubsub.broker.server;

import io.netty.channel.Channel;
import io.netty.handler.codec.mqtt.*;
import org.relzx.pubsub.common.entity.ClientInfo;
import org.relzx.pubsub.common.server.Server;

public interface Broker extends Server {
    void connack(MqttConnAckMessage message,Channel channel);
    void publish(MqttPublishMessage message,Channel channel);
    void puback(int messageId,Channel channel);
    void pubrec(int messageId,Channel channel);
    void pubrel(int messageId,Channel channel);
    void pubcomp(int messageId,Channel channel);
    void suback(MqttSubscribeMessage message,Channel channel);
    void unsuback(MqttUnsubscribeMessage message,Channel channel);
    void pong(MqttMessage message,Channel channel);
    void disconnect(Channel channel);
    void sessionCheck(ClientInfo clientInfo);
    interface Callback{

        void onMessage(Object msg, Channel channel);
        void onConnectRec(MqttConnectMessage message,Channel channel);
        void onPublishRec(MqttPublishMessage message,Channel channel);
        void onPubAckRec(MqttMessage message,Channel channel);
        void onPubRecRec(MqttMessage message,Channel channel);
        void onPubRelRec(MqttMessage message,Channel channel);
        void onPubCompRec(MqttMessage message,Channel channel);
        void onSubscribeRec(MqttSubscribeMessage message,Channel channel);
        void onUnsubscribeRec(MqttUnsubscribeMessage message,Channel channel);

        void onPingRec(MqttMessage message,Channel channel);
        void onDisconnectRec(MqttMessage message,Channel channel);
    }
}
