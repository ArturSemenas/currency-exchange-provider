# Currency Exchange App - AWS Terraform Deployment

## Prerequisites
- AWS CLI configured (`aws sts get-caller-identity` works)
- Terraform >= 1.0 installed
- SSH key pair at `~/.ssh/aws-currency-exchange` (+ `.pub`)

## Quick Deploy

```powershell
cd terraform
terraform fmt -recursive
terraform validate
terraform plan -out=tfplan
terraform apply tfplan
```

## Outputs
- Instance ID, Public IP/DNS
- `ssh_command` to connect
- `app_url`, `swagger_url`

## Access
- SSH: use `terraform output ssh_command`
- App: `http://<PUBLIC_IP>:8080`
- Swagger: `http://<PUBLIC_IP>:8080/swagger-ui.html`

## Cleanup
```powershell
terraform destroy
```
