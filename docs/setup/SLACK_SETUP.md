# Slack App Setup Guide

This guide walks you through creating and configuring the Slack App required for OpenClaw4J.

## Prerequisites
- A Slack Workspace where you have permission to install apps.
- [ngrok](https://ngrok.com/) installed (for local development/testing).

---

## 1. Create the App

1. Go to [api.slack.com/apps](https://api.slack.com/apps).
2. Click **Create New App**.
3. Select **From scratch**.
4. Enter App Name: **OpenClaw4J** (or your preferred name).
5. Select your Development Slack Workspace.
6. Click **Create App**.

---

## 2. Configure Permissions (Scopes)

1. In the left sidebar, click **OAuth & Permissions**.
2. Scroll down to **Scopes** > **Bot Token Scopes**.
3. Click **Add an OAuth Scope** and add the following:
   - `chat:write` (Allows the bot to send messages)
   - `channels:history` (Allows the bot to read messages in public channels)
   - `groups:history` (Allows the bot to read messages in private channels - optional but recommended)
   - `im:history` (Allows the bot to read direct messages)
   - `app_mentions:read` (Allows the bot to see when it is mentioned)
   - `users:read` (Optional: useful for getting user names later)

---

## 3. Install App to Workspace

1. Scroll up to the top of the **OAuth & Permissions** page.
2. Click **Install to Workspace**.
3. Click **Allow** to authorize the bot in your workspace.
4. **Copy the "Bot User OAuth Token"**.
   - It starts with `xoxb-...`.
   - Paste this into your `.env` file as `SLACK_BOT_TOKEN`.

---

## 4. Get Signing Secret

1. In the left sidebar, click **Basic Information**.
2. Scroll down to **App Credentials**.
3. Find **Signing Secret** and click **Show**.
4. **Copy the Signing Secret**.
   - Paste this into your `.env` file as `SLACK_SIGNING_SECRET`.

---

## 5. Start OpenClaw4J Locally with ngrok

Before configuring event subscriptions, your app needs to be reachable by Slack.

1. Start the Spring Boot application:
   ```bash
   ./gradlew bootRun
   ```
2. In a separate terminal, start ngrok to expose port 8080:
   ```bash
   ngrok http 8080
   ```
3. Copy the `https` URL from ngrok (e.g., `https://abc1-23-45-67-89.ngrok-free.app`).

---

## 6. Configure Event Subscriptions

1. In the Slack API dashboard sidebar, click **Event Subscriptions**.
2. Toggle **Enable Events** to **On**.
3. In the **Request URL** field, enter your ngrok URL appended with `/slack/events`:
   - Example: `https://abc1-23-45-67-89.ngrok-free.app/slack/events`
   - Slack will immediately send a verification request. If your app is running and ngrok is correct, it should verify successfully (Green "Verified" checkmark).
4. Scroll down to **Subscribe to bot events**.
5. Click **Add Bot User Event** and add:
   - `message.channels` (Listens for messages in channels)
   - `message.groups` (Listens for messages in private channels)
   - `message.im` (Listens for DMs)
   - `app_mention` (Listens for @mentions)
6. Click **Save Changes** at the bottom (Important!).

> **Note:** Whenever you restart ngrok, your URL changes. You must update this Request URL in the Slack dashboard each time, or use a paid ngrok plan with a static domain.

---

## 7. Test the Bot

1. Go to your Slack Workspace.
2. Go to a channel (e.g., `#general`) or create a new testing channel.
3. **Invite the bot** to the channel:
   - Type `/invite @OpenClaw4J` (or whatever you named it).
4. Send a message: "Hello".
5. The bot should (for Slice 1) echo your message back:
   > 🦞 **OpenClaw4J received your message:**
   > Hello
   > _I'm currently in echo mode (Slice 1)..._

---

## Troubleshooting

- **"dispatch_failed" / "challenge_failed"**: Ensure your app is running and ngrok is pointing to the correct port (8080). Check `application.yml` logs for incoming requests.
- **Bot doesn't respond**: Did you invite the bot to the channel? Bots mostly can't see messages in channels they aren't in.
- **401 Unauthorized**: Check your `SLACK_BOT_TOKEN` in `.env`.
- **400 Bad Request (Invalid signature)**: Check your `SLACK_SIGNING_SECRET` in `.env`.
