#!/bin/bash
# Database Backup Script for TTelGo
# Usage: ./backup-database.sh [database_name] [backup_directory]

set -e

# Configuration
DB_NAME=${1:-ttelgo_prod}
BACKUP_DIR=${2:-/home/ubuntu/backups}
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="${BACKUP_DIR}/ttelgo_db_backup_${TIMESTAMP}.sql"
BACKUP_FILE_COMPRESSED="${BACKUP_FILE}.gz"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}TTelGo Database Backup${NC}"
echo -e "${GREEN}========================================${NC}"
echo "Database: $DB_NAME"
echo "Backup directory: $BACKUP_DIR"
echo ""

# Create backup directory if it doesn't exist
mkdir -p "$BACKUP_DIR"

# Check if PostgreSQL is running
if ! pg_isready -U postgres > /dev/null 2>&1; then
    echo -e "${RED}ERROR: PostgreSQL is not running or not accessible${NC}"
    exit 1
fi

# Perform backup
echo -e "${YELLOW}Creating database backup...${NC}"
PGPASSWORD="${DB_PASSWORD:-postgres}" pg_dump -U postgres -h localhost -F c -b -v -f "${BACKUP_FILE}.dump" "$DB_NAME" 2>&1 | tee /tmp/backup.log

# Also create SQL dump for easy inspection
echo -e "${YELLOW}Creating SQL dump...${NC}"
PGPASSWORD="${DB_PASSWORD:-postgres}" pg_dump -U postgres -h localhost -F p -f "$BACKUP_FILE" "$DB_NAME" 2>&1 | tee -a /tmp/backup.log

# Compress SQL dump
echo -e "${YELLOW}Compressing backup...${NC}"
gzip -f "$BACKUP_FILE"

# Get file size
BACKUP_SIZE=$(du -h "${BACKUP_FILE_COMPRESSED}" | cut -f1)

echo ""
echo -e "${GREEN}✓ Backup completed successfully!${NC}"
echo "Backup file: ${BACKUP_FILE_COMPRESSED}"
echo "Size: $BACKUP_SIZE"
echo ""

# List recent backups
echo -e "${YELLOW}Recent backups:${NC}"
ls -lh "$BACKUP_DIR"/*.sql.gz 2>/dev/null | tail -5 || echo "No previous backups found"

# Cleanup old backups (keep last 7 days)
echo ""
echo -e "${YELLOW}Cleaning up old backups (keeping last 7 days)...${NC}"
find "$BACKUP_DIR" -name "ttelgo_db_backup_*.sql.gz" -mtime +7 -delete
echo -e "${GREEN}✓ Cleanup completed${NC}"
