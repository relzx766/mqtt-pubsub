package org.relzx.pubsub.client;

import io.netty.handler.codec.mqtt.*;
import org.relzx.pubsub.common.server.Server;

public interface Client extends Server {
    void connect();

    void reconnect();

    void disconnect();

    void publish(String topic, MqttQoS qoS, String content);

    void publish(String topic, MqttQoS qoS, String content, boolean isRetain, boolean isDup);


    void subscribe(MqttQoS qoS, long timeout, String... topic);

    void unsubscribe(String... topic);

    void connack(MqttConnAckMessage message);

    void puback(MqttPubAckMessage message);


    void suback(MqttSubAckMessage message);

    void unsuback(MqttUnsubAckMessage message);

    void pong(MqttMessage message);

    void ping();

    void pingTask();


    interface Callback {


        void onConnectLost();

        void onConnected();

        void onConnectFailed();

        void onReconnectFailed();

        void onMessage(Object msg);

        void onMessageReceived(MqttPublishMessage message);
    }
}
