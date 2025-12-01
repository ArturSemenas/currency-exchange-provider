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

## Phase 1: Prerequisites & Setup ✅

### 1.1 AWS Account Creation ✅
- [x] AWS Free Tier account created and verified
- [x] Account type: Personal
- [x] Basic Support - Free plan activated

### 1.2 Install Required Tools ✅

- [x] **AWS CLI 2.32.6 installed and configured**
  - Installation method: Direct download (AWSCLIV2.msi)
  - Verification: `aws --version` ✅

- [x] **Terraform 1.14.0 installed and added to PATH**
  - Installation: Direct download from terraform.io
  - Extracted to: C:\terraform
  - PATH updated: Terraform accessible globally
  - Verification: `terraform --version` ✅

- [x] **Git installed**
  - Already present in development environment

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

### 1.3 Create IAM User for Terraform (Automated)

- [x] **Add IAM User Resource to Terraform Configuration**
  - Created `terraform/main.tf` with IAM user resources
  - Created `terraform/outputs.tf` with sensitive outputs
  - IAM user: `terraform-deploy`
  - Policies: AmazonEC2FullAccess, AmazonVPCFullAccess

- [x] **Apply Terraform Configuration**
  ```powershell
  terraform init
  terraform apply -auto-approve
  ```
  - Resources created: 4 (IAM user, 2 policy attachments, access key)
  - IAM User ARN: `arn:aws:iam::655885323803:user/terraform-deploy`

- [x] **Configure AWS CLI with Generated Keys**
  - Credentials stored securely (not in version control)
  - Configured region: `us-east-1`
  - Output format: `json`
  - **Note**: Access keys stored in `~/.aws/credentials` (local only)

- [x] **Verify IAM User Permissions**
  - Verified with: `aws sts get-caller-identity`
  - Tested EC2 access: `aws ec2 describe-regions` ✅

### 1.4 Generate SSH Key Pair

- [x] **Create SSH Key Pair**
  - Directory: `C:\Users\a.semenas\.ssh`
  - Algorithm: RSA 2048-bit
  - Private key: `aws-currency-exchange` (1,831 bytes)
  - Public key: `aws-currency-exchange.pub` (403 bytes)
  - Fingerprint: `SHA256:Um9McrlUyW8joVAX2QCdfrSWPR8SL02ZSpLjG44rDhw`
  - Comment: `terraform-ec2-access`
  - Passphrase: None (for automation)

- [x] **Set Proper Permissions (Windows)**
  - Private key set to read-only for current user
  - Inheritance removed for security

- [x] **Verify Key Files Created**
  - Both files verified and accessible
  - Public key content validated

### 1.5 Gather API Keys

- [x] **Skipped** - Will use mock providers only
  - Mock Provider 1: Port 8091 (Fixer.io format)
  - Mock Provider 2: Port 8092 (ExchangeRatesAPI format)
  - No external API keys required for testing

---

## Phase 2: Terraform Project Setup ✅

### 2.1 Create Directory Structure

- [x] **Terraform directory created** in Phase 1.3
  - Location: `terraform/`
  - Files: `main.tf`, `variables.tf`, `outputs.tf`, `user-data.sh`, `terraform.tfvars`, `README.md`

### 2.2 Update .gitignore

- [x] **Added Terraform and AWS credential exclusions**
  - Terraform state files, `.terraform/`, `tfplan`
  - `terraform.tfvars` (contains secrets)
  - AWS credentials directories

### 2.3-2.8 Terraform Configuration Files

- [x] **All Terraform files created and validated**
  - `variables.tf`: All input variables (region, instance, SSH, passwords, API keys)
  - `main.tf`: Complete infrastructure (VPC, subnet, IGW, SG, EC2, EIP, IAM)
  - `outputs.tf`: Deployment outputs (IPs, URLs, SSH command)
  - `user-data.sh`: EC2 initialization script (Docker, git clone, docker-compose)
  - `terraform.tfvars`: Template with placeholders (gitignored)
  - `README.md`: Deployment documentation

### 2.9 GitHub Actions Deployment ✅

- [x] **Integrated Terraform into main CI pipeline**
  - Added `workflow_dispatch` trigger with `terraform_action` input
  - Manual job in `ci.yml` (only runs when manually triggered)
  - Actions: `none` (default), `plan`, `apply`, `destroy`
  - Uses GitHub Secrets for credentials
  - Configures AWS CLI, Terraform, SSH keys dynamically
  - Outputs deployment results to workflow summary

- [x] **Created `.github/GITHUB_SECRETS_SETUP.md`**
  - Step-by-step instructions for configuring GitHub Secrets
  - Required secrets: `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `SSH_PRIVATE_KEY`, `SSH_PUBLIC_KEY`, `POSTGRES_PASSWORD`, `ALLOWED_SSH_CIDR`
  - PowerShell commands to retrieve secret values
  - Security best practices
  - Instructions for triggering deployment from main CI workflow

### 2.10 Validation

- [x] **Terraform configuration validated**
  - `terraform fmt -recursive` applied successfully
  - `terraform validate` passed with no errors
  - Ready for deployment via GitHub Actions

---

**Phase 2 Status**: ✅ Complete - All Terraform files created, GitHub Actions workflow ready, validation passed

---

## Phase 3: GitHub Actions Deployment Setup ✅

### 3.1 Configure GitHub Secrets

- [x] **Configured all GitHub Secrets via GitHub CLI**
  - Used `gh secret set` command for automated configuration
  - All 6 required secrets successfully configured

- [x] **AWS Credentials**
  - `AWS_ACCESS_KEY_ID`: AKIAZRNOPZYNXPTBYS44
  - `AWS_SECRET_ACCESS_KEY`: (configured from Terraform output)

- [x] **SSH Keys**
  - `SSH_PRIVATE_KEY`: From `~/.ssh/aws-currency-exchange`
  - `SSH_PUBLIC_KEY`: From `~/.ssh/aws-currency-exchange.pub`

- [x] **Application Secrets**
  - `ALLOWED_SSH_CIDR`: 188.69.15.168/32
  - `POSTGRES_PASSWORD`: !/PCl1-!4PW>[epjyqj3 (strong 20-char password)

- [x] **Verification**
  ```powershell
  gh secret list
  ```
  - All 6 secrets listed and configured

### 3.2 Test GitHub Actions Workflow

- [ ] **Trigger manual workflow**
  ```powershell
  # Option 1: Open GitHub Actions in browser
  Start-Process "https://github.com/ArturSemenas/currency-exchange-provider/actions"
  
  # Option 2: Trigger via GitHub CLI
  gh workflow run ci.yml --ref main -f terraform_action=plan
  ```
  - Workflow: "CI - Build and Test"
  - Select branch: `main`
  - Terraform action: `plan`
  - Click "Run workflow" (or use CLI command above)

- [ ] **Review plan output**
  - Check workflow logs for Terraform plan
  - Verify resources to be created (~15 resources: VPC, subnet, IGW, RT, SG, EC2, EIP, etc.)
  - No errors in fmt, init, validate, plan steps
  - Review infrastructure changes before applying

### 3.3 Deploy to AWS via GitHub Actions

- [ ] **Run apply workflow**
  ```powershell
  # Option 1: Via browser
  Start-Process "https://github.com/ArturSemenas/currency-exchange-provider/actions"
  
  # Option 2: Via GitHub CLI
  gh workflow run ci.yml --ref main -f terraform_action=apply
  ```
  - Workflow: "CI - Build and Test"
  - Terraform action: `apply`
  - Review plan output first, then confirm deployment

- [ ] **Monitor deployment**
  ```powershell
  # Watch workflow runs
  gh run list --workflow=ci.yml
  
  # View specific run logs
  gh run view --log
  ```
  - Deployment takes ~5-10 minutes
  - Wait for all steps to complete (green checkmarks)
  - Review any errors if deployment fails

- [ ] **Retrieve deployment outputs**
  ```powershell
  # View workflow summary with Terraform outputs
  gh run view
  ```
  - Public IP address
  - SSH command
  - Application URL (http://<IP>:8080)
  - Swagger URL (http://<IP>:8080/swagger-ui.html)

- [ ] **Verify deployment**
  ```powershell
  # Test SSH access
  ssh -i ~/.ssh/aws-currency-exchange ec2-user@<PUBLIC_IP>
  
  # Test application endpoint
  Invoke-RestMethod http://<PUBLIC_IP>:8080/actuator/health
  
  # Open Swagger UI
  Start-Process http://<PUBLIC_IP>:8080/swagger-ui.html
  ```

---

**Phase 3 Status**: ✅ Complete - GitHub Secrets configured, ready for deployment

---

## Phase 4: Post-Deployment Verification

### 4.1 Wait for User Data Completion

- [ ] **Wait for initialization** (3-5 minutes after deployment)
  - User data script installs Docker, clones repo, starts containers
  - Monitor from GitHub Actions workflow logs or AWS Console
  - AWS Console: EC2 → Instances → Instance → Status checks → Wait for "2/2 checks passed"

### 4.2 SSH Connection

- [ ] **Get SSH command from GitHub Actions output**
  - View workflow summary or check outputs.txt from deployment logs
  - Format: `ssh -i ~/.ssh/aws-currency-exchange ec2-user@<PUBLIC_IP>`

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

- [ ] **Get public IP from GitHub Actions output**
  - View workflow summary or use AWS CLI
  ```powershell
  $publicIp = aws ec2 describe-instances `
    --filters "Name=tag:Name,Values=currency-exchange-app-instance" "Name=instance-state-name,Values=running" `
    --query "Reservations[0].Instances[0].PublicIpAddress" `
    --output text
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

- [ ] **Destroy via GitHub Actions**
  ```powershell
  # Trigger destroy workflow
  gh workflow run ci.yml --ref main -f terraform_action=destroy
  
  # Monitor destroy progress
  gh run list --workflow=ci.yml
  gh run view --log
  ```
  - Takes ~2-3 minutes
  - All AWS resources will be deleted

- [ ] **Alternative: Destroy via local Terraform (if needed)**
  ```powershell
  cd terraform
  terraform destroy -auto-approve
  ```

- [ ] **Verify destruction in AWS Console**
  - EC2 → Instances (should be terminated)
  - EC2 → Volumes (should be deleted)
  - EC2 → Elastic IPs (should be released)
  - VPC → Your VPCs (should be deleted)

- [ ] **Clean up GitHub Secrets (optional)**
  ```powershell
  # List all secrets
  gh secret list
  
  # Delete specific secrets if no longer needed
  gh secret delete AWS_ACCESS_KEY_ID
  gh secret delete AWS_SECRET_ACCESS_KEY
  # ... etc
  ```

---

## Troubleshooting Guide

### Issue: GitHub Actions workflow fails
**Solution:**
```powershell
# Check AWS credentials are set correctly
gh secret list

# View detailed workflow logs
gh run view --log

# Re-run failed workflow
gh run rerun <RUN_ID>
```

### Issue: AWS credentials invalid
**Solution:**
```powershell
# Verify AWS CLI access
aws sts get-caller-identity

# Update GitHub Secrets if needed
echo "NEW_ACCESS_KEY" | gh secret set AWS_ACCESS_KEY_ID
echo "NEW_SECRET_KEY" | gh secret set AWS_SECRET_ACCESS_KEY
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

- [x] ✅ AWS Free Tier account created and verified
- [x] ✅ All tools installed (AWS CLI 2.32.6, Terraform 1.14.0, GitHub CLI 2.83.1)
- [x] ✅ IAM user created (terraform-deploy)
- [x] ✅ SSH key pair generated
- [x] ✅ Terraform files created and validated
- [x] ✅ GitHub Actions workflow integrated
- [x] ✅ GitHub Secrets configured (6 secrets)
- [ ] ⏳ Infrastructure deployed via GitHub Actions
- [ ] ⏳ EC2 instance running and accessible
- [ ] ⏳ All Docker containers healthy
- [ ] ⏳ API endpoints responding correctly
- [ ] ⏳ Swagger UI accessible
- [ ] ⏳ Database populated with seed data
- [ ] ⏳ Redis cache working
- [ ] ⏳ Mock providers responding
- [ ] ⏳ Monitoring and backups configured
- [ ] ⏳ Cost alerts set up
- [ ] ⏳ Documentation completed

**Estimated Total Time:** 3-4 hours (first deployment)

**Monthly Cost:**
- Months 1-12: **$0.00** (Free Tier)
- After Month 12: **~$11.50** (if keeping running 24/7)
