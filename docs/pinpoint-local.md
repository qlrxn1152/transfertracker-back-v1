# Pinpoint Local APM

This project can be monitored locally with Apache Pinpoint 3.1.1.

## Start Pinpoint

```bash
./scripts/pinpoint-up.sh
```

Wait until the script prints `Pinpoint is starting` before running the application from IntelliJ.
The script waits for the local HBase schema first, using HBase container logs as the readiness signal, because starting the app too early can make the Pinpoint agent log temporary `Connection reset` messages while the collector is not ready.
If HBase was created with an older local configuration and keeps failing at this step, reset only the local Pinpoint containers and volumes with `./scripts/pinpoint-down.sh --volumes`, then run `./scripts/pinpoint-up.sh` again.
If it still fails, copy the HBase log lines printed by `pinpoint-up.sh`; those lines show the actual schema or HBase startup error.
When the log contains `Created table AgentId` or `Tables already exist`, the local schema is ready.

The local Pinpoint Web UI is available at:

```text
http://localhost:18080
```

The script uses the existing local Pinpoint Docker checkout:

```text
/Users/dhoon/Desktop/pinpoint-docker-clean
```

It keeps `transfer-tracker` on port `8080` by moving Pinpoint Web to `18080`, Pinpoint MySQL to `13306`, and Pinpoint Redis to `16379`.

For local development, HBase uses `infra/pinpoint/hbase-create-local.hbase`.
This keeps each Pinpoint table to a single region so the first bootstrap finishes quickly on a local Docker runtime.

Pinpoint Web also raises the local trace lookup limits for scatter/transaction inspection:

```text
web.hbase.selectSpans.limit=2000
web.hbase.selectAllSpans.limit=2000
web.hbase.trace.max.results.limit=200000
web.callstack.selectSpans.limit=20000
```

## Run TransferTracker With The Pinpoint Agent

```bash
./scripts/run-with-pinpoint.sh
```

Defaults:

```text
PINPOINT_AGENT_DIR=/Users/dhoon/Desktop/pinpoint-agent-3.1.1
PINPOINT_AGENT_ID=transfer-tracker-local
PINPOINT_APPLICATION_NAME=transfer-tracker
PINPOINT_PROFILE=local
PINPOINT_COLLECTOR_IP=127.0.0.1
```

After the application receives traffic, choose `transfer-tracker` in the Pinpoint Web UI. It can take a short moment for the application to appear.

## Stop Pinpoint

```bash
./scripts/pinpoint-down.sh
```
