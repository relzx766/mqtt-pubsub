package org.relzx.pubsub.client.processor;

import io.netty.channel.Channel;
import io.netty.handler.codec.mqtt.MqttMessage;
import lombok.extern.slf4j.Slf4j;
import org.relzx.pubsub.common.entity.ProcessResult;
import org.relzx.pubsub.common.util.MqttMessageUtil;

import java.util.concurrent.TimeUnit;


@Slf4j
public class PingProcessor extends Processor<ProcessResult> {
    public Channel channel;
    public long keepAlive = clientProperties.getKeepAlive();


    @Override
    public ProcessResult call() throws InterruptedException {
        if (!isCancelled() && !receivedAck.get()) {
            synchronized (receivedAck) {
                receivedAck.wait(TimeUnit.SECONDS.toMillis(keepAlive));
            }
        }
        return receivedAck.get()?ProcessResult.SUCCESS:ProcessResult.FAILED;

      /*  while (!isCancelled()) {
            receivedAck.set(false);
            ping(channel);
            if (!isCancelled() && !receivedAck.get()) {
                synchronized (receivedAck) {
                    receivedAck.wait(TimeUnit.SECONDS.toMillis(keepAlive) / 2);
                }
            }
            if (!receivedAck.get()) {
                TimeoutException te = new TimeoutException("Did not receive a response for a long time : " + keepAlive + "s");
                if (cb != null) {
                    cb.onConnectLost(te);
                }
                throw te;
            }
            Thread.sleep(TimeUnit.SECONDS.toMillis(keepAlive));
        }
        return null;*/
    }



    public ProcessResult ping(Channel channel) throws Exception {
        MqttMessage msg = MqttMessageUtil.pingReqMessage();
        log.info("[ping]==>{}：", channel.remoteAddress());
        channel.writeAndFlush(msg);
       return execute().get(keepAlive,TimeUnit.MILLISECONDS);
    }

    public void processAck( MqttMessage msg) {
        synchronized (receivedAck) {
            receivedAck.set(true);
            receivedAck.notify();
        }
    }


}
