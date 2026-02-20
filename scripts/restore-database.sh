#!/bin/bash
# Database Restore Script for TTelGo
# Usage: ./restore-database.sh [backup_file] [database_name]

set -e

# Configuration
BACKUP_FILE=${1}
DB_NAME=${2:-ttelgo_prod}

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}TTelGo Database Restore${NC}"
echo -e "${GREEN}========================================${NC}"

# Validate backup file
if [ -z "$BACKUP_FILE" ]; then
    echo -e "${RED}ERROR: Backup file not specified${NC}"
    echo "Usage: ./restore-database.sh [backup_file] [database_name]"
    exit 1
fi

if [ ! -f "$BACKUP_FILE" ]; then
    echo -e "${RED}ERROR: Backup file not found: $BACKUP_FILE${NC}"
    exit 1
fi

# Check if PostgreSQL is running
if ! pg_isready -U postgres > /dev/null 2>&1; then
    echo -e "${RED}ERROR: PostgreSQL is not running or not accessible${NC}"
    exit 1
fi

# Confirm restore
echo -e "${YELLOW}WARNING: This will overwrite the database: $DB_NAME${NC}"
echo "Backup file: $BACKUP_FILE"
read -p "Are you sure you want to continue? (yes/no): " confirm

if [ "$confirm" != "yes" ]; then
    echo "Restore cancelled."
    exit 0
fi

# Determine backup type and restore
if [[ "$BACKUP_FILE" == *.dump ]]; then
    # Custom format backup
    echo -e "${YELLOW}Restoring from custom format backup...${NC}"
    PGPASSWORD="${DB_PASSWORD:-postgres}" pg_restore -U postgres -h localhost -d "$DB_NAME" -c -v "$BACKUP_FILE" 2>&1 | tee /tmp/restore.log
elif [[ "$BACKUP_FILE" == *.gz ]]; then
    # Compressed SQL dump
    echo -e "${YELLOW}Restoring from compressed SQL dump...${NC}"
    gunzip -c "$BACKUP_FILE" | PGPASSWORD="${DB_PASSWORD:-postgres}" psql -U postgres -h localhost -d "$DB_NAME" 2>&1 | tee /tmp/restore.log
else
    # Plain SQL dump
    echo -e "${YELLOW}Restoring from SQL dump...${NC}"
    PGPASSWORD="${DB_PASSWORD:-postgres}" psql -U postgres -h localhost -d "$DB_NAME" < "$BACKUP_FILE" 2>&1 | tee /tmp/restore.log
fi

echo ""
echo -e "${GREEN}✓ Database restore completed!${NC}"
