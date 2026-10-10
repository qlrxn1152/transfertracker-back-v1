# New Relic on Railway

TransferTracker can be monitored on Railway with the New Relic Java agent.

## Required Railway Variables

Set these variables on the `transfertracker-back-v1` service in the `production` environment:

```text
NEW_RELIC_LICENSE_KEY=<your New Relic ingest license key>
NEW_RELIC_APP_NAME=transfer-tracker-prod
```

`NEW_RELIC_LICENSE_KEY` must be a New Relic **INGEST - LICENSE** key. A user API key or browser key
will let the app start, but the Java agent will log `Invalid license key` and will not report APM
data.

Do not commit the license key to Git.

## Runtime

Railway may build this service with Railpack, so the live service start command is set with
the Railway service `startCommand`. `RAILPACK_START_CMD` and `NIXPACKS_START_CMD` are kept in sync
as fallback variables.

The service `buildCommand` builds the Spring Boot jar and downloads the current New Relic Java
agent into `/app/newrelic`. This is done during build because the smaller Railpack runtime image
does not include `curl`.

The service `startCommand` then runs the Spring Boot jar with:

```text
-Dnewrelic.config.license_key=$NEW_RELIC_LICENSE_KEY
-Dnewrelic.config.app_name=$NEW_RELIC_APP_NAME
-Dnewrelic.config.log_file_name=STDOUT
-Dnewrelic.config.distributed_tracing.enabled=true
-Dserver.port=$PORT
-javaagent:/app/newrelic/newrelic.jar
```

The New Relic configuration flags are placed before `-javaagent` so the agent can read them during
startup.

## Verify

After deploying, check Railway logs for New Relic startup lines:

```bash
railway logs --service transfertracker-back-v1 --lines 100
```

Then send a few requests to the production app and check New Relic APM for `transfer-tracker-prod`.
