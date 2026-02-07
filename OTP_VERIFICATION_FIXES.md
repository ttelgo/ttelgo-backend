# OTP Verification API Fixes

## Issues Identified and Fixed

### 1. **Enhanced Logging**
   - Added detailed logging at each step of OTP verification
   - Logs now show:
     - Email normalization process
     - Number of tokens found
     - Token expiry status
     - OTP verification result
   
   **Location:** `AuthService.verifyOtp()`

### 2. **Better Error Handling**
   - Added explicit null checks for `otpToken` after lookup
   - Improved error messages to distinguish between:
     - No tokens found
     - All tokens expired
     - Invalid OTP
   
   **Location:** `AuthService.verifyOtp()`

### 3. **Enhanced DTO Validation**
   - Added `@Email` validation for email field
   - Added `@Pattern` validation for phone field (E.164 format)
   - Better validation error messages
   
   **Location:** `OtpVerifyRequest.java`

### 4. **Improved Token Lookup**
   - Added explicit check for empty token list before filtering
   - Better logging of token expiry checks
   - More descriptive error messages

## Common Issues and Solutions

### Issue 1: "Invalid or expired OTP" when OTP is correct

**Possible Causes:**
1. **Email case mismatch**: Email stored in lowercase but request has mixed case
   - **Solution**: Email is now normalized to lowercase in both request and verification
   
2. **OTP expired**: OTP expires after 5 minutes
   - **Solution**: Check logs for expiry time vs current time
   
3. **OTP already used**: OTP marked as `isUsed=true`
   - **Solution**: Request a new OTP
   
4. **Maximum attempts exceeded**: User tried more than 3 times
   - **Solution**: Request a new OTP

### Issue 2: OTP not found in database

**Possible Causes:**
1. **Email normalization mismatch**: Email stored differently than requested
   - **Solution**: Both request and verification now normalize email to lowercase
   
2. **OTP not saved**: Database transaction issue
   - **Solution**: Check logs for "OTP token saved to database" message

### Issue 3: OTP verification fails silently

**Solution**: Enhanced logging now shows:
- Token lookup results
- Expiry checks
- Verification attempts
- Error details

## Testing the Fix

### 1. Request OTP
```bash
POST /api/v1/auth/otp/request
{
  "email": "test@example.com",
  "purpose": "LOGIN"
}
```

### 2. Verify OTP (use the OTP received via email)
```bash
POST /api/v1/auth/otp/verify
{
  "email": "test@example.com",
  "otp": "123456"
}
```

### 3. Check Logs
Look for these log messages:
- `"OTP verification request received for email=..."`
- `"Found X unused OTP tokens for email: ..."`
- `"OTP token found for verification: id=..."`
- `"Verifying OTP: providedOtp='...'"`
- `"OTP verification result: isValid=..."`
- `"OTP verified successfully for token id=..."`

## Debugging Tips

1. **Enable DEBUG logging** in `application-dev.yml`:
   ```yaml
   logging:
     level:
       com.tiktel.ttelgo.auth: DEBUG
   ```

2. **Check database** for OTP tokens:
   ```sql
   SELECT id, email, phone, is_used, attempts, max_attempts, expires_at, created_at
   FROM otp_tokens
   WHERE email = 'test@example.com'
   ORDER BY created_at DESC;
   ```

3. **Verify email normalization**:
   - Check that email is stored in lowercase in database
   - Check that verification request normalizes email to lowercase

4. **Check OTP expiry**:
   - OTP expires 5 minutes after creation
   - Check `expires_at` vs current time

## Next Steps

If issues persist:

1. **Check application logs** for detailed error messages
2. **Verify database connection** and OTP token storage
3. **Test with a fresh OTP** (request new OTP and verify immediately)
4. **Check email delivery** to ensure OTP is being sent correctly

