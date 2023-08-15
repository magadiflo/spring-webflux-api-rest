# Sección: API RESTFull usando RestController

---

## Dependencias iniciales

````xml
<!--Spring Boot versión: 3.1.2-->
<!--Java versión: 17-->
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-mongodb-reactive</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webflux</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>io.projectreactor</groupId>
        <artifactId>reactor-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
````

## Creando Proyecto REST

Copiamos las clases, interfaces, propiedades, etc. del proyecto de
[**spring-boot-webflux**](https://github.com/magadiflo/spring-boot-webflux.git) a fin de no empezar desde cero y
**centrarnos en desarrollar la capa REST**, por lo tanto, dejaremos hasta este punto tal como se ve en la imagen:

![archivos-iniciales.png](./assets/archivos-iniciales.png)

Como se aprecia, tenemos todas las capas excepto la de **/resources o /controllers**, quien contendrá nuestras clases
controladoras anotadas con @RestController.

## RestController - GET Listar productos

Creamos nuestro controlador del tipo **RestController** y empezamos a implementar los métodos handler empezando por el
método listar:

````java

@RestController
@RequestMapping(path = "/api/v1/products")
public class ProductController {
    private final IProductService productService;

    public ProductController(IProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public Mono<ResponseEntity<Flux<Product>>> getAllProducts() {
        return Mono.just(ResponseEntity.ok(this.productService.findAll()));
    }
}
````

En el código anterior observamos que el método **getAllProducts()** retorna un ``Mono<ResponseEntity<Flux<Product>>>``,
aunque también habríamos podido implementar ese método de esta manera:

````java

@RestController
@RequestMapping(path = "/api/v1/products")
public class ProductController {
    /* omitted code */

    @GetMapping
    public Flux<Product> getAllProducts() {
        return this.productService.findAll();
    }
}
````

Es decir, ahora se está devolviendo un ``Flux<Product>``, pero ambos métodos tendría el mismo propósito, que es obtener
todos los productos. Sin embargo, difieren en cómo se manejan la respuesta y el tipo de flujo que devuelven. Veamos
cada uno de ellos:

### Primer método

````java
public class ProductController {
    @GetMapping
    public Mono<ResponseEntity<Flux<Product>>> getAllProducts() {
        return Mono.just(ResponseEntity.ok(this.productService.findAll()));
    }
}
````

En este método, estás devolviendo un ``Mono`` que contiene una instancia de  ``ResponseEntity`` que, a su vez, contiene
un ``Flux`` de productos. Esto **significa que estás encapsulando el flujo de productos dentro de una Response Entity**,
lo que te **permite incluir información adicional junto con los datos** (en este caso, el estado de respuesta HTTP,
cabeceras, etc.).

### Segundo método

````java
public class ProductController {
    @GetMapping
    public Flux<Product> getAllProducts() {
        return this.productService.findAll();
    }
}
````

En este segundo método, **simplemente estás devolviendo el flujo de productos directamente.** No estás encapsulando el
flujo en una entidad de respuesta específica. **Esto podría ser útil si** deseas mantener la respuesta más simple y **no
necesitas incluir metadatos adicionales en la respuesta.**

Listo, en mi caso optaré por utilizar aquellas respuestas que incluyen el uso del **ResponseEntity**. Ahora realizamos
una petición al endpoint y vemos su funcionamiento:

````bash
curl -v http://localhost:8080/api/v1/products | jq
>
> --- Respuesta
< HTTP/1.1 200 OK
< 
[
  {
    "id": "64dbee062870a52236cda908",
    "name": "Sony Cámara HD",
    "price": 680.6,
    "createAt": "2023-08-15",
    "image": null,
    "category": {
      "id": "64dbee062870a52236cda903",
      "name": "Electrónico"
    }
  },
  {
    "id": "64dbee062870a52236cda909",
    "name": "Bicicleta Monteñera",
    "price": 1800.6,
    "createAt": "2023-08-15",
    "image": null,
    "category": {
      "id": "64dbee062870a52236cda904",
      "name": "Deporte"
    }
  },
  {...},
 ]
````

## RestController - GET ver producto

A continuación se muestra la implementación para ver un producto por su id:

````java

@RestController
@RequestMapping(path = "/api/v1/products")
public class ProductController {
    /* omitted code */
    @GetMapping(path = "/{id}")
    public Mono<ResponseEntity<Product>> getProduct(@PathVariable String id) {
        return this.productService.findById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}
````

Realizamos la petición con un **producto existente:**

````bash
curl -v http://localhost:8080/api/v1/products/64dbf481f239914cea4e43bc | jq

-- Respuesta
< HTTP/1.1 200 OK
{
  "id": "64dbf481f239914cea4e43bc",
  "name": "Colchón Medallón 2 plazas",
  "price": 710,
  "createAt": "2023-08-15",
  "image": null,
  "category": {
    "id": "64dbf481f239914cea4e43ae",
    "name": "Muebles"
  }
}
````

Realizamos la petición con un **producto cuyo id no existe:**

````bash
 curl -v http://localhost:8080/api/v1/products/64dbf481f239914cea4e43bx | jq
 
--- Respuesta
< HTTP/1.1 404 Not Found
````

## RestController - POST crear producto

````java

@RestController
@RequestMapping(path = "/api/v1/products")
public class ProductController {
    /* omitted code */
    @PostMapping
    public Mono<ResponseEntity<Product>> createProduct(@RequestBody Product product) {
        if (product.getCreateAt() == null) {
            product.setCreateAt(LocalDate.now());
        }
        return this.productService.saveProduct(product)
                .map(productDB -> ResponseEntity
                        .created(URI.create("/api/v1/products/" + productDB.getId()))
                        .body(productDB));
    }
}
````

Realizamos la petición para crear un producto:

````bash
curl -v -X POST -H "Content-Type: application/json" -d "{\"name\": \"Vidrio templado\", \"price\": 890.50, \"category\": {\"id\": \"64dbf805a735203c6c342b1f\", \"name\": \"Decoración\"}}" http://localhost:8080/api/v1/products

--- Respuesta
< HTTP/1.1 201 Created
< Location: /api/v1/products/64dbf930a735203c6c342b2e
< Content-Type: application/json
<
{
  "id":"64dbf930a735203c6c342b2e",
  "name":"Vidrio templado",
  "price":890.5,
  "createAt":"2023-08-15",
  "image":null,
  "category":{
    "id":"64dbf805a735203c6c342b1f",
    "name":"Decoración"
    }
}
````
