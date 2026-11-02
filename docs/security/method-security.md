# Method Security

```java
@Configuration
@EnableReactiveMethodSecurity
class MethodSecurityConfig { }
```

```java
@Service
class ProductAdminService {

    @PreAuthorize("hasRole('ADMIN')")
    public Mono<Void> delete(String id) { ... }

    @PreAuthorize("#ownerId == authentication.name")
    public Flux<Product> myProducts(String ownerId) { ... }

    @PostAuthorize("returnObject.ownerId == authentication.name")
    public Mono<Product> get(String id) { ... }
}
```

Methods must return a `Publisher` (`Mono`/`Flux`) or be Kotlin `suspend` functions: the check is
evaluated when the result is **subscribed**, using the security context from the Reactor Context. A
denied call produces an `AccessDeniedException` signal → 403.

`@PreFilter`/`@PostFilter` work on `Flux` arguments/results element by element.

!!! tip
    Keep URL rules for coarse access (authenticated? which scope?) and method security for domain rules
    (ownership, tenant). Both can coexist.
