# Similar Products API

Aplicación Spring Boot que expone en el puerto **5000** la operación acordada con front-end:

```
GET /product/{productId}/similar
```

Devuelve el detalle de los productos similares al dado, ordenados por similitud, componiendo las dos APIs existentes (`/product/{id}/similarids` y `/product/{id}` en `localhost:3001`).

## Stack

- Java 21 (virtual threads) + Spring Boot 4.1
- Maven (wrapper incluido, no requiere instalación)
- Caffeine para caché en memoria

## Ejecución

```bash
# mocks e infraestructura (desde la raíz del repo)
docker-compose up -d simulado influxdb grafana

# la aplicación
cd similar-products
./mvnw spring-boot:run

# prueba rápida
curl http://localhost:5000/product/1/similar

# test de carga
docker-compose run --rm k6 run scripts/test.js
```

Tests unitarios y de controller: `./mvnw test`

## Arquitectura

```
Controller  →  Service  →  Client (RestClient)  →  APIs existentes (:3001)
                              │
                           Caffeine cache
```

| Capa | Clase | Responsabilidad |
|------|-------|-----------------|
| Controller | `SimilarProductsController` | Expone el contrato REST; `ApiExceptionHandler` traduce `ProductNotFoundException` a 404 |
| Service | `SimilarProductsService` | Orquesta: obtiene los IDs similares y pide los detalles **en paralelo**, preservando el orden de similitud |
| Client | `ProductApiClient` | Única puerta de salida HTTP; encapsula timeouts, manejo de errores y caché |
| Config | `ProductApiClientConfig`, `CacheConfig`, properties tipadas | Toda la configuración es externa (`application.properties`) |

## Decisiones de diseño

### Concurrencia: virtual threads en lugar de WebFlux

Los detalles de los N productos similares se piden en paralelo con un executor de
virtual threads (Java 21), y `spring.threads.virtual.enabled=true` hace que Tomcat
atienda cada request en un virtual thread. Esto da la escalabilidad de I/O no bloqueante
(200 usuarios concurrentes del test de carga) manteniendo código imperativo, simple de
leer y de depurar — el motivo por el que se descartó el stack reactivo.

### Resiliencia: timeouts + degradación parcial

Los mocks simulan productos con delays de 1s, 5s y 50s, y errores 404/500. Sin
timeouts, un solo producto lento colgaría el request (y con 200 VUs, todo el servicio).

- **Timeouts**: 1s de conexión, 2s de lectura (configurables).
- **Degradación parcial**: si el detalle de un producto similar falla (404, 500 o
  timeout), se omite de la respuesta y se devuelven los demás con 200. Es preferible
  responder rápido con 2 de 3 productos que fallar o tardar 50s por el tercero.
- **404 del contrato**: solo cuando el producto *consultado* no existe
  (la llamada a `/similarids` devuelve 404).

**Tradeoff asumido**: los productos cuyo detalle tarda más de 2s (delays de 5s y 50s en
los mocks) quedan fuera de la respuesta. Se priorizó latencia y throughput sobre
completitud; el umbral es un ajuste de configuración, no de código.

### Caché: Caffeine con TTL diferenciado por resultado

Dos cachés en memoria (máx. 10.000 entradas):

- `similar-ids`: ids similares por producto (TTL 30s).
- `products`: detalle por producto, con **expiración variable por entrada**
  (`ProductExpiry`, un `Expiry` de Caffeine):
  - **Aciertos: 30s** — un detalle de producto sano se reutiliza durante todo el TTL.
  - **Fallos: 15s (caché negativa)** — si un producto falló (404/500/timeout) se cachea
    el resultado vacío para no reintentar en cada request, pero con un TTL más corto
    para que un upstream que se recupera vuelva a aparecer en las respuestas antes.
    El valor equilibra frescura y latencia estable: cada reintento de un producto que
    sigue caído bloquea (por el `sync`) a las peticiones concurrentes de ese producto
    durante el read timeout, así que reintentar demasiado a menudo degrada el p95.

Con `sync = true` se evita la estampida de caché: ante N requests concurrentes del
mismo producto frío, solo un hilo llama al upstream y el resto espera ese resultado.

Efecto medido: la primera petición de un producto con upstream lento tarda ~2s (el
timeout), las siguientes ~3ms, y a los 5s se hace un único reintento del producto
fallido sin perder la caché de los sanos.

### Manejo de errores

| Situación | Respuesta |
|-----------|-----------|
| Producto consultado no existe | `404` (contrato) |
| Detalle de un similar falla o expira | Se omite; `200` con el resto |
| Producto sin similares | `200` con `[]` |

### Observabilidad: métricas de caché

Las cachés registran estadísticas (`recordStats`) que Micrometer expone vía Actuator,
para poder ajustar TTL y tamaño con datos reales en lugar de intuición:

```bash
# hit rate de la caché de productos
curl "localhost:5000/actuator/metrics/cache.gets?tag=cache:products&tag=result:hit"
curl "localhost:5000/actuator/metrics/cache.gets?tag=cache:products&tag=result:miss"

# tamaño y desalojos
curl "localhost:5000/actuator/metrics/cache.size?tag=cache:products"
curl "localhost:5000/actuator/metrics/cache.evictions?tag=cache:products"

# cachés registradas
curl localhost:5000/actuator/caches
```

## Configuración

| Property | Default | Descripción |
|----------|---------|-------------|
| `product-api.base-url` | `http://localhost:3001` | Base URL de las APIs existentes |
| `product-api.connect-timeout` | `1s` | Timeout de conexión |
| `product-api.read-timeout` | `2s` | Timeout de lectura por llamada |
| `product-api.cache.ttl` | `30s` | Expiración de resultados correctos |
| `product-api.cache.failure-ttl` | `15s` | Expiración de fallos cacheados (caché negativa) |
| `product-api.cache.max-size` | `10000` | Entradas máximas por caché |

## Resultados del test de carga (k6, 200 VUs, 5 escenarios)

| Métrica | Valor |
|---------|-------|
| Throughput | ~290 req/s |
| Latencia mediana | 11 ms |
| p95 | 84 ms |
| Máximo | 2.1 s (primera petición con caché fría, acotado por el read timeout) |
| Errores | 0 — los 5 escenarios (normal, notFound, error, slow, verySlow) completados |
