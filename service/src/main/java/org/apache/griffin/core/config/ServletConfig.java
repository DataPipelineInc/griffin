package org.apache.griffin.core.config;

import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.jetty.JettyEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @CLassName ServletConfig
 * @Description Jetty Config
 * @Author goodman
 * @Date 2019-02-18 15:48
 * @Version 1.0
 **/
@Configuration
public class ServletConfig {
    /**
     * Jetty Port
     **/
    @Value("${jetty.port}")
    private int jettyPort;

    /**
     * Jetty Session Time out
     */
    private static final int SESSION_TIMEOUT = 1;


    @Bean
    public EmbeddedServletContainerFactory servletContainer() {
        JettyEmbeddedServletContainerFactory factory = new JettyEmbeddedServletContainerFactory();
        factory.setPort(jettyPort);
        factory.setSessionTimeout(SESSION_TIMEOUT, TimeUnit.HOURS);
        return factory;
    }
}
