package com.example.reactive.playbook.codinginterview;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;
import reactor.core.Disposable;
import reactor.test.StepVerifier;

class P06PriceFeedTest {

    static P06PriceFeed.Tick t(long p) { return new P06PriceFeed.Tick("ACME", p); }

    @Test
    void lateSubscriber_getsLastThree_thenLiveTicks() {
        var feed = new P06PriceFeed(3);
        for (long p = 100; p <= 105; p++) feed.publish(t(p));
        StepVerifier.create(feed.ticks().map(P06PriceFeed.Tick::priceCents).take(5))
                .expectNext(103L, 104L, 105L)                  // replayed history
                .then(() -> { feed.publish(t(106)); feed.publish(t(107)); })
                .expectNext(106L, 107L)                        // live
                .verifyComplete();
    }

    @Test
    void manySubscribers_shareOneUpstream_andReceiveTheSameTicks() {
        var feed = new P06PriceFeed(1);
        List<Long> a = new CopyOnWriteArrayList<>(), b = new CopyOnWriteArrayList<>();
        Disposable da = feed.ticks().subscribe(x -> a.add(x.priceCents()));
        Disposable db = feed.ticks().subscribe(x -> b.add(x.priceCents()));
        assertThat(feed.subscriberCount()).isEqualTo(2);
        feed.publish(t(1)); feed.publish(t(2));
        assertThat(a).containsExactly(1L, 2L).isEqualTo(b);
        da.dispose();
        assertThat(feed.subscriberCount()).isEqualTo(1);       // cancelling one doesn't affect others
        db.dispose();
    }

    @Test
    void filterPerSymbol_andCompletionReachesSubscribers() {
        var feed = new P06PriceFeed(10);
        feed.publish(new P06PriceFeed.Tick("ACME", 1)); feed.publish(new P06PriceFeed.Tick("INFY", 2)); feed.publish(new P06PriceFeed.Tick("ACME", 3));
        feed.close();
        StepVerifier.create(feed.ticksFor("ACME").map(P06PriceFeed.Tick::priceCents)).expectNext(1L, 3L).verifyComplete();
    }
}
