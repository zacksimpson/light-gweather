# Google Cloud Setup

This guide shows you how to create a Google Weather API key and set limits on it so you aren't billed. It takes about 15 minutes. You'll need a Google account and a payment card. If you're building this for your own phone, you'll stay inside Google's free monthly allowance.

---

## Step 1: Create a project

1. Go to the [Google Cloud console](https://console.cloud.google.com) and sign in.
2. Click the project picker at the top of the page, then click **New Project**.
3. Give it any name (I used `light-gweather`) and create it.

## Step 2: Turn on billing

1. In the console, open **Billing**.
2. Link your new project to a billing account. If you don't have one yet, you'll set one up here. This is where the card goes.
3. Check that billing shows as enabled for your project.

## Step 3: Turn on the Weather API

1. In the console, search for **Weather API**.
2. Click **Enable**. (If it says **Manage**, it's already on.)

## Step 4: Create your API key

1. Go to **Google Maps Platform > Keys & Credentials**.
2. Click **Create credentials**, then **API key**.
3. Open the **Select API restrictions** list, clear everything except **Weather API**, and click **Create**.

Google shows you the key. Copy it somewhere for now. You can always get it again later from the same page by clicking **Show key**.

## Step 5: Set your limits

This is the part that keeps you safe. Do both of these.

### Daily quota

1. Go to **Google Maps Platform > Quotas**.
2. Pick **Weather API**.
3. Find the "per day" limits for these three, click **Edit** on each, and set them to **100**. They have names like "Weather API - Current Conditions Usage per day":
   * Current Conditions
   * Forecast Days
   * Forecast Hours

100 a day is plenty for one phone, and it stays under Google's free monthly allowance. If you see "per day" limits for anything else (history, minutes, map tiles, alerts), set those to **0** if Google lets you. The app doesn't use them.

### Budget alert

1. Go to **Billing > Budgets & alerts** and click **Create budget**.
2. Choose **Alerts only** and give it a name.
3. Pick your project, set the amount to something small like **$1**, and leave the default alert settings.
4. Click **Finish**.

Heads up: this only sends an email. It won't stop anything, and the first one can take a few hours to show up. The daily quota above is what actually protects you.

---

## Step 6: Give the key to the app

1. In this project's folder, make a copy of the file `local.properties.example` and rename the copy to `local.properties`.
2. Open it and paste your key after the `=`, so the line looks like this:

```
googleWeatherApiKey=YOUR_KEY_HERE
```

3. Save it.

That file stays on your computer and is never uploaded anywhere.

---

## You're done!

Your key is ready and your limits are in place. Head back to the main [README](../README.md) to build and install the app.
