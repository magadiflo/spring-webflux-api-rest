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

## RestController - PUT actualizar producto

El código implementado para el endpoint de actualizar producto es el siguiente:

````java

@RestController
@RequestMapping(path = "/api/v1/products")
public class ProductController {
    @PutMapping(path = "/{id}")
    public Mono<ResponseEntity<Product>> updateProduct(@PathVariable String id, @RequestBody Product product) {
        return this.productService.findById(id)             // Mono<Product>
                .flatMap(productDB -> {                     // Mono<Product>
                    productDB.setName(product.getName());
                    productDB.setPrice(product.getPrice());
                    productDB.setCategory(product.getCategory());
                    return this.productService.saveProduct(productDB); // Mono<Product>
                })
                .map(ResponseEntity::ok)                    // Mono<ResponseEntity<Product>>
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}
````

En este punto es necesario hacer una diferencia entre **flatMap()** y **map()**:

1. ``map`` **en un Mono**, es un operador que se utiliza para **transformar el valor contenido en el Mono en otro
   valor.** En otras palabras, toma el valor actual dentro del Mono y aplica una función a ese valor para producir un
   nuevo valor. **El resultado es un nuevo Mono que contiene el valor transformado.**

   Ahora, tomando como referencia el código anterior vemos que implementamos ``map(ResponseEntity::ok)``, la explicación
   sería: Cuando el flujo llega a ese **map()** llega como un ``Mono<Product>``, luego el map lo que hace es coger el
   valor interno de ese **Mono**, es decir coge el **Product** y lo transforma en su interior; en nuestro caso, lo que
   hacemos con ese **Product** es convertirlo en un **ResponseEntity.ok(product)** o en su defecto la forma abreviada
   sería **ResponseEntity::ok**, finalmente el **map()** retorna un **Mono** del valor transformado, es decir retorna un
   ``Mono<ResponseEntity<Product>>``.


2. ``flatMap`` **en un Mono**, es un operador que se utiliza para transformar el valor contenido en un Mono en otro
   Mono (permitiendo operaciones reactivas anidadas). La diferencia clave entre map y flatMap es que flatMap permite
   trabajar con valores que también son reactivos. **El flatMap se encarga de manejar la "desenvoltura" de los Monos
   anidados. El resultado final es un Mono que contiene el valor transformado.**

   Ahora, tomando como referencia el código anterior vemos que implementamos:
   ````
   .flatMap(productDB -> {                                 // Mono<Product>
        productDB.setName(product.getName());
        productDB.setPrice(product.getPrice());
        productDB.setCategory(product.getCategory());
        return this.productService.saveProduct(productDB); // Mono<Product>
    })
   ````

   Cuando el flujo llega al **flatMap()** llega como un ``Mono<Product>``, lo que hace el **flatMap** es coger el valor
   interno del Mono, o sea coge el **Product**, luego dentro del flatMap **se aplica alguna transformación
   a ese Product** e incluso se utiliza el **productService** para guardar el producto modificado y retorna el valor
   devuelto por el **saveProduct()** que es un ``Mono<Product>``, finalmente lo que hace el **flatMap()** es eliminar
   las múltiples envolturas que puedan haber del Mono, es decir si en vez de flatMap, usáramos el **map()** lo que haría
   ese map, luego de que el **saveProduct()** retorne un ``Mono<Product>`` sería retornar al flujo un
   ``Mono<Mono<Product>>``, es decir lo que está haciendo es envolver la respuesta dada por el **saveProduct()** dentro
   de un **Mono** y eso no queremos, por eso es que usamos el **flatMap()** ya que este se encarga de aplanar la
   respuesta en un solo **Mono**.

Listo, ahora sí actualizamos un producto existente:

````bash
curl -v -X PUT -H "Content-Type: application/json" -d "{\"name\": \"Scooter\", \"price\": 5000.50, \"category\": {\"id\": \"64dbfdc182114c2f7ab5361b\", \"name\": \"Electrónico\"}}" http://localhost:8080/api/v1/products/64dbfdc282114c2f7ab53621 | jq

--- Respuesta
< HTTP/1.1 200 OK
< Content-Type: application/json
{
  "id": "64dbfdc282114c2f7ab53621",
  "name": "Scooter",
  "price": 5000.5,
  "createAt": "2023-08-15",
  "image": null,
  "category": {
    "id": "64dbfdc182114c2f7ab5361b",
    "name": "Electrónico"
  }
}
````

Si tratamos de actualizar un producto que no existe:

````bash
curl -v -X PUT -H "Content-Type: application/json" -d "{\"name\": \"Scooter\", \"price\": 5000.50, \"category\": {\"id\": \"64dbfdc182114c2f7ab5361b\", \"name\": \"Electrónico\"}}" http://localhost:8080/api/v1/products/64dbfdc282114c2f7ab5lkju | jq

--- Respuesta
< HTTP/1.1 404 Not Found
````

## RestController - DELETE eliminar producto

Implementamos el endpoint para eliminar el producto:

````java

@RestController
@RequestMapping(path = "/api/v1/products")
public class ProductController {
    /* omitted code */
    @DeleteMapping(path = "/{id}")
    public Mono<ResponseEntity<Void>> deleteProduct(@PathVariable String id) {
        return this.productService.findById(id)
                .flatMap(productDB -> this.productService.delete(productDB).then(Mono.just(true)))  // (1)
                .map(isDeleted -> new ResponseEntity<Void>(HttpStatus.NO_CONTENT))                  // (2)
                .defaultIfEmpty(ResponseEntity.notFound().build());                                 // (3)
    }
}
````

**(1)** en esa parte del código estamos llamando al  ``this.productService.delete(productDB)`` quien nos retorna un
``Mono<Void>``, precisamente porque nos retorna un ``Mono<Void>`` es que usamos el método **then()** para crear otro
**Mono** que tenga un tipo que no sea **Void**, en mi caso, creé un ``Mono<Boolean>`` para que el flujo continúe en el
siguiente operador **map()**. **¿Qué pasa si no hubiera usado el then(), es decir si solo hubiera retornado el
this.productService.delete(productDB)?**, pues como el método **delete()** retorna un ``Mono<Void>``, le estaríamos
diciendo al flujo que lo que sigue es vacío, por lo tanto, ya no entraría en el operador **map()** sino se pasaría al
operador **defaultIfEmpty()**.

**(2)** usamos el **new** para crear el objeto ResponseEntity indicándole que es **Void**. En este caso no usamos el
método estático como en el **(3)** porque si lo hacemos nos muestra el error diciendo que lo que se está retornando es
un Object: ``Mono<ResponseEntity<Object>>`` y lo que nosotros tenemos que retornar es un ``Mono<ResponseEntity<Void>>``,
eso lo conseguimos con el **new ResponseEntity<Void>()**. Ahora, en el **(3)**, que corresponde al método  
**defaultIfEmpty()** sí acepta la creación del ResponseEntity con el método estático.

Eliminando un producto:

````bash
curl -v -X DELETE http://localhost:8080/api/v1/products/64dc26198f7b916486d2fc2f

--- Respuesta
>
< HTTP/1.1 204 No Content
````

Eliminando nuevamente el mismo producto:

````bash
curl -v -X DELETE http://localhost:8080/api/v1/products/64dc26198f7b916486d2fc2f

--- Respuesta
>
< HTTP/1.1 404 Not Found
````

## Subiendo solo imagen

Implementaremos un endpoint que nos permitirá **subir una imagen en función del id del producto** que pasemos como un
path variable:

````java

@RestController
@RequestMapping(path = "/api/v1/products")
public class ProductController {

    /* omitted code */

    @Value("${config.uploads.path}")
    private String uploadsPath;

    /* omitted code */
    @PostMapping(path = "/upload/{id}")
    public Mono<ResponseEntity<Product>> uploadImage(@PathVariable String id, @RequestPart FilePart imageFile) { // (1)
        return this.productService.findById(id)
                .flatMap(productDB -> {
                    String imageName = UUID.randomUUID().toString() + "-" + imageFile.filename()
                            .replace(" ", "")
                            .replace(":", "")
                            .replace("\\", "");
                    productDB.setImage(imageName);

                    return imageFile.transferTo(new File(this.uploadsPath + productDB.getImage())) // (2)
                            .then(this.productService.saveProduct(productDB));
                })
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}
````

**(1)** del código anterior, vemos que como segundo parámetro usamos ``@RequestPart FilePart imageFile``, donde:

- **FilePart**, es una especialización de ``Part`` que representa un archivo cargado recibido en un request de
  **multipart**. Un ``Part`` es una representación de una parte en un request ``multipart/form-data``. El origen de una
  solicitud **multipart** puede ser un formulario de navegador, en cuyo caso cada parte es un
  **FormFieldPart o FilePart**. Los request multipart también se pueden usar fuera de un navegador para datos de
  cualquier tipo de contenido (por ejemplo, JSON, PDF, etc.).
- **@RequestPart**, anotación que se puede usar para asociar la parte de una solicitud **"multipart/form-data"** con un
  argumento de método.
- **imageFile**, aparte de ser la variable asociada al FilePart, también es el nombre del campo con el que se debe
  enviar la imagen en la solicitud.

**(2)** en el return vemos dos partes, la primera es la ejecución del ``imageFile.transferTo(new File(ruta-e-imagen))``
quien retorna un ``Mono<Void>``, es decir, en esta primera parte nos encargamos de subir la imagen al servidor donde
estará almacenado y una vez finalizado ese proceso el método **transferTo()** retorna un ``Mono<Void>``, por
consiguiente, hasta ese punto finalizó el proceso de subida de imagen, entonces **para continuar con un nuevo flujo**,
en esta segunda parte usamos el ``.then(this.productService.saveProduct(productDB));`` e internamente usamos el servicio
para guarda o actualizar el producto en la base de datos. Como respuesta, el método ``saveProduct(productDB)`` nos
retorna un ``Mono<Product>`` que es lo que finalmente retorna el **flatMap()**.

Podemos utilizar **Postman** para subir la imagen, en el apartado de **Body** seleccionar **form-data**, ingresar el
nombre del campo que almacenará la imagen, en este caso sería **imageFile**, adjuntar la imagen y enviar la solicitud.
En mi caso seguiré usando **curl**, ya que es más fácil tener los comandos que se ejecutan para colocarlos en este
informe:

````bash
curl -v -X POST -H "Content-Type: multipart/form-data" -F "imageFile=@C:\Users\USUARIO\Downloads\bicicleta.png" http://localhost:8080/api/v1/products/upload/64dce9cc4db0da636eb5928d | jq

--- Respuesta
< HTTP/1.1 200 OK
< Content-Type: application/json
<
{
  "id": "64dce9cc4db0da636eb5928d",
  "name": "Bicicleta Monteñera",
  "price": 1800.6,
  "createAt": "2023-08-16",
  "image": "04e7567d-39d7-4f3c-bf12-7010f4961681-bicicleta.png",
  "category": {
    "id": "64dce9cc4db0da636eb59288",
    "name": "Deporte"
  }
}
````

**DONDE**

- **"Content-Type: multipart/form-data"** indica que estás enviando datos de formulario multipartes
- **-F**, especifica el campo **imageFile** que contiene la imagen que deseas cargar. La **-F** significa **"form field"
  o "campo de formulario"**. Específicamente, se utiliza para adjuntar datos en formato multipart/form-data, que es
  comúnmente utilizado para **enviar archivos y campos de formulario** a través de solicitudes HTTP POST.
- El símbolo **@** indica que el valor siguiente debe ser interpretado como un archivo.

## Subiendo imagen junto a su producto

Creamos el método que permitirá subir un producto junto a su imagen al mismo tiempo:

````java

@RestController
@RequestMapping(path = "/api/v1/products")
public class ProductController {
    /* omitted code */
    @PostMapping(path = "/product-with-image")
    public Mono<ResponseEntity<Product>> createProductWithImage(Product product, @RequestPart FilePart imageFile) {
        if (product.getCreateAt() == null) {
            product.setCreateAt(LocalDate.now());
        }

        String imageName = UUID.randomUUID().toString() + "-" + imageFile.filename()
                .replace(" ", "")
                .replace(":", "")
                .replace("\\", "");
        product.setImage(imageName);

        return imageFile.transferTo(new File(this.uploadsPath + product.getImage()))
                .then(this.productService.saveProduct(product)
                        .map(productDB -> ResponseEntity
                                .created(URI.create("/api/v1/products/" + productDB.getId()))
                                .body(productDB))
                );
    }
}
````

No podemos subir los datos del producto como un json, tiene que ser el **Content-Type del request como un form-data**
por eso es que **al primer argumento Product product le quitamos el @RequestBody**.

Cuando mandemos la petición **escribiremos campo por campo todos los pertenecientes a Product y en automático se
mapearán al argumento product**.

La implementación del método **createProductWithImage()** es una fusión de los métodos implementados en
**createProduct() y uploadImage()**.

Ahora, haremos una petición a nuestro endpoint usando curl:

````bash
curl -v -X POST -H "Content-Type: multipart/form-data" -F "name=casa de perrito" -F "price=8900.50" -F "category.id=64dd00a45600b9132d8a4652" -F "category.name=Muebles" -F "imageFile=@C:\Users\USUARIO\Downloads\casa.png" http://localhost:8080/api/v1/products/product-with-image | jq

--- Respuesta
< HTTP/1.1 201 Created
< Location: /api/v1/products/64dd01fd5600b9132d8a4664
< Content-Type: application/json
< Content-Length: 215
{
  "id": "64dd01fd5600b9132d8a4664",
  "name": "casa de perrito",
  "price": 8900.5,
  "createAt": "2023-08-16",
  "image": "b1de00c2-d642-4747-a38c-7ab19e1abf80-casa.png",
  "category": {
    "id": "64dd00a45600b9132d8a4652",
    "name": "Muebles"
  }
````

El parámetro **-F** se utiliza para indicar que **estás adjuntando un campo de formulario en la solicitud POST**. La
abreviación **-F significa "form field" o "campo de formulario"**. Específicamente, se utiliza para adjuntar datos en
formato multipart/form-data, que es comúnmente utilizado para enviar archivos y campos de formulario a través de
solicitudes HTTP POST. En el caso del campo **imageFile** estamos usando el símbolo **@** que indica que el valor
siguiente debe ser interpretado como un archivo.

