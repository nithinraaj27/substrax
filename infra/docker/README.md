# Docker (local dev)

The service Dockerfiles copy prebuilt Spring Boot JARs from each service's `target/` directory. If you run `docker compose build` before packaging the apps, you'll see errors like:

- `COPY target/<service>-0.0.1-SNAPSHOT.jar ...: not found`

## Build JARs first

From the repo root:

```bash
./infra/docker/build-jars.sh
```

Then build/run Docker:

```bash
docker compose -f infra/docker/docker-compose.yml up --build
```

