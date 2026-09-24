package com.example.backend.web;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.boot.webmvc.autoconfigure.error.ErrorViewResolver;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;

@Component
public class SpaErrorViewResolver implements ErrorViewResolver {

    @Override
    public ModelAndView resolveErrorView(
            HttpServletRequest request,
            HttpStatus status,
            Map<String, Object> model) {
        String path = (String) request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        if (status != HttpStatus.NOT_FOUND || path == null || !isFrontendRoute(path)) {
            return null;
        }
        return new ModelAndView("forward:/index.html");
    }

    private boolean isFrontendRoute(String path) {
        return !path.equals("/api")
                && !path.startsWith("/api/")
                && !path.equals("/actuator")
                && !path.startsWith("/actuator/")
                && !lastSegment(path).contains(".");
    }

    private String lastSegment(String path) {
        return path.substring(path.lastIndexOf('/') + 1);
    }
}
