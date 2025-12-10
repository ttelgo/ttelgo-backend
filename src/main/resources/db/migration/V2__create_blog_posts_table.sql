-- Create blog_posts table
CREATE TABLE IF NOT EXISTS blog_posts (
    id BIGSERIAL PRIMARY KEY,
    slug VARCHAR(255) UNIQUE NOT NULL,
    title VARCHAR(255) NOT NULL,
    excerpt TEXT,
    content TEXT,  -- PostgreSQL uses TEXT instead of LONGTEXT
    featured_image VARCHAR(500),
    category VARCHAR(100) NOT NULL,
    read_time VARCHAR(50),
    is_featured BOOLEAN DEFAULT FALSE,
    is_published BOOLEAN DEFAULT FALSE,
    published_at TIMESTAMP,
    author_id BIGINT,
    author_name VARCHAR(255),
    meta_title VARCHAR(255),
    meta_description TEXT,
    tags VARCHAR(500),
    view_count BIGINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create index on slug for faster lookups
CREATE INDEX IF NOT EXISTS idx_blog_posts_slug ON blog_posts(slug);

-- Create index on is_published for filtering
CREATE INDEX IF NOT EXISTS idx_blog_posts_published ON blog_posts(is_published) WHERE is_published = TRUE;

-- Create index on category for filtering
CREATE INDEX IF NOT EXISTS idx_blog_posts_category ON blog_posts(category);

-- Create index on published_at for sorting
CREATE INDEX IF NOT EXISTS idx_blog_posts_published_at ON blog_posts(published_at DESC);

