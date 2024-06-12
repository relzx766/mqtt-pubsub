package org.relzx.pubsub.common.entity;

import io.netty.channel.Channel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ClientInfo {
    private String clientIdentifier;
    private boolean isWillRetain;
    private int willQos;
    private boolean isWillFlag;
    private String willTopic;
    private byte[] willMessage;
    private Channel channel;
    private long sessionTime;
}
