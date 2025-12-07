package com.example.agent.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.server.ConfigurableWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.stereotype.Component;

@Component
public class ServerPortCustomizer implements WebServerFactoryCustomizer<ConfigurableWebServerFactory> {

    @Autowired
    private AgentProperties agentProperties;

    @Override
    public void customize(ConfigurableWebServerFactory factory) {
        factory.setPort(agentProperties.getPort());
    }
}