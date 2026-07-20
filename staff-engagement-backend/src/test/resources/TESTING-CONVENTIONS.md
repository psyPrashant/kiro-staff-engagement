# Testing Conventions

This document defines the testing conventions for the `staff-engagement-backend` module.

## Test Class Naming

The suffix declares what kind of test it is, and the kind determines which folder it lives in:

| Suffix | Kind | Spring context? | Database? |
|--------|------|-----------------|-----------|
| `<ClassUnderTest>Test` | Unit test | No | No |
| `<ClassUnderTest>IntegrationTest` | Integration test | Yes | Yes (Testcontainers) |
| `<Behaviour>PropertyTest` | jqwik property test | Usually no | No |

Examples:
- `TaskServiceTest` tests `TaskServiceImpl`
- `TaskIntegrationTest` exercises the task endpoints against a real database
- `TaskEditDeletePropertyTest` asserts invariants over generated inputs

## Package Structure

Unit tests mirror the package of the class under test. Integration and property
tests get their own sub-package per module, because they cut across layers and
would otherwise clutter the layer folders:

```
src/test/java/com/psybergate/staff_engagement/
├── support/                    # BaseIntegrationTest, TestcontainersConfiguration
├── integration/                # cross-cutting: Flyway, health, domain model, REST wiring
├── StaffEngagementApplicationTests.java
└── <module>/
    ├── web/                    # controller unit tests        (mirrors main/<module>/web)
    ├── service/                # service unit tests           (mirrors main/<module>/service)
    ├── domain/                 # entity / rule unit tests     (mirrors main/<module>/domain)
    ├── dto/                    # DTO validation + mapping     (mirrors main/<module>/dto)
    ├── integration/            # @SpringBootTest, container-backed
    └── property/               # jqwik property tests
```

So `task/service/TaskServiceTest.java` tests `task/service/TaskServiceImpl.java`, while
`task/integration/TaskIntegrationTest.java` and `task/property/TaskPropertyTest.java`
sit alongside it without crowding the layer folders.

## Test Method Naming

Use descriptive camelCase names prefixed with `should`:

```
should<ExpectedBehavior>When<Condition>()
```

Examples:
- `shouldPersistEntityWhenValidDataProvided()`
- `shouldThrowExceptionWhenInputIsNull()`

## Testing Against Interfaces

Every service is an interface (`TaskService`) with a single implementation
(`TaskServiceImpl`). Follow the same rule tests as production code:

- **Collaborator mocks use the interface.** In controller slice tests,
  `@MockitoBean private TaskService taskService;` — never the `Impl`.
- **The service's own unit test instantiates the implementation**, because that
  is the unit under test. Use `@InjectMocks private TaskServiceImpl taskService;`,
  or construct it directly and hold it in an interface-typed variable:

  ```java
  private TaskService service() {
      return new TaskServiceImpl(taskRepository, interactionRepository);
  }
  ```

No other test should mention an `Impl` type.

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

### `@InjectMocks`

Annotates the field for the class under test — the concrete implementation, since
Mockito must instantiate it.

### `@MockitoBean`

Replaces a bean in a Spring slice test. Declare it with the **interface** type.

## General Guidelines

- Prefer constructor injection in production code so `@InjectMocks` works cleanly.
- Use AssertJ (`assertThat`) for assertions — it provides readable fluent assertions.
- Use Mockito `verify()` to confirm interactions with collaborators.
- Keep unit tests free of Spring context — no `@SpringBootTest` on unit tests.
- Integration tests that need Spring + database should extend `BaseIntegrationTest`
  from the `support` package.
