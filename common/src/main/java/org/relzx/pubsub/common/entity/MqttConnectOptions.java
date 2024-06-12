package org.relzx.pubsub.common.entity;

import io.netty.handler.codec.mqtt.MqttVersion;
import lombok.Builder;
import lombok.Data;
import org.relzx.pubsub.common.util.CollectionUtil;
import org.relzx.pubsub.common.util.StringUtil;

@Data
@Builder
public class MqttConnectOptions {

    // 可变报头部分
    private MqttVersion mqttVersion = MqttVersion.MQTT_3_1_1;
    private boolean isWillRetain = false;
    private int willQos = 0;
    private boolean isWillFlag = false;
    private boolean isCleanSession = false;
    private int keepAliveTime = 60;

    // 有效载荷 ：客户端标识符，遗嘱主题，遗嘱消息，用户名，密码
    private String clientIdentifier = "";
    private String willTopic = "";
    private byte[] willMessage;
    private String username = "";
    private byte[] password;

    public boolean isHasUsername() {
        return StringUtil.isNotBlank(this.username);
    }

    public boolean isHasPassword() {
        return isHasUsername()&&CollectionUtil.isNotEmpty(this.password);
    }
}
