package org.relzx.pubsub.client.processor;

import io.netty.channel.Channel;
import io.netty.handler.codec.mqtt.MqttMessageIdAndPropertiesVariableHeader;
import io.netty.handler.codec.mqtt.MqttUnsubAckMessage;
import io.netty.handler.codec.mqtt.MqttUnsubscribeMessage;
import lombok.extern.slf4j.Slf4j;
import org.relzx.pubsub.common.entity.ProcessResult;
import org.relzx.pubsub.common.util.MessageIdUtil;
import org.relzx.pubsub.common.util.MqttMessageUtil;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
public class UnsubscribeProcessor extends Processor<ProcessResult> {

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

    public ProcessResult unsubscribe(Channel channel, String... topics) throws Exception {
        return unsubscribe(channel, clientProperties.getTimeout(), topics);
    }

    public ProcessResult unsubscribe(Channel channel, long timeout, String... topics) throws Exception {
        this.timeout = timeout;
        int id = 0;
        try {
            id = MessageIdUtil.getForClient();
            this.msgId = id;
            MqttUnsubscribeMessage msg = MqttMessageUtil.unsubscribeMessage(id, List.of(topics));
            log.info("取消订阅主题==>{}" , msg);
            channel.writeAndFlush(msg);
            return execute().get(timeout, TimeUnit.MILLISECONDS);
        } finally {
            MessageIdUtil.release(id);
        }
    }
    public void processAck( MqttUnsubAckMessage msg) {
        MqttMessageIdAndPropertiesVariableHeader variableHeader = msg.idAndPropertiesVariableHeader();
        if (variableHeader.messageId() == msgId) {
            synchronized (receivedAck) {
                receivedAck.set(true);
                receivedAck.notify();
            }
        }
    }

}
