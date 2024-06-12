package org.relzx.pubsub.client.properties;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.Properties;
@Slf4j
public class PropertiesFactory {
    static volatile ClientProperties clientProperties = null;

    public static ClientProperties getProperties() {
        if (clientProperties == null) {
            synchronized (ClientProperties.class) {
                if (clientProperties == null) {
                    clientProperties = new ClientProperties();
                    loadProperties(clientProperties);
                }
            }
        }
        return clientProperties;
    }

    private static void loadProperties(ClientProperties clientProperties) {
        Properties prop = new Properties();
        InputStream input = null;
        try {
            String filename = "config.properties";
            input = PropertiesFactory.class.getClassLoader().getResourceAsStream(filename);
            if (input == null) {
                throw new IOException("未找到配置文件");
            }
            prop.load(input);
            clientProperties.setHost(prop.getProperty("host", "127.0.0.1"));
            clientProperties.setPort(Integer.parseInt(prop.getProperty("port", "8080")));
            clientProperties.setUsername(prop.getProperty("username",""));
            clientProperties.setPassword(prop.getProperty("password",""));
            clientProperties.setTimeout(Long.parseLong(prop.getProperty("timeout","10000")));
            clientProperties.setKeepAlive(Integer.parseInt(prop.getProperty("keepAlive", "30")));
            clientProperties.setMaxReconnectAttempts(Integer.parseInt(prop.getProperty("max-reconnect-attempts", "0")));
            clientProperties.setWillTopic(prop.getProperty("will-topic",""));
            clientProperties.setWillMessage(prop.getProperty("will-message",""));
            clientProperties.setWillQos(Integer.parseInt(prop.getProperty("will-qos", "0")));
            clientProperties.setWillFlag(Boolean.parseBoolean(prop.getProperty("will-flag","false")));
            clientProperties.setWillRetain(Boolean.parseBoolean(prop.getProperty("will-retain","false")));
            clientProperties.setClientIdentifier(prop.getProperty("clientIdentifier", InetAddress.getLocalHost().getHostAddress()));
            log.info("客户端配置加载成功=>{}",clientProperties);
            input.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
