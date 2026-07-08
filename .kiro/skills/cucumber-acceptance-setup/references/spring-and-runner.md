# Spring context, Suite runner, and config classes

These classes are identical across projects — only the `package` line and the scanned package change.
Substitute `{{BASE_PACKAGE}}` throughout.

## AcceptanceSpringConfig — owns Playwright and the component scan

Place this **just outside** the scanned package (e.g. `{{BASE_PACKAGE}}.acceptanceinfra.config`) so
the component scan of `{{BASE_PACKAGE}}.acceptance` does not re-enter the config itself.

```java
package {{BASE_PACKAGE}}.acceptanceinfra.config;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import io.cucumber.spring.ScenarioScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan("{{BASE_PACKAGE}}.acceptance")
public class AcceptanceSpringConfig {

  @Value("#{new Boolean('${playwright.headless}')}")
  private Boolean headless;

  @Bean(destroyMethod = "close")
  public Playwright playwright() {
    return Playwright.create();
  }

  @Bean(destroyMethod = "close")
  public Browser browser(Playwright playwright) {
    BrowserType.LaunchOptions options = new BrowserType.LaunchOptions()
      .setTimeout(60_000)
      .setHeadless(headless);
    if (!headless) {
      options.setSlowMo(500); // slow down so a human can watch a headed run
    }
    return playwright.chromium().launch(options);
  }

  @Bean(destroyMethod = "close")
  @ScenarioScope
  public BrowserContext browserContext(Browser browser) {
    // setIgnoreHTTPSErrors lets the browser accept a self-signed cert if the app is served
    // over HTTPS in an ephemeral environment. Harmless against plain-HTTP localhost.
    return browser.newContext(new Browser.NewContextOptions().setIgnoreHTTPSErrors(true));
  }

  @Bean(destroyMethod = "close")
  @ScenarioScope
  public Page page(BrowserContext context) {
    return context.newPage();
  }
}
```

Key points:

- `Playwright` and `Browser` are singletons (created once, reused) — expensive to start.
- `BrowserContext` and `Page` are `@ScenarioScope` — a fresh, isolated browser context per scenario,
  so cookies/storage never bleed between scenarios.
- `headless` comes from the `playwright.headless` property, itself driven by an env var
  (`PLAYWRIGHT_HEADLESS`) so CI runs headless and local debugging can run headed.

## RunAcceptanceTests — the JUnit Platform Suite

```java
package {{BASE_PACKAGE}}.acceptance.run;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "{{BASE_PACKAGE}}.acceptance")
@ConfigurationParameter(
  key = PLUGIN_PROPERTY_NAME,
  value = "pretty, summary, html:target/cucumber-report.html, json:target/cucumber-report.json"
)
public class RunAcceptanceTests {}
```

- `@SelectClasspathResource("features")` points at `src/test/resources/features`.
- `GLUE` must be the package that contains step defs, hooks, and the Cucumber context.

### Reports — this suite is the source of truth

The `PLUGIN_PROPERTY_NAME` value is what actually drives report output when tests run through the
`@Suite` under the JUnit Platform engine. After a run you get:

- **`target/cucumber-report.html`** — a self-contained, human-readable HTML report (open it in a
  browser). This is the report you share and inspect.
- **`target/cucumber-report.json`** — machine-readable output for any downstream tooling.

> **Keep the plugin list here in sync with `cucumber.properties`, and treat *this* param as
> authoritative.** Under the JUnit Platform engine, the suite's `@ConfigurationParameter` takes
> precedence — if you list only `html:` here but `html,json` in `cucumber.properties`, the JSON report
> silently won't be produced. List every plugin you want (`pretty, summary, html:…, json:…`) on this
> annotation so the HTML report is generated regardless of how the suite is launched. The
> `cucumber.properties` file (below) is a convenience for the plain-Maven/non-suite path and should
> carry the same plugin list.

## AcceptanceCucumberContext — binds Cucumber to the Spring context

```java
package {{BASE_PACKAGE}}.acceptance.config;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.test.context.ContextConfiguration;
import {{BASE_PACKAGE}}.acceptanceinfra.config.AcceptanceSpringConfig;

@CucumberContextConfiguration
@ContextConfiguration(classes = AcceptanceSpringConfig.class)
public class AcceptanceCucumberContext {}
```

Exactly one class in the glue may carry `@CucumberContextConfiguration`.

## PropertiesConfig — loads `application.properties`

```java
package {{BASE_PACKAGE}}.acceptance.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

@Configuration
@PropertySource("classpath:application.properties")
public class PropertiesConfig {

  @Bean
  public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
    return new PropertySourcesPlaceholderConfigurer();
  }
}
```

`application.properties` maps env vars to the properties the config beans read — see
`assets/application.properties`:

```properties
app_base_url=${APP_BASE_URL}
db_url=${ACCEPTANCE_DB_URL}
db_username=${ACCEPTANCE_DB_USERNAME}
db_password=${ACCEPTANCE_DB_PASSWORD}
playwright.headless=${PLAYWRIGHT_HEADLESS:true}
```

## EnvironmentConfig and DatabaseConfig — typed access to those properties

```java
package {{BASE_PACKAGE}}.acceptance.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class EnvironmentConfig {

  private final String appBaseUrl;

  public EnvironmentConfig(@Value("${app_base_url}") String appBaseUrl) {
    this.appBaseUrl = appBaseUrl;
  }

  public String appBaseUrl() {
    return appBaseUrl;
  }
}
```

```java
package {{BASE_PACKAGE}}.acceptance.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DatabaseConfig {

  private final String url;
  private final String username;
  private final String password;

  public DatabaseConfig(
    @Value("${db_url}") String url,
    @Value("${db_username}") String username,
    @Value("${db_password}") String password
  ) {
    this.url = url;
    this.username = username;
    this.password = password;
  }

  public String url() { return url; }
  public String username() { return username; }
  public String password() { return password; }
}
```

Add fields (test-support URL, email inbox URL) to `EnvironmentConfig` only when you introduce an API
or email driver that needs them — keep it lean at setup time.
