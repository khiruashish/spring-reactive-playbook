package com.example.reactive.playbook.codinginterview;

import java.time.Duration;
import java.util.List;
import java.util.function.Function;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Problem 11: search-as-you-type: debounce keystrokes, skip duplicates, and CANCEL stale searches when a new one starts. */
public class P11SearchAsYouType {

    public static Flux<List<String>> results(Flux<String> keystrokes, Function<String, Mono<List<String>>> search, Duration quiet) {
        return keystrokes
                .map(String::strip)
                .sampleTimeout(q -> Mono.delay(quiet))     // debounce: emit only after `quiet` with no new keystroke
                .filter(q -> q.length() >= 2)              // don't search for "j"
                .distinctUntilChanged()                    // "jav" -> "java" -> "jav" still searches, "jav" twice doesn't
                .switchMap(q -> search.apply(q));          // new query cancels the in-flight one: no out-of-order results
    }
}
