# Reactive Transactions

Spring's transaction abstraction works reactively through `ReactiveTransactionManager`
(`R2dbcTransactionManager`, `ReactiveMongoTransactionManager`). The transaction is bound to the
**Reactor Context**, not a thread.

## Declarative

```java
@Service
class TransferService {

    @Transactional                          // works on methods returning Mono/Flux
    public Mono<Void> transfer(long from, long to, long amountMinor) {
        return accounts.debit(from, amountMinor)
                .then(accounts.credit(to, amountMinor))
                .then(ledger.record(from, to, amountMinor));
        // any onError → rollback; completion → commit; cancellation → rollback
    }
}
```

## Programmatic

```java
TransactionalOperator tx = TransactionalOperator.create(transactionManager);

Mono<Void> work = accounts.debit(from, amount)
        .then(accounts.credit(to, amount))
        .as(tx::transactional);
```

## Rules

1. The whole pipeline must be **returned** — a nested `subscribe()` runs outside the transaction.
2. Everything inside must use the **same reactive connection factory**; a `WebClient` call inside a
   transaction is not part of it (and holds the DB connection while waiting — avoid remote calls inside
   transactions).
3. `@Transactional` on a method returning `Flux` commits when the Flux **completes** — streaming a huge
   result keeps the transaction open.
4. Self-invocation (`this.method()`) bypasses the proxy, exactly as in Spring MVC.

!!! tip "Database + message broker"
    A DB transaction cannot include a Kafka send. Use the **transactional outbox**: write the event to an
    `outbox` table in the same transaction and relay it asynchronously (Debezium CDC or a poller).
