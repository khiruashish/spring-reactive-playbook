# Kotlin Coroutines

Spring WebFlux has first-class Kotlin coroutine support: `suspend` functions instead of `Mono`, `Flow`
instead of `Flux` — same non-blocking engine, imperative-looking code.

```kotlin
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
```

```kotlin
@RestController
@RequestMapping("/products")
class ProductController(private val repository: ProductCoroutineRepository,
                        private val client: WebClient) {

    @GetMapping("/{id}")
    suspend fun get(@PathVariable id: String): Product =
        repository.findById(id) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)

    @GetMapping
    fun all(): Flow<Product> = repository.findAll()

    @GetMapping("/{id}/view")
    suspend fun view(@PathVariable id: String): ProductView = coroutineScope {
        val product = async { repository.findById(id)!! }                       // concurrent
        val stock = async { client.get().uri("/stock/{id}", id).retrieve().awaitBody<Stock>() }
        ProductView(product.await(), stock.await())
    }
}

interface ProductCoroutineRepository : CoroutineCrudRepository<Product, String>
```

| Reactor | Coroutines |
|---|---|
| `Mono<T>` | `suspend fun(): T?` |
| `Flux<T>` | `Flow<T>` |
| `Mono.zip(a, b)` | `async { }` + `await()` in `coroutineScope` |
| `.block()` | never — `runBlocking` only in `main`/tests |
| Context | `ReactorContext` coroutine element |
| `mono.awaitSingle()` / `flux.asFlow()` | bridges in `kotlinx-coroutines-reactor` |

Coroutines and Reactor interoperate freely, so a team can mix Java services written with Reactor and
Kotlin services written with coroutines on the same platform.
