# 2. Monitoring as an opt-in Compose profile

Status: accepted

## Context
The portal should be observable: operators need to see failures, why they happened, and business health. Monitoring tools (Prometheus, Grafana, Loki, Alertmanager) are heavy, and most developers do not need them to work on the domain logic.

## Decision
Put the monitoring services in a Compose profile named `monitoring`. They start only with `make up-monitoring`, so `make up` stays light.

- **Metrics:** the API exposes Micrometer metrics at `/actuator/prometheus`. Prometheus scrapes them over the internal Docker network. The backend publishes no host port and nginx does not proxy `/actuator`, so the endpoint is not public.
- **Business metrics:** counters are kept in one `DisputeMetrics` component, so names stay consistent and dashboards keep working.
- **Logs:** the API writes ECS JSON to stdout. Grafana Alloy ships logs from this project's containers to Loki, so a `requestId` links a log line to a request.
- **Dashboards and alerts** are provisioned from files in `infra/monitoring`, so they are reviewed like code.
- **Alerts** are routed through Alertmanager. The receiver is empty until a team chooses a destination.

## Consequences
- Metrics and logs live only on the developer's machine and are lost on `make reset`.
- Alloy reads the Docker socket read-only, which gives it host-level access. This is acceptable on a laptop and is a reason not to run this profile on shared machines.
- Container CPU and memory are not available on Docker Desktop for Mac, because cAdvisor cannot name containers there.
- Production needs a managed metrics and logging platform. The Prometheus scrape job and the ECS log format are portable to such a platform.

## Alternatives considered
- **Always-on monitoring:** slows every `make up` and adds memory pressure to every developer machine.
- **Application-level dashboards only:** cannot show database or JVM health, and cannot alert.
- **A hosted SaaS monitor from the start:** needs an account, credentials and a network path the repository should not assume.
