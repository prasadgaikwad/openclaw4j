# Slack Reference

The Slack integration provides OpenClaw4J with its primary communication channel, allowing it to interact with users directly in their workspace.

## Features

-   **Message Dispatch**: Handled primarily via the `SlackChannelAdapter`.
-   **Formatted Output**: Automatically formats markdown to its Slack equivalent.
-   **Independent Messaging**: Allows the agent to post messages or fetch context independently of its main response loop.

## Available Tools

#### `postSlackMessage(String channelId, String text)`
Posts a message to a Slack channel. Most useful for broad communication, status updates, or when you need to send information to a different channel from where the conversation originated.

#### `getChannelHistory(String channelId, Integer limit)`
Retrieves recent messages from a channel to gain context on a conversation or thread. This is useful for recalling details of a past discussion or understanding the state of a thread.

---

## Slack Formatting

Slack message formatting is distinct from standard Markdown. OpenClaw4J includes a `SlackFormatter` that automatically converts:

-   Bold (`**text**` $\rightarrow$ `*text*`)
-   Italic (`*text*` $\rightarrow$ `_text_`)
-   Links (`[label](url)` $\rightarrow$ `<url|label>`)
-   Lists and code blocks.
