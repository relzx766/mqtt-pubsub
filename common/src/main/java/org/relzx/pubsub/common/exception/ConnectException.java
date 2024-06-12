package org.relzx.pubsub.common.exception;

import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import lombok.Getter;

@Getter
public class ConnectException extends Exception{
    MqttConnectReturnCode returnCode;


    public ConnectException(MqttConnectReturnCode returnCode) {
        this.returnCode = returnCode;
    }
}
