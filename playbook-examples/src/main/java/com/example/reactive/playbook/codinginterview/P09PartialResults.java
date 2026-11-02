package com.example.reactive.playbook.codinginterview;

import java.util.List;
import java.util.function.Function;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Problem 9: import 1,000 rows; a bad row must not kill the stream. Return successes AND failures with reasons. */
public class P09PartialResults {

    public sealed interface Outcome permits Ok, Failed { String id(); }
    public record Ok(String id, String value) implements Outcome { }
    public record Failed(String id, String reason) implements Outcome { }
    public record Report(List<Ok> ok, List<Failed> failed) { }

    public static Mono<Report> importAll(Flux<String> ids, Function<String, Mono<String>> process, int concurrency) {
        return ids
                .flatMapSequential(id -> process.apply(id)
                        .<Outcome>map(v -> new Ok(id, v))
                        .onErrorResume(e -> Mono.just(new Failed(id, e.getMessage()))),   // contain the error PER ELEMENT
                        concurrency)
                .collectList()
                .map(all -> new Report(
                        all.stream().filter(Ok.class::isInstance).map(Ok.class::cast).toList(),
                        all.stream().filter(Failed.class::isInstance).map(Failed.class::cast).toList()));
    }
}
