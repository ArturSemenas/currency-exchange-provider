#!/bin/bash
# terraform/user-data.sh

set -e
exec > >(tee /var/log/user-data.log)
exec 2>&1

echo "=== User-data start $(date) ==="

dnf update -y

dnf install -y docker git wget
systemctl enable docker
systemctl start docker

usermod -aG docker ec2-user

DOCKER_COMPOSE_VERSION="v2.24.0"
curl -L "https://github.com/docker/compose/releases/download/$${DOCKER_COMPOSE_VERSION}/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose
ln -sf /usr/local/bin/docker-compose /usr/bin/docker-compose

echo "Docker versions:"
docker --version
docker-compose --version

echo "Setting up SSH key for GitHub..."
mkdir -p /home/ec2-user/.ssh
echo "${github_deploy_key_b64}" | base64 -d > /home/ec2-user/.ssh/github-deploy-key
chmod 600 /home/ec2-user/.ssh/github-deploy-key
chown ec2-user:ec2-user /home/ec2-user/.ssh/github-deploy-key

# Configure git to use the deploy key
cat > /home/ec2-user/.ssh/config << 'SSHCFG'
Host github.com
  HostName github.com
  User git
  IdentityFile ~/.ssh/github-deploy-key
  StrictHostKeyChecking no
SSHCFG
chmod 600 /home/ec2-user/.ssh/config
chown ec2-user:ec2-user /home/ec2-user/.ssh/config

echo "Cloning repository..."
cd /home/ec2-user
sudo -u ec2-user git clone git@github.com:ArturSemenas/currency-exchange-provider.git
cd currency-exchange-provider

# Create environment file (mock providers)
cat > .env << 'ENVEOF'
POSTGRES_USER=postgres
POSTGRES_PASSWORD=${postgres_password}
POSTGRES_DB=currency_exchange_db
FIXER_API_KEY=${fixer_api_key}
EXCHANGERATESAPI_KEY=${exchangeratesapi_key}
MOCK_PROVIDER_1_URL=http://mock-provider-1:8091
MOCK_PROVIDER_2_URL=http://mock-provider-2:8092
ENVEOF

chown ec2-user:ec2-user .env
chmod 600 .env

echo "Starting docker-compose..."
docker-compose up -d

sleep 45

docker-compose ps

echo "Creating systemd service..."
cat > /etc/systemd/system/currency-exchange.service << 'SVCEOF'
[Unit]
Description=Currency Exchange Application
Requires=docker.service
After=docker.service

[Service]
Type=oneshot
RemainAfterExit=yes
WorkingDirectory=/home/ec2-user/currency-exchange-provider
ExecStart=/usr/local/bin/docker-compose up -d
ExecStop=/usr/local/bin/docker-compose down
User=ec2-user

[Install]
WantedBy=multi-user.target
SVCEOF

systemctl daemon-reload
systemctl enable currency-exchange.service

PUBLIC_IP=$(curl -s http://169.254.169.254/latest/meta-data/public-ipv4)

echo "App: http://$${PUBLIC_IP}:8080"
echo "Swagger: http://$${PUBLIC_IP}:8080/swagger-ui.html"
echo "=== User-data end $(date) ==="
