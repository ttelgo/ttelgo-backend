#!/bin/bash
# Fix systemd service to properly load JWT_SECRET

sudo tee /etc/systemd/system/ttelgo-backend.service > /dev/null << 'EOF'
[Unit]
Description=TTelGo Backend Service
After=network.target postgresql.service

[Service]
Type=simple
User=ubuntu
WorkingDirectory=/home/ubuntu/ttelgo-backend
Environment="JWT_SECRET=TtelGo2026SecureJWTSecretKeyForProductionUseMin256BitsRequiredForHS256Algorithm"
Environment="JWT_EXPIRATION=2592000000"
Environment="JWT_REFRESH_EXPIRATION=5184000000"
EnvironmentFile=/home/ubuntu/ttelgo-backend/.env
ExecStart=/usr/bin/java -jar -Dspring.profiles.active=prod /home/ubuntu/ttelgo-backend/ttelgo-backend-0.0.1-SNAPSHOT.jar
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
EOF

sudo systemctl daemon-reload
sudo systemctl restart ttelgo-backend
echo "Service updated and restarted"

