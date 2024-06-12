package org.relzx.pubsub.client;

import io.netty.handler.codec.mqtt.MqttConnectMessage;

public interface Auth {
   void  login();


   public interface Callback{
      void onLogin();
      void onLoginFailed();
   }
}
