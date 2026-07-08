# The Maven module (`pom.xml`)

Copy `assets/pom.xml.template`, substitute the placeholders, and drop it at
`{{MODULE_PATH}}/pom.xml`. This is a **test-only** module — every dependency is `test` scope because
the acceptance tests live under `src/test`.

## Dependency rationale

| Dependency | Why |
| --- | --- |
| `io.cucumber:cucumber-java` | Gherkin step definitions in Java |
| `io.cucumber:cucumber-spring` | Lets Cucumber resolve step/actor/page beans from the Spring context and gives you the `cucumber-glue` scenario scope |
| `io.cucumber:cucumber-junit-platform-engine` | Runs Cucumber under the JUnit Platform |
| `org.junit.platform:junit-platform-suite` | The `@Suite` annotation that selects the `features` classpath resource |
| `com.microsoft.playwright:playwright` | The browser automation the drivers use |
| `org.awaitility:awaitility` | Polling for async outcomes (e.g. email delivery) without `Thread.sleep` |
| `org.assertj:assertj-core` | Fluent assertions in domain/assertions classes |
| `com.fasterxml.jackson.core:jackson-databind` | Deserialising API/test-support JSON responses |
| `org.springframework:spring-context` / `spring-test` / `spring-jdbc` | The context, test support, and `ResourceDatabasePopulator` used by the SQL runner |
| `{{JDBC_DEP}}` (e.g. `org.postgresql:postgresql`) | JDBC driver for the test database |
| `org.apache.commons:commons-lang3` | Small string/util helpers (optional but handy) |

## Version pinning

Pin every version explicitly (as the template does) rather than inheriting from a parent BOM — this
module is meant to be portable and self-contained, so it should not depend on the host project's
dependency management. When you copy it in, bump the versions to the latest compatible set for the
project's JDK. The template's versions are a known-good baseline for Java 21.

## Surefire

The `maven-surefire-plugin` runs the `@Suite` class as a normal test. Set `useModulePath=false` so
the classpath (not the Java module path) is used — Cucumber's classpath scanning for glue and
features expects this. Without it, glue discovery silently finds nothing.

## Java version

Set `maven.compiler.source`/`target` to the project's JDK (17+). Playwright and modern Cucumber need
a current JDK; the template assumes 21.
