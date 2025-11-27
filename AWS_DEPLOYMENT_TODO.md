# AWS EC2 Deployment with Terraform & Docker - TODO List

## ✅ AWS Free Tier Verification

### **Free Tier Eligible Resources (12 Months)**
| Resource | Free Tier Limit | Our Usage | Cost | Status |
|----------|----------------|-----------|------|--------|
| **EC2 t2.micro** | 750 hours/month | 720 hours/month (24/7) | $0 | ✅ FREE |
| **EBS Storage** | 30 GB General Purpose (SSD) | 20 GB | $0 | ✅ FREE |
| **Elastic IP** | 1 EIP (while attached) | 1 EIP | $0 | ✅ FREE |
| **Data Transfer OUT** | 15 GB/month | ~3-5 GB/month | $0 | ✅ FREE |
| **Data Transfer IN** | Unlimited | ~1-2 GB/month | $0 | ✅ FREE |
| **VPC** | Always free | 1 VPC | $0 | ✅ FREE |
| **Security Groups** | Always free | 1 SG | $0 | ✅ FREE |
| **CloudWatch Metrics** | 10 metrics, 10 alarms | 5 metrics, 2 alarms | $0 | ✅ FREE |
| **CloudWatch Logs** | 5 GB ingestion, 5 GB storage | ~1 GB/month | $0 | ✅ FREE |

### **Not Using (Would Exceed Free Tier)**
- ❌ RDS (managed PostgreSQL) - $15/month for db.t3.micro
- ❌ ElastiCache (managed Redis) - $12/month for cache.t2.micro  
- ❌ Application Load Balancer - $16/month + data processing
- ❌ NAT Gateway - $32/month + data processing

### **Our Cost-Saving Strategy**
✅ **Use Docker containers** on single EC2 instance instead of managed services  
✅ **PostgreSQL in Docker** instead of RDS  
✅ **Redis in Docker** instead of ElastiCache  
✅ **Direct EC2 access** instead of Load Balancer  
✅ **t2.micro instance** (1 vCPU, 1 GB RAM) - sufficient for dev/test

### **Memory Allocation (1 GB Total)**
| Container | Memory Limit | Purpose |
|-----------|--------------|---------|
| Spring Boot App | 512 MB | Main application |
| PostgreSQL | 256 MB | Database |
| Redis | 64 MB | Cache |
| Mock Provider 1 | 64 MB | Test service |
| Mock Provider 2 | 64 MB | Test service |
| System Reserved | ~40 MB | OS overhead |
| **Total** | **~1000 MB** | **Fits in 1 GB** ✅ |

### **Estimated Costs**
| Period | Cost | Notes |
|--------|------|-------|
| **Months 1-12** | **$0/month** | Within free tier limits |
| **After Month 12** | **~$11.50/month** | t2.micro + EBS + data transfer |
| **Yearly (after free tier)** | **~$138/year** | Still very cost-effective |

---

## Phase 1: Prerequisites & Setup

### 1.1 AWS Account Creation
- [ ] **Create AWS Free Tier account**
  - Go to https://aws.amazon.com/free/
  - Click "Create a Free Account"
  - Enter email address and account name
  - **Important**: Choose "Personal" account type for free tier
  - Provide contact information and credit card
  - **Note**: Credit card required for identity verification (won't be charged)
  - Verify phone number via SMS
  - Choose "Basic Support - Free" plan
  - Wait for account activation email (~5 minutes)

- [ ] **Set up Multi-Factor Authentication (MFA)**
  - Sign in to AWS Console
  - Go to IAM → Users → Your username
  - Security credentials tab
  - Click "Assign MFA device"
  - Use Google Authenticator or Authy app
  - **Security best practice** - required for production

- [ ] **Create Billing Alerts**
  - Go to Billing Dashboard → Billing preferences
  - Enable "Receive Free Tier Usage Alerts"
  - Enter email for alerts
  - Enable "Receive Billing Alerts"
  - **Critical**: Get notified before charges occur

- [ ] **Set up AWS Budget**
  - Go to AWS Budgets
  - Create budget → "Zero spend budget"
  - Alert threshold: $0.01
  - Email notification
  - **Purpose**: Immediate alert if free tier exceeded

### 1.2 Install Required Tools (Windows)

#### Install AWS CLI
- [ ] **Download AWS CLI v2**
  - Download from: https://awscli.amazonaws.com/AWSCLIV2.msi
  - Run installer (default settings)
  - Verify installation:
    ```powershell
    aws --version
    # Expected: aws-cli/2.x.x Python/3.x.x Windows/...
    ```

- [ ] **Alternative: Install via Chocolatey**
  ```powershell
  choco install awscli -y
  aws --version
  ```

#### Install Terraform
- [ ] **Download Terraform**
  - Download from: https://terraform.io/downloads
  - Extract terraform.exe to C:\terraform
  - Add to PATH environment variable
  - Verify installation:
    ```powershell
    terraform --version
    # Expected: Terraform v1.x.x
    ```

- [ ] **Alternative: Install via Chocolatey**
  ```powershell
  choco install terraform -y
  terraform --version
  ```

#### Install Git (if not already installed)
- [ ] **Verify Git installation**
  ```powershell
  git --version
  ```
- [ ] **Install if needed**
  ```powershell
  choco install git -y
  ```

### 1.3 Create IAM User for Terraform

- [ ] **Create IAM User**
  - AWS Console → IAM → Users → Add user
  - Username: `terraform-deploy`
  - Access type: ✅ Programmatic access (Access key)
  - Click Next

- [ ] **Attach Policies**
  - Attach existing policies directly:
    - ✅ `AmazonEC2FullAccess`
    - ✅ `AmazonVPCFullAccess`
  - Click Next → Create user
  - **Important**: Download CSV with credentials immediately
  - Save CSV to safe location (e.g., password manager)

- [ ] **Configure AWS CLI Credentials**
  ```powershell
  aws configure
  ```
  - AWS Access Key ID: `[from CSV]`
  - AWS Secret Access Key: `[from CSV]`
  - Default region name: `us-east-1`
  - Default output format: `json`

- [ ] **Verify AWS CLI Configuration**
  ```powershell
  aws sts get-caller-identity
  ```
  - Should show your account ID and user ARN
  - If error: Check credentials in `~/.aws/credentials`

### 1.4 Generate SSH Key Pair

- [ ] **Create SSH Key Pair**
  ```powershell
  # Navigate to .ssh directory (create if doesn't exist)
  mkdir -Force $HOME\.ssh
  cd $HOME\.ssh
  
  # Generate RSA key pair
  ssh-keygen -t rsa -b 2048 -f aws-currency-exchange -C "terraform-ec2-access"
  ```
  - Press Enter for no passphrase (or enter passphrase for extra security)
  - Creates two files:
    - `aws-currency-exchange` (private key - keep secure!)
    - `aws-currency-exchange.pub` (public key - will upload to AWS)

- [ ] **Set Proper Permissions (Windows)**
  ```powershell
  # Make private key read-only for current user
  icacls "$HOME\.ssh\aws-currency-exchange" /inheritance:r
  icacls "$HOME\.ssh\aws-currency-exchange" /grant:r "$($env:USERNAME):(R)"
  ```

- [ ] **Verify Key Files Created**
  ```powershell
  ls $HOME\.ssh\aws-currency-exchange*
  ```
  - Should show both files

### 1.5 Gather API Keys

- [ ] **Get Fixer.io API Key** (Optional - mock providers work without it)
  - Sign up at https://fixer.io/
  - Free plan: 100 requests/month
  - Copy API key from dashboard
  - Save to password manager or note securely

- [ ] **Get ExchangeRatesAPI Key** (Optional)
  - Sign up at https://exchangerate-api.com/
  - Free plan: 1,500 requests/month
  - Copy API key
  - Save securely

- [ ] **Note**: Application works with just mock providers if you don't have API keys

---

## Phase 2: Terraform Project Setup

### 2.1 Create Directory Structure

- [ ] **Create Terraform Directory**
  ```powershell
  cd "c:\Work\Study\AI Copilot\Cur_ex_app"
  mkdir terraform
  cd terraform
  ```

- [ ] **Create Empty Files**
  ```powershell
  New-Item -ItemType File main.tf
  New-Item -ItemType File variables.tf
  New-Item -ItemType File outputs.tf
  New-Item -ItemType File terraform.tfvars
  New-Item -ItemType File user-data.sh
  New-Item -ItemType File README.md
  ```

### 2.2 Update .gitignore

- [ ] **Add Terraform files to .gitignore**
  ```powershell
  # Add to root .gitignore
  Add-Content ..\.gitignore @"
  
  # Terraform
  terraform/.terraform/
  terraform/*.tfstate
  terraform/*.tfstate.backup
  terraform/.terraform.lock.hcl
  terraform/terraform.tfvars
  terraform/tfplan
  terraform/destroy-plan
  "@
  ```

### 2.3 Create variables.tf

- [ ] **Create variables.tf with all input variables**
  ```hcl
  # terraform/variables.tf
  
  variable "aws_region" {
    description = "AWS region for resources"
    type        = string
    default     = "us-east-1"
  }
  
  variable "instance_type" {
    description = "EC2 instance type (t2.micro for free tier)"
    type        = string
    default     = "t2.micro"
  }
  
  variable "ssh_public_key_path" {
    description = "Path to SSH public key file"
    type        = string
    default     = "~/.ssh/aws-currency-exchange.pub"
  }
  
  variable "allowed_ssh_cidr" {
    description = "CIDR block allowed for SSH access (your IP/32)"
    type        = string
    # Get your IP from https://whatismyip.com and append /32
  }
  
  variable "app_name" {
    description = "Application name for tagging"
    type        = string
    default     = "currency-exchange-app"
  }
  
  variable "environment" {
    description = "Environment name"
    type        = string
    default     = "production"
  }
  
  variable "postgres_password" {
    description = "PostgreSQL database password"
    type        = string
    sensitive   = true
  }
  
  variable "fixer_api_key" {
    description = "Fixer.io API key (optional)"
    type        = string
    sensitive   = true
    default     = "YOUR_KEY_HERE"
  }
  
  variable "exchangeratesapi_key" {
    description = "ExchangeRatesAPI key (optional)"
    type        = string
    sensitive   = true
    default     = "YOUR_KEY_HERE"
  }
  
  variable "root_volume_size" {
    description = "Root EBS volume size in GB (max 30 for free tier)"
    type        = number
    default     = 20
  }
  ```

### 2.4 Create main.tf

- [ ] **Create main.tf with provider configuration**
  ```hcl
  # terraform/main.tf
  
  terraform {
    required_version = ">= 1.0"
    required_providers {
      aws = {
        source  = "hashicorp/aws"
        version = "~> 5.0"
      }
    }
  }
  
  provider "aws" {
    region = var.aws_region
    
    default_tags {
      tags = {
        Environment = var.environment
        Application = var.app_name
        ManagedBy   = "Terraform"
      }
    }
  }
  ```

- [ ] **Add data source for Amazon Linux 2023 AMI**
  ```hcl
  # Get latest Amazon Linux 2023 AMI
  data "aws_ami" "amazon_linux_2023" {
    most_recent = true
    owners      = ["amazon"]
    
    filter {
      name   = "name"
      values = ["al2023-ami-*-x86_64"]
    }
    
    filter {
      name   = "virtualization-type"
      values = ["hvm"]
    }
  }
  ```

- [ ] **Create VPC and Networking**
  ```hcl
  # VPC
  resource "aws_vpc" "main" {
    cidr_block           = "10.0.0.0/16"
    enable_dns_hostnames = true
    enable_dns_support   = true
    
    tags = {
      Name = "${var.app_name}-vpc"
    }
  }
  
  # Public Subnet
  resource "aws_subnet" "public" {
    vpc_id                  = aws_vpc.main.id
    cidr_block              = "10.0.1.0/24"
    availability_zone       = "${var.aws_region}a"
    map_public_ip_on_launch = true
    
    tags = {
      Name = "${var.app_name}-public-subnet"
    }
  }
  
  # Internet Gateway
  resource "aws_internet_gateway" "main" {
    vpc_id = aws_vpc.main.id
    
    tags = {
      Name = "${var.app_name}-igw"
    }
  }
  
  # Route Table
  resource "aws_route_table" "public" {
    vpc_id = aws_vpc.main.id
    
    route {
      cidr_block = "0.0.0.0/0"
      gateway_id = aws_internet_gateway.main.id
    }
    
    tags = {
      Name = "${var.app_name}-public-rt"
    }
  }
  
  # Route Table Association
  resource "aws_route_table_association" "public" {
    subnet_id      = aws_subnet.public.id
    route_table_id = aws_route_table.public.id
  }
  ```

- [ ] **Create Security Group**
  ```hcl
  resource "aws_security_group" "app" {
    name        = "${var.app_name}-sg"
    description = "Security group for ${var.app_name}"
    vpc_id      = aws_vpc.main.id
    
    # SSH access (restrict to your IP)
    ingress {
      from_port   = 22
      to_port     = 22
      protocol    = "tcp"
      cidr_blocks = [var.allowed_ssh_cidr]
      description = "SSH access from allowed IP"
    }
    
    # Spring Boot application
    ingress {
      from_port   = 8080
      to_port     = 8080
      protocol    = "tcp"
      cidr_blocks = ["0.0.0.0/0"]
      description = "Spring Boot application"
    }
    
    # HTTP (for future nginx reverse proxy)
    ingress {
      from_port   = 80
      to_port     = 80
      protocol    = "tcp"
      cidr_blocks = ["0.0.0.0/0"]
      description = "HTTP access"
    }
    
    # Outbound - allow all
    egress {
      from_port   = 0
      to_port     = 0
      protocol    = "-1"
      cidr_blocks = ["0.0.0.0/0"]
      description = "Allow all outbound traffic"
    }
    
    tags = {
      Name = "${var.app_name}-sg"
    }
  }
  ```

- [ ] **Create SSH Key Pair**
  ```hcl
  resource "aws_key_pair" "deployer" {
    key_name   = "${var.app_name}-key"
    public_key = file(pathexpand(var.ssh_public_key_path))
    
    tags = {
      Name = "${var.app_name}-ssh-key"
    }
  }
  ```

- [ ] **Create EC2 Instance**
  ```hcl
  resource "aws_instance" "app" {
    ami           = data.aws_ami.amazon_linux_2023.id
    instance_type = var.instance_type
    
    key_name               = aws_key_pair.deployer.key_name
    vpc_security_group_ids = [aws_security_group.app.id]
    subnet_id              = aws_subnet.public.id
    
    # User data script for initialization
    user_data = templatefile("${path.module}/user-data.sh", {
      postgres_password       = var.postgres_password
      fixer_api_key          = var.fixer_api_key
      exchangeratesapi_key   = var.exchangeratesapi_key
    })
    
    # Root volume (EBS)
    root_block_device {
      volume_size           = var.root_volume_size
      volume_type           = "gp3"
      delete_on_termination = true
      
      tags = {
        Name = "${var.app_name}-root-volume"
      }
    }
    
    # Prevent accidental termination
    disable_api_termination = false
    
    tags = {
      Name = "${var.app_name}-instance"
    }
  }
  ```

- [ ] **Create Elastic IP**
  ```hcl
  resource "aws_eip" "app" {
    domain   = "vpc"
    instance = aws_instance.app.id
    
    tags = {
      Name = "${var.app_name}-eip"
    }
    
    depends_on = [aws_internet_gateway.main]
  }
  ```

### 2.5 Create outputs.tf

- [ ] **Create outputs.tf for important values**
  ```hcl
  # terraform/outputs.tf
  
  output "instance_id" {
    description = "EC2 instance ID"
    value       = aws_instance.app.id
  }
  
  output "public_ip" {
    description = "Elastic IP address"
    value       = aws_eip.app.public_ip
  }
  
  output "public_dns" {
    description = "Public DNS name"
    value       = aws_instance.app.public_dns
  }
  
  output "ssh_command" {
    description = "SSH command to connect to instance"
    value       = "ssh -i ~/.ssh/aws-currency-exchange ec2-user@${aws_eip.app.public_ip}"
  }
  
  output "app_url" {
    description = "Application URL"
    value       = "http://${aws_eip.app.public_ip}:8080"
  }
  
  output "swagger_url" {
    description = "Swagger UI URL"
    value       = "http://${aws_eip.app.public_ip}:8080/swagger-ui.html"
  }
  
  output "security_group_id" {
    description = "Security group ID"
    value       = aws_security_group.app.id
  }
  
  output "vpc_id" {
    description = "VPC ID"
    value       = aws_vpc.main.id
  }
  ```

### 2.6 Create user-data.sh

- [ ] **Create user-data.sh initialization script**
  ```bash
  #!/bin/bash
  # terraform/user-data.sh
  
  set -e  # Exit on any error
  
  # Redirect all output to log file
  exec > >(tee /var/log/user-data.log)
  exec 2>&1
  
  echo "=================================================="
  echo "Starting user-data script at $(date)"
  echo "=================================================="
  
  # Update system packages
  echo "Updating system packages..."
  dnf update -y
  
  # Install Docker
  echo "Installing Docker..."
  dnf install -y docker git wget
  systemctl start docker
  systemctl enable docker
  
  # Add ec2-user to docker group
  usermod -aG docker ec2-user
  
  # Install Docker Compose
  echo "Installing Docker Compose..."
  DOCKER_COMPOSE_VERSION="v2.24.0"
  curl -L "https://github.com/docker/compose/releases/download/$${DOCKER_COMPOSE_VERSION}/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
  chmod +x /usr/local/bin/docker-compose
  ln -sf /usr/local/bin/docker-compose /usr/bin/docker-compose
  
  # Verify installations
  echo "Verifying installations..."
  docker --version
  docker-compose --version
  
  # Clone application repository
  echo "Cloning application repository..."
  cd /home/ec2-user
  sudo -u ec2-user git clone https://github.com/ArturSemenas/currency-exchange-provider.git
  cd currency-exchange-provider
  
  # Create .env file with secrets
  echo "Creating environment file..."
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
  
  # Start Docker Compose services
  echo "Starting Docker Compose services..."
  docker-compose up -d
  
  # Wait for services to be healthy
  echo "Waiting for services to start..."
  sleep 45
  
  # Check service status
  echo "Checking service status..."
  docker-compose ps
  
  # Show application logs
  echo "Application logs (last 30 lines):"
  docker-compose logs --tail=30 currency-exchange-app
  
  # Create systemd service for auto-start on reboot
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
  
  # Create backup script
  echo "Creating backup script..."
  cat > /home/ec2-user/backup-db.sh << 'BACKUPEOF'
  #!/bin/bash
  BACKUP_DIR="/home/ec2-user/backups"
  DATE=$(date +%Y%m%d_%H%M%S)
  
  mkdir -p $BACKUP_DIR
  docker exec currency-exchange-db pg_dump -U postgres currency_exchange_db > $BACKUP_DIR/db_backup_$DATE.sql
  
  # Keep only last 7 backups
  ls -t $BACKUP_DIR/db_backup_*.sql | tail -n +8 | xargs -r rm -f
  
  echo "Backup completed: $BACKUP_DIR/db_backup_$DATE.sql"
  BACKUPEOF
  
  chmod +x /home/ec2-user/backup-db.sh
  chown ec2-user:ec2-user /home/ec2-user/backup-db.sh
  
  # Create update script
  echo "Creating update script..."
  cat > /home/ec2-user/update-app.sh << 'UPDATEEOF'
  #!/bin/bash
  cd /home/ec2-user/currency-exchange-provider
  
  echo "Pulling latest code..."
  git pull origin main
  
  echo "Rebuilding containers..."
  docker-compose down
  docker-compose build --no-cache
  docker-compose up -d
  
  echo "Waiting for services..."
  sleep 30
  
  echo "Service status:"
  docker-compose ps
  UPDATEEOF
  
  chmod +x /home/ec2-user/update-app.sh
  chown ec2-user:ec2-user /home/ec2-user/update-app.sh
  
  # Get public IP
  PUBLIC_IP=$(curl -s http://169.254.169.254/latest/meta-data/public-ipv4)
  
  echo "=================================================="
  echo "User-data script completed at $(date)"
  echo "Application URL: http://$PUBLIC_IP:8080"
  echo "Swagger UI: http://$PUBLIC_IP:8080/swagger-ui.html"
  echo "=================================================="
  ```

### 2.7 Create terraform.tfvars

- [ ] **Get your public IP address**
  - Visit https://whatismyip.com/
  - Note your IPv4 address (e.g., 203.0.113.42)

- [ ] **Create terraform.tfvars with your values**
  ```hcl
  # terraform/terraform.tfvars
  # WARNING: This file contains sensitive data - added to .gitignore
  
  aws_region              = "us-east-1"
  instance_type           = "t2.micro"
  ssh_public_key_path     = "~/.ssh/aws-currency-exchange.pub"
  allowed_ssh_cidr        = "YOUR_IP_HERE/32"  # Replace with your IP from whatismyip.com
  postgres_password       = "your_secure_password_here_min_12_chars"
  fixer_api_key          = "YOUR_FIXER_KEY_OR_LEAVE_DEFAULT"
  exchangeratesapi_key   = "YOUR_EXCHANGERATESAPI_KEY_OR_LEAVE_DEFAULT"
  root_volume_size        = 20  # GB (max 30 for free tier)
  ```
  
  **Replace**:
  - `YOUR_IP_HERE` with your IP from whatismyip.com
  - `your_secure_password_here_min_12_chars` with strong password
  - API keys if you have them (otherwise mock providers will work)

### 2.8 Create README.md

- [ ] **Create terraform/README.md with documentation**
  ```markdown
  # Currency Exchange App - AWS Terraform Deployment
  
  ## Free Tier Resources
  - EC2 t2.micro instance (1 vCPU, 1 GB RAM)
  - 20 GB EBS storage
  - 1 Elastic IP
  - All within AWS Free Tier for 12 months
  
  ## Prerequisites
  - AWS CLI configured with credentials
  - Terraform >= 1.0
  - SSH key pair generated
  
  ## Quick Deploy
  
  1. Initialize Terraform:
     ```bash
     terraform init
     ```
  
  2. Review plan:
     ```bash
     terraform plan
     ```
  
  3. Apply configuration:
     ```bash
     terraform apply
     ```
  
  4. Get outputs:
     ```bash
     terraform output
     ```
  
  ## Accessing the Application
  
  - SSH: `terraform output ssh_command`
  - App: `terraform output app_url`
  - Swagger: `terraform output swagger_url`
  
  ## Cleanup
  
  ```bash
  terraform destroy
  ```
  
  ## Cost Estimate
  - First 12 months: $0/month (Free Tier)
  - After 12 months: ~$11.50/month
  ```

---

## Phase 3: Terraform Deployment

### 3.1 Initialize Terraform

- [ ] **Navigate to terraform directory**
  ```powershell
  cd "c:\Work\Study\AI Copilot\Cur_ex_app\terraform"
  ```

- [ ] **Initialize Terraform**
  ```powershell
  terraform init
  ```
  - Downloads AWS provider plugins
  - Creates `.terraform` directory
  - Creates `.terraform.lock.hcl` file
  - Expected output: "Terraform has been successfully initialized!"

- [ ] **Verify initialization**
  ```powershell
  ls .terraform
  ```
  - Should show providers directory

### 3.2 Validate Configuration

- [ ] **Format Terraform files**
  ```powershell
  terraform fmt -recursive
  ```
  - Formats all .tf files to canonical format

- [ ] **Validate syntax**
  ```powershell
  terraform validate
  ```
  - Expected: "Success! The configuration is valid."
  - If errors: Fix syntax issues and re-run

- [ ] **Check for common issues**
  - Verify terraform.tfvars has your IP address (not YOUR_IP_HERE)
  - Verify SSH public key path is correct
  - Verify all sensitive values are set

### 3.3 Plan Infrastructure

- [ ] **Create execution plan**
  ```powershell
  terraform plan -out=tfplan
  ```
  - Reviews what will be created
  - Saves plan to tfplan file
  - Expected: ~10-12 resources to be created

- [ ] **Review plan output**
  - Check resources being created:
    - ✅ 1 VPC
    - ✅ 1 Subnet
    - ✅ 1 Internet Gateway
    - ✅ 1 Route Table + Association
    - ✅ 1 Security Group
    - ✅ 1 SSH Key Pair
    - ✅ 1 EC2 Instance (t2.micro)
    - ✅ 1 Elastic IP
  - Verify no unexpected charges

- [ ] **Save plan details for review**
  ```powershell
  terraform show tfplan > plan-review.txt
  ```
  - Review plan-review.txt in detail
  - Verify instance_type = "t2.micro"
  - Verify ami is Amazon Linux 2023
  - Verify root volume size ≤ 30 GB

### 3.4 Apply Infrastructure

- [ ] **Apply Terraform configuration**
  ```powershell
  terraform apply tfplan
  ```
  - No prompt needed (plan already confirmed)
  - Takes ~3-5 minutes to complete
  - Watch for any errors

- [ ] **Monitor creation progress**
  - Creating VPC... ✓
  - Creating subnet... ✓
  - Creating internet gateway... ✓
  - Creating route table... ✓
  - Creating security group... ✓
  - Creating key pair... ✓
  - Creating EC2 instance... ✓ (takes 2-3 min)
  - Creating Elastic IP... ✓

- [ ] **Capture outputs**
  ```powershell
  terraform output > outputs.txt
  notepad outputs.txt
  ```
  - Save public_ip, ssh_command, app_url, swagger_url

- [ ] **Note deployment time**
  - Record when deployment completed
  - User data script takes ~3-5 minutes after instance creation

---

## Phase 4: Post-Deployment Verification

### 4.1 Wait for User Data Completion

- [ ] **Wait for initialization** (3-5 minutes after terraform apply)
  - User data script installs Docker, clones repo, starts containers
  - Monitor from AWS Console: EC2 → Instances → Instance → Status checks
  - Wait for "2/2 checks passed"

### 4.2 SSH Connection

- [ ] **Get SSH command from output**
  ```powershell
  terraform output ssh_command
  ```

- [ ] **Connect via SSH**
  ```powershell
  # Copy command from output
  ssh -i ~/.ssh/aws-currency-exchange ec2-user@<PUBLIC_IP>
  ```
  - First time: Type "yes" to accept host key
  - Should connect successfully
  - If connection refused: Wait 1-2 more minutes

- [ ] **Check user-data log**
  ```bash
  sudo tail -100 /var/log/user-data.log
  ```
  - Should show "User-data script completed"
  - Look for any errors

- [ ] **Check Docker installation**
  ```bash
  docker --version
  docker-compose --version
  docker ps
  ```
  - Should show 6 containers running

### 4.3 Verify Docker Containers

- [ ] **Navigate to app directory**
  ```bash
  cd /home/ec2-user/currency-exchange-provider
  ```

- [ ] **Check all containers**
  ```bash
  docker-compose ps
  ```
  - Expected containers:
    - ✅ currency-exchange-app (healthy)
    - ✅ currency-exchange-db (postgres, healthy)
    - ✅ currency-exchange-redis (healthy)
    - ✅ mock-provider-1 (healthy)
    - ✅ mock-provider-2 (healthy)
    - ✅ pgadmin (healthy)

- [ ] **Check application logs**
  ```bash
  docker-compose logs currency-exchange-app | tail -50
  ```
  - Look for "Started CurrencyExchangeProviderApplication"
  - Should show no errors

- [ ] **Check database**
  ```bash
  docker exec -it currency-exchange-db psql -U postgres -d currency_exchange_db -c "\dt"
  ```
  - Should show tables: currencies, exchange_rates, users, roles, user_roles

- [ ] **Check Redis**
  ```bash
  docker exec -it currency-exchange-redis redis-cli ping
  ```
  - Should respond: PONG

### 4.4 Test API Endpoints (from local machine)

- [ ] **Get public IP**
  ```powershell
  $publicIp = (terraform output -raw public_ip)
  ```

- [ ] **Test health endpoint**
  ```powershell
  Invoke-RestMethod -Uri "http://${publicIp}:8080/actuator/health"
  ```
  - Expected: `{"status":"UP"}`

- [ ] **Access Swagger UI**
  - Open browser: `http://<PUBLIC_IP>:8080/swagger-ui.html`
  - Should load API documentation

- [ ] **Test public endpoint**
  ```powershell
  Invoke-RestMethod -Uri "http://${publicIp}:8080/api/v1/currencies"
  ```
  - Should return empty array [] or list of currencies

- [ ] **Test authenticated endpoint**
  ```powershell
  $headers = @{
      'Authorization' = 'Basic ' + [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("admin:admin123"))
  }
  Invoke-RestMethod -Uri "http://${publicIp}:8080/api/v1/currencies?currency=USD" -Method POST -Headers $headers
  ```
  - Should return currency object or 409 if already exists

### 4.5 Verify AWS Console

- [ ] **Check EC2 Dashboard**
  - AWS Console → EC2 → Instances
  - Verify instance state: "running"
  - Check CPU utilization (should be < 20%)
  - Check network activity

- [ ] **Check Elastic IP**
  - EC2 → Elastic IPs
  - Verify IP is allocated and associated
  - Note: Free while attached to running instance

- [ ] **Check Security Group**
  - EC2 → Security Groups
  - Verify rules:
    - SSH (22) from your IP
    - HTTP (8080) from anywhere
    - HTTP (80) from anywhere

- [ ] **Check Free Tier Usage**
  - Billing Dashboard → Free Tier
  - Verify EC2 hours usage
  - Verify EBS storage usage
  - Should be well within limits

---

## Phase 5: Monitoring & Maintenance

### 5.1 Set Up CloudWatch Monitoring

- [ ] **View instance metrics**
  - EC2 → Instances → Select instance → Monitoring tab
  - Metrics: CPU, Network, Disk
  - All should show normal activity

- [ ] **Create CloudWatch alarm for high CPU**
  ```powershell
  aws cloudwatch put-metric-alarm `
    --alarm-name currency-exchange-high-cpu `
    --alarm-description "Alert when CPU exceeds 80%" `
    --metric-name CPUUtilization `
    --namespace AWS/EC2 `
    --statistic Average `
    --period 300 `
    --threshold 80 `
    --comparison-operator GreaterThanThreshold `
    --evaluation-periods 2
  ```

- [ ] **Create alarm for status check failure**
  ```powershell
  aws cloudwatch put-metric-alarm `
    --alarm-name currency-exchange-status-check `
    --alarm-description "Alert on instance status check failure" `
    --metric-name StatusCheckFailed `
    --namespace AWS/EC2 `
    --statistic Maximum `
    --period 300 `
    --threshold 1 `
    --comparison-operator GreaterThanOrEqualToThreshold `
    --evaluation-periods 1
  ```

### 5.2 Database Backups

- [ ] **Test backup script**
  ```bash
  ssh -i ~/.ssh/aws-currency-exchange ec2-user@<PUBLIC_IP>
  ./backup-db.sh
  ls -lh backups/
  ```
  - Should create backup file

- [ ] **Schedule daily backups via cron**
  ```bash
  crontab -e
  # Add line:
  0 2 * * * /home/ec2-user/backup-db.sh >> /home/ec2-user/backup.log 2>&1
  ```
  - Runs daily at 2 AM UTC

### 5.3 Create EBS Snapshot

- [ ] **Create manual snapshot**
  - EC2 → Volumes → Select volume
  - Actions → Create snapshot
  - Description: "Initial backup after deployment"
  - Create snapshot

- [ ] **Verify snapshot created**
  - EC2 → Snapshots
  - Should show snapshot with "completed" status

### 5.4 Document Important Information

- [ ] **Create infrastructure document**
  ```
  AWS Currency Exchange Deployment Info
  =====================================
  Deployment Date: [DATE]
  Public IP: [IP FROM terraform output]
  Instance ID: [ID FROM terraform output]
  SSH Key: ~/.ssh/aws-currency-exchange
  
  URLs:
  - Application: http://[IP]:8080
  - Swagger UI: http://[IP]:8080/swagger-ui.html
  - Health: http://[IP]:8080/actuator/health
  
  Credentials:
  - PostgreSQL: postgres / [password from tfvars]
  - Admin User: admin / admin123
  - Premium User: premium / admin123
  - Regular User: user / admin123
  
  SSH Access:
  ssh -i ~/.ssh/aws-currency-exchange ec2-user@[PUBLIC_IP]
  
  Backup Location: /home/ec2-user/backups/
  
  Free Tier Expiration: [12 months from deployment]
  ```

---

## Phase 6: Testing & Validation

### 6.1 Functional Testing

- [ ] **Test all API endpoints** (use Swagger UI or PowerShell)
  - GET /api/v1/currencies
  - POST /api/v1/currencies (admin)
  - GET /api/v1/currencies/exchange-rates
  - POST /api/v1/currencies/refresh (admin)
  - GET /api/v1/currencies/trends (premium/admin)

- [ ] **Test authentication**
  - User role (should fail on admin endpoints)
  - Premium role (access trends, not admin)
  - Admin role (full access)

- [ ] **Test rate aggregation**
  ```bash
  ssh ec2-user@<PUBLIC_IP>
  docker-compose logs currency-exchange-app | grep "provider"
  ```
  - Should see rates from all 4 providers

### 6.2 Performance Testing

- [ ] **Monitor resource usage**
  ```bash
  docker stats
  ```
  - Check memory usage (should be < 900 MB total)
  - Check CPU usage (should be < 50% normally)

- [ ] **Test concurrent requests** (from local machine)
  ```powershell
  1..10 | ForEach-Object -Parallel {
      Invoke-RestMethod -Uri "http://<PUBLIC_IP>:8080/api/v1/currencies"
  } -ThrottleLimit 10
  ```
  - Should handle 10 concurrent requests
  - Check response time (should be < 2 seconds)

### 6.3 Reliability Testing

- [ ] **Test container restart**
  ```bash
  docker-compose restart currency-exchange-app
  docker-compose logs -f currency-exchange-app
  ```
  - Should restart cleanly within 30 seconds

- [ ] **Test instance reboot**
  ```powershell
  aws ec2 reboot-instances --instance-ids <INSTANCE_ID>
  ```
  - Wait 2-3 minutes
  - Containers should auto-start via systemd service
  - Verify: `ssh` and `docker-compose ps`

---

## Phase 7: Cost Optimization

### 7.1 Monitor Free Tier Usage

- [ ] **Check daily usage**
  - Billing Dashboard → Free Tier
  - EC2 hours: Should be ~24 hours/day = 720 hours/month
  - EBS storage: Should be 20 GB
  - Data transfer: Track monthly usage

- [ ] **Set up cost alerts**
  - Already done in Phase 1.1
  - Check email for alerts
  - Review weekly

### 7.2 Optimize Resources

- [ ] **Clean up unused Docker images**
  ```bash
  ssh ec2-user@<PUBLIC_IP>
  docker system prune -a --volumes -f
  ```
  - Frees up disk space

- [ ] **Adjust container memory limits** (if needed)
  - Edit docker-compose.yml
  - Add mem_limit directives
  - Restart containers

### 7.3 Plan for Post-Free Tier

- [ ] **Calculate monthly costs after 12 months**
  - t2.micro: $8.47/month (us-east-1)
  - EBS 20 GB: $2.00/month
  - Data transfer: ~$0.90/month (estimated)
  - **Total: ~$11.37/month**

- [ ] **Consider alternatives if needed**
  - Lightsail: $3.50/month for similar specs
  - Stop instance when not in use
  - Use scheduled scaling (stop at night)

---

## Phase 8: Cleanup / Destroy

### 8.1 Before Destroying

- [ ] **Create final backup**
  ```bash
  ssh ec2-user@<PUBLIC_IP>
  ./backup-db.sh
  ```

- [ ] **Download backup to local**
  ```powershell
  scp -i ~/.ssh/aws-currency-exchange ec2-user@<PUBLIC_IP>:/home/ec2-user/backups/db_backup_*.sql .
  ```

- [ ] **Export important data** (if needed)
  - Currency list
  - Exchange rate history
  - Configuration files

### 8.2 Destroy Infrastructure

- [ ] **Review what will be destroyed**
  ```powershell
  cd "c:\Work\Study\AI Copilot\Cur_ex_app\terraform"
  terraform plan -destroy -out=destroy-plan
  terraform show destroy-plan
  ```

- [ ] **Destroy all resources**
  ```powershell
  terraform apply destroy-plan
  ```
  - Confirm: type "yes"
  - Takes ~2-3 minutes
  - All AWS resources will be deleted

- [ ] **Verify destruction in AWS Console**
  - EC2 → Instances (should be terminated)
  - EC2 → Volumes (should be deleted)
  - EC2 → Elastic IPs (should be released)
  - VPC → Your VPCs (should be deleted)

- [ ] **Clean up local files**
  ```powershell
  rm terraform.tfstate*
  rm tfplan destroy-plan
  rm plan-review.txt outputs.txt
  ```

---

## Troubleshooting Guide

### Issue: terraform init fails
**Solution:**
```powershell
# Check AWS credentials
aws sts get-caller-identity

# Re-configure if needed
aws configure

# Try init again
terraform init
```

### Issue: SSH connection refused
**Causes:**
- Instance still booting (wait 2-3 minutes)
- Security group blocking your IP
- Wrong SSH key

**Solution:**
```powershell
# Check instance status
aws ec2 describe-instance-status --instance-ids <ID>

# Verify your current IP
Invoke-RestMethod -Uri https://api.ipify.org

# Update security group if IP changed
terraform apply -replace="aws_security_group.app"
```

### Issue: Containers not starting
**Solution:**
```bash
ssh ec2-user@<PUBLIC_IP>

# Check user-data log
sudo tail -100 /var/log/user-data.log

# Check Docker service
sudo systemctl status docker

# Manually start containers
cd /home/ec2-user/currency-exchange-provider
docker-compose up -d

# Check logs
docker-compose logs
```

### Issue: Application not accessible on port 8080
**Solution:**
```bash
# Check if app is running
docker-compose ps

# Check application logs
docker-compose logs currency-exchange-app

# Check if port is listening
netstat -tlnp | grep 8080

# Check security group
aws ec2 describe-security-groups --group-ids <SG_ID>
```

### Issue: Out of memory
**Solution:**
```bash
# Check memory usage
free -h
docker stats

# Restart containers with memory limits
docker-compose down
# Edit docker-compose.yml to add mem_limit
docker-compose up -d
```

---

## Success Checklist

- [ ] ✅ AWS Free Tier account created and verified
- [ ] ✅ All tools installed (AWS CLI, Terraform, SSH)
- [ ] ✅ Terraform files created and configured
- [ ] ✅ Infrastructure deployed successfully
- [ ] ✅ EC2 instance running and accessible
- [ ] ✅ All Docker containers healthy
- [ ] ✅ API endpoints responding correctly
- [ ] ✅ Swagger UI accessible
- [ ] ✅ Database populated with seed data
- [ ] ✅ Redis cache working
- [ ] ✅ Mock providers responding
- [ ] ✅ Monitoring and backups configured
- [ ] ✅ Cost alerts set up
- [ ] ✅ Documentation completed

**Estimated Total Time:** 3-4 hours (first deployment)

**Monthly Cost:**
- Months 1-12: **$0.00** (Free Tier)
- After Month 12: **~$11.50** (if keeping running 24/7)
