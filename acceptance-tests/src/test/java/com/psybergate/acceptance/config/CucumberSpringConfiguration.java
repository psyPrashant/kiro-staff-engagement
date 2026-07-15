package com.psybergate.acceptance.config;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.test.context.ContextConfiguration;

/**
 * Cucumber's entry point into the Spring context.
 *
 * <p>This class is intentionally a plain glue class — it carries {@link CucumberContextConfiguration}
 * so Cucumber can bootstrap Spring, but it is NOT itself a {@code @Component}/{@code @Configuration}.
 * The actual bean definitions live in {@link AcceptanceTestConfig}, referenced via
 * {@link ContextConfiguration}. Keeping the two roles separate avoids the cucumber-spring 7.x error
 * "Glue class ... was (meta-)annotated with @Component", which is thrown when a glue class is also
 * a Spring-managed component (it would be registered twice — once by Cucumber, once by Spring).
 */
@CucumberContextConfiguration
@ContextConfiguration(classes = AcceptanceTestConfig.class)
public class CucumberSpringConfiguration {
}
