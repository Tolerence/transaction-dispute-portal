# Monitoring runbook

What each alert means, what to check first, and how to confirm the system is healthy again.

Start the local stack with `make up-monitoring`. Open the dashboard at http://127.0.0.1:3000 (Dispute Portal → Dispute Portal API), Prometheus at http://127.0.0.1:9090 and Alertmanager at http://127.0.0.1:9093.

Logs are in Loki and on the dashboard's **Logs** row. Each backend log line is JSON with a `requestId`. Use that ID to follow one request.

## Alerts

### BackendDown (critical)
Prometheus cannot scrape the API for one minute.

1. `docker compose --profile monitoring ps backend`. If it is not `healthy`, read its logs: `docker compose logs --tail=200 backend`.
2. A startup failure (for example a missing `DB_PASSWORD` or an unreachable database) shows as a stack trace in the logs. Fix the setting in `.env`, then `docker compose up -d backend`.
3. If the container is running but the scrape still fails, check `http://127.0.0.1:9090/targets`.

Recovered when: `up{job="dispute-portal"}` is 1 and the **Backend scrape** panel shows UP.

### PostgresDown (critical)
The Postgres exporter cannot reach the database. The API will fail too.

1. `docker compose ps db`, then `docker compose logs --tail=100 db`.
2. Check the disk, since Postgres stops writing when its volume fills.
3. If the volume is corrupted, do not delete it without a backup. See the backup note below.

Recovered when: `pg_up` is 1.

### HighServerErrorRate (warning)
More than 5% of API requests returned 5xx for five minutes.

1. Open the **Backend ERROR lines** panel and pick a recent line.
2. Copy its `requestId` and search for it in the same panel. The stack trace is in the surrounding lines.
3. If the errors all come from one endpoint, check the **p95 latency by endpoint** panel to see whether it is also slow.
4. If errors start right after a deploy, roll back to the previous image.

Recovered when: the **5xx share** stat returns below 1%.

### HighLatencyP95 (warning)
The 95th-percentile response time has been above one second for ten minutes.

1. Check **Connection pool (API)**. If `waiting` is above zero, see DatabasePoolExhausted.
2. Check **JVM → GC pause time**. Long pauses mean the heap is too small or under pressure.
3. Check **Postgres commits per second** for an unusual spike, which can mean a runaway job.

### DatabasePoolExhausted (warning)
Requests are waiting for a database connection for two minutes.

1. Look at **Postgres connections**. Many idle connections point to a leak. Many active ones point to slow queries.
2. Restart the API only as a last resort, because it drops in-flight requests. Graceful shutdown waits up to 20 seconds.

### HighHeapUsage (warning)
Heap use is above 90% of its maximum for five minutes.

1. Check **Heap used vs maximum** to see whether it is climbing (a leak) or flat (too small).
2. If it is climbing, capture a heap dump and restart the API.

### FrontendErrorSpike (warning)
More than 10 browser errors in 15 minutes.

1. Open **Backend logs → Browser errors** and check the `route` and `message` fields.
2. A single route with the same message points to one broken page. Reproduce it in the browser.

### DisputeBacklogGrowing (info)
More than 50 disputes have been waiting for action for 30 minutes.

This is a business signal, not a fault. Check **Waiting for action now** and tell the review team. Reviewers can clear the queue from the review screens.

## Routine checks

- **Review decisions per hour** and **Submitted (1h)** should follow normal business hours. A sudden drop to zero during business hours means customers cannot submit.
- **Submissions refused by rules (1h)** rising sharply can mean a bug in the eligibility rules, or customers are being sent to the wrong transactions.
- **Self-review attempts blocked (1h)** should be rare. A rise means someone is testing the reviewer rules, so check the **Review dispute audit trail** log panel.

## Backup note

The database lives in the `db-data` Docker volume. The local stack has no backups. Before any change that could lose data, run:

```bash
docker compose exec -T db pg_dump -U dispute dispute_portal > backup-$(date +%Y%m%d).sql
```

Production must use managed backups with point-in-time recovery. Do not rely on this command there.

## Known limits

- Alerts are shown in Alertmanager but are not sent anywhere until a receiver is added in `infra/monitoring/alertmanager/alertmanager.yml`.
- Container CPU and memory are not monitored on Docker Desktop for Mac.
- Metrics and logs are kept on the local machine only. They are lost on `make reset`.
