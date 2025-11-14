# Vision Cloud Function

This folder contains a lightweight Google Cloud Function that wraps the
Vision API **Object Localization** feature. The Android app will call this
function instead of shipping a TensorFlow Lite model.

## Files

| File | Purpose |
|------|---------|
| `index.js` | Function entry point. Accepts a base64 image and returns objects + bounding boxes. |
| `package.json` | Declares the single dependency (`@google-cloud/vision`). |
| `.gitignore` | Prevents accidental commits of service-account keys. |

## Usage

1. **Place your service-account key**
   - Copy the JSON key you downloaded from Google Cloud into this directory.
   - Name it something like `service-account-key.json` (the file is ignored by git).

2. **Deploy (from this folder)**

```bash
export GOOGLE_APPLICATION_CREDENTIALS="$(pwd)/service-account-key.json"
gcloud functions deploy detectPantryItems \
  --runtime nodejs20 \
  --trigger-http \
  --allow-unauthenticated \
  --region us-central1
```

3. **Call the function** from Android by POSTing a JSON body:

```json
{
  "imageBase64": "<base64-encoded-image>"
}
```

The response looks like:

```json
{
  "objects": [
    {
      "name": "Bottle",
      "score": 0.87,
      "vertices": [ { "x": 0.12, "y": 0.18 }, ... ]
    }
  ]
}
```

Adjust authentication (API keys / Firebase / IAM) as needed before going to production.

