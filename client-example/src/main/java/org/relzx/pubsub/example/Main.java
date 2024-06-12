package org.relzx.pubsub.example;

import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttSubscriptionOption;
import io.netty.handler.codec.mqtt.MqttTopicSubscription;
import lombok.extern.slf4j.Slf4j;
import org.relzx.pubsub.client.MqttClient;
import org.relzx.pubsub.client.container.MqttListenerContainer;
import org.relzx.pubsub.client.listener.MqttMessageListener;
import org.relzx.pubsub.client.properties.ClientProperties;
import org.relzx.pubsub.client.properties.PropertiesFactory;
@Slf4j
public class Main {
    public static void main(String[] args) {
        ClientProperties properties= PropertiesFactory.getProperties();
        MqttClient client = MqttClient.getInstance();
        client.start();
        MqttListenerContainer container = new MqttListenerContainer();
        MqttTopicSubscription subscription = new MqttTopicSubscription("123", MqttQoS.AT_MOST_ONCE);
        container.addListener(new MqttMessageListener(subscription) {
            @Override
            public void onMessage(MqttMessage message) {
                log.info("收到消息:{}",message);
            }
        });

        new Thread(()->{
            for (;;) {
                try {
                client.publish("123",MqttQoS.AT_LEAST_ONCE,"hello");

                    Thread.sleep(100);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}