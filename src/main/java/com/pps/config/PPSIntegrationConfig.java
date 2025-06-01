package com.pps.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.config.EnableIntegrationManagement;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.jms.dsl.Jms;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

import com.pps.handler.BShandler;
import com.pps.handler.PaymentMSGValidationhandler;
import com.pps.utils.PPSConstants;

import jakarta.jms.ConnectionFactory;
import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableIntegration
@EnableIntegrationManagement
@Slf4j
public class PPSIntegrationConfig {

	@Value("${active-mq.pps-req-queue}")
	public String ppsReqQueue;

	@Value("${active-mq.pps-dlq-queue}")
	public String ppsDlqQueue;

	@Value("${active-mq.bs-req-queue}")
	public String bsReqQueue;

	@Value("${active-mq.bs-res-queue}")
	public String bsResQueue;

	// --- Channels ---
	@Bean
	public MessageChannel validatedChannel() {
		return new DirectChannel();
	}

	@Bean
	public MessageChannel bsChannel() {
		return new DirectChannel();
	}

	// --- Step 1: Read JSON from JMS queue and validate ---
	@Bean
	public IntegrationFlow inputFlow(ConnectionFactory connectionFactory, JmsTemplate jmsTemplate) {

		return IntegrationFlow
				.from(Jms.messageDrivenChannelAdapter(connectionFactory)
						.destination(ppsReqQueue))
				.<Message<String>>handle(new PaymentMSGValidationhandler())
				.route(Message.class, message -> message.getHeaders().containsKey(PPSConstants.FAILED),
						mapping-> mapping
						.subFlowMapping(true, sf-> sf.handle(msg-> {
							jmsTemplate.convertAndSend(ppsDlqQueue, msg.getPayload());
						}))
						.subFlowMapping(false, sf -> sf.channel(validatedChannel()))
						)
				.get();
	}

	// --- Step 2: Send to processing queue (validatedJsonQueue) ---
	@Bean
	public IntegrationFlow sendToProcessingQueue(ConnectionFactory connectionFactory, JmsTemplate jmsTemplate) {
		return IntegrationFlow.from(validatedChannel())
				.handle(new BShandler())
				.handle(Jms.outboundAdapter(connectionFactory).destination(bsReqQueue))
				.get();
	}

	// --- Step 3: Listen for reply on responseQueue ---
	@Bean
	public IntegrationFlow responseListener(ConnectionFactory connectionFactory) {
		return IntegrationFlow
				.from(Jms.messageDrivenChannelAdapter(connectionFactory)
						.destination(bsResQueue))
				.handle((payload, headers) -> {
					log.info("--->>>processing Completed<<<--- Received response: " + payload.toString());
					return null;
				})
				.get();
	}
}