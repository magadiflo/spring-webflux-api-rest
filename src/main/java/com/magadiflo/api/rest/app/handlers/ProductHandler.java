package com.magadiflo.api.rest.app.handlers;

import com.magadiflo.api.rest.app.models.documents.Category;
import com.magadiflo.api.rest.app.models.documents.Product;
import com.magadiflo.api.rest.app.models.services.IProductService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.FormFieldPart;
import org.springframework.http.codec.multipart.Part;
import org.springframework.http.server.RequestPath;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.net.URI;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Component
public class ProductHandler {

    private final IProductService productService;
    private final Validator validator;

    @Value("${config.uploads.path}")
    private String uploadsPath;

    public ProductHandler(IProductService productService, Validator validator) {
        this.productService = productService;
        this.validator = validator;
    }

    public Mono<ServerResponse> listAllProducts(ServerRequest request) {
        Flux<Product> productFlux = this.productService.findAll();
        return ServerResponse.ok().body(productFlux, Product.class);
    }

    public Mono<ServerResponse> showDetails(ServerRequest request) {
        String id = request.pathVariable("id");
        return this.productService.findById(id)
                .flatMap(productDB -> ServerResponse.ok().bodyValue(productDB))
                .switchIfEmpty(ServerResponse.notFound().build());
    }

    public Mono<ServerResponse> createProduct(ServerRequest request) {
        RequestPath requestPath = request.requestPath();
        Mono<Product> productMono = request.bodyToMono(Product.class);
        return productMono
                .flatMap(product -> {
                    if (product.getCreateAt() == null) {
                        product.setCreateAt(LocalDate.now());
                    }
                    return this.productService.saveProduct(product);
                })
                .flatMap(productDB -> ServerResponse
                        .created(URI.create(requestPath.value() + "/" + productDB.getId()))
                        .bodyValue(productDB));
    }

    public Mono<ServerResponse> createProductWithValidation(ServerRequest request) {
        RequestPath requestPath = request.requestPath();
        Mono<Product> productMono = request.bodyToMono(Product.class);
        return productMono
                .flatMap(product -> {

                    Errors errors = new BeanPropertyBindingResult(product, Product.class.getName());
                    this.validator.validate(product, errors);

                    if (errors.hasErrors()) {
                        return Flux.fromIterable(errors.getFieldErrors())
                                .map(fieldError -> String.format("[Validación 2] El campo %s %s", fieldError.getField(), fieldError.getDefaultMessage()))
                                .collectList()
                                .flatMap(listStings -> ServerResponse.badRequest().bodyValue(listStings));
                    }

                    if (product.getCreateAt() == null) {
                        product.setCreateAt(LocalDate.now());
                    }
                    return this.productService.saveProduct(product)
                            .flatMap(productDB -> ServerResponse
                                    .created(URI.create(requestPath.value() + "/" + productDB.getId()))
                                    .bodyValue(productDB));
                });
    }

    public Mono<ServerResponse> updateProduct(ServerRequest request) {
        String id = request.pathVariable("id");
        Mono<Product> productMono = request.bodyToMono(Product.class);
        Mono<Product> productMonoDB = this.productService.findById(id);

        return productMonoDB.zipWith(productMono, (productDB, product) -> {
                    productDB.setName(product.getName());
                    productDB.setPrice(product.getPrice());
                    productDB.setCategory(product.getCategory());
                    return productDB;
                })
                .flatMap(this.productService::saveProduct)
                .flatMap(productDB -> ServerResponse.ok().bodyValue(productDB))
                .switchIfEmpty(ServerResponse.notFound().build());
    }

    public Mono<ServerResponse> deleteProduct(ServerRequest request) {
        String id = request.pathVariable("id");

        return this.productService.findById(id)
                .flatMap(productDB -> this.productService.delete(productDB).then(Mono.just(true)))
                .flatMap(isDeleted -> ServerResponse.noContent().build())
                .switchIfEmpty(ServerResponse.notFound().build());
    }

    public Mono<ServerResponse> uploadImageFile(ServerRequest request) {
        String id = request.pathVariable("id");
        Mono<Product> productMonoDB = this.productService.findById(id);

        return request.multipartData()
                .map(MultiValueMap::toSingleValueMap)
                .map(stringPartMap -> stringPartMap.get("imageFile"))
                .cast(FilePart.class)
                .zipWith(productMonoDB, (filePart, productDB) -> {
                    String imageName = UUID.randomUUID().toString() + "-" + filePart.filename()
                            .replace(" ", "")
                            .replace(":", "")
                            .replace("\\", "");
                    productDB.setImage(imageName);

                    return filePart.transferTo(new File(this.uploadsPath + productDB.getImage()))
                            .then(this.productService.saveProduct(productDB));
                })
                .flatMap(productDBMono -> ServerResponse.ok().body(productDBMono, Product.class))
                .switchIfEmpty(ServerResponse.notFound().build());
    }

    public Mono<ServerResponse> createProductWithImage(ServerRequest request) {
        RequestPath requestPath = request.requestPath();

        Mono<Product> productMono = request.multipartData()
                .map(stringPartMultiValueMap -> {
                    Map<String, Part> singleValueMap = stringPartMultiValueMap.toSingleValueMap();
                    FormFieldPart name = (FormFieldPart) singleValueMap.get("name");
                    FormFieldPart price = (FormFieldPart) singleValueMap.get("price");
                    FormFieldPart categoryId = (FormFieldPart) singleValueMap.get("category.id");
                    FormFieldPart categoryName = (FormFieldPart) singleValueMap.get("category.name");

                    Category category = new Category();
                    category.setId(categoryId.value());
                    category.setName(categoryName.value());

                    return new Product(name.value(), Double.parseDouble(price.value()), category);
                });


        return request.multipartData()
                .map(MultiValueMap::toSingleValueMap)
                .map(stringPartMap -> stringPartMap.get("imageFile"))
                .cast(FilePart.class)
                .zipWith(productMono, (filePart, product) -> {
                    String imageName = UUID.randomUUID().toString() + "-" + filePart.filename()
                            .replace(" ", "")
                            .replace(":", "")
                            .replace("\\", "");
                    product.setImage(imageName);
                    product.setCreateAt(LocalDate.now());

                    return filePart.transferTo(new File(this.uploadsPath + product.getImage()))
                            .then(this.productService.saveProduct(product));
                })
                .flatMap(productDBMono -> productDBMono.flatMap(product -> ServerResponse
                        .created(URI.create(requestPath.value() + "/" + product.getId()))
                        .bodyValue(product)));
    }
}

