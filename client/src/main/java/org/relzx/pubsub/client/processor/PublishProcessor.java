package org.relzx.pubsub.client.processor;

import io.netty.channel.Channel;
import io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader;
import io.netty.handler.codec.mqtt.MqttPubAckMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.extern.slf4j.Slf4j;
import org.relzx.pubsub.common.entity.ProcessResult;
import org.relzx.pubsub.common.util.MessageIdUtil;
import org.relzx.pubsub.common.util.MqttMessageUtil;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Slf4j
public class PublishProcessor extends Processor<ProcessResult> {
    private int msgId;

    @Override
    public ProcessResult call() throws Exception {
        if (!isCancelled() && !receivedAck.get()) {
            synchronized (receivedAck) {
                receivedAck.wait(timeout);
            }
        }
        return receivedAck.get() ? ProcessResult.SUCCESS : ProcessResult.FAILED;
    }

    public ProcessResult publish(Channel channel, String topic, MqttQoS qoS, String content) throws Exception {
        return publish(channel,topic,qoS,content,false,false);
    }
    public ProcessResult publish(Channel channel, String topic, MqttQoS qoS, String content, boolean isRetain,boolean isDup) throws Exception {
      return   publish(channel,topic,qoS,content,clientProperties.getTimeout(),isRetain,isDup);
    }
    public ProcessResult publish(Channel channel, String topic, MqttQoS qoS, String content, long timeout,boolean isRetain,boolean isDup) throws Exception {
        this.timeout = timeout;
        int id = 0;
        try {
            id =MqttQoS.AT_MOST_ONCE.equals(qoS)?-1: MessageIdUtil.getForClient();
            this.msgId = id;
            MqttPublishMessage msg = MqttMessageUtil.publishMessage(topic,
                    content.getBytes(StandardCharsets.UTF_8),
                    qoS,
                    isRetain,
                    id,
                    isDup
            );
            log.info("发送消息==>{}", msg);
            channel.writeAndFlush(msg);
            if (MqttQoS.AT_MOST_ONCE.equals(qoS)){
                    processAck();
            }
            return execute().get(timeout, TimeUnit.MILLISECONDS);
        } finally {
            MessageIdUtil.release(id);
        }
    }

    public void processAck(MqttPubAckMessage msg) {
        MqttMessageIdVariableHeader variableHeader = msg.variableHeader();
        if (variableHeader.messageId() == msgId&&!receivedAck.get()) {
           processAck();
        }
    }
    public void processAck(){
        synchronized (receivedAck) {
            receivedAck.set(true);
            receivedAck.notify();
        }
    }
}
