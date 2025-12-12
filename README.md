# Pantry Pal

Pantry Pal is an Android mobile application designed to connect communities with local food pantries and help reduce food waste. The app allows users to discover nearby pantries, donate food items, browse available products, and get personalized recipe recommendations based on available pantry stock.

## Overview

This project was developed as part of a mobile application development course at the University of New Haven. The app serves as a platform where community members can find and contribute to local food pantries, making it easier to share resources and support those in need.

## Key Features

### 🏠 Home & Discovery
- **Location-Based Search**: Find nearby food pantries using your device's GPS location
- **Google Places Integration**: Automatically discovers pantry locations in your area
- **Distance Calculation**: See how far each pantry is from your current location
- **Search Functionality**: Filter pantries by name, address, or description

### 📸 Product Detection & Publishing
- **CameraX Integration**: Take photos of food items using the device camera
- **ML Kit Image Labeling**: Automatically detects and identifies food products in images
- **Manual Entry**: Option to manually add items if automatic detection doesn't work
- **Pantry Publishing**: Create and publish pantries with your donated items
- **Image Storage**: Store product images in Firebase Cloud Storage

### 🛒 Shopping Cart & Reservations
- **Product Selection**: Browse available items from different pantries
- **Cart Management**: Add items to cart and manage quantities
- **Order Creation**: Reserve items from pantries
- **Order History**: View your past reservations and orders

### 👤 User Profile & Authentication
- **Multiple Login Options**: 
  - Email/password authentication
  - Google Sign-In integration
  - Biometric (fingerprint) authentication
- **Profile Management**: 
  - Customize username
  - Upload profile pictures
  - Manage account settings
- **Donor Dashboard**: Track your donations and manage your published pantries

### 🍳 Recipe Recommendations
- **AI-Powered Recipe Generation**: Uses DeepSeek API to generate personalized recipes
- **Ingredient-Based Suggestions**: Get recipe ideas based on items in your cart or pantry
- **Recipe Details**: View step-by-step cooking instructions, ingredients, cooking time, and difficulty level
- **Recipe Sharing**: Share recipes as text files with friends and family
- **Public Recipe Links**: Generate shareable links for recipes that anyone can access

### 🔔 Push Notifications
- **Real-Time Updates**: Receive notifications when new pantries are created or updated
- **Firebase Cloud Messaging**: Integrated with FCM for reliable notification delivery
- **Cloud Functions**: Server-side triggers for automatic notification sending

### 📍 Location Services
- **Fused Location Provider**: Accurate location tracking using Google Play Services
- **Permission Handling**: Properly requests and manages location permissions
- **Nearby Search**: Combines Firestore data with Google Places API for comprehensive results

## Technology Stack

### Frontend
- **Language**: Kotlin
- **UI Framework**: Android Views with ViewBinding
- **Architecture**: Activity-based with MVVM patterns
- **Camera**: CameraX API for modern camera functionality
- **ML**: Google ML Kit for image labeling and product detection
- **Location**: Google Play Services Location API
- **Maps**: Google Places API

### Backend
- **Database**: Firebase Firestore (NoSQL)
- **Storage**: Firebase Cloud Storage (for images)
- **Authentication**: Firebase Authentication
- **Cloud Functions**: Node.js for server-side logic
- **Notifications**: Firebase Cloud Messaging (FCM)

### APIs & Services
- **DeepSeek API**: Recipe generation using AI
- **Google Places API**: Location-based pantry discovery
- **Firebase Services**: Complete backend infrastructure

## Project Structure

```
Pantry-Pal/
├── app/
│   └── src/main/
│       ├── java/com/unh/pantrypalonevo/
│       │   ├── Activities/          # Main app screens
│       │   ├── Adapters/            # RecyclerView adapters
│       │   ├── Models/              # Data models
│       │   └── Services/            # API services
│       └── res/                     # Resources (layouts, drawables, etc.)
├── functions/                       # Firebase Cloud Functions
├── cloud-functions/                 # Additional cloud functions
├── firestore.rules                  # Firestore security rules
└── firebase.json                    # Firebase configuration
```

## Setup Instructions

### Prerequisites
- Android Studio (latest version)
- Android SDK 33 or higher
- Kotlin plugin
- Firebase account
- Google Cloud account (for Places API)

### Configuration Steps

1. **Clone the repository**
   ```bash
   git clone https://github.com/UNH-Android-Fall2025/Pantry-Pal.git
   cd Pantry-Pal
   ```

2. **Firebase Setup**
   - Create a Firebase project at https://console.firebase.google.com
   - Download `google-services.json` and place it in `app/` directory
   - Enable Firestore, Authentication, Cloud Storage, and Cloud Messaging

3. **API Keys**
   - Add your DeepSeek API key to `app/src/main/res/values/strings.xml`:
     ```xml
     <string name="deepseek_api_key">YOUR_API_KEY_HERE</string>
     ```
   - Add your Google Places API key:
     ```xml
     <string name="google_places_api_key">YOUR_API_KEY_HERE</string>
     ```

4. **Build the project**
   ```bash
   ./gradlew assembleDebug
   ```

5. **Deploy Firestore Rules**
   ```bash
   firebase deploy --only firestore:rules
   ```

6. **Deploy Cloud Functions** (optional)
   ```bash
   cd functions
   npm install
   firebase deploy --only functions
   ```

## Testing & Demo

**Important Note for Professor**: 

A test account with username **test3** is present in the system and can be used to check the functionality of all features or to demo the project. This test account has been set up with sample data and can be used to:

- Test all authentication methods (email/password, Google Sign-In, fingerprint)
- Explore pantry browsing and product selection features
- Test recipe generation with the DeepSeek API
- Verify push notification functionality
- Demo the complete user flow from login to recipe sharing
- Test product detection and pantry publishing
- Check cart and reservation functionality

To use the test account, simply log in with the **test3** credentials when launching the app. This account is fully functional and ready for demonstration purposes.

## Key Functionalities

### For Donors
1. Take photos of food items using the camera
2. Let ML Kit automatically detect products
3. Review and confirm detected items
4. Create a pantry with location and items
5. Publish the pantry for others to see

### For Recipients
1. Browse nearby pantries on the home screen
2. View available products in each pantry
3. Add items to cart
4. Reserve items for pickup
5. Get recipe suggestions based on available items

### Recipe Features
1. Add items to cart from pantries
2. Navigate to Recipes section
3. Generate AI-powered recipe suggestions
4. View detailed recipe instructions
5. Share recipes as text files

## Firebase Structure

The app uses a nested Firestore structure:

```
pantries/
  {pantryId}/
    - name, address, lastUpdated
    donors/
      {donorId}/
        - name, contact
        items/
          {itemId}/
            - name, quantity, category, imageUri, etc.
    reservations/
      {reservationId}/
        - userId, items, status, etc.

users/
  {userId}/
    - username, email, profilePictureUrl, notificationToken, etc.

shared_recipes/
  {recipeId}/
    - title, ingredients, steps, time, difficulty, etc.
```

## Security

- Firestore security rules ensure users can only modify their own data
- Authentication required for most operations
- Public read access for shared recipes
- Cloud Functions have special permissions for notification sending

## Permissions Required

- **Camera**: For taking photos of food items
- **Location**: For finding nearby pantries
- **Storage**: For accessing and uploading images
- **Notifications**: For receiving push notifications
- **Internet**: For API calls and Firebase operations

## Known Limitations

- Recipe generation requires internet connection
- Location services need GPS to be enabled
- Some features require Firebase project to be properly configured
- Image detection accuracy depends on photo quality

## Future Enhancements

- Enhanced product categorization
- Expiration date tracking
- Donation history analytics
- Social features for community engagement
- Offline mode support

## Contributors

This project was developed as part of a course assignment at the University of New Haven.

## License

This project is for educational purposes.

---

**For questions or issues, please contact the development team or refer to the project documentation.**

