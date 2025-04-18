package org.aeis.stt.service;


import lombok.extern.log4j.Log4j2;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;


@Service
public class STTProcessorService {


    private final Logger log = org.slf4j.LoggerFactory.getLogger(STTProcessorService.class);


    @Autowired
    private  WebSocketSender webSocketSender;

    @KafkaListener(topics = "${spring.kafka.consumer.topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void SttOutputListener(String message) {
        log.info("Received message: {}", message);
        webSocketSender.send(message);

    }


}
