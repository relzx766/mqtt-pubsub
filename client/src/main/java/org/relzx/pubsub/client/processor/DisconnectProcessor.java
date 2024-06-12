package org.relzx.pubsub.client.processor;

import io.netty.channel.Channel;
import io.netty.handler.codec.mqtt.MqttMessage;
import org.relzx.pubsub.common.entity.ProcessResult;
import org.relzx.pubsub.common.util.MqttMessageUtil;



public class DisconnectProcessor extends Processor<ProcessResult>{
    @Override
    public ProcessResult call() throws Exception {
        return null;
    }
    public  void disconnect(Channel channel){
        MqttMessage mqttMessage = MqttMessageUtil.disConnectMessage();
        channel.writeAndFlush(mqttMessage);
    }
}
