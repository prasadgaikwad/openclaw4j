# Date and Time Reference

OpenClaw4J includes a temporal awareness tool that allows it to maintain the correct current context for scheduling and answering user questions about time.

## Features

-   **System Access**: Retrieves the system's current datetime.
-   **Contextualization**: Adjusted for the user's locale and timezone where possible.

## Available Tools

#### `getCurrentDateTime()`
Returns the current date and time in a human-readable format, including the timezone. Use this whenever the user asks "What time is it?" or when you need to calculate relative times (e.g. "in 5 minutes").

---

## Technical Details

The `DateTimeTools` class interacts with `LocalDateTime` and uses the `LocaleContextHolder` to provide the current system context for each tool call.
