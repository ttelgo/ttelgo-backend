# Bundle API Error Troubleshooting Guide

## Overview
This guide helps you troubleshoot 500 errors when calling the bundles API endpoints.

## What Was Fixed

1. **Comprehensive Exception Handling**
   - All bundle loading methods have try-catch blocks
   - EsimGoClient catches all RestTemplate exceptions
   - GlobalExceptionHandler returns 200 for bundle endpoints instead of 500

2. **Detailed Logging Added**
   - Controller logs every API call with parameters
   - Service methods log entry/exit and bundle counts
   - EsimGoClient logs all API calls and errors with full details
   - GlobalExceptionHandler logs complete exception details

## How to Troubleshoot

### Step 1: Check Backend Logs

When you call the bundles API, you should see logs like this:

```
=== BUNDLES API CALL START ===
Request params - page: 0, size: 10000, type: local, countryIso: null, search: null
=== listLocalBundles() called ===
=== loadAllBundles() called ===
Loading all bundles from eSIMGo API (cache miss or expired)
Calling eSIMGo API: https://api.esim-go.com/v2.4/catalogue?page=1&perPage=100&direction=asc
```

### Step 2: Look for Error Messages

If there's an error, you'll see detailed logs:

**If eSIMGo API fails:**
```
=== HTTP CLIENT ERROR calling eSIMGo API ===
URL: https://api.esim-go.com/v2.4/catalogue?page=1&perPage=100&direction=asc
Status: 401
Response body: [error details]
```

**If there's a code error:**
```
=== CRITICAL ERROR in loadAllBundles() ===
Error message: [specific error]
Error class: [exception class name]
Full stack trace: [complete stack trace]
Root cause: [if available]
```

**If GlobalExceptionHandler catches it:**
```
=== GLOBAL EXCEPTION HANDLER TRIGGERED ===
Request path: /api/v1/bundles
Error class: [exception class]
Error message: [error message]
Full stack trace: [complete stack trace]
```

### Step 3: Common Issues and Solutions

#### Issue 1: eSIMGo API Authentication Error
**Symptoms:** HTTP 401 or 403 errors in logs
**Solution:** Check `application-dev.yml` - verify the API key is correct

#### Issue 2: Network/Connection Error
**Symptoms:** `ResourceAccessException` or connection timeout
**Solution:** 
- Check internet connection
- Verify eSIMGo API is accessible
- Check firewall/proxy settings

#### Issue 3: NullPointerException
**Symptoms:** `NullPointerException` in logs
**Solution:** Check which object is null from the stack trace

#### Issue 4: JSON Parsing Error
**Symptoms:** `HttpMessageNotReadableException` or JSON parsing errors
**Solution:** eSIMGo API might have changed response format

### Step 4: Check Logs Location

Logs are printed to:
- **Console:** If running `mvn spring-boot:run`, logs appear in the terminal
- **Log file:** Check `application-dev.yml` for logging configuration

### Step 5: Enable DEBUG Logging

To see even more details, add this to `application-dev.yml`:

```yaml
logging:
  level:
    com.tiktel.ttelgo.plan: DEBUG
    com.tiktel.ttelgo.integration.esimgo: DEBUG
```

## Expected Behavior

**When eSIMGo API is available:**
- Logs show successful API calls
- Bundles are loaded and cached
- Response contains bundle data

**When eSIMGo API is unavailable:**
- Logs show error details
- Returns 200 status with empty bundles array
- **NO 500 errors should occur**

## Testing

1. **Restart backend server:**
   ```bash
   mvn spring-boot:run
   ```

2. **Make a test request:**
   ```bash
   curl http://localhost:8080/api/v1/bundles?size=10000
   ```

3. **Check the backend console logs** - you should see detailed logging

4. **Expected response (even on error):**
   ```json
   {
     "success": true,
     "message": "No bundles available",
     "data": {
       "bundles": []
     }
   }
   ```

## Next Steps

If you still see 500 errors after restarting:
1. Check the backend console logs for the detailed error messages
2. Look for the `=== CRITICAL ERROR ===` or `=== GLOBAL EXCEPTION HANDLER TRIGGERED ===` messages
3. Share the specific error class and message from the logs

