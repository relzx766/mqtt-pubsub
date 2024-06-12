package org.relzx.pubsub.client.processor;

import io.netty.channel.Channel;
import io.netty.handler.codec.mqtt.MqttMessageIdAndPropertiesVariableHeader;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttSubAckMessage;
import io.netty.handler.codec.mqtt.MqttSubscribeMessage;
import lombok.extern.slf4j.Slf4j;
import org.relzx.pubsub.common.entity.ProcessResult;
import org.relzx.pubsub.common.util.MessageIdUtil;
import org.relzx.pubsub.common.util.MqttMessageUtil;

import java.util.concurrent.TimeUnit;

@Slf4j
public class SubscribeProcessor extends Processor<ProcessResult> {

    private int msgId;

    @Override
    public ProcessResult call() throws Exception {
        if (!isCancelled() && !receivedAck.get()) {
            synchronized (receivedAck) {
                receivedAck.wait(timeout);
            }
        }
        return receivedAck.get()?ProcessResult.SUCCESS:ProcessResult.FAILED;
    }
    public ProcessResult subscribe(Channel channel,MqttQoS qos,String topics) throws Exception {
        return subscribe(channel,qos,clientProperties.getTimeout(),topics);
    }
    public ProcessResult subscribe(Channel channel, MqttQoS qoS,long timeout,String... topics)  throws Exception{
        this.timeout = timeout;
        int id = 0;
        try {
            id = MessageIdUtil.getForClient();
            this.msgId = id;
            MqttSubscribeMessage msg = MqttMessageUtil.subscribeMessage(id, qoS, topics);
            log.info("订阅主题==>{}：" , msg);
            channel.writeAndFlush(msg);
            return execute().get(timeout, TimeUnit.MILLISECONDS);
        } finally {
            MessageIdUtil.release(id);
        }
    }
    public void processAck( MqttSubAckMessage msg) {
        MqttMessageIdAndPropertiesVariableHeader variableHeader = msg.idAndPropertiesVariableHeader();
        if (variableHeader.messageId() == msgId) {
            synchronized (receivedAck) {
                receivedAck.set(true);
                receivedAck.notify();
            }
        }
    }
}
