package org.relzx.pubsub.broker.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.codec.mqtt.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.relzx.pubsub.broker.properties.BrokerProperties;
import org.relzx.pubsub.broker.properties.factory.BrokerPropertiesFactory;
import org.relzx.pubsub.broker.service.*;
import org.relzx.pubsub.common.entity.ClientInfo;
import org.relzx.pubsub.common.event.*;
import org.relzx.pubsub.common.exception.ReqRejectException;
import org.relzx.pubsub.common.util.EventBusUtil;
import org.relzx.pubsub.common.util.MqttMessageUtil;

@Slf4j
@Data
public class MqttBroker implements Broker, Broker.Callback {
    private static MqttBroker INSTANCE;
    private NioEventLoopGroup boss = new NioEventLoopGroup();
    private NioEventLoopGroup worker = new NioEventLoopGroup();
    private BrokerProperties brokerProperties = BrokerPropertiesFactory.getInstance();
    private ConnectService connectService;
    private PublishService publishService;
    private SubscribeService subscribeService;
    private PubAckService pubAckService;
    private PubRecService pubRecService;
    private PubRelService pubRelService;

    private MqttBroker() {
        // 初始化代码
    }

    public static synchronized MqttBroker getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new MqttBroker();
            INSTANCE.initServices();
        }
        return INSTANCE;
    }

    private void initServices() {
        connectService = ConnectService.getInstance();
        publishService = PublishService.getInstance();
        subscribeService = SubscribeService.getInstance();
        pubAckService = PubAckService.getInstance();
        pubRecService = PubRecService.getInstance();
        pubRelService=PubRelService.getInstance();
    }



    @Override
    public void start() {
        registerShutdownHook();
        long beginTime = System.currentTimeMillis();
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap
                .group(boss, worker)
                .channel(NioServerSocketChannel.class)
                .childHandler(new BrokerInitializer())
                .childOption(ChannelOption.SO_KEEPALIVE, true);
        try {
            ChannelFuture channelFuture = serverBootstrap.bind(brokerProperties.getHost(), brokerProperties.getPort()).sync();
            channelFuture.addListener(future -> {
                if (future.isSuccess()) {
                    log.info("broker已启动,耗时:{}", System.currentTimeMillis() - beginTime);
                }
            });
        } catch (InterruptedException e) {
            log.error("broker启动失败", e);
            shutdown();
        }

    }

    @Override
    public void shutdown() {
        if (worker != null) {
            worker.shutdownGracefully();
        }
        if (boss != null) {
            boss.shutdownGracefully();
        }
        log.info("broker成功关闭");
    }

    @Override
    public void connack(MqttConnAckMessage message, Channel channel) {
        channel.writeAndFlush(message);
    }

    @Override
    public void publish(MqttPublishMessage message, Channel channel) {
        channel.writeAndFlush(message);
        ClientInfo clientInfo = connectService.getClientInfo(channel);
        EventBusUtil.postAsync(new PublishEvent(
                clientInfo,
                message
        ));
    }

    @Override
    public void puback(int messageId, Channel channel) {
        MqttPubAckMessage ackMessage = MqttMessageUtil.pubAckMessage(messageId);
        channel.writeAndFlush(ackMessage);
        ClientInfo clientInfo = connectService.getClientInfo(channel);
        EventBusUtil.postAsync(new PubAckEvent(
                clientInfo.getClientIdentifier(),
                messageId
        ));
    }

    @Override
    public void pubrec(int messageId, Channel channel) {
        MqttMessage pubRecMessage = MqttMessageUtil.pubRecMessage(messageId);
        channel.writeAndFlush(pubRecMessage);
        ClientInfo clientInfo = connectService.getClientInfo(channel);
        EventBusUtil.postAsync(new PubRecEvent(
                clientInfo, messageId

        ));
    }

    @Override
    public void pubrel(int messageId, Channel channel) {
        MqttMessage recMessage = MqttMessageUtil.pubRelMessage(messageId);
        channel.writeAndFlush(recMessage);
        ClientInfo clientInfo = connectService.getClientInfo(channel);
        EventBusUtil.postAsync(new PubRelEvent(
                clientInfo,messageId
        ));
    }

    @Override
    public void pubcomp(int messageId, Channel channel) {
        MqttMessage compMessage = MqttMessageUtil.pubCompMessage(messageId);
        channel.writeAndFlush(compMessage);
        ClientInfo clientInfo = connectService.getClientInfo(channel);
        EventBusUtil.postAsync(new PubCompEvent(
                clientInfo,messageId
        ));
    }

    @Override
    public void suback(MqttSubscribeMessage message, Channel channel) {
        MqttSubAckMessage subAckMessage = MqttMessageUtil.subAckMessage(message.payload().topicSubscriptions(), message.variableHeader().messageId());
        channel.writeAndFlush(subAckMessage);
    }

    @Override
    public void unsuback(MqttUnsubscribeMessage message, Channel channel) {
        MqttUnsubAckMessage unsubAckMessage = MqttMessageUtil.unsubAckMessage(message.variableHeader().messageId());
        channel.writeAndFlush(unsubAckMessage);
    }

    @Override
    public void pong(MqttMessage message, Channel channel) {
        MqttMessage pongMessage = MqttMessageUtil.pingRespMessage();
        channel.writeAndFlush(pongMessage);
        EventBusUtil.postAsync(new PongEvent(channel));
    }

    @Override
    public void disconnect(Channel channel) {
        connectService.disconnect(channel);
        channel.close();
    }

    @Override
    public void sessionCheck(ClientInfo clientInfo) {
        if (!connectService.isSessionAlive(clientInfo)) {
            throw new ReqRejectException("该客户端未登录或session已过期");
        }
    }
    @Override
    public void onMessage(Object msg, Channel channel) {
        log.info("接收到消息:{}",msg);
        if (msg != null) {
            MqttMessage message = (MqttMessage) msg;
            MqttMessageType messagedType = message.fixedHeader().messageType();
            switch (messagedType) {
                case CONNECT -> onConnectRec((MqttConnectMessage) message, channel);
                case PUBLISH -> onPublishRec((MqttPublishMessage) message, channel);
                case PUBACK -> onPubAckRec(message, channel);
                case PUBREC -> onPubRecRec(message, channel);
                case PUBREL -> onPubRelRec(message, channel);
                case PUBCOMP -> onPubCompRec(message, channel);
                case SUBSCRIBE -> onSubscribeRec((MqttSubscribeMessage) message, channel);
                case UNSUBSCRIBE -> onUnsubscribeRec((MqttUnsubscribeMessage) message, channel);
                case DISCONNECT -> onDisconnectRec(message, channel);
            }
        }

    }

    @Override
    public void onConnectRec(MqttConnectMessage message, Channel channel) {
        MqttConnAckMessage ackMessage = connectService.connect(message, channel);
        connack(ackMessage, channel);
    }

    @Override
    public void onPublishRec(MqttPublishMessage message, Channel channel) {
        ClientInfo clientInfo = connectService.getClientInfo(channel);
        try {
            sessionCheck(clientInfo);
            if (connectService.isSessionAlive(clientInfo)) {
                EventBusUtil.postAsync(new PublishRecEvent(
                        clientInfo,
                        message
                ));
            }
        } catch (ReqRejectException e) {
            log.warn("reject publish message from {},cause:{}", clientInfo.getClientIdentifier(), e.getMessage());
        }


    }

    @Override
    public void onPubAckRec(MqttMessage message, Channel channel) {
        ClientInfo clientInfo = connectService.getClientInfo(channel);
        try {
            sessionCheck(clientInfo);
            MqttMessageIdVariableHeader variableHeader = (MqttMessageIdVariableHeader) message.variableHeader();
            EventBusUtil.postAsync(new PubAckRecEvent(
                    clientInfo,
                    variableHeader.messageId()
            ));
        } catch (ReqRejectException e) {
            log.warn("reject puback message from {},cause:{}", clientInfo.getClientIdentifier(), e.getMessage());
        }

    }

    @Override
    public void onPubRecRec(MqttMessage message, Channel channel) {
        ClientInfo clientInfo = connectService.getClientInfo(channel);
        try {
            sessionCheck(clientInfo);
            MqttMessageIdVariableHeader variableHeader = (MqttMessageIdVariableHeader) message.variableHeader();
            EventBusUtil.postAsync(new PubRecRecEvent(
                    clientInfo,
                    variableHeader.messageId()
            ));
        } catch (ReqRejectException e) {
            log.warn("reject pubrec message from {},cause:{}", clientInfo.getClientIdentifier(), e.getMessage());
        }
    }

    @Override
    public void onPubRelRec(MqttMessage message, Channel channel) {
        ClientInfo clientInfo = connectService.getClientInfo(channel);
        try {
            sessionCheck(clientInfo);
            MqttMessageIdVariableHeader variableHeader = (MqttMessageIdVariableHeader) message.variableHeader();
            EventBusUtil.postAsync(new PubRelRecEvent(
                    clientInfo, variableHeader.messageId()
            ));
        } catch (ReqRejectException e) {
            log.warn("reject pubrel message from {},cause:{}", clientInfo.getClientIdentifier(), e.getMessage());
        }

    }

    @Override
    public void onPubCompRec(MqttMessage message, Channel channel) {

    }

    @Override
    public void onSubscribeRec(MqttSubscribeMessage message, Channel channel) {
        ClientInfo clientInfo = connectService.getClientInfo(channel);
        try {
            sessionCheck(clientInfo);
            EventBusUtil.postAsync(new SubscribeRecEvent(
                    clientInfo,
                    message
            ));
        } catch (ReqRejectException e) {
            log.warn("reject subscribe message from {},cause:{}", clientInfo.getClientIdentifier(), e.getMessage());
        }

    }

    @Override
    public void onUnsubscribeRec(MqttUnsubscribeMessage message, Channel channel) {
        ClientInfo clientInfo = connectService.getClientInfo(channel);
        try {
            sessionCheck(clientInfo);
            EventBusUtil.postAsync(new UnsubscribeRecEvent(
                    clientInfo, message

            ));
        } catch (ReqRejectException e) {
            log.warn("reject unsubscribe message from {},cause:{}", clientInfo.getClientIdentifier(), e.getMessage());
        }

    }

    @Override
    public void onPingRec(MqttMessage message, Channel channel) {
        pong(message, channel);
    }

    @Override
    public void onDisconnectRec(MqttMessage message, Channel channel) {
        disconnect(channel);
    }


    private class BrokerInitializer extends ChannelInitializer<SocketChannel> {

        @Override
        protected void initChannel(SocketChannel ch) throws Exception {
            ch.pipeline().addLast(new MqttDecoder());
            ch.pipeline().addLast(new InboundHandler());
            ch.pipeline().addLast(MqttEncoder.INSTANCE);
        }



        private class InboundHandler extends ChannelInboundHandlerAdapter{
            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                onMessage(msg,ctx.channel());
            }
        }

    }
}
