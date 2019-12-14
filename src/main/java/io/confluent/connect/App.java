package io.confluent.connect;

import org.apache.commons.io.IOUtils;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DeleteTopicsResult;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.common.KafkaFuture;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public class App {

  public static Map<String, Object> loadConfig(String configFile) throws IOException {
    if (!Files.exists(Paths.get(configFile))) {
      throw new IOException(configFile + " not found.");
    }
    final Properties cfg = new Properties();
    try (InputStream inputStream = new FileInputStream(configFile)) {
      cfg.load(inputStream);
    }
    Map<String, Object> config = new HashMap<>();
    for (final String name : cfg.stringPropertyNames()) {
      config.put(name, cfg.getProperty(name));
    }
    return config;
  }

  public static void main(String[] args)
      throws ExecutionException, InterruptedException, IOException {
    Map<String, Object> config = loadConfig("/Users/alexdiachenko/pkc-zm6j0.txt");
    List<String> physicalClusters = IOUtils
        .readLines(new FileInputStream("/Users/alexdiachenko/deactivated_pccs.txt"),
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
