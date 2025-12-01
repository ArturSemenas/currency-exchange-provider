# Outputs for Terraform deployment

output "iam_user_name" {
  description = "Name of the created IAM user"
  value       = aws_iam_user.terraform_deploy.name
}

output "iam_user_arn" {
  description = "ARN of the created IAM user"
  value       = aws_iam_user.terraform_deploy.arn
}

output "terraform_access_key_id" {
  description = "Access key ID for Terraform IAM user"
  value       = aws_iam_access_key.terraform_deploy.id
  sensitive   = true
}

output "terraform_secret_access_key" {
  description = "Secret access key for Terraform IAM user"
  value       = aws_iam_access_key.terraform_deploy.secret
  sensitive   = true
}

output "aws_configure_instructions" {
  description = "Instructions to configure AWS CLI"
  value       = <<-EOT
    Run the following commands to configure AWS CLI:
    
    aws configure set aws_access_key_id ${aws_iam_access_key.terraform_deploy.id}
    aws configure set aws_secret_access_key ${nonsensitive(aws_iam_access_key.terraform_deploy.secret)}
    aws configure set region us-east-1
    aws configure set output json
    
    Or use interactive mode:
    aws configure
  EOT
}

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
