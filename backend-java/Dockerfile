FROM node:20-alpine@sha256:fb4cd12c85ee03686f6af5362a0b0d56d50c58a04632e6c0fb8363f609372293 AS frontend-build
WORKDIR /workspace/dms
COPY dms/package.json dms/pnpm-lock.yaml ./
RUN corepack enable && pnpm install --frozen-lockfile
COPY dms/ ./
RUN pnpm build:web

FROM eclipse-temurin:17-jdk-jammy@sha256:723151f3fc88ca2060153ee08ab8dbbea7983d6ed6f2622fe440acf178737c94 AS backend-build
ENV LANG=C.UTF-8
ENV MAVEN_OPTS="-Djava.net.preferIPv4Stack=true -Dmaven.wagon.http.retryHandler.count=3"
WORKDIR /workspace/backend-java
COPY backend-java/mvnw backend-java/pom.xml ./
COPY backend-java/.mvn .mvn
RUN --mount=type=cache,id=dms-maven-repository,target=/root/.m2 \
    ./mvnw --batch-mode --no-transfer-progress dependency:go-offline
COPY backend-java/ ./
RUN --mount=type=cache,id=dms-maven-repository,target=/root/.m2 \
    ./mvnw --batch-mode --no-transfer-progress package -DskipTests

FROM eclipse-temurin:17-jre-jammy@sha256:475d8e96b4b2bfe08999e5e854755c773af1581acdf959a4545d88f0696a2339
ENV LANG=C.UTF-8
RUN groupadd --system --gid 10001 dms \
    && useradd --system --uid 10001 --gid dms --home-dir /app --shell /usr/sbin/nologin dms
WORKDIR /app
COPY --chown=dms:dms --from=backend-build /workspace/backend-java/target/dms-backend-1.0.0.jar /app/app.jar
COPY --chown=dms:dms --from=frontend-build /workspace/dms/dist /app/dms/dist
ENV FRONTEND_DIST=/app/dms/dist
EXPOSE 8081
USER 10001:10001
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD bash -c 'exec 3<>/dev/tcp/127.0.0.1/8081 && printf "GET /actuator/health HTTP/1.0\r\nHost: localhost\r\n\r\n" >&3 && head -n 1 <&3 | grep -q " 200 "'
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
