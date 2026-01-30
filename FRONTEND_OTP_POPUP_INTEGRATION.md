# Frontend OTP Popup Integration Guide

## Overview
This guide explains how to integrate the OTP authentication flow with a popup-based UI where:
1. User sees a popup to enter OTP
2. After OTP verification, user information is displayed in a Guest/User popup menu

---

## API Endpoints

### 1. Request OTP
**Endpoint:** `POST /api/v1/auth/otp/request`

**Request Body:**
```json
{
  "email": "user@example.com",
  "purpose": "LOGIN"
}
```

**Response:**
```json
{
  "success": true,
  "message": "OTP sent successfully",
  "data": "OTP sent successfully"
}
```

**Frontend Action:** Show OTP input popup

---

### 2. Verify OTP
**Endpoint:** `POST /api/v1/auth/otp/verify`

**Request Body:**
```json
{
  "email": "user@example.com",
  "otp": "123456"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 900000,
    "user": {
      "id": 1,
      "email": "user@example.com",
      "fullName": "John Doe",
      "phone": null,
      "firstName": "John",
      "lastName": "Doe",
      "isEmailVerified": true,
      "isPhoneVerified": false,
      "role": "USER"
    }
  }
}
```

**Frontend Action:** 
- Close OTP popup
- Show user information in Guest/User menu popup
- Store JWT token for authenticated requests

---

## Frontend Implementation Flow

### Step 1: Show OTP Request Popup
```javascript
// When user clicks login/guest menu
function showOtpPopup() {
  // Show popup with email input
  // User enters email
  // Call: POST /api/v1/auth/otp/request
}
```

### Step 2: Show OTP Input Popup
```javascript
// After OTP request succeeds
function showOtpInputPopup(email) {
  // Show popup with 6-digit OTP input field
  // User enters OTP
  // Call: POST /api/v1/auth/otp/verify
}
```

### Step 3: Show User Menu Popup
```javascript
// After OTP verification succeeds
function showUserMenuPopup(authResponse) {
  const user = authResponse.data.user;
  
  // Determine if user is Guest or Registered User
  const userType = user.role === 'USER' ? 'User' : 'Guest';
  
  // Show popup menu with:
  // - User name: user.fullName or user.email
  // - User type: Guest/User
  // - Email: user.email
  // - Profile options
  // - Logout option
  
  // Store token for future requests
  localStorage.setItem('accessToken', authResponse.data.token);
  localStorage.setItem('refreshToken', authResponse.data.refreshToken);
}
```

---

## React/Next.js Example

```jsx
import { useState } from 'react';

function OtpAuthPopup() {
  const [email, setEmail] = useState('');
  const [otp, setOtp] = useState('');
  const [step, setStep] = useState('email'); // 'email' | 'otp' | 'success'
  const [userInfo, setUserInfo] = useState(null);

  // Step 1: Request OTP
  const handleRequestOtp = async () => {
    try {
      const response = await fetch('http://localhost:8080/api/v1/auth/otp/request', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, purpose: 'LOGIN' })
      });
      
      const data = await response.json();
      if (data.success) {
        setStep('otp'); // Show OTP input
      }
    } catch (error) {
      console.error('OTP request failed:', error);
    }
  };

  // Step 2: Verify OTP
  const handleVerifyOtp = async () => {
    try {
      const response = await fetch('http://localhost:8080/api/v1/auth/otp/verify', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, otp })
      });
      
      const data = await response.json();
      if (data.success) {
        setUserInfo(data.data.user);
        setStep('success');
        
        // Store tokens
        localStorage.setItem('accessToken', data.data.token);
        localStorage.setItem('refreshToken', data.data.refreshToken);
        
        // Show user menu popup
        showUserMenu(data.data.user);
      }
    } catch (error) {
      console.error('OTP verification failed:', error);
    }
  };

  // Step 3: Show User Menu
  const showUserMenu = (user) => {
    // Close OTP popup
    // Show user menu popup with user info
    const userType = user.role === 'USER' ? 'User' : 'Guest';
    // Display user menu
  };

  return (
    <div className="otp-popup">
      {step === 'email' && (
        <div>
          <h2>Enter Email</h2>
          <input 
            type="email" 
            value={email} 
            onChange={(e) => setEmail(e.target.value)}
            placeholder="your@email.com"
          />
          <button onClick={handleRequestOtp}>Send OTP</button>
        </div>
      )}
      
      {step === 'otp' && (
        <div>
          <h2>Enter OTP</h2>
          <p>OTP sent to {email}</p>
          <input 
            type="text" 
            value={otp} 
            onChange={(e) => setOtp(e.target.value)}
            placeholder="123456"
            maxLength={6}
          />
          <button onClick={handleVerifyOtp}>Verify</button>
        </div>
      )}
      
      {step === 'success' && userInfo && (
        <div>
          <h2>Welcome, {userInfo.fullName || userInfo.email}!</h2>
          <p>User Type: {userInfo.role === 'USER' ? 'User' : 'Guest'}</p>
        </div>
      )}
    </div>
  );
}
```

---

## User Menu Popup Structure

After successful OTP verification, show a popup menu with:

```jsx
function UserMenuPopup({ user, onClose }) {
  const userType = user.role === 'USER' ? 'User' : 'Guest';
  
  return (
    <div className="user-menu-popup">
      <div className="user-info">
        <h3>{user.fullName || user.email}</h3>
        <p className="user-type">{userType}</p>
        <p className="user-email">{user.email}</p>
      </div>
      
      <div className="menu-options">
        <button onClick={() => navigateToProfile()}>Profile</button>
        <button onClick={() => navigateToOrders()}>My Orders</button>
        <button onClick={() => navigateToSettings()}>Settings</button>
        <button onClick={handleLogout}>Logout</button>
      </div>
    </div>
  );
}
```

---

## Error Handling

### OTP Request Errors
```json
{
  "success": false,
  "message": "Invalid email format",
  "error": {
    "code": "ERR_1001",
    "message": "Invalid request"
  }
}
```

### OTP Verification Errors
```json
{
  "success": false,
  "message": "Invalid OTP",
  "error": {
    "code": "ERR_1002",
    "message": "Invalid OTP"
  }
}
```

**Frontend should:**
- Show error message in popup
- Allow retry
- Clear OTP input on error

---

## Token Usage

After OTP verification, use the token for authenticated requests:

```javascript
// Example: Get current user
async function getCurrentUser() {
  const token = localStorage.getItem('accessToken');
  
  const response = await fetch('http://localhost:8080/api/v1/users/me', {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });
  
  return response.json();
}
```

---

## Summary

1. **OTP Request Popup:** User enters email → API sends OTP
2. **OTP Input Popup:** User enters 6-digit OTP → API verifies
3. **User Menu Popup:** Shows user info (Guest/User) → User can access profile/orders/etc.

**Key Points:**
- Store JWT token after successful verification
- Use token in `Authorization: Bearer <token>` header for protected endpoints
- Show appropriate user type (Guest vs User) based on `user.role`
- Handle errors gracefully with retry options

