# DeepSeek Recipe Integration Setup Guide

## ✅ What's Been Done

1. **Created DeepSeekService.kt** - Service class for generating recipes using DeepSeek API
2. **Updated RecipeActivity** - Now uses DeepSeek instead of RecipeDatabase
3. **Updated HomePageActivity** - Recipe button now opens RecipeActivity
4. **Updated CartActivity** - Recipe button passes cart items to RecipeActivity
5. **Removed RecipeDatabase dependency** - RecipeActivity no longer uses hardcoded recipes

## 📋 Setup Steps

### Step 1: Get DeepSeek API Key

1. Go to https://platform.deepseek.com/
2. Sign up or log in
3. Navigate to API Keys section
4. Create a new API key
5. Copy the API key

### Step 2: Add API Key to Project

1. Open `app/src/main/res/values/strings.xml`
2. Find: `<string name="deepseek_api_key">YOUR_DEEPSEEK_API_KEY_HERE</string>`
3. Replace `YOUR_DEEPSEEK_API_KEY_HERE` with your actual API key

### Step 3: Test the Integration

1. Build and run the app
2. Add items to cart (or use detected items)
3. Click the "Recipes" button
4. Recipes will be generated using DeepSeek AI

## 🎯 How It Works

### From Cart:
- User adds items to cart
- Clicks "Recipes" button in CartActivity
- Cart items are passed to RecipeActivity
- DeepSeek generates recipes based on cart items

### From Home:
- User clicks "Recipes" button in HomePageActivity
- If items available, they're passed to RecipeActivity
- Otherwise, user can manually enter items

### Manual Entry:
- If no items are provided, RecipeActivity shows a dialog
- User can enter items separated by commas
- DeepSeek generates recipes for those items

## 📝 Features

- ✅ AI-powered recipe generation
- ✅ Supports multiple ingredients
- ✅ Shows loading progress
- ✅ Handles errors gracefully
- ✅ Fallback to manual entry if no items

## ⚠️ Important Notes

- DeepSeek API requires an API key (get it from https://platform.deepseek.com/)
- API calls are made on background thread (no blocking)
- Recipes are generated in real-time using AI
- Check DeepSeek API pricing/limits

## 🔧 Troubleshooting

1. **No recipes generated:**
   - Check API key is correct in strings.xml
   - Verify DeepSeek API key is valid
   - Check internet connection
   - Review Logcat for error messages

2. **API errors:**
   - Verify API key has proper permissions
   - Check API quota/limits
   - Ensure billing is enabled (if required)

## 📚 API Documentation

- DeepSeek API: https://platform.deepseek.com/api-docs/
- Model used: `deepseek-chat`

