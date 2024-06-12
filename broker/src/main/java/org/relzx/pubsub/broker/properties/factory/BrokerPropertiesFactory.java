package org.relzx.pubsub.broker.properties.factory;

import lombok.extern.slf4j.Slf4j;
import org.relzx.pubsub.broker.properties.BrokerProperties;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
@Slf4j
public class BrokerPropertiesFactory {
    private static volatile BrokerProperties instance = null;

    public static BrokerProperties getInstance() {
        if (instance == null) {
            synchronized (BrokerProperties.class) {
                if (instance == null) {
                    instance = new BrokerProperties();
                    loadProperties(instance);
                }
            }
        }
        return instance;
    }

    private static void loadProperties(BrokerProperties brokerProperties) {
        Properties prop = new Properties();
        InputStream input = null;
        try {
            String filename = "config.properties";
            input = BrokerPropertiesFactory.class.getClassLoader().getResourceAsStream(filename);
            if (input == null) {
                throw new IOException("未找到配置文件");
            }
            prop.load(input);
            brokerProperties.setHost(prop.getProperty("broker.host", "127.0.0.1"));
            brokerProperties.setPort(Integer.parseInt(prop.getProperty("broker.port", "8080")));
            brokerProperties.setPassword(prop.getProperty("mqtt.password",""));
            brokerProperties.setMqttVersion(Integer.parseInt(prop.getProperty("mqtt.version","4")));
            brokerProperties.setSessionTimeout(Integer.parseInt(prop.getProperty("mqtt.session.timeout","60")));
            brokerProperties.setSessionCacheCapacity(Integer.parseInt(prop.getProperty("mqtt.session.cache.capacity","100")));
            brokerProperties.setProcessTimeout(Long.parseLong(prop.getProperty("mqtt.process.timeout","10000")));
            brokerProperties.setMessageCacheTime(Long.parseLong(prop.getProperty("mqtt.message.cache.time","600000")));
            brokerProperties.setMessageCacheMaxSize(Integer.parseInt(prop.getProperty("mqtt.message.cache.maxSize","4000")));
            brokerProperties.setMessageCacheInitCapacity(Integer.parseInt(prop.getProperty("mqtt.message.cache.capacity","2000")));
            brokerProperties.setPubackPendingCacheMaxSize(Integer.parseInt(prop.getProperty("mqtt.process.puback.cache.maxSize","4000")));
            brokerProperties.setPubackPendingCacheInitCapacity(Integer.parseInt(prop.getProperty("mqtt.process.puback.cache.capacity","2000")));
            log.info("服务器配置加载成功=>{}", brokerProperties);
            input.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
