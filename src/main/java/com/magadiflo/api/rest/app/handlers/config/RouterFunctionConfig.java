package com.magadiflo.api.rest.app.handlers.config;

import com.magadiflo.api.rest.app.handlers.ProductHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
public class RouterFunctionConfig {

    @Bean
    public RouterFunction<ServerResponse> routes(ProductHandler productHandler) {
        return RouterFunctions.route(RequestPredicates.GET("/api/v2/products").or(RequestPredicates.GET("/api/v3/products")), productHandler::listAllProducts)
                .andRoute(RequestPredicates.GET("/api/v2/products/{id}"), productHandler::showDetails)
                .andRoute(RequestPredicates.POST("/api/v2/products"), productHandler::createProduct)
                .andRoute(RequestPredicates.POST("/api/v2/products/create-product-with-validation"), productHandler::createProductWithValidation)
                .andRoute(RequestPredicates.PUT("/api/v2/products/{id}"), productHandler::updateProduct)
                .andRoute(RequestPredicates.DELETE("/api/v2/products/{id}"), productHandler::deleteProduct)
                .andRoute(RequestPredicates.POST("/api/v2/products/upload/{id}"), productHandler::uploadImageFile)
                .andRoute(RequestPredicates.POST("/api/v2/products/product-with-image"), productHandler::createProductWithImage);
    }
}
