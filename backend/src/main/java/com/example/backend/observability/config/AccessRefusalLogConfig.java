package com.example.backend.observability.config;

import com.example.backend.observability.AccessRefusalLog;
import com.example.backend.observability.RouteTemplates;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * The refusal record both security chains write through.
 *
 * <p>The application's own request mapping is named explicitly: actuator registers further
 * mappings of related types, and a refused request is described by the route the
 * application would have served it on. Looked up lazily, on the first refusal, so the
 * security chains — built before the MVC infrastructure in some contexts — do not have to
 * wait for it, and a context with no MVC at all (a management child) still starts.
 */
@Configuration(proxyBeanMethods = false)
public class AccessRefusalLogConfig {

    @Bean
    RouteTemplates routeTemplates(
            @Qualifier("requestMappingHandlerMapping")
            ObjectProvider<RequestMappingHandlerMapping> mapping) {
        return new RouteTemplates(mapping::getIfAvailable);
    }

    @Bean
    AccessRefusalLog accessRefusalLog(RouteTemplates routeTemplates) {
        return new AccessRefusalLog(routeTemplates);
    }
}
