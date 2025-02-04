package org.uniupo.it;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.uniupo.it.mqtt.MQTTConnection;
import org.uniupo.it.util.Topics;

public class Main {
    public static void main(String[] args) {
        try {
            MQTTConnection.getInstance().subscribe(Topics.NEW_MACHINE_TOPIC, (topic, message) -> {
                String[] topicParts = topic.split("/");
                String instituteId = topicParts[1];
                String machineId = topicParts[2];
                System.out.println("New machine added: " + machineId);

                String batFilePath = "src/main/resources/Macchinetta/start_machine.bat";
                ProcessBuilder processBuilder = new ProcessBuilder(batFilePath, instituteId, machineId);



                processBuilder.start();
            });
        } catch (MqttException e) {
            System.out.println("Errore nella connessione al broker MQTT.");
            throw new RuntimeException(e);
        }
    }
}