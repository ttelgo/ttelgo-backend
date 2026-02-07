# OTP Verification Debugging Guide

## Current Issue
OTP verification is returning 500 Internal Server Error after recent changes.

## Recent Changes Made
1. Enhanced user creation logic with better null checks
2. Improved name extraction from email
3. Added DataIntegrityViolationException handling
4. Added defensive programming for all required fields

## How to Debug

### 1. Check Application Logs
Look for these log messages in your application logs:
- `"OTP verification request received for email=..."`
- `"Found X unused OTP tokens for email: ..."`
- `"OTP token found for verification: id=..."`
- `"OTP verified successfully for token id=..."`
- `"Implicit registration: Created CUSTOMER user..."`
- `"Failed to create user during OTP verification: ..."`

### 2. Check for Specific Errors
Look for these error patterns in logs:
- `NullPointerException`
- `DataIntegrityViolationException`
- `ConstraintViolationException`
- `IllegalArgumentException`

### 3. Test with Simple Case
Try verifying OTP for an email that:
- Has a valid OTP token in database
- User doesn't exist yet (will trigger user creation)

### 4. Database Check
Check if user already exists:
```sql
SELECT * FROM users WHERE email = 'abdulqayumsabir41@gmail.com';
```

Check OTP token:
```sql
SELECT * FROM otp_tokens 
WHERE email = 'abdulqayumsabir41@gmail.com' 
AND is_used = false 
ORDER BY created_at DESC;
```

## Potential Issues Fixed

1. **Name Field**: Now safely extracts name from email, handles edge cases
2. **Email Validation**: Ensures email is never null before user creation
3. **Duplicate User**: Handles case where user already exists
4. **Name Length**: Limits name to 255 characters to prevent database issues

## Next Steps

1. **Check Logs**: Look for the actual exception in application logs
2. **Test Again**: Try OTP verification with the same email
3. **Check Database**: Verify OTP token exists and is not expired
4. **Verify User**: Check if user already exists in database

If error persists, share the actual exception from logs for further debugging.

