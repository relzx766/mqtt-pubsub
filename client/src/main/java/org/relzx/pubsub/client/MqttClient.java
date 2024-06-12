package org.relzx.pubsub.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.mqtt.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import org.relzx.pubsub.client.processor.*;
import org.relzx.pubsub.client.properties.ClientProperties;
import org.relzx.pubsub.client.properties.PropertiesFactory;
import org.relzx.pubsub.common.entity.MqttConnectOptions;
import org.relzx.pubsub.common.entity.ProcessResult;
import org.relzx.pubsub.common.enums.ConnectStatus;
import org.relzx.pubsub.common.event.*;
import org.relzx.pubsub.common.util.EventBusUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

@Slf4j
@Data
public class MqttClient implements Auth, Auth.Callback, Client, Client.Callback {
    private NioEventLoopGroup loopGroup;
    private final ClientProperties clientProperties;
    private ConnectProcessor connectProcessor;
    private PingProcessor pingProcessor;
    private MqttConnectOptions connectOptions;
    private static final MqttClient INSTANCE = new MqttClient();
    private int reconnectCount = 0;
    private ConnectStatus connectStatus = ConnectStatus.FAIL;
    private ConnectStatus loginStatus = ConnectStatus.FAIL;
    private Executor executor = Executors.newVirtualThreadPerTaskExecutor();
    private Channel channel;
    private ArrayList<PublishProcessor> publishProcessorList = new ArrayList<>();
    private ArrayList<SubscribeProcessor> subscribeProcessorList = new ArrayList<>();
    private ArrayList<UnsubscribeProcessor> unSubscribeProcessorList = new ArrayList<>();
    private FutureTask<Void> pingTask;
    private FutureTask<Void> reconnectTask;

    private MqttClient() {
        registerShutdownHook();
        clientProperties = PropertiesFactory.getProperties();
        initConnectOption(clientProperties);
    }

    public static MqttClient getInstance() {
        return INSTANCE;
    }

    private void initConnectOption(ClientProperties properties) {
        connectOptions = MqttConnectOptions.builder()
                .clientIdentifier(properties.getClientIdentifier())
                .username(properties.getUsername())
                .password(properties.getPassword().getBytes())
                .willTopic(properties.getWillTopic())
                .willMessage(properties.getWillMessage().getBytes())
                .willQos(properties.getWillQos())
                .isWillRetain(properties.isWillRetain())
                .isCleanSession(false)
                .keepAliveTime(properties.getKeepAlive())
                .mqttVersion(MqttVersion.MQTT_3_1_1)
                .build();
    }


    @Override
    public synchronized void login() {
        if (!ConnectStatus.SUCCESS.equals(connectStatus)) {
            log.info("未与broker==>{}:{}建立连接,无法登录", clientProperties.getHost(), clientProperties.getHost());
        } else {
            switch (loginStatus) {
                case SUCCESS -> log.info("已登录,无需重新登录");
                case CONNECTING -> log.info("正在登录");
                case FAIL -> {
                    try {
                        setLoginStatus(ConnectStatus.CONNECTING);
                        connectProcessor = new ConnectProcessor();
                        ProcessResult processResult = connectProcessor.connect(channel, connectOptions);
                        if (ProcessResult.SUCCESS.equals(processResult)) {
                            onLogin();
                        } else {
                            onLoginFailed();
                        }
                    } catch (Exception e) {
                        log.error("",e);
                        onLoginFailed();
                    }
                }
            }
        }
    }

    @Override
    public void onLogin() {
        setLoginStatus(ConnectStatus.SUCCESS);
        log.info("成功登录broker==>{}:{}", clientProperties.getHost(), clientProperties.getPort());
        pingTask();
    }

    @Override
    public void onLoginFailed() {
        setLoginStatus(ConnectStatus.FAIL);
        log.error("登录失败");
    }

    @Override
    public synchronized void connect() {
        loopGroup = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(loopGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new ClientInitializer());
        try {
            ChannelFuture channelFuture = bootstrap.connect(clientProperties.getHost(), clientProperties.getPort()).sync();
            setConnectStatus(ConnectStatus.CONNECTING);
            if (channelFuture.isSuccess()) {
                this.channel=channelFuture.channel();
                onConnected();
            } else {
                onConnectFailed();
            }
        } catch (InterruptedException e) {
            shutdown();
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized void reconnect() {
        int maxTimes = clientProperties.getMaxReconnectAttempts();
        if (ConnectStatus.FAIL.equals(connectStatus) && (maxTimes < 0 || reconnectCount <= maxTimes)) {
            reconnectTask = new FutureTask<>(() -> {
                long startTime = System.currentTimeMillis();
                while (maxTimes < 0 || reconnectCount <= maxTimes) {
                    long begin = System.currentTimeMillis();
                    log.info("尝试重连");
                    connect();
                    long end = System.currentTimeMillis();
                    if (ConnectStatus.SUCCESS.equals(connectStatus)) {
                        break;
                    }
                    reconnectCount++;
                    log.info("重连中==>次数:{},耗时:{},总耗时:{}", reconnectCount, end - begin, end - startTime);
                }
                log.error("已达最大重连次数");
                onReconnectFailed();
                return null;
            });
            executor.execute(() -> {
                reconnectTask.run();
            });
        }
    }

    @Override
    public void disconnect() {
        new DisconnectProcessor().disconnect(channel);
        if (null != loopGroup) {
            loopGroup.shutdownGracefully();
        }
    }

    @Override
    public void publish(String topic, MqttQoS qoS, String content) {
        publish(topic, qoS, content, false, false);
    }

    @Override
    public void publish(String topic, MqttQoS qoS, String content, boolean isRetain, boolean isDup) {
        PublishProcessor processor = new PublishProcessor();
        int index = publishProcessorList.size();
        publishProcessorList.add(processor);
        try {
            ProcessResult result = processor.publish(channel, topic, qoS, content, isRetain, isDup);
            if (ProcessResult.SUCCESS.equals(result)) {
                if (MqttQoS.EXACTLY_ONCE.equals(qoS)) {
                    //TODO pubrel
                }
            } else {
                log.error("发布失败==>topic:{} content:{}", topic, content);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            publishProcessorList.remove(index);
        }
    }


    @Override
    public synchronized void subscribe(MqttQoS qoS, long timeout, String... topics) {
        SubscribeProcessor subscribeProcessor = new SubscribeProcessor();
        //记录插入后的索引,删除更快
        int index = subscribeProcessorList.size();
        subscribeProcessorList.add(subscribeProcessor);
        try {
            ProcessResult result = subscribeProcessor.subscribe(channel, qoS, timeout, topics);
            if (ProcessResult.SUCCESS.equals(result)) {
                List<SubscribeEvent> events = Arrays.stream(topics)
                        .map(topic->{
                            MqttTopicSubscription subscription = new MqttTopicSubscription(topic, qoS);
                            return new SubscribeEvent(subscription);
                        })
                        .toList();
                EventBusUtil.postAsync(events);
                log.info("成功订阅:{}", Arrays.stream(topics).toArray());
            } else {
                log.info("订阅失败:{}", Arrays.stream(topics).toArray());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            subscribeProcessorList.remove(index);
        }
    }

    @Override
    public synchronized void unsubscribe(String... topics) {
        List<UnsubscribeEvent> events = Arrays.stream(topics)
                .map(topic -> new UnsubscribeEvent(topic, UnsubscribeEvent.EventSource.CLIENT))
                .toList();
        EventBusUtil.postAsync(events);
        UnsubscribeProcessor processor = new UnsubscribeProcessor();
        int index = unSubscribeProcessorList.size();
        unSubscribeProcessorList.add(processor);
        try {
            ProcessResult result = processor.unsubscribe(channel, topics);
            if (ProcessResult.SUCCESS.equals(result)) {
                log.info("取消订阅:{}", Arrays.stream(topics).toArray());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            unSubscribeProcessorList.remove(index);
        }
    }

    @Override
    public void connack(MqttConnAckMessage message) {
        connectProcessor.processAck(message);
    }

    @Override
    public void puback(MqttPubAckMessage message) {
        for (PublishProcessor publishProcessor : publishProcessorList) {
            log.info("收到puback:{}",message.variableHeader().messageId());
            publishProcessor.processAck(message);
        }
    }

    @Override
    public void suback(MqttSubAckMessage message) {
        for (SubscribeProcessor subscribeProcessor : subscribeProcessorList) {
            subscribeProcessor.processAck(message);
        }
    }

    @Override
    public void unsuback(MqttUnsubAckMessage message) {
        for (UnsubscribeProcessor unsubscribeProcessor : unSubscribeProcessorList) {
            unsubscribeProcessor.processAck(message);
        }
    }

    @Override
    public void pong(MqttMessage message) {
        pingProcessor.processAck(message);
    }


    @Override
    public synchronized void ping() {
        try {
            ProcessResult result = pingProcessor.ping(channel);
            if (ProcessResult.FAILED.equals(result)) {
                throw new ConnectTimeoutException();
            }
        } catch (Exception e) {
            if (e instanceof ConnectTimeoutException) {
                log.error("ping no received");
                onConnectLost();
            } else {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void pingTask() {
        pingProcessor = new PingProcessor();
        pingTask = new FutureTask<>(() -> {
            while (ConnectStatus.SUCCESS.equals(connectStatus)) {
                ping();
            }
            return null;
        });
        executor.execute(() -> pingTask.run());
    }

    @Override
    public void start() {
        log.info("mqtt client starting....");
        connect();
    }

    @Override
    public void shutdown() {
        EventBusUtil.post(new ShutDownEvent());
        setConnectStatus(ConnectStatus.FAIL);
        setLoginStatus(ConnectStatus.FAIL);
        if (reconnectTask != null) {
            reconnectTask.cancel(true);
        }
        if (pingTask != null) {
            pingTask.cancel(true);
        }
        if (connectProcessor != null) {
            connectProcessor.cancel(true);
        }
        for (PublishProcessor processor : publishProcessorList) {
            processor.cancel(true);
        }
        for (SubscribeProcessor processor : subscribeProcessorList) {
            processor.cancel(true);
        }
        for (UnsubscribeProcessor processor : unSubscribeProcessorList) {
            processor.cancel(true);
        }
        if (channel != null) {
            disconnect();
        }

    }


    @Override
    public void onConnectLost() {
        log.warn("与broker==>{}:{}断开连接", clientProperties.getHost(), clientProperties.getPort());
        reconnect();
    }

    @Override
    public void onConnected() {
        setConnectStatus(ConnectStatus.SUCCESS);
        log.info("连接成功==>{}:{}", clientProperties.getHost(), clientProperties.getPort());
        reconnectCount = 0;
        login();
    }

    @Override
    public void onConnectFailed() {
        setConnectStatus(ConnectStatus.FAIL);
        log.error("连接失败==>{}:{}", clientProperties.getHost(), clientProperties.getPort());
        reconnect();
    }

    @Override
    public void onReconnectFailed() {
        log.error("重连失败");
        shutdown();
    }

    @Override
    public void onMessage(Object msg) {
        if (null != msg) {
            MqttMessage message = (MqttMessage) msg;
            MqttFixedHeader fixedHeader = message.fixedHeader();
            MqttMessageType messagedType = fixedHeader.messageType();
            switch (messagedType) {
                case CONNACK -> connack((MqttConnAckMessage) message);
                case PUBLISH -> onMessageReceived((MqttPublishMessage) message);
                case PUBACK -> puback((MqttPubAckMessage) message);
                case SUBACK -> suback((MqttSubAckMessage) message);
                case UNSUBACK -> unsuback((MqttUnsubAckMessage) message);
                case PINGRESP -> pong(message);
                case PUBREL -> {
                    //TODO
                }
                case PUBCOMP -> {
                    //TODO
                }
            }

        }

    }

    @Override
    public void onMessageReceived(MqttPublishMessage message) {
        EventBusUtil.postAsync(new PublishRecEvent(message));
    }

    private class ClientInitializer extends ChannelInitializer<SocketChannel> {

        @Override
        protected void initChannel(SocketChannel ch) {
            ChannelPipeline pipeline = ch.pipeline();
            pipeline.addLast(new MqttDecoder());
            pipeline.addLast(new InboundHandler());
            pipeline.addLast(MqttEncoder.INSTANCE);
        }
        private class InboundHandler extends ChannelInboundHandlerAdapter{
            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                onMessage(msg);
            }
        }
    }
}
