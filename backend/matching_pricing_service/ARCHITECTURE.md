# Matching & Pricing — Clean Architecture

Dependency rule:

```text
adapter/in       ──> application ──> domain
adapter/outbound ──> application ──> domain
configuration wires ports to adapters
```

- `domain`: business models, value objects and pricing rules. It must not import Spring, MongoDB, RabbitMQ or HTTP classes.
- `application/port/in`: use cases exposed to inbound adapters.
- `application/port/out`: capabilities required by use cases (persistence, Trip Service and routing).
- `application/service`: implementations of inbound use cases; depends only on domain and ports.
- `adapter/in/web`: REST controllers and transport DTOs.
- `adapter/in/messaging`: RabbitMQ consumers.
- `adapter/outbound/persistence/mongo`: MongoDB documents, Spring Data repositories, mappers and port adapters.
- `adapter/outbound/client`: HTTP adapters for Trip Service and the routing provider.
- `configuration`: Spring wiring and framework configuration.

Do not pass Mongo documents or HTTP DTOs into the domain. Map them at the adapter boundary.

## Implementation order

1. Complete domain models and pricing policies.
2. Define input commands/results and output port contracts.
3. Implement application services with unit tests using fake ports in test scope only.
4. Implement MongoDB, Trip Service and routing adapters.
5. Implement REST controllers.
6. Implement idempotent event handling, then enable RabbitMQ consumers.

