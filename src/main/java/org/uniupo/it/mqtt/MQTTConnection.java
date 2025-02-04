package org.uniupo.it.mqtt;

import io.github.cdimascio.dotenv.Dotenv;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.UUID;

public class MQTTConnection {
    private static MQTTConnection instance;
    private MqttClient client;
    private static final Dotenv dotenv = Dotenv.configure().load();

    private static final String BROKER_URL = "ssl://broker.emqx.io:8883";
    private static final String CLIENT_ID = "amministrazione-backend"+ UUID.randomUUID();
    private static final String CA_CERT_PATH = dotenv.get("CA_CERT_PATH");

    private MQTTConnection() {
        try {
            if (instance != null) {
                throw new RuntimeException("Use getInstance() method to get the singleton instance");
            }
            MemoryPersistence persistence = new MemoryPersistence();
            client = new MqttClient(BROKER_URL, CLIENT_ID, persistence);

            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setAutomaticReconnect(true);
            options.setSocketFactory(getSSLSocketFactory());

            client.connect(options);
        } catch (MqttException e) {
            System.out.println("Errore nella connessione al broker MQTT");
            throw new RuntimeException("Errore nella connessione al broker MQTT", e);
        }
    }

    private SSLSocketFactory getSSLSocketFactory() {
        try {
            // Carica il certificato CA
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            FileInputStream caInput = new FileInputStream(CA_CERT_PATH);
            X509Certificate caCert = (X509Certificate) cf.generateCertificate(caInput);
            caInput.close();

            // Crea un KeyStore e importa il certificato
            KeyStore caKs = KeyStore.getInstance(KeyStore.getDefaultType());
            caKs.load(null, null);
            caKs.setCertificateEntry("ca-certificate", caCert);

            // Crea un TrustManager che si fida del CA nel KeyStore
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(caKs);

            // Crea un SSLContext con il TrustManager
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, tmf.getTrustManagers(), null);

            return context.getSocketFactory();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            throw new RuntimeException("Errore nella configurazione SSL", e);
        }
    }

    public static MQTTConnection getInstance() {
        if (instance == null) {
            synchronized (MQTTConnection.class) {
                if (instance == null) {
                    instance = new MQTTConnection();
                }
            }
        }
        return instance;
    }


    public void publish(String topic, String message) throws MqttException {
        MqttMessage mqttMessage = new MqttMessage(message.getBytes());
        mqttMessage.setQos(1);
        client.publish(topic, mqttMessage);
    }

    public void subscribe(String topic, IMqttMessageListener listener) throws MqttException {
        client.subscribe(topic, listener);
    }

    public void disconnect() {
        if (client != null && client.isConnected()) {
            try {
                client.disconnect();
            } catch (MqttException e) {
                throw new RuntimeException("Errore nella disconnessione dal broker MQTT", e);
            }
        }
    }
}