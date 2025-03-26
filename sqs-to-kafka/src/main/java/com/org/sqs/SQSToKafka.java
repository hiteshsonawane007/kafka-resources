package com.org.sqs;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

public class SQSToKafka {


    static final String TOPIC = "first_topic";
    static final String BOOTSTRAP_SERVERS = "localhost:9092";

    private static Producer<Long, String> createProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "MyKafkaProducer");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        return new KafkaProducer<>(props);
    }

    public static void main(String[] args) {
        System.out.println("-----------------START------------------");
        final Producer<Long, String> producer = createProducer();

        try {
            //BasicAWSCredentials awsCreds = new BasicAWSCredentials("", "");
            BasicSessionCredentials sessionCredentials =
                    new BasicSessionCredentials("", "", "");

            AmazonSQS sqs = AmazonSQSClientBuilder.standard()
                    .withCredentials(new AWSStaticCredentialsProvider(sessionCredentials))
                    .withRegion(Regions.US_EAST_1)
                    .build();

            ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest("https://sqs.us-east-1.amazonaws.com/071704770940/eds-atlas-nonprod-cms-sqs")
                    .withWaitTimeSeconds(10)
                    .withMaxNumberOfMessages(10);
            List<Message> sqsMessages = sqs.receiveMessage(receiveMessageRequest).getMessages();

            for (Message msg : sqsMessages) {
                System.out.println("Messagees-----------------" + msg.getBody());
                final ProducerRecord<Long, String> record = new ProducerRecord<>(TOPIC, msg.getBody());
                RecordMetadata metadata = producer.send(record).get();
                System.out.printf("sent record(key=%s value='%s')" + " metadata(partition=%d, offset=%d)\n",
                        record.key(), record.value(), metadata.partition(), metadata.offset());
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } finally {
            producer.flush();
            producer.close();
        }
        System.out.println("-----------------END------------------");
    }
}
