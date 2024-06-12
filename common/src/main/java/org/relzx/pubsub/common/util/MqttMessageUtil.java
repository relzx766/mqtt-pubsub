package org.relzx.pubsub.common.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.*;
import org.relzx.pubsub.common.entity.MqttConnectOptions;

import java.util.ArrayList;
import java.util.List;

public class MqttMessageUtil {

    public static MqttConnectReturnCode connectReturnCodeForException(Throwable cause) {
        MqttConnectReturnCode code;
        if (cause instanceof MqttUnacceptableProtocolVersionException) {
            // 不支持的协议版本
            code = MqttConnectReturnCode.CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION;
        } else if (cause instanceof MqttIdentifierRejectedException) {
            // 不合格的clientId
            code = MqttConnectReturnCode.CONNECTION_REFUSED_IDENTIFIER_REJECTED;
        } else {
            code = MqttConnectReturnCode.CONNECTION_REFUSED_SERVER_UNAVAILABLE;
        }
        return code;
    }
    public static MqttConnectMessage connectMessage(MqttConnectOptions options){
        MqttFixedHeader fixedHeader = new MqttFixedHeader(
                MqttMessageType.CONNECT,
                false,
                MqttQoS.AT_MOST_ONCE,
                false,
                10);
        MqttConnectVariableHeader variableHeader = new MqttConnectVariableHeader(
                options.getMqttVersion().protocolName(),
                options.getMqttVersion().protocolLevel(),
                options.isHasUsername(),
                options.isHasPassword(),
                options.isWillRetain(),
                options.getWillQos(),
                options.isWillFlag(),
                options.isCleanSession(),
                options.getKeepAliveTime());
        MqttConnectPayload payload = new MqttConnectPayload(
                options.getClientIdentifier(),
                options.getWillTopic(),
                options.getWillMessage(),
                options.getUsername(),
                options.getPassword());
        return new MqttConnectMessage(fixedHeader, variableHeader, payload);
    }
    public static MqttConnAckMessage connAckMessage(MqttConnectReturnCode returnCode, boolean sessionPresent) {
        MqttFixedHeader fixedHeader = new MqttFixedHeader(
                MqttMessageType.CONNACK,
                false,
                MqttQoS.AT_MOST_ONCE,
                false,
                0);
        MqttConnAckVariableHeader variableHeader = new MqttConnAckVariableHeader(
                returnCode,
                sessionPresent);
        return (MqttConnAckMessage) MqttMessageFactory.newMessage(
                fixedHeader,
                variableHeader,
                null);
    }

    public static MqttMessage disConnectMessage() {
        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.DISCONNECT, false, MqttQoS.AT_MOST_ONCE,
                false, 0x02);
        return new MqttMessage(mqttFixedHeader);
    }

    public static MqttSubscribeMessage subscribeMessage(int messageId, MqttQoS qos, String... topics) {
        List<MqttTopicSubscription> list = getMqttTopicSubscriptions(qos, topics);
        return subscribeMessage(messageId, list);
    }

    public static List<MqttTopicSubscription> getMqttTopicSubscriptions(MqttQoS qos, String... topics) {
        List<MqttTopicSubscription> list = new ArrayList<>();
        for (String topic : topics) {
            MqttTopicSubscription sb = new MqttTopicSubscription(topic, qos);
            list.add(sb);
        }
        return list;
    }

    public static MqttSubscribeMessage subscribeMessage(int messageId, List<MqttTopicSubscription> mqttTopicSubscriptions) {
        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.SUBSCRIBE, false, MqttQoS.AT_LEAST_ONCE,
                false, 0);
        MqttMessageIdVariableHeader mqttMessageIdVariableHeader = MqttMessageIdVariableHeader.from(messageId);
        MqttSubscribePayload mqttSubscribePayload = new MqttSubscribePayload(mqttTopicSubscriptions);
        return new MqttSubscribeMessage(mqttFixedHeader, mqttMessageIdVariableHeader, mqttSubscribePayload);
    }

    public static MqttSubAckMessage subAckMessage(List<MqttTopicSubscription> subscriptions,int messageId) {
        List<Integer> qosList = subscriptions.stream().map(sub -> sub.qualityOfService().value()).toList();
        return subAckMessage(messageId, qosList);
    }
    public static MqttSubAckMessage subAckMessage(int messageId, List<Integer> mqttQoSList) {
        return (MqttSubAckMessage) MqttMessageFactory.newMessage(
                new MqttFixedHeader(MqttMessageType.SUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0),
                MqttMessageIdVariableHeader.from(messageId),
                new MqttSubAckPayload(mqttQoSList));
    }
    public static MqttUnsubscribeMessage unsubscribeMessage(int messageId, List<String> topicList) {
        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.UNSUBSCRIBE, false, MqttQoS.AT_MOST_ONCE,
                false, 0x02);
        MqttMessageIdVariableHeader variableHeader = MqttMessageIdVariableHeader.from(messageId);
        MqttUnsubscribePayload mqttUnsubscribeMessage = new MqttUnsubscribePayload(topicList);
        return new MqttUnsubscribeMessage(mqttFixedHeader, variableHeader, mqttUnsubscribeMessage);
    }

    public static MqttUnsubAckMessage unsubAckMessage(int messageId) {
        return (MqttUnsubAckMessage) MqttMessageFactory.newMessage(
                new MqttFixedHeader(MqttMessageType.UNSUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0),
                MqttMessageIdVariableHeader.from(messageId),
                null);
    }
    public static MqttMessage pingReqMessage() {
        return MqttMessageFactory.newMessage(
                new MqttFixedHeader(MqttMessageType.PINGREQ, false, MqttQoS.AT_MOST_ONCE, false, 0),
                null,
                null);
    }

    public static MqttMessage pingRespMessage() {
        return MqttMessageFactory.newMessage(
                new MqttFixedHeader(MqttMessageType.PINGRESP, false, MqttQoS.AT_MOST_ONCE, false, 0),
                null,
                null);
    }

    public static MqttPublishMessage publishMessage(String topic, byte[] payload, int qosValue, int messageId, boolean isRetain) {
        return publishMessage(topic, payload, qosValue, isRetain, messageId, false);
    }

    public static MqttPublishMessage publishMessage(String topic, byte[] payload, int qosValue, boolean isRetain, int messageId, boolean isDup) {
        return (MqttPublishMessage) MqttMessageFactory.newMessage(
                new MqttFixedHeader(MqttMessageType.PUBLISH, isDup, MqttQoS.valueOf(qosValue), isRetain, 0),
                new MqttPublishVariableHeader(topic, messageId),
                Unpooled.buffer().writeBytes(payload));
    }
    public static MqttPublishMessage publishMessage(String topic, byte[] payload, MqttQoS qoS, boolean isRetain, int messageId, boolean isDup) {
        return (MqttPublishMessage) MqttMessageFactory.newMessage(
                new MqttFixedHeader(MqttMessageType.PUBLISH, isDup, qoS, isRetain, 0),
                new MqttPublishVariableHeader(topic, messageId),
                Unpooled.buffer().writeBytes(payload));
    }
    public static MqttPublishMessage duePublishMessage(MqttPublishMessage message){
        MqttFixedHeader fixedHeader = message.fixedHeader();
        MqttPublishVariableHeader variableHeader = message.variableHeader();
        ByteBuf payload = message.payload();
        return publishMessage(variableHeader.topicName(),payload.array(),fixedHeader.qosLevel(), fixedHeader.isRetain(), variableHeader.packetId(),true);
    }
    public static MqttMessage pubCompMessage(int messageId) {
        return MqttMessageFactory.newMessage(
                new MqttFixedHeader(MqttMessageType.PUBCOMP, false, MqttQoS.AT_MOST_ONCE, false, 0),
                MqttMessageIdVariableHeader.from(messageId),
                null);
    }

    public static MqttPubAckMessage pubAckMessage(int messageId) {
        return (MqttPubAckMessage) MqttMessageFactory.newMessage(
                new MqttFixedHeader(MqttMessageType.PUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0),
                MqttMessageIdVariableHeader.from(messageId),
                null);
    }

    public static MqttMessage pubRecMessage(int messageId) {
        return MqttMessageFactory.newMessage(
                new MqttFixedHeader(MqttMessageType.PUBREC, false, MqttQoS.AT_MOST_ONCE, false, 0),
                MqttMessageIdVariableHeader.from(messageId),
                null);
    }
    public static MqttMessage pubRelMessage(int messageId){
        return MqttMessageFactory.newMessage(
                new MqttFixedHeader(MqttMessageType.PUBREL, false, MqttQoS.AT_MOST_ONCE, false, 0),
                MqttMessageIdVariableHeader.from(messageId),
                null
        );
    }

}
