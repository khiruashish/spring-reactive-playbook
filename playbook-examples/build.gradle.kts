// Every code sample in the "Project Reactor" and "Testing" chapters lives here as a passing test,
// and is embedded into the docs with pymdownx.snippets — the docs cannot drift from working code.
dependencies {
    implementation("io.projectreactor:reactor-core")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
}
