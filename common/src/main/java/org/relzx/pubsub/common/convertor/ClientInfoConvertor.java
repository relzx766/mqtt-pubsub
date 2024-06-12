package org.relzx.pubsub.common.convertor;

import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttConnectPayload;
import io.netty.handler.codec.mqtt.MqttConnectVariableHeader;
import org.relzx.pubsub.common.entity.ClientInfo;
import org.relzx.pubsub.common.entity.MqttConnectOptions;

public class ClientInfoConvertor {
    /**
     * {@link MqttConnectOptions} 转换为 {@link ClientInfo}
     */
    public static ClientInfo MCO2ClientInfo(MqttConnectOptions options){
       return ClientInfo.builder()
                .clientIdentifier(options.getClientIdentifier())
                .willQos(options.getWillQos())
                .willMessage(options.getWillMessage())
                .willTopic(options.getWillTopic())
                .isWillFlag(options.isWillFlag())
                .isWillRetain(options.isWillRetain())
                .build();
    }

    /**
     * {@link MqttConnectMessage}转换为{@link ClientInfo}
     */
    public static ClientInfo MCM2ClientInfo(MqttConnectMessage message){
        MqttConnectVariableHeader variable = message.variableHeader();
        MqttConnectPayload payload = message.payload();
        return ClientInfo.builder()
                .clientIdentifier(payload.clientIdentifier())
                .willQos(variable.willQos())
                .isWillFlag(variable.isWillFlag())
                .isWillRetain(variable.isWillRetain())
                .willTopic(payload.willTopic())
                .willMessage(payload.willMessageInBytes())
                .build();
    }
}
