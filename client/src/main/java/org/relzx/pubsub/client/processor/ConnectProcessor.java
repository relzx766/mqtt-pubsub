package org.relzx.pubsub.client.processor;

import io.netty.channel.Channel;
import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.handler.codec.mqtt.MqttConnAckVariableHeader;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.relzx.pubsub.common.entity.MqttConnectOptions;
import org.relzx.pubsub.common.entity.ProcessResult;
import org.relzx.pubsub.common.util.MqttMessageUtil;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Getter
public class ConnectProcessor extends Processor<ProcessResult> {
    private Exception e;


    @Override
    public ProcessResult call() throws Exception {
        if (!isCancelled() && !receivedAck.get() && e == null) {
            synchronized (receivedAck) {
                receivedAck.wait(timeout);
            }
        }
        if (e != null) {
            throw e;
        }
        return receivedAck.get() ? ProcessResult.SUCCESS : ProcessResult.FAILED;
    }

    public ProcessResult connect(Channel channel, MqttConnectOptions options) throws Exception {
       return connect(channel, options, clientProperties.getTimeout());
    }

    public ProcessResult connect(Channel channel, MqttConnectOptions options, long timeout) throws Exception {
        this.timeout = timeout;
        MqttConnectMessage message = MqttMessageUtil.connectMessage(options);
        channel.writeAndFlush(message);
        log.info("发起登录连接==>{}", message);
        return execute().get(timeout, TimeUnit.MILLISECONDS);
    }

    public void processAck(MqttConnAckMessage msg) {
        MqttConnAckVariableHeader mqttConnAckVariableHeader = msg.variableHeader();
        String errormsg = "";
        switch (mqttConnAckVariableHeader.connectReturnCode()) {
            case CONNECTION_ACCEPTED:
                synchronized (receivedAck) {
                    receivedAck.set(true);
                    receivedAck.notify();
                }
                return;
            case CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD:
                errormsg = "用户名密码错误";
                break;
            case CONNECTION_REFUSED_IDENTIFIER_REJECTED:
                errormsg = "clientId不允许链接";
                break;
            case CONNECTION_REFUSED_SERVER_UNAVAILABLE:
                errormsg = "服务不可用";
                break;
            case CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION:
                errormsg = "mqtt 版本不可用";
                break;
            case CONNECTION_REFUSED_NOT_AUTHORIZED:
                errormsg = "未授权登录";
                break;
            default:
                errormsg = "未知问题";
                break;
        }

        synchronized (receivedAck) {
            e = new IOException(errormsg);
            receivedAck.notify();
        }
    }
}
