package com.jdurangop.api;

import com.jdurangop.api.handler.BookHandler;
import com.jdurangop.api.handler.UserHandler;
import com.jdurangop.api.interceptor.HeaderInsertionInterceptor;
import com.jdurangop.api.interceptor.HeaderValidationInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
public class RouterRest {
    @Bean
    public RouterFunction<ServerResponse> routerFunction(BookHandler bookHandler,
                                                         UserHandler userHandler,
                                                         HeaderValidationInterceptor headerValidationInterceptor,
                                                         HeaderInsertionInterceptor headerInsertionInterceptor) {
        return route(GET("/api/book/{id}"), bookHandler::getFindByCode)
            .andRoute(POST("/api/book"), bookHandler::postSave)
            .filter(headerValidationInterceptor.andThen(headerInsertionInterceptor))
            .andRoute(GET("/api/user/{id}"), userHandler::getFindById)
            .andRoute(POST("/api/user"), userHandler::postSave);
    }
}
