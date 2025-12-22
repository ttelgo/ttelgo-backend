# How to Create an Admin User

To access the admin panel, you need a user account with `ADMIN` or `SUPER_ADMIN` role.

## Steps to Create an Admin User

### Method 1: Register and Update Role (Recommended)

1. **Register via Frontend:**
   - Navigate to `http://localhost:5173/signup` (or your frontend URL)
   - Enter your email address
   - Verify the OTP code sent to your email
   - This creates a user with `role = 'USER'` by default

2. **Update Role in Database:**
   
   Connect to your PostgreSQL database and run:
   
   ```sql
   UPDATE users 
   SET role = 'ADMIN' 
   WHERE email = 'your-email@example.com';
   ```
   
   Or use the provided SQL script:
   ```bash
   # Edit create-admin-user.sql and replace 'your-email@example.com' with your email
   # Then run it against your database
   psql -U your_username -d your_database -f create-admin-user.sql
   ```

3. **Verify the Update:**
   ```sql
   SELECT id, email, role FROM users WHERE email = 'your-email@example.com';
   ```
   You should see `role = 'ADMIN'`

4. **Log Out and Log Back In:**
   - Log out from the frontend (if logged in)
   - Log in again at `http://localhost:5173/login`
   - Enter your email and verify OTP
   - This will generate a new JWT token with the ADMIN role

5. **Access Admin Panel:**
   - Navigate to `http://localhost:5173/admin`
   - You should now be able to access all admin endpoints

### Method 2: Create Admin User Directly in Database (Advanced)

If you want to create an admin user directly in the database:

```sql
-- Create user with ADMIN role
INSERT INTO users (email, role, is_email_verified, created_at, updated_at)
VALUES ('admin@example.com', 'ADMIN', true, NOW(), NOW());
```

**Note:** You'll still need to complete OTP verification via the frontend to be able to log in, as the system requires OTP tokens for authentication.

## Available Roles

- `USER` - Standard user (default for new registrations)
- `ADMIN` - Administrative user with access to admin panel
- `SUPER_ADMIN` - Super administrator with full system access

## Troubleshooting

### Issue: Still getting 403 Forbidden after updating role

**Solution:** Make sure you:
1. Logged out completely
2. Logged in again (this generates a new JWT token with the updated role)
3. Checked that the JWT token in localStorage contains the ADMIN role

You can verify your token by:
1. Opening browser DevTools → Application → Local Storage
2. Find `auth_token` key
3. Copy the token value
4. Decode it at https://jwt.io
5. Check the `role` claim - it should be `"ADMIN"` or `"SUPER_ADMIN"`

### Issue: JWT token doesn't have the updated role

**Solution:** The JWT token is generated when you log in and includes the role at that time. If you update the role after logging in, you need to:
1. Log out
2. Log in again
3. This generates a new JWT token with the updated role

## Quick Reference

**Frontend URLs:**
- Registration: `http://localhost:5173/signup`
- Login: `http://localhost:5173/login`
- Admin Panel: `http://localhost:5173/admin`

**Database Query:**
```sql
-- Update user to ADMIN
UPDATE users SET role = 'ADMIN' WHERE email = 'your-email@example.com';

-- Update user to SUPER_ADMIN
UPDATE users SET role = 'SUPER_ADMIN' WHERE email = 'your-email@example.com';
```

