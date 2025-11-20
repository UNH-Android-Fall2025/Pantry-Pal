# Complete Step-by-Step Guide: Google Places API for Nearby Pantry Shops

## 📋 Overview
This guide will help you integrate Google Places API to fetch real-world pantry shops near the user's location.

---

## STEP 1: Google Cloud Console Setup

### 1.1 Go to Google Cloud Console
1. Open your browser and go to: https://console.cloud.google.com/
2. Sign in with your Google account
3. Select the **same project** that your Firebase app uses (check `google-services.json` for project ID)

### 1.2 Enable Required APIs
1. In the left sidebar, click **"APIs & Services"** → **"Library"**
2. Search for **"Places API"** and click on it
3. Click **"ENABLE"** button
4. Search for **"Places API (New)"** (if available) and enable it too
5. Search for **"Geocoding API"** and enable it (for address conversion)

### 1.3 Create API Key
1. Go to **"APIs & Services"** → **"Credentials"**
2. Click **"+ CREATE CREDENTIALS"** → **"API key"**
3. A new API key will be created
4. **IMPORTANT:** Click on the newly created API key to edit it
5. Under **"Application restrictions"**:
   - Select **"Android apps"**
   - Click **"+ ADD AN ITEM"**
   - Enter your package name: `com.unh.pantrypalonevo`
   - Click **"Add an item"** under SHA-1 certificate fingerprints
   - To get SHA-1:
     - Open terminal/command prompt
     - Run: `cd android` (if using React Native) or navigate to your project
     - Run: `./gradlew signingReport` (Mac/Linux) or `gradlew.bat signingReport` (Windows)
     - Look for SHA1 in the output under `Variant: debug`
     - Copy the SHA-1 value (looks like: `AA:BB:CC:DD:...`)
     - Paste it in the Google Cloud Console
6. Under **"API restrictions"**:
   - Select **"Restrict key"**
   - Check only:
     - ✅ Places API
     - ✅ Places API (New) - if enabled
     - ✅ Geocoding API
7. Click **"SAVE"**

### 1.4 Enable Billing (Required)
⚠️ **IMPORTANT:** Google Places API requires billing to be enabled
1. Go to **"Billing"** in the left sidebar
2. If billing is not enabled, click **"LINK A BILLING ACCOUNT"**
3. Follow the prompts to add a payment method
4. **Note:** Google provides $200 free credit per month, which covers most usage

---

## STEP 2: Add Dependencies to Project

### 2.1 Update build.gradle.kts
Open `app/build.gradle.kts` and add the Places SDK dependency:

```kotlin
dependencies {
    // ... existing dependencies ...
    
    // Google Places SDK
    implementation("com.google.android.libraries.places:places:3.5.0")
}
```

**Action Required:** I'll add this for you in the next step.

---

## STEP 3: Add API Key to Project

### 3.1 Add to strings.xml
Open `app/src/main/res/values/strings.xml` and add:

```xml
<string name="google_places_api_key">YOUR_API_KEY_HERE</string>
```

**Action Required:** Replace `YOUR_API_KEY_HERE` with the API key from Step 1.3

### 3.2 Add to AndroidManifest.xml
Open `app/src/main/AndroidManifest.xml` and add inside `<application>` tag:

```xml
<meta-data
    android:name="com.google.android.geo.API_KEY"
    android:value="@string/google_places_api_key" />
```

**Action Required:** I'll add this for you.

---

## STEP 4: Create Places Service Class

### 4.1 Create PlacesService.kt
This class will handle all Google Places API calls.

**Action Required:** I'll create this file for you.

---

## STEP 5: Update HomePageActivity

### 5.1 Integrate Places API
Update `HomePageActivity` to:
- Fetch nearby pantries from Google Places API
- Merge with Firestore pantries
- Display all results sorted by distance

**Action Required:** I'll update this for you.

---

## STEP 6: Testing

### 6.1 Test on Real Device
1. Build and install the app on a real Android device (emulator may not have accurate location)
2. Grant location permission when prompted
3. Open the Home screen
4. You should see:
   - Pantries from Firestore (if any)
   - Pantries from Google Places API (nearby food banks, pantries, etc.)

### 6.2 Verify API Key
- If you see "API key not valid" error:
  - Check that API key is correct in `strings.xml`
  - Verify API key restrictions in Google Cloud Console
  - Ensure billing is enabled

### 6.3 Check Logs
- Open Logcat in Android Studio
- Filter by "PlacesService" or "HomePageActivity"
- Look for any error messages

---

## STEP 7: Troubleshooting

### Common Issues:

1. **"API key not valid"**
   - Solution: Check API key in strings.xml matches Google Cloud Console
   - Verify API restrictions allow your app's package name and SHA-1

2. **"Billing not enabled"**
   - Solution: Enable billing in Google Cloud Console

3. **"No results found"**
   - Solution: Try different search keywords or increase search radius
   - Check that location permission is granted

4. **"Places API not enabled"**
   - Solution: Go back to Step 1.2 and enable Places API

---

## 📝 Notes

- **Cost:** Google Places API charges per request. Check pricing: https://developers.google.com/maps/billing-and-pricing/pricing
- **Free Tier:** $200 credit per month (covers ~40,000 requests)
- **Rate Limits:** Be mindful of API rate limits in production

---

## ✅ Checklist

Before testing, ensure:
- [ ] Places API enabled in Google Cloud Console
- [ ] API key created and restricted
- [ ] Billing enabled
- [ ] API key added to strings.xml
- [ ] Dependencies added to build.gradle.kts
- [ ] AndroidManifest.xml updated
- [ ] Code changes implemented (I'll do this)
- [ ] App built and installed on device
- [ ] Location permission granted

---

**Ready? Let me implement the code changes now!**

