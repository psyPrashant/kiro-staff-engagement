# Testing Conventions

This document defines the testing conventions for the `staff-engagement-backend` module.

## Test Class Naming

- Unit test classes: `<ClassUnderTest>Test.java`
  - Example: `GreetingServiceTest.java` tests `GreetingService.java`
- Integration test classes: `<ClassUnderTest>IntegrationTest.java`
  - Example: `GreetingRepositoryIntegrationTest.java` tests `GreetingRepository.java`

## Test Method Naming

Use descriptive camelCase names prefixed with `should`:

```
should<ExpectedBehavior>When<Condition>()
```

Examples:
- `shouldReturnMorningGreetingBeforeNoon()`
- `shouldPersistEntityWhenValidDataProvided()`
- `shouldThrowExceptionWhenInputIsNull()`

## Package Structure

Test packages mirror the source tree:

```
src/main/java/com/psybergate/staff_engagement/greeting/GreetingService.java
src/test/java/com/psybergate/staff_engagement/greeting/GreetingServiceTest.java
```

Each production package has a corresponding test package. This keeps tests co-located
with the code they exercise and makes navigation straightforward.

## Annotation Usage Guidelines

### `@ExtendWith(MockitoExtension.class)`

Use on **unit test** classes that need Mockito mocks. This avoids loading a Spring
application context, keeping tests fast and isolated.

```java
@ExtendWith(MockitoExtension.class)
class MyServiceTest { ... }
```

### `@Mock`

Annotates fields that Mockito should create as mock instances. Place one `@Mock` per
dependency of the class under test.

```java
@Mock
private MyRepository myRepository;
```

### `@InjectMocks`

Annotates the field for the class under test. Mockito injects all `@Mock` fields
into this instance via constructor injection.

```java
@InjectMocks
private MyService myService;
```

### `@Test`

Marks a method as a test case. Every test method must be annotated with
`org.junit.jupiter.api.Test`.

```java
@Test
void shouldDoSomethingMeaningful() { ... }
```

## General Guidelines

- Prefer constructor injection in production code so `@InjectMocks` works cleanly.
- Use AssertJ (`assertThat`) for assertions — it provides readable fluent assertions.
- Use Mockito `verify()` to confirm interactions with collaborators.
- Keep unit tests free of Spring context — no `@SpringBootTest` on unit tests.
- Integration tests that need Spring + database should extend `BaseIntegrationTest`.
