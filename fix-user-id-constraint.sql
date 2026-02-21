-- Allow user_id to be NULL (since it's optional in the API)
ALTER TABLE orders ALTER COLUMN user_id DROP NOT NULL;

