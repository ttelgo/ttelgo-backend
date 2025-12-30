# Create Admin User Script

This script creates an admin user in the database that you can use to login to the admin panel.

## Credentials

- **Email:** `admin@ttelgo.com`
- **Password:** `Admin@123456`
- **Role:** `ADMIN`

## How to Run

### Option 1: Using psql command line

```bash
psql -U your_username -d your_database_name -f create-admin-user-script.sql
```

### Option 2: Using a Database Client (pgAdmin, DBeaver, etc.)

1. Open your database client
2. Connect to your database
3. Open the file `create-admin-user-script.sql`
4. Execute the script

### Option 3: Copy and Paste

1. Open `create-admin-user-script.sql`
2. Copy the INSERT statement
3. Paste and execute in your database client

## After Creating the User

1. Go to `http://localhost:5173/admin/login`
2. Use the credentials above to login
3. You should now have access to the admin panel

## Change the Password

If you want to change the password, you can:

1. Use the admin login page to login
2. Or create a new user via the registration form and update the role in the database:

```sql
UPDATE users 
SET role = 'ADMIN' 
WHERE email = 'your-email@example.com';
```

## Notes

- The password is hashed using BCrypt (Spring Security default)
- The script uses `ON CONFLICT` to update the user if the email already exists
- Make sure your `users` table has the unique constraint on `email` column

