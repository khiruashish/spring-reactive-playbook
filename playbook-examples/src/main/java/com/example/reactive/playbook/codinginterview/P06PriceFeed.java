package com.example.reactive.playbook.codinginterview;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/** Problem 6: one live price feed shared by many subscribers; late joiners get the last N prices immediately. */
public class P06PriceFeed {

    public record Tick(String symbol, long priceCents) { }

    private final Sinks.Many<Tick> sink;

    public P06PriceFeed(int replayLast) {
        this.sink = Sinks.many().replay().limit(replayLast);          // hot + bounded replay for late subscribers
    }

    /** Producers call this; FAIL_FAST surfaces concurrent-emission bugs (use a serialized/busy-looping handler if needed). */
    public void publish(Tick t) { sink.emitNext(t, Sinks.EmitFailureHandler.FAIL_FAST); }

    public void close() { sink.emitComplete(Sinks.EmitFailureHandler.FAIL_FAST); }

    public Flux<Tick> ticks() { return sink.asFlux(); }               // expose as Flux: callers can't emit

    public Flux<Tick> ticksFor(String symbol) { return ticks().filter(t -> t.symbol().equals(symbol)); }

    public int subscriberCount() { return sink.currentSubscriberCount(); }
}
