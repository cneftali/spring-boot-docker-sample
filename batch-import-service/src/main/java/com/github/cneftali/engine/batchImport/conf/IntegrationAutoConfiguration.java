package com.github.cneftali.engine.batchImport.conf;

import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.integration.scheduling.PollerMetadata.DEFAULT_POLLER;

import com.github.cneftali.job.commons.batch.JobLaunchRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.core.Pollers;
import org.springframework.integration.gateway.MessagingGatewaySupport;
import org.springframework.integration.http.inbound.HttpRequestHandlingMessagingGateway;
import org.springframework.integration.http.inbound.RequestMapping;
import org.springframework.integration.http.support.DefaultHttpHeaderMapper;
import org.springframework.integration.mapping.HeaderMapper;
import org.springframework.integration.scheduling.PollerMetadata;

@Configuration
@EnableIntegration
public class IntegrationAutoConfiguration {

    @Autowired
    private Environment env;

    @Bean
    public ExpressionParser parser() {
        return new SpelExpressionParser();
    }

    @Bean
    public HeaderMapper<HttpHeaders> headerMapper() {
        return new DefaultHttpHeaderMapper();
    }

    @Bean(name = DEFAULT_POLLER)
    protected PollerMetadata poller() {
        return Pollers.fixedRate(100).get();
    }

    @Bean
    public IntegrationFlow httpPostFlow() {
        return IntegrationFlows.from(httpPostGate()).channel("requestChannel").handle("endpoint", "post").get();
    }

//    @Bean
//    protected MessageHandler logger() {
//        final LoggingHandler loggingHandler = new LoggingHandler("INFO");
//        loggingHandler.setLoggerName("com.github.cneftali.engine.batchImport.LOGGER");
//        // This is redundant because the default expression is exactly "payload"
//        // loggingHandler.setExpression("payload");
//        return loggingHandler;
//    }

    @Bean
    public MessagingGatewaySupport httpPostGate() {
        final HttpRequestHandlingMessagingGateway handler = new HttpRequestHandlingMessagingGateway(true);
        handler.setRequestMapping(createMapping(new HttpMethod[] { POST }, env.getRequiredProperty("application.job.name")));
        handler.setStatusCodeExpression(parser().parseExpression("T(org.springframework.http.HttpStatus).CREATED"));
        handler.setRequestPayloadType(JobLaunchRequest.class);
        handler.setHeaderMapper(headerMapper());
        return handler;
    }

    private RequestMapping createMapping(final HttpMethod[] method, final String... path) {
        final RequestMapping requestMapping = new RequestMapping();
        requestMapping.setMethods(method);
        requestMapping.setConsumes(APPLICATION_JSON_VALUE);
        requestMapping.setProduces(APPLICATION_JSON_VALUE);
        requestMapping.setPathPatterns(path);
        return requestMapping;
    }
}
