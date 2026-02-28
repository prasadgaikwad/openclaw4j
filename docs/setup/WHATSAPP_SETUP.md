# WhatsApp Business Cloud API Setup Guide

This guide walks you through creating and configuring the WhatsApp Business App required for OpenClaw4J.

## Prerequisites
- A [Meta Developer Account](https://developers.facebook.com/).
- [ngrok](https://ngrok.com/) installed (for local development/testing).
- A valid phone number to use for testing (Meta provides a test number by default).

---

## 1. Create a Meta App

1. Go to the [Meta App Dashboard](https://developers.facebook.com/apps).
2. Click **Create App**.
3. Select **Other** as the use case.
4. Select **Business** as the app type.
5. Enter an App Name (e.g., **OpenClaw4J**) and contact email.
6. Click **Create App**.

---

## 2. Add WhatsApp to Your App

1. In the "Add products to your app" section, find **WhatsApp** and click **Set up**.
2. You will be prompted to select or create a Meta Business Account. Follow the prompts.
3. Once set up, you'll be taken to the **WhatsApp > API Setup** page.

---

## 3. Get API Credentials

On the **API Setup** page, you will find:

1. **Temporary Access Token**: Copy this.
   - *Note: This expires in 24 hours. For production, you'll need a Permanent System User Token.*
2. **Phone Number ID**: Copy the ID associated with the test number (or your own number).
3. **Pasted these into your `.env` file**:
   ```bash
   WHATSAPP_ACCESS_TOKEN=your-access-token
   WHATSAPP_PHONE_NUMBER_ID=your-phone-number-id
   WHATSAPP_VERIFY_TOKEN=any-random-string-you-choose
   ```

---

## 4. Start OpenClaw4J Locally with ngrok

Before configuring the webhook, your app needs to be reachable by Meta.

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

## 5. Configure Webhook

1. In the Meta App Dashboard sidebar, go to **WhatsApp > Configuration**.
2. Click **Edit** next to "Callback URL".
3. In the **Callback URL** field, enter your ngrok URL appended with `/whatsapp/webhook`:
   - Example: `https://abc1-23-45-67-89.ngrok-free.app/whatsapp/webhook`
4. In the **Verify Token** field, enter the same value you put in your `.env` (e.g., `my-secret-token`).
5. Click **Verify and Save**.
6. Under **Webhook fields**, click **Manage**.
7. Find the **messages** row and click **Subscribe**.

---

## 6. Test the Bot

1. Go back to the **API Setup** page.
2. Under "Step 2: Send messages with the API", find the "To" field.
3. Add your own WhatsApp number as a recipient and follow the verification steps.
4. Send a test message from your WhatsApp number to the test number provided by Meta.
5. The bot should process your message and reply via the Cloud API.

---

## Troubleshooting

- **Verification Failed**: Ensure your app is running and ngrok is pointing to port 8080. Check terminal logs for the `/whatsapp/webhook` GET request.
- **Message Not Received**: Ensure you have subscribed to the `messages` webhook field in the Configuration tab.
- **401 Unauthorized**: Your Access Token might have expired (they last 24h).
- **Format Issues**: WhatsApp expects phone numbers without the `+` prefix (e.g., `1234567890`).
