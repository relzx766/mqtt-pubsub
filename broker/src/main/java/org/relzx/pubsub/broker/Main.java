package org.relzx.pubsub.broker;

import org.relzx.pubsub.broker.server.MqttBroker;

public class Main {
    public static void main(String[] args) {
        MqttBroker mqttBroker = MqttBroker.getInstance();
        mqttBroker.start();
    }
}