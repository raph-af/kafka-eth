package com.zenika.kafkaeth;

import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.kstream.*;

import com.zenika.kafkaeth.domain.Transaction;

import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig;
import io.confluent.kafka.streams.serdes.avro.GenericAvroSerde;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;

public class BlockchainStream {

    private static final String TOPIC_INPUT = "transactions";
    private static final String TOPIC_OUTPUT = "stream-bc-data";

    public static void main(String[] args) {

        final Properties streamsConfiguration = initStreamProperties();

        StreamsBuilder builder = new StreamsBuilder();

        KStream<String, Transaction> inputTx = builder.stream(TOPIC_INPUT);

        Long windowSizeMs = TimeUnit.MINUTES.toMillis(2);
        Long advanceMs = TimeUnit.SECONDS.toMillis(2);
        TimeWindows timeWindows = TimeWindows.of(windowSizeMs).advanceBy(advanceMs);
        /*KTable<String, Long> nbTxByUser = inputTx.map((key, transaction) -> new KeyValue<>(transaction.getFrom(), transaction))
               .groupByKey()
               .count();
        */
        KTable<String, Long> nbTxByUser = inputTx.map((key, transaction) -> new KeyValue<>(transaction.getFrom(), transaction))
                                                 .groupByKey()
                                                 .count();


        nbTxByUser.toStream().to(TOPIC_OUTPUT, Produced.with(Serdes.String(), Serdes.Long()));

        KafkaStreams streams = new KafkaStreams(builder.build(), streamsConfiguration);

        streams.start();

        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
    }

    private static Properties initStreamProperties() {
        final Properties streamsConfiguration = new Properties();
        streamsConfiguration.put(StreamsConfig.APPLICATION_ID_CONFIG, "stream-avro-bc-example");
        streamsConfiguration.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        streamsConfiguration.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9092");
        streamsConfiguration.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        streamsConfiguration.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, SpecificAvroSerde.class);
        streamsConfiguration.put(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, "http://localhost:8081");

        return streamsConfiguration;
    }
}
