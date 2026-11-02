package com.example.reactive.playbook.reactor;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.BufferOverflowStrategy;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.test.StepVerifier;

class BackpressureTest {

    @Test
    void subscriberControlsDemand() {
        // --8<-- [start:request]
        List<Long> requests = new CopyOnWriteArrayList<>();
        List<Integer> received = new CopyOnWriteArrayList<>();

        Flux.range(1, 10)
                .doOnRequest(requests::add)                       // observe demand flowing upstream
                .subscribe(new BaseSubscriber<Integer>() {
                    @Override
                    protected void hookOnSubscribe(Subscription s) {
                        request(2);                               // "I can handle 2 for now"
                    }

                    @Override
                    protected void hookOnNext(Integer value) {
                        received.add(value);
                        if (received.size() % 2 == 0) {
                            request(2);                           // processed a batch → ask for more
                        }
                    }
                });

        assertThat(received).hasSize(10);
        assertThat(requests).containsOnly(2L);
        // --8<-- [end:request]
    }

    @Test
    void limitRateReshapesDemand() {
        // --8<-- [start:limitRate]
        List<Long> requests = new CopyOnWriteArrayList<>();
        Flux.range(1, 100)
                .doOnRequest(requests::add)
                .limitRate(10)        // request 10 upfront, then replenish 75% (8) at a time
                .blockLast();         // block() requests unbounded — limitRate protects the source
        assertThat(requests.getFirst()).isEqualTo(10L);
        assertThat(requests).contains(8L);
        // --8<-- [end:limitRate]
    }

    @Test
    void overflowStrategies() {
        // --8<-- [start:overflow]
        // A fast producer (1..100, emitted without looking at demand) meets a slow subscriber that
        // requests 1 element, and only later requests more. What survives depends on the strategy:
        assertThat(slowConsumer(fastProducer().onBackpressureDrop()))
                .containsExactly("1", "C", "|later");   // 2..100 dropped on arrival; completion needs no demand

        assertThat(slowConsumer(fastProducer().onBackpressureLatest()))
                .containsExactly("1", "|later", "100", "C");                // only the newest is kept

        assertThat(slowConsumer(fastProducer().onBackpressureBuffer(5, dropped -> { }, BufferOverflowStrategy.DROP_OLDEST)))
                .containsExactly("1", "|later", "96", "97", "98", "99", "100", "C"); // newest 5

        assertThat(slowConsumer(fastProducer().onBackpressureBuffer(5)))     // default: ERROR on overflow
                .containsExactly("1", "|later", "2", "3", "4", "5", "6", "error:OverflowException");
        // the overflow error is delayed until the buffered elements have been drained
        // --8<-- [end:overflow]
    }

    /** Requests 1 element on subscribe, then 1000 more "later"; records what it receives. */
    static List<String> slowConsumer(Flux<Integer> source) {
        List<String> received = new CopyOnWriteArrayList<>();
        BaseSubscriber<Integer> subscriber = new BaseSubscriber<>() {
            @Override
            protected void hookOnSubscribe(Subscription s) {
                request(1);
            }

            @Override
            protected void hookOnNext(Integer value) {
                received.add(String.valueOf(value));
            }

            @Override
            protected void hookOnError(Throwable t) {
                received.add("error:" + t.getClass().getSimpleName());
            }

            @Override
            protected void hookOnComplete() {
                received.add("C");
            }
        };
        source.subscribe(subscriber);
        received.add("|later");
        subscriber.request(1_000);
        return received;
    }

    /** Emits 1..100 synchronously, regardless of downstream demand, then completes. */
    static Flux<Integer> fastProducer() {
        return Flux.create(sink -> {
            for (int i = 1; i <= 100; i++) {
                sink.next(i);
            }
            sink.complete();
        }, FluxSink.OverflowStrategy.IGNORE);
    }

    @Test
    void batchingOperators() {
        // --8<-- [start:batching]
        StepVerifier.create(Flux.range(1, 7).buffer(3))
                .expectNext(List.of(1, 2, 3), List.of(4, 5, 6), List.of(7))
                .verifyComplete();

        // size OR time, whichever comes first — ideal for bulk inserts / batched Kafka sends
        StepVerifier.withVirtualTime(() -> Flux.interval(Duration.ofMillis(300)).take(5)
                        .bufferTimeout(10, Duration.ofSeconds(1)))
                .thenAwait(Duration.ofSeconds(2))
                .expectNext(List.of(0L, 1L, 2L, 3L))  // timer starts at the first element (t=300ms) → flush at 1.3s
                .expectNext(List.of(4L))              // flushed on completion
                .verifyComplete();

        StepVerifier.create(Flux.range(1, 6).window(2).flatMap(w -> w.reduce(Integer::sum)))
                .expectNext(3, 7, 11)                 // window = buffer of Fluxes (streamed, not collected)
                .verifyComplete();

        StepVerifier.create(Flux.just("apple", "bean", "avocado", "beet")
                        .groupBy(s -> s.charAt(0))
                        .flatMap(g -> g.count().map(c -> g.key() + "=" + c)))
                .expectNextMatches(s -> s.equals("a=2") || s.equals("b=2"))
                .expectNextMatches(s -> s.equals("a=2") || s.equals("b=2"))
                .verifyComplete();
        // --8<-- [end:batching]
    }
}
