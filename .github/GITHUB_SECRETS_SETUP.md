# GitHub Actions Secrets Setup for Terraform AWS Deployment

This document explains how to configure GitHub Secrets for automated AWS deployment via the main CI pipeline.

## Required Secrets

Navigate to your repository: **Settings → Secrets and variables → Actions → New repository secret**

### AWS Credentials
- **`AWS_ACCESS_KEY_ID`**: IAM user access key ID (from Terraform output or AWS Console)
- **`AWS_SECRET_ACCESS_KEY`**: IAM user secret access key (from Terraform output)

### SSH Keys
- **`SSH_PRIVATE_KEY`**: Content of `~/.ssh/aws-currency-exchange` (private key)
- **`SSH_PUBLIC_KEY`**: Content of `~/.ssh/aws-currency-exchange.pub` (public key)

### Application Secrets
- **`POSTGRES_PASSWORD`**: Strong database password (16+ characters)
- **`ALLOWED_SSH_CIDR`**: Your IP address in CIDR format (e.g., `203.0.113.42/32`)

## Getting Secret Values

### Get your public IP with CIDR notation:
```powershell
$ip = Invoke-RestMethod https://api.ipify.org
Write-Output "$ip/32"
```

### Generate strong password:
```powershell
Add-Type -AssemblyName System.Web
[System.Web.Security.Membership]::GeneratePassword(16,4)
```

### Get SSH keys:
```powershell
# Private key
Get-Content $HOME\.ssh\aws-currency-exchange -Raw

# Public key
Get-Content $HOME\.ssh\aws-currency-exchange.pub -Raw
```

### Get AWS credentials:
```powershell
cd terraform
terraform output -raw terraform_access_key_id
terraform output -raw terraform_secret_access_key
```

## Workflow Trigger

The Terraform deployment is integrated into the main CI pipeline as a manual job:

### To deploy infrastructure:
1. Go to **Actions** tab
2. Select **CI - Build and Test** workflow
3. Click **Run workflow**
4. Select branch: `main`
5. Choose Terraform action from dropdown:
   - `none` - Skip deployment (default for regular CI runs)
   - `plan` - Preview infrastructure changes
   - `apply` - Deploy infrastructure to AWS
   - `destroy` - Remove all AWS resources
6. Click "Run workflow"

### Workflow behavior:
- **Regular push/PR**: Runs tests and quality checks only (no Terraform)
- **Manual trigger with action**: Runs tests + Terraform deployment
- **Terraform job**: Only runs when manually triggered with non-'none' action

## Security Notes
- Never commit secrets to Git
- Rotate credentials regularly
- Use least-privilege IAM policies
- Enable MFA on AWS root account
- Review CloudTrail logs periodically
