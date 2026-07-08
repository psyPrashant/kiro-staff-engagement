# Email testing

## When to write an email scenario

Assert on email only when the acceptance criterion explicitly says a notification email is sent.
Don't bolt email assertions onto a scenario testing something else unless email delivery is part of
that same AC.

## The `@requires-email` tag

Tag any scenario that asserts on email delivery with `@requires-email`. This is a **provider-neutral**
tag on purpose — it triggers lifecycle hooks that:

1. `@Before` — check the email provider is reachable (fail fast if not), then clear the inbox.
2. `@After` — clear the inbox again.

This is separate from the SQL cleanup — the inbox is not a database table.

```gherkin
@epic-WIDG @story-WIDG-4 @slice-WIDG-4.1 @requires-email
Scenario Outline: [WIDG-4.1] The owning team is emailed when a consumer requests access
  ...
```

Name the tag for the *capability* (`@requires-email`), never for a specific tool. If you swap the
email backend (a local SMTP-capture tool, a cloud email service, etc.), the tag and every scenario
survive unchanged — only the driver behind it changes.

## Four-layer pattern, provider-blind above the driver

| Layer | Responsibility | Example |
| --- | --- | --- |
| Feature file | Domain-language Gherkin — never names the email tool | `features/widget/` |
| Step definitions | Thin delegation to the assertions class | `NotificationSteps` |
| Domain (assertions) | Poll for the email, assert content, store it in `TestWorld` | `NotificationAssertions` |
| Driver | HTTP calls to the email provider's API (provider-specific) | `EmailInboxDriver` |

Only the driver knows which email tool is in use. Gherkin, step defs, and the assertions class are
provider-blind.

## Always poll — email is async

Never assert synchronously on email. Poll with Awaitility:

```java
await()
  .atMost(10, SECONDS)
  .untilAsserted(() -> {
    List<EmailMessage> messages = emailInboxDriver.findByRecipient(recipient);
    assertThat(messages)
      .as("notification email to %s should arrive", recipient)
      .isNotEmpty();
    testWorld.setLastEmail(messages.getLast());
  });
```

Store the matched message in `TestWorld` inside the Awaitility block so downstream `And` steps can
assert on it without re-polling.

## What to assert

| What | How |
| --- | --- |
| Recipient | `findByRecipient(email)` — assert the list is non-empty |
| Body/subject keywords | `containsIgnoringCase(keyword)` — never exact text |
| Action link | assert a path fragment (`contains("access-requests")`), not a full URL |
| Subject | only when the exact subject wording is part of the AC |

Never hardcode full URLs or exact body text — copy changes would break the test for no behavioural
reason.

## Gherkin example

```gherkin
@epic-WIDG @story-WIDG-4 @slice-WIDG-4.1 @requires-email
Scenario Outline: [WIDG-4.1] The owning team is emailed when a consumer requests access
  Given a consumer signed in as "<email>" with password "<password>"
  And the active team is "<consumer_team>"
  When the consumer requests access to "<widget_name>"
  Then a notification email is sent to "<owner_email>"
  And the notification email mentions the requesting team "<consumer_team>"
  And the notification email contains a link to review the request

  Examples:
    | email            | password     | consumer_team | widget_name | owner_email        |
    | user@example.com | password123! | ALPHA         | My Widget   | owner@example.com  |
```
