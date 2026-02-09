# Social Media Login Configuration Status

## Backend Status Summary

| Provider | Backend Status | Endpoint | Configuration Required |
|----------|---------------|----------|----------------------|
| **Google** | ✅ **FULLY CONFIGURED** | `POST /api/v1/auth/google` | `GOOGLE_OAUTH_CLIENT_ID` |
| **Apple** | ✅ **FULLY CONFIGURED** | `POST /api/v1/auth/apple` | `APPLE_OAUTH_CLIENT_ID` |
| **Facebook** | ❌ **NOT IMPLEMENTED** | N/A | N/A |

---

## ✅ Google Sign-In - Backend Status

### Implementation Status: **COMPLETE**

**Endpoint:** `POST /api/v1/auth/google`

**Request Body:**
```json
{
  "idToken": "google-id-token-jwt-string"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "accessToken": "jwt-access-token",
    "refreshToken": "jwt-refresh-token",
    "tokenType": "Bearer",
    "expiresIn": 86400000,
    "user": {
      "id": 1,
      "email": "user@gmail.com",
      "name": "Full Name",
      "firstName": "First",
      "lastName": "Last",
      "isEmailVerified": true,
      "role": "USER"
    }
  }
}
```

**Features:**
- ✅ Google ID token verification using `GoogleIdTokenVerifier`
- ✅ Email verification check (rejects if `email_verified` is false)
- ✅ User creation with `provider = GOOGLE`
- ✅ JWT access token and refresh token generation
- ✅ Session management

**Configuration Required:**
```yaml
# application.yml
google:
  oauth:
    client-id: ${GOOGLE_OAUTH_CLIENT_ID:}
```

**Environment Variable:**
```bash
GOOGLE_OAUTH_CLIENT_ID=your-google-client-id.apps.googleusercontent.com
```

---

## ✅ Apple Sign-In - Backend Status

### Implementation Status: **COMPLETE**

**Endpoint:** `POST /api/v1/auth/apple`

**Request Body:**
```json
{
  "identityToken": "apple-identity-token-jwt-string"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "accessToken": "jwt-access-token",
    "refreshToken": "jwt-refresh-token",
    "tokenType": "Bearer",
    "expiresIn": 86400000,
    "user": {
      "id": 1,
      "email": "user@example.com",
      "name": "Full Name",
      "firstName": "First",
      "lastName": "Last",
      "isEmailVerified": true,
      "role": "USER"
    }
  }
}
```

**Features:**
- ✅ Apple identity token verification using JWKS
- ✅ Public key caching (1 hour TTL)
- ✅ JWT signature validation
- ✅ Issuer validation (`https://appleid.apple.com`)
- ✅ Audience validation (bundle ID)
- ✅ Handles email on first login only
- ✅ User lookup by Apple user ID or email
- ✅ JWT access token and refresh token generation
- ✅ Session management

**Configuration Required:**
```yaml
# application.yml
apple:
  oauth:
    client-id: ${APPLE_OAUTH_CLIENT_ID:}
```

**Environment Variable:**
```bash
APPLE_OAUTH_CLIENT_ID=com.yourcompany.yourapp
```

---

## ❌ Facebook Sign-In - Backend Status

### Implementation Status: **NOT IMPLEMENTED**

**Current State:**
- ✅ `AuthProvider.FACEBOOK` enum exists in `User` entity
- ❌ No Facebook OAuth service
- ❌ No Facebook login endpoint
- ❌ No Facebook token verification

**To Implement Facebook:**
1. Create `FacebookOAuthService` similar to `GoogleOAuthService`
2. Add `POST /api/v1/auth/facebook` endpoint
3. Add `FacebookLoginRequest` DTO
4. Implement Facebook access token verification
5. Add configuration for Facebook App ID and App Secret

---

## Frontend Configuration Guide

### 1. Google Sign-In - Frontend Setup

#### For React Native (Expo/React Native)

**Install Dependencies:**
```bash
npm install @react-native-google-signin/google-signin
# or for Expo
expo install expo-auth-session expo-crypto
```

**Configuration:**

1. **Get Google OAuth Client ID:**
   - Go to [Google Cloud Console](https://console.cloud.google.com/)
   - Create OAuth 2.0 credentials
   - Get Client ID for iOS/Android/Web

2. **React Native Setup:**
```javascript
import { GoogleSignin } from '@react-native-google-signin/google-signin';

// Configure Google Sign-In
GoogleSignin.configure({
  webClientId: 'YOUR_GOOGLE_WEB_CLIENT_ID.apps.googleusercontent.com',
  iosClientId: 'YOUR_GOOGLE_IOS_CLIENT_ID.apps.googleusercontent.com', // iOS only
  offlineAccess: true,
});

// Sign in function
const signInWithGoogle = async () => {
  try {
    await GoogleSignin.hasPlayServices();
    const userInfo = await GoogleSignin.signIn();
    const idToken = userInfo.idToken; // This is what you send to backend
    
    // Send to your backend
    const response = await fetch('http://your-backend/api/v1/auth/google', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        idToken: idToken,
      }),
    });
    
    const data = await response.json();
    if (data.success) {
      // Save accessToken and refreshToken
      await AsyncStorage.setItem('accessToken', data.data.accessToken);
      await AsyncStorage.setItem('refreshToken', data.data.refreshToken);
      // Navigate to home screen
    }
  } catch (error) {
    console.error('Google Sign-In Error:', error);
  }
};
```

3. **Expo Setup (Alternative):**
```javascript
import * as Google from 'expo-auth-session/providers/google';
import * as WebBrowser from 'expo-web-browser';

WebBrowser.maybeCompleteAuthSession();

const [request, response, promptAsync] = Google.useAuthRequest({
  iosClientId: 'YOUR_IOS_CLIENT_ID',
  androidClientId: 'YOUR_ANDROID_CLIENT_ID',
  webClientId: 'YOUR_WEB_CLIENT_ID',
});

useEffect(() => {
  if (response?.type === 'success') {
    const { id_token } = response.params;
    
    // Send to backend
    fetch('http://your-backend/api/v1/auth/google', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ idToken: id_token }),
    })
    .then(res => res.json())
    .then(data => {
      if (data.success) {
        // Save tokens and navigate
      }
    });
  }
}, [response]);
```

**Important:** Use the same `GOOGLE_OAUTH_CLIENT_ID` in backend that matches your frontend client ID.

---

### 2. Apple Sign-In - Frontend Setup

#### For React Native (Expo/React Native)

**Install Dependencies:**
```bash
# For React Native
npm install react-native-apple-authentication

# For Expo
expo install expo-apple-authentication
```

**Configuration:**

1. **Get Apple Client ID (Bundle ID):**
   - Go to [Apple Developer Portal](https://developer.apple.com/)
   - Create App ID
   - Enable "Sign In with Apple" capability
   - Bundle ID format: `com.yourcompany.yourapp`

2. **React Native Setup:**
```javascript
import { appleAuth } from '@invertase/react-native-apple-authentication';

const signInWithApple = async () => {
  try {
    const appleAuthRequestResponse = await appleAuth.performRequest({
      requestedOperation: appleAuth.Operation.LOGIN,
      requestedScopes: [appleAuth.Scope.EMAIL, appleAuth.Scope.FULL_NAME],
    });

    if (!appleAuthRequestResponse.identityToken) {
      throw new Error('Apple Sign-In failed - no identity token returned');
    }

    const { identityToken } = appleAuthRequestResponse;
    
    // Send to your backend
    const response = await fetch('http://your-backend/api/v1/auth/apple', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        identityToken: identityToken,
      }),
    });
    
    const data = await response.json();
    if (data.success) {
      // Save accessToken and refreshToken
      await AsyncStorage.setItem('accessToken', data.data.accessToken);
      await AsyncStorage.setItem('refreshToken', data.data.refreshToken);
      // Navigate to home screen
    }
  } catch (error) {
    console.error('Apple Sign-In Error:', error);
  }
};
```

3. **Expo Setup:**
```javascript
import * as AppleAuthentication from 'expo-apple-authentication';

const signInWithApple = async () => {
  try {
    const credential = await AppleAuthentication.signInAsync({
      requestedScopes: [
        AppleAuthentication.AppleAuthenticationScope.FULL_NAME,
        AppleAuthentication.AppleAuthenticationScope.EMAIL,
      ],
    });

    if (credential.identityToken) {
      // Send to backend
      const response = await fetch('http://your-backend/api/v1/auth/apple', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          identityToken: credential.identityToken,
        }),
      });
      
      const data = await response.json();
      if (data.success) {
        // Save tokens and navigate
      }
    }
  } catch (error) {
    console.error('Apple Sign-In Error:', error);
  }
};
```

**Important:** 
- Use the same `APPLE_OAUTH_CLIENT_ID` (Bundle ID) in backend
- Apple only provides email on first login
- Subsequent logins will use Apple user ID

---

### 3. Facebook Sign-In - Frontend Setup (When Backend is Implemented)

**Install Dependencies:**
```bash
npm install react-native-fbsdk-next
# or for Expo
expo install expo-facebook
```

**Configuration:**

1. **Get Facebook App ID:**
   - Go to [Facebook Developers](https://developers.facebook.com/)
   - Create App
   - Get App ID and App Secret

2. **React Native Setup:**
```javascript
import { LoginManager, AccessToken } from 'react-native-fbsdk-next';

const signInWithFacebook = async () => {
  try {
    const result = await LoginManager.logInWithPermissions(['public_profile', 'email']);
    
    if (result.isCancelled) {
      throw new Error('User cancelled Facebook login');
    }

    const data = await AccessToken.getCurrentAccessToken();
    if (data) {
      const accessToken = data.accessToken;
      
      // Send to your backend (when implemented)
      const response = await fetch('http://your-backend/api/v1/auth/facebook', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          accessToken: accessToken,
        }),
      });
      
      const data = await response.json();
      if (data.success) {
        // Save tokens and navigate
      }
    }
  } catch (error) {
    console.error('Facebook Sign-In Error:', error);
  }
};
```

---

## Environment Variables Setup

### Backend (.env or application.yml)

```bash
# Google OAuth
GOOGLE_OAUTH_CLIENT_ID=your-google-client-id.apps.googleusercontent.com

# Apple OAuth
APPLE_OAUTH_CLIENT_ID=com.yourcompany.yourapp

# Facebook OAuth (when implemented)
FACEBOOK_APP_ID=your-facebook-app-id
FACEBOOK_APP_SECRET=your-facebook-app-secret
```

### Frontend (.env)

```bash
# Google
REACT_APP_GOOGLE_CLIENT_ID=your-google-client-id.apps.googleusercontent.com

# Apple
REACT_APP_APPLE_CLIENT_ID=com.yourcompany.yourapp

# Facebook
REACT_APP_FACEBOOK_APP_ID=your-facebook-app-id
```

---

## Testing Checklist

### Google Sign-In
- [ ] Google OAuth Client ID configured in backend
- [ ] Google Client ID configured in frontend
- [ ] Frontend can get Google ID token
- [ ] Backend endpoint `/api/v1/auth/google` works
- [ ] Access token and refresh token received
- [ ] User created/updated in database

### Apple Sign-In
- [ ] Apple Bundle ID configured in backend
- [ ] Apple Sign-In enabled in Apple Developer Portal
- [ ] Frontend can get Apple identity token
- [ ] Backend endpoint `/api/v1/auth/apple` works
- [ ] Access token and refresh token received
- [ ] User created/updated in database (even without email)

### Facebook Sign-In
- [ ] ❌ Backend not implemented yet
- [ ] Facebook App ID and Secret configured (when implemented)
- [ ] Frontend can get Facebook access token (when implemented)

---

## Common Issues & Solutions

### Google Sign-In Issues

**Problem:** "Invalid Google ID token"
- ✅ Check that `GOOGLE_OAUTH_CLIENT_ID` matches the client ID used in frontend
- ✅ Ensure token is not expired
- ✅ Verify email is verified in Google account

**Problem:** "Email not verified"
- ✅ User must verify email in Google account before signing in

### Apple Sign-In Issues

**Problem:** "Invalid Apple identity token"
- ✅ Check that `APPLE_OAUTH_CLIENT_ID` matches your Bundle ID
- ✅ Ensure token is not expired
- ✅ Verify Sign In with Apple is enabled in Apple Developer Portal

**Problem:** "Email not provided"
- ✅ This is normal - Apple only provides email on first login
- ✅ Backend handles this by using Apple user ID

### Facebook Sign-In Issues

**Problem:** Not implemented
- ✅ Backend needs to be implemented first
- ✅ See "To Implement Facebook" section above

---

## Summary

### ✅ Ready to Use:
- **Google Sign-In:** Fully configured, ready for frontend integration
- **Apple Sign-In:** Fully configured, ready for frontend integration

### ❌ Not Ready:
- **Facebook Sign-In:** Backend not implemented

### Frontend Action Items:
1. ✅ Install Google Sign-In SDK
2. ✅ Install Apple Sign-In SDK
3. ✅ Configure OAuth Client IDs/Bundle IDs
4. ✅ Implement sign-in functions
5. ✅ Send tokens to backend endpoints
6. ✅ Save access/refresh tokens
7. ✅ Handle authentication state

---

## Next Steps

1. **For Google & Apple:** Start implementing frontend integration using the code examples above
2. **For Facebook:** Request backend implementation if needed
3. **Test:** Use Postman to test backend endpoints first, then integrate frontend
4. **Production:** Ensure all OAuth credentials are properly secured

---

**Last Updated:** 2026-01-27
**Status:** Google ✅ | Apple ✅ | Facebook ❌

