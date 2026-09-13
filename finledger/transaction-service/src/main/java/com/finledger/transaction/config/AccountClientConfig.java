package com.finledger.transaction.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.config.TopicBuilder;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.web.client.RestClient;
import com.finledger.transaction.security.JwtService;

@Configuration
public class AccountClientConfig {

    @Bean
    RestClient accountRestClient(RestClient.Builder builder,
                                 @Value("${account-service.url:http://localhost:8081}") String accountServiceUrl,
                                 JwtService jwtService) {
        return builder.baseUrl(accountServiceUrl)
                .defaultHeader("Authorization", "Bearer " + jwtService.issueServiceToken("transaction-service"))
                .build();
    }

    @Bean
    NewTopic moneySentTopic() {
        return TopicBuilder.name("money.sent").partitions(3).replicas(1).build();
    }

}
