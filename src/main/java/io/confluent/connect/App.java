package io.confluent.connect;

import org.apache.commons.io.IOUtils;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DeleteTopicsResult;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.common.KafkaFuture;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public class App {

  public static Map<String, Object> buildConfigs(String kafkaEndpoint, String userName, String password) {
    Map<String, Object> config = new HashMap<>();
    config.put("bootstrap.servers", kafkaEndpoint);
    config.put("ssl.endpoint.identification.algorithm", "https");
    config.put("sasl.mechanism", "PLAIN");
    config.put("request.timeout.ms", "20000");
    config.put("retry.backoff.ms", "500");
    config.put("sasl.jaas.config", String.format("org.apache.kafka.common.security.plain.PlainLoginModule required username=\"%s\" password=\"%s\";", userName, password));
    config.put("security.protocol", "SASL_SSL");
    return config;
  }

  public static void main(String[] args)
      throws ExecutionException, InterruptedException, IOException {

    String kafkaEndpoint = args[0];
    String userName = args[1];
    String password = args[2];
    String pccToBeDeletedPath = args[3];

    Map<String, Object> config = buildConfigs(kafkaEndpoint, userName, password);
    List<String> physicalClusters = IOUtils
        .readLines(new FileInputStream(pccToBeDeletedPath),
            StandardCharsets.UTF_8);
    AdminClient adminClient = AdminClient.create(config);

    ListTopicsResult listTopicsResult = adminClient.listTopics();
    KafkaFuture<Set<String>> namesFuture = listTopicsResult.names();
    Set<String> topics = namesFuture.get();
    topics.stream().forEach(t ->
    {
      System.out.printf("Processing topic %s\n", t);
      physicalClusters.stream().filter(pc -> t.contains(pc.trim()))
          .forEach(s -> {
            System.out.printf("%s is matching, deleting it\n", s);
            deleteTopic(adminClient, t);
          });
    });
  }

  private static void deleteTopic(AdminClient adminClient, String topicName) {
    DeleteTopicsResult deleteTopicsResult = adminClient
        .deleteTopics(Collections.singletonList(topicName));
    deleteTopicsResult.values().entrySet().stream().forEach(
        stringKafkaFutureEntry -> {
          String topic = stringKafkaFutureEntry.getKey();
          try {
            stringKafkaFutureEntry.getValue().get();
            System.out.printf("Topic %s has been deleted\n", topic);
          } catch (Exception e) {
            System.out.printf("Unable to delete topic %s, error: %s\n", topic, e.getMessage());
          }
        });
  }
}
