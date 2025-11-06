# 🧪 Testing Guide for PantryPal App

## How to Test the Application

### Option 1: Test in Android Studio (Recommended)

#### Prerequisites:
1. **Android Studio** installed (latest version)
2. **Physical Android Device** OR **Android Emulator**
   - Physical device: Enable USB debugging in Developer Options
   - Emulator: Create an AVD (Android Virtual Device) with API 33+

#### Step-by-Step Testing:

1. **Open Project in Android Studio**
   ```
   File → Open → Select your PantryPaloneVo folder
   ```

2. **Connect Device/Emulator**
   - Physical device: Connect via USB, accept debugging prompt
   - Emulator: Start from AVD Manager (Tools → Device Manager)

3. **Sync Gradle**
   - Click "Sync Now" if prompted
   - Wait for dependencies to download (TensorFlow Lite, CameraX, etc.)

4. **Build the Project**
   - Click the green "Run" button (▶️) or press `Shift + F10`
   - Or use: `Build → Make Project` then `Run → Run 'app'`

5. **Grant Permissions**
   - On first launch, the app will request camera permission
   - Click "Allow" when prompted

6. **Access PantryDetectorActivity**
   - **Method 1 (ADB Command):**
     ```bash
     adb shell am start -n com.unh.pantrypalonevo/.PantryDetectorActivity
     ```
   - **Method 2 (Add Button):** Add a button in HomePageActivity to launch it
   - **Method 3 (Direct Intent):** The activity is already registered in AndroidManifest

### Option 2: Test via ADB (Command Line)

1. **Build APK:**
   ```bash
   cd C:\Users\rajul\AndroidStudioProjects\PantryPaloneVo
   .\gradlew assembleDebug
   ```

2. **Install APK:**
   ```bash
   adb install app\build\outputs\apk\debug\app-debug.apk
   ```

3. **Launch PantryDetectorActivity:**
   ```bash
   adb shell am start -n com.unh.pantrypalonevo/.PantryDetectorActivity
   ```

### Option 3: Generate Signed APK for Distribution

1. **Build → Generate Signed Bundle / APK**
2. Follow the wizard to create a signed APK
3. Install on any Android device

---

## What to Test

### ✅ Core Functionality Tests:

1. **Camera Permission**
   - App should request camera permission on first launch
   - Should handle permission denial gracefully

2. **Camera Preview**
   - Camera feed should display immediately
   - Preview should be full-screen
   - Should be portrait orientation (locked)

3. **Real-Time Detection**
   - Point camera at pantry items (banana, apple, bottle, etc.)
   - Should see green bounding boxes around detected items
   - Labels should show item name + confidence percentage
   - FPS counter should display in top-right corner

4. **Performance**
   - FPS should be 30+ (ideally 60-90+ with GPU acceleration)
   - Detection should be smooth without lag
   - No crashes or ANR (Application Not Responding)

5. **UI Elements**
   - Back button in top-left should work
   - FPS counter updates in real-time
   - Overlay draws correctly over camera preview

6. **Edge Cases**
   - Test with no items in frame (should show empty overlay)
   - Test with multiple items (should detect all)
   - Test with low light conditions
   - Test with different camera angles

---

## Quick Test Checklist

- [ ] App builds successfully without errors
- [ ] Camera permission is requested and granted
- [ ] Camera preview displays correctly
- [ ] Detection boxes appear around items
- [ ] Labels show correct item names
- [ ] FPS counter updates (should be >30 FPS)
- [ ] Back button returns to previous screen
- [ ] No crashes or errors in Logcat
- [ ] Works on both physical device and emulator

---

## Troubleshooting

### Issue: App won't build
- **Solution:** Check `build.gradle.kts` for missing dependencies
- Run `File → Invalidate Caches / Restart` in Android Studio

### Issue: Camera permission denied
- **Solution:** Go to Settings → Apps → PantryPal → Permissions → Enable Camera

### Issue: No detections appearing
- **Solution:** 
  - Check that `pantry_ultra_fast_int8.tflite` exists in `app/src/main/assets/`
  - Check that `pantry_classes.txt` exists with 25 class labels
  - Check Logcat for error messages

### Issue: Low FPS (<30)
- **Solution:**
  - Ensure GPU delegate is being used (check Logcat for "GPU delegate" messages)
  - Test on a physical device (emulators are slower)
  - Reduce detection frequency if needed

### Issue: App crashes on launch
- **Solution:**
  - Check Logcat for stack traces
  - Verify all dependencies are downloaded
  - Ensure model file is in assets folder

---

## Testing with Different Items

The model detects these 25 items:
1. banana, apple, sandwich, orange, broccoli, carrot
2. hot dog, pizza, donut, cake
3. bottle, wine glass, cup
4. fork, knife, spoon, bowl
5. backpack, book, cell phone
6. refrigerator, microwave, oven, toaster, sink

**Test with real items** from your pantry for best results!

---

## Viewing Logs

In Android Studio:
1. Open **Logcat** tab (bottom panel)
2. Filter by: `PantryDetector` or `PantryDetectorActivity`
3. Look for:
   - `"Camera started successfully"`
   - `"FPS: XX"` messages
   - Any error messages

---

## Performance Benchmarks

Expected performance:
- **High-end device (GPU):** 60-90+ FPS
- **Mid-range device (GPU):** 30-60 FPS
- **Low-end device (CPU):** 15-30 FPS
- **Emulator:** 10-20 FPS (not ideal for testing)

---

## Next Steps After Testing

Once testing is complete:
1. Add a button in HomePageActivity to launch PantryDetectorActivity
2. Integrate detection results with your database/Firestore
3. Add functionality to save detected items
4. Implement batch detection for multiple items
5. Add export/sharing features

---

**Happy Testing! 🚀**


