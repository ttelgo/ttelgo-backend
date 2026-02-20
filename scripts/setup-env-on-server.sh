#!/bin/bash
# Setup Environment File on Server
# This script helps create the .env file on the production server
# Usage: Run this script on the server and follow the prompts

set -e

ENV_FILE="/home/ubuntu/ttelgo-backend/.env"
ENV_TEMPLATE="/home/ubuntu/ttelgo-backend/.env.production.template"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}TTelGo Environment Setup${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

# Check if .env already exists
if [ -f "$ENV_FILE" ]; then
    echo -e "${YELLOW}WARNING: .env file already exists at: $ENV_FILE${NC}"
    read -p "Do you want to overwrite it? (yes/no): " overwrite
    if [ "$overwrite" != "yes" ]; then
        echo "Setup cancelled."
        exit 0
    fi
    # Backup existing file
    cp "$ENV_FILE" "${ENV_FILE}.backup.$(date +%Y%m%d_%H%M%S)"
    echo -e "${GREEN}✓ Existing .env backed up${NC}"
fi

# Create .env file from template if template exists
if [ -f "$ENV_TEMPLATE" ]; then
    cp "$ENV_TEMPLATE" "$ENV_FILE"
    echo -e "${GREEN}✓ Created .env from template${NC}"
else
    # Create basic .env file
    cat > "$ENV_FILE" << 'EOF'
# TTelGo Backend Production Environment Variables
SPRING_PROFILES_ACTIVE=prod

# Database Configuration
DATABASE_URL=jdbc:postgresql://localhost:5432/ttelgo_prod
DB_USERNAME=ttelgo_user
DB_PASSWORD=

# Stripe Configuration
STRIPE_SECRET_KEY=
STRIPE_PUBLISHABLE_KEY=
STRIPE_WEBHOOK_SECRET=

# JWT Configuration
JWT_SECRET=
JWT_EXPIRATION=2592000000
JWT_REFRESH_EXPIRATION=5184000000

# Email Configuration
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=support@ttelgo.com
MAIL_PASSWORD=
MAIL_FROM=support@ttelgo.com

# ESIMGO API Configuration
ESIMGO_API_KEY=

# Redis Configuration
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# Server Configuration
SERVER_PORT=8080
EOF
    echo -e "${GREEN}✓ Created basic .env file${NC}"
fi

echo ""
echo -e "${YELLOW}Now you need to edit the .env file with your actual values:${NC}"
echo -e "${BLUE}  nano $ENV_FILE${NC}"
echo ""
echo -e "${YELLOW}Required values to set:${NC}"
echo "  1. DB_PASSWORD - Database password"
echo "  2. STRIPE_SECRET_KEY - Stripe secret key (sk_live_...)"
echo "  3. STRIPE_PUBLISHABLE_KEY - Stripe publishable key (pk_live_...)"
echo "  4. STRIPE_WEBHOOK_SECRET - Stripe webhook secret (whsec_...)"
echo "  5. JWT_SECRET - Secure random string (min 256 bits)"
echo "  6. MAIL_PASSWORD - Gmail app-specific password"
echo "  7. ESIMGO_API_KEY - eSIM Go API key"
echo ""

# Set proper permissions
chmod 600 "$ENV_FILE"
chown ubuntu:ubuntu "$ENV_FILE"

echo -e "${GREEN}✓ File permissions set (600 - owner read/write only)${NC}"
echo ""
echo -e "${GREEN}Setup complete! Edit the file with: nano $ENV_FILE${NC}"
