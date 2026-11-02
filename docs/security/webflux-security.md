# WebFlux Security

```kotlin
implementation("org.springframework.boot:spring-boot-starter-security")
```

## The filter chain

```java
@Configuration
@EnableWebFluxSecurity
class SecurityConfig {

    @Bean
    SecurityWebFilterChain security(ServerHttpSecurity http) {
        return http
                .authorizeExchange(auth -> auth
                        .pathMatchers("/actuator/health/**", "/public/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/products/**").permitAll()
                        .pathMatchers("/admin/**").hasRole("ADMIN")
                        .anyExchange().authenticated())
                .httpBasic(Customizer.withDefaults())
                .csrf(ServerHttpSecurity.CsrfSpec::disable)     // only for stateless token APIs!
                .build();
    }

    @Bean
    MapReactiveUserDetailsService users(PasswordEncoder encoder) {
        UserDetails user = User.withUsername("ana").password(encoder.encode("pw")).roles("USER").build();
        UserDetails admin = User.withUsername("root").password(encoder.encode("pw")).roles("ADMIN").build();
        return new MapReactiveUserDetailsService(user, admin);
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
```

For users in a database, implement `ReactiveUserDetailsService` with a reactive repository.

## Reading the current user

=== "In a controller"

    ```java
    @GetMapping("/me")
    Mono<String> me(@AuthenticationPrincipal Mono<UserDetails> user) {
        return user.map(UserDetails::getUsername);
    }
    ```

=== "Anywhere in the pipeline"

    ```java
    Mono<String> currentUser = ReactiveSecurityContextHolder.getContext()
            .map(SecurityContext::getAuthentication)
            .map(Authentication::getName);
    ```

!!! danger "`SecurityContextHolder` is empty in WebFlux"
    The servlet `SecurityContextHolder` is `ThreadLocal`-based and is not populated in a reactive
    application. Always use `ReactiveSecurityContextHolder` (or method arguments).

## Testing secured endpoints

```java
@WebFluxTest(ProductController.class)
@Import(SecurityConfig.class)
class ProductSecurityTest {
    @Autowired WebTestClient client;

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanDelete() {
        client.delete().uri("/admin/products/1").exchange().expectStatus().is2xxSuccessful();
    }

    @Test
    void anonymousIsRejected() {
        client.mutateWith(SecurityMockServerConfigurers.csrf())
                .post().uri("/products").exchange().expectStatus().isUnauthorized();
    }
}
```

`spring-security-test` also offers `mockJwt()`, `mockOAuth2Login()` and `mockUser()` mutators for
`WebTestClient`.
