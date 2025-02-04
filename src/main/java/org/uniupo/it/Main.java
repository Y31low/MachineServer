package org.uniupo.it;

import io.github.cdimascio.dotenv.Dotenv;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.uniupo.it.db.SchemaManager;
import org.uniupo.it.mqtt.MQTTConnection;
import org.uniupo.it.util.Topics;

public class Main {
    public static void main(String[] args) {
        final Dotenv dotenv = Dotenv.configure().load();
        try {
            MQTTConnection.getInstance().subscribe(Topics.NEW_MACHINE_TOPIC, (topic, message) -> {
                String[] topicParts = topic.split("/");
                String instituteId = topicParts[1];
                String machineId = topicParts[2];
                System.out.println("New machine added: " + machineId);

                try {
                    SchemaManager.createSchemaForMachine(instituteId, machineId);
                    System.out.println("Database schema created successfully for machine: " + machineId);
                } catch (Exception e) {
                    System.err.println("Error creating database schema: " + e.getMessage());
                }

                String batFilePath = dotenv.get("BAT_FILE_PATH");
                System.out.println("Starting new machine process: " + batFilePath + " " + instituteId + " " + machineId);
                ProcessBuilder processBuilder = new ProcessBuilder(batFilePath, instituteId, machineId);
                try {
                    processBuilder.start();
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Errore nell'avvio del processo."+e.getMessage());
                    throw new RuntimeException(e);
                }

            });
        } catch (MqttException e) {
            System.out.println("Errore nella connessione al broker MQTT.");
            throw new RuntimeException(e);
        }
    }
}