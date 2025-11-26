# API Examples - Currency Exchange Provider

Complete examples for all API endpoints with curl and PowerShell commands.

## Table of Contents

1. [Authentication](#authentication)
2. [Currency Management](#currency-management)
3. [Exchange Rates](#exchange-rates)
4. [Trend Analysis](#trend-analysis)
5. [Admin Operations](#admin-operations)
6. [Error Scenarios](#error-scenarios)

---

## Authentication

The API uses HTTP Basic Authentication. Include credentials in the `Authorization` header.

### Test Users

| Username | Password | Authority | Access |
|----------|----------|-----------|--------|
| `user` | `admin123` | USER | Public endpoints only |
| `premium` | `admin123` | PREMIUM_USER | Public + Trends |
| `admin` | `admin123` | ADMIN | All endpoints |

### Authentication Examples

**curl:**
```bash
# Using -u flag
curl -u admin:admin123 http://localhost:8080/api/v1/currencies

# Using Authorization header
curl -H "Authorization: Basic YWRtaW46YWRtaW4xMjM=" \
  http://localhost:8080/api/v1/currencies
```

**PowerShell:**
```powershell
# Create headers with credentials
$headers = @{
    'Authorization' = 'Basic YWRtaW46YWRtaW4xMjM='  # admin:admin123
}

Invoke-RestMethod -Uri 'http://localhost:8080/api/v1/currencies' -Headers $headers
```

**Base64 Encoding Credentials:**
```bash
# admin:admin123
echo -n 'admin:admin123' | base64
# Result: YWRtaW46YWRtaW4xMjM=

# premium:admin123
echo -n 'premium:admin123' | base64
# Result: cHJlbWl1bTphZG1pbjEyMw==

# user:admin123
echo -n 'user:admin123' | base64
# Result: dXNlcjphZG1pbjEyMw==
```

---

## Currency Management

### 1. Get All Currencies

**Endpoint:** `GET /api/v1/currencies`  
**Access:** Public (no authentication required)

**curl:**
```bash
curl http://localhost:8080/api/v1/currencies
```

**PowerShell:**
```powershell
Invoke-RestMethod -Uri 'http://localhost:8080/api/v1/currencies'
```

**Response:**
```json
[
  {
    "code": "USD",
    "name": "US Dollar"
  },
  {
    "code": "EUR",
    "name": "Euro"
  },
  {
    "code": "GBP",
    "name": "British Pound"
  }
]
```

---

### 2. Add New Currency

**Endpoint:** `POST /api/v1/currencies?currency={code}`  
**Access:** ADMIN only

**curl:**
```bash
# Add Japanese Yen
curl -X POST "http://localhost:8080/api/v1/currencies?currency=JPY" \
  -u admin:admin123

# Add Swiss Franc
curl -X POST "http://localhost:8080/api/v1/currencies?currency=CHF" \
  -u admin:admin123
```

**PowerShell:**
```powershell
# Create admin headers
$adminHeaders = @{
    'Authorization' = 'Basic YWRtaW46YWRtaW4xMjM='
}

# Add Japanese Yen
Invoke-RestMethod -Uri 'http://localhost:8080/api/v1/currencies?currency=JPY' `
  -Method POST `
  -Headers $adminHeaders

# Add Swiss Franc
Invoke-RestMethod -Uri 'http://localhost:8080/api/v1/currencies?currency=CHF' `
  -Method POST `
  -Headers $adminHeaders
```

**Success Response (201 Created):**
```json
{
  "code": "JPY",
  "name": "Japanese Yen"
}
```

**Error Response (409 Conflict - Currency Exists):**
```json
{
  "timestamp": "2025-11-26T15:30:00",
  "status": 409,
  "error": "Conflict",
  "message": "Currency already exists: JPY",
  "path": "/api/v1/currencies"
}
```

**Error Response (400 Bad Request - Invalid Code):**
```json
{
  "timestamp": "2025-11-26T15:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid currency code: XYZ",
  "path": "/api/v1/currencies"
}
```

**Error Response (403 Forbidden - USER attempts to add):**
```json
{
  "timestamp": "2025-11-26T15:30:00",
  "status": 403,
  "error": "Forbidden",
  "message": "Access Denied",
  "path": "/api/v1/currencies"
}
```

---

## Exchange Rates

### 3. Convert Currency

**Endpoint:** `GET /api/v1/currencies/exchange-rates?amount={amount}&from={from}&to={to}`  
**Access:** Public (no authentication required)

**curl:**
```bash
# Convert 100 USD to EUR
curl "http://localhost:8080/api/v1/currencies/exchange-rates?amount=100&from=USD&to=EUR"

# Convert 50 EUR to GBP
curl "http://localhost:8080/api/v1/currencies/exchange-rates?amount=50&from=EUR&to=GBP"

# Convert 1000 JPY to USD
curl "http://localhost:8080/api/v1/currencies/exchange-rates?amount=1000&from=JPY&to=USD"
```

**PowerShell:**
```powershell
# Convert 100 USD to EUR
Invoke-RestMethod -Uri 'http://localhost:8080/api/v1/currencies/exchange-rates?amount=100&from=USD&to=EUR'

# Convert 50 EUR to GBP
Invoke-RestMethod -Uri 'http://localhost:8080/api/v1/currencies/exchange-rates?amount=50&from=EUR&to=GBP'

# Convert with stored result
$result = Invoke-RestMethod -Uri 'http://localhost:8080/api/v1/currencies/exchange-rates?amount=100&from=USD&to=EUR'
Write-Host "100 USD = $($result.convertedAmount) EUR (rate: $($result.rate))"
```

**Success Response (200 OK):**
```json
{
  "from": "USD",
  "to": "EUR",
  "amount": 100.00,
  "convertedAmount": 92.35,
  "rate": 0.923500,
  "timestamp": "2025-11-26T14:30:00"
}
```

**Error Response (404 Not Found - Rate Unavailable):**
```json
{
  "timestamp": "2025-11-26T15:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Exchange rate not found for currency pair: XYZ -> ABC",
  "path": "/api/v1/currencies/exchange-rates"
}
```

**Error Response (400 Bad Request - Validation Error):**
```json
{
  "timestamp": "2025-11-26T15:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v1/currencies/exchange-rates",
  "validationErrors": [
    {
      "field": "amount",
      "rejectedValue": "0",
      "message": "Amount must be greater than 0"
    }
  ]
}
```

---

## Trend Analysis

### 4. Get Currency Trend

**Endpoint:** `GET /api/v1/currencies/trends?from={from}&to={to}&period={period}`  
**Access:** PREMIUM_USER or ADMIN

**Supported Periods:**
- `12H` - 12 hours (minimum)
- `24H` - 24 hours
- `7D` - 7 days
- `30D` - 30 days
- `3M` - 3 months
- `6M` - 6 months
- `1Y` - 1 year

**curl:**
```bash
# 7-day trend for USD/EUR
curl "http://localhost:8080/api/v1/currencies/trends?from=USD&to=EUR&period=7D" \
  -u premium:admin123

# 3-month trend for EUR/GBP
curl "http://localhost:8080/api/v1/currencies/trends?from=EUR&to=GBP&period=3M" \
  -u admin:admin123

# 1-year trend for USD/JPY
curl "http://localhost:8080/api/v1/currencies/trends?from=USD&to=JPY&period=1Y" \
  -u premium:admin123
```

**PowerShell:**
```powershell
# Premium user credentials
$premiumHeaders = @{
    'Authorization' = 'Basic cHJlbWl1bTphZG1pbjEyMw=='
}

# 7-day trend
Invoke-RestMethod -Uri 'http://localhost:8080/api/v1/currencies/trends?from=USD&to=EUR&period=7D' `
  -Headers $premiumHeaders

# 3-month trend
Invoke-RestMethod -Uri 'http://localhost:8080/api/v1/currencies/trends?from=EUR&to=GBP&period=3M' `
  -Headers $premiumHeaders

# Display formatted result
$trend = Invoke-RestMethod -Uri 'http://localhost:8080/api/v1/currencies/trends?from=USD&to=EUR&period=7D' `
  -Headers $premiumHeaders
Write-Host $trend.description
```

**Success Response (200 OK) - Appreciation:**
```json
{
  "baseCurrency": "USD",
  "targetCurrency": "EUR",
  "period": "7D",
  "trendPercentage": 2.35,
  "description": "USD appreciated by 2.35% against EUR over the last 7 days"
}
```

**Success Response (200 OK) - Depreciation:**
```json
{
  "baseCurrency": "EUR",
  "targetCurrency": "GBP",
  "period": "3M",
  "trendPercentage": -1.82,
  "description": "EUR depreciated by 1.82% against GBP over the last 3 months"
}
```

**Error Response (403 Forbidden - USER attempts to access):**
```json
{
  "timestamp": "2025-11-26T15:30:00",
  "status": 403,
  "error": "Forbidden",
  "message": "Access Denied",
  "path": "/api/v1/currencies/trends"
}
```

**Error Response (400 Bad Request - Invalid Period):**
```json
{
  "timestamp": "2025-11-26T15:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid period format: 5H. Use format: 12H, 7D, 3M, or 1Y",
  "path": "/api/v1/currencies/trends",
  "validationErrors": [
    {
      "field": "period",
      "rejectedValue": "5H",
      "message": "Invalid period format. Use: 12H, 7D, 3M, or 1Y (minimum 12 hours)"
    }
  ]
}
```

**Error Response (404 Not Found - Insufficient Data):**
```json
{
  "timestamp": "2025-11-26T15:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Insufficient historical data for USD/EUR over period: 1Y",
  "path": "/api/v1/currencies/trends"
}
```

---

## Admin Operations

### 5. Manually Refresh Exchange Rates

**Endpoint:** `POST /api/v1/currencies/refresh`  
**Access:** ADMIN only

**curl:**
```bash
# Trigger manual refresh
curl -X POST http://localhost:8080/api/v1/currencies/refresh \
  -u admin:admin123
```

**PowerShell:**
```powershell
# Admin credentials
$adminHeaders = @{
    'Authorization' = 'Basic YWRtaW46YWRtaW4xMjM='
}

# Trigger refresh
Invoke-RestMethod -Uri 'http://localhost:8080/api/v1/currencies/refresh' `
  -Method POST `
  -Headers $adminHeaders

# Display formatted result
$result = Invoke-RestMethod -Uri 'http://localhost:8080/api/v1/currencies/refresh' `
  -Method POST `
  -Headers $adminHeaders
Write-Host "$($result.message) - Updated: $($result.updatedCount) rates at $($result.timestamp)"
```

**Success Response (200 OK):**
```json
{
  "message": "Exchange rates updated successfully",
  "updatedCount": 24,
  "timestamp": "2025-11-26T15:00:00"
}
```

**Error Response (500 Internal Server Error - Provider Failure):**
```json
{
  "timestamp": "2025-11-26T15:30:00",
  "status": 500,
  "error": "Internal Server Error",
  "message": "Failed to refresh exchange rates",
  "path": "/api/v1/currencies/refresh"
}
```

---

## Error Scenarios

### Testing Error Handling

#### 1. Invalid Currency Code

**curl:**
```bash
curl "http://localhost:8080/api/v1/currencies/exchange-rates?amount=100&from=INVALID&to=EUR"
```

**Response (400 Bad Request):**
```json
{
  "timestamp": "2025-11-26T15:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v1/currencies/exchange-rates",
  "validationErrors": [
    {
      "field": "from",
      "rejectedValue": "INVALID",
      "message": "Currency code must be exactly 3 uppercase letters"
    }
  ]
}
```

#### 2. Missing Required Parameter

**curl:**
```bash
curl "http://localhost:8080/api/v1/currencies/exchange-rates?from=USD&to=EUR"
# Missing 'amount' parameter
```

**Response (400 Bad Request):**
```json
{
  "timestamp": "2025-11-26T15:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v1/currencies/exchange-rates",
  "validationErrors": [
    {
      "field": "amount",
      "rejectedValue": null,
      "message": "Amount is required"
    }
  ]
}
```

#### 3. Unauthorized Access (Missing Credentials)

**curl:**
```bash
curl -X POST "http://localhost:8080/api/v1/currencies?currency=JPY"
# No -u flag, no Authorization header
```

**Response (401 Unauthorized):**
```json
{
  "timestamp": "2025-11-26T15:30:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Full authentication is required to access this resource",
  "path": "/api/v1/currencies"
}
```

#### 4. Insufficient Permissions

**curl:**
```bash
# USER attempting to add currency (requires ADMIN)
curl -X POST "http://localhost:8080/api/v1/currencies?currency=JPY" \
  -u user:admin123
```

**Response (403 Forbidden):**
```json
{
  "timestamp": "2025-11-26T15:30:00",
  "status": 403,
  "error": "Forbidden",
  "message": "Access Denied",
  "path": "/api/v1/currencies"
}
```

#### 5. Duplicate Currency

**curl:**
```bash
# Adding USD which already exists
curl -X POST "http://localhost:8080/api/v1/currencies?currency=USD" \
  -u admin:admin123
```

**Response (409 Conflict):**
```json
{
  "timestamp": "2025-11-26T15:30:00",
  "status": 409,
  "error": "Conflict",
  "message": "Currency already exists: USD",
  "path": "/api/v1/currencies"
}
```

---

## Complete Workflow Example

### Setting Up and Testing All Features

**curl:**
```bash
# 1. Check available currencies (Public)
curl http://localhost:8080/api/v1/currencies

# 2. Add new currencies (ADMIN)
curl -X POST "http://localhost:8080/api/v1/currencies?currency=JPY" -u admin:admin123
curl -X POST "http://localhost:8080/api/v1/currencies?currency=GBP" -u admin:admin123
curl -X POST "http://localhost:8080/api/v1/currencies?currency=CHF" -u admin:admin123

# 3. Trigger rate refresh (ADMIN)
curl -X POST http://localhost:8080/api/v1/currencies/refresh -u admin:admin123

# 4. Wait a few seconds for rates to populate
sleep 5

# 5. Convert currency (Public)
curl "http://localhost:8080/api/v1/currencies/exchange-rates?amount=100&from=USD&to=EUR"

# 6. Check trend (PREMIUM_USER or ADMIN)
curl "http://localhost:8080/api/v1/currencies/trends?from=USD&to=EUR&period=12H" -u premium:admin123
```

**PowerShell:**
```powershell
# Setup headers
$adminHeaders = @{ 'Authorization' = 'Basic YWRtaW46YWRtaW4xMjM=' }
$premiumHeaders = @{ 'Authorization' = 'Basic cHJlbWl1bTphZG1pbjEyMw==' }

# 1. Check available currencies
Write-Host "Step 1: Getting currencies..." -ForegroundColor Cyan
$currencies = Invoke-RestMethod -Uri 'http://localhost:8080/api/v1/currencies'
$currencies | Format-Table

# 2. Add new currencies
Write-Host "`nStep 2: Adding currencies..." -ForegroundColor Cyan
'JPY', 'GBP', 'CHF' | ForEach-Object {
    $result = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/currencies?currency=$_" `
      -Method POST -Headers $adminHeaders
    Write-Host "  Added: $($result.code) - $($result.name)"
}

# 3. Trigger rate refresh
Write-Host "`nStep 3: Refreshing rates..." -ForegroundColor Cyan
$refresh = Invoke-RestMethod -Uri 'http://localhost:8080/api/v1/currencies/refresh' `
  -Method POST -Headers $adminHeaders
Write-Host "  $($refresh.message) - $($refresh.updatedCount) rates updated"

# 4. Wait for rates
Write-Host "`nStep 4: Waiting for rates to populate..." -ForegroundColor Cyan
Start-Sleep -Seconds 5

# 5. Convert currency
Write-Host "`nStep 5: Converting 100 USD to EUR..." -ForegroundColor Cyan
$conversion = Invoke-RestMethod -Uri 'http://localhost:8080/api/v1/currencies/exchange-rates?amount=100&from=USD&to=EUR'
Write-Host "  100 USD = $($conversion.convertedAmount) EUR (rate: $($conversion.rate))"

# 6. Check trend
Write-Host "`nStep 6: Getting trend analysis..." -ForegroundColor Cyan
$trend = Invoke-RestMethod -Uri 'http://localhost:8080/api/v1/currencies/trends?from=USD&to=EUR&period=12H' `
  -Headers $premiumHeaders
Write-Host "  $($trend.description)"

Write-Host "`nWorkflow completed successfully!" -ForegroundColor Green
```

---

## Testing with Swagger UI

For interactive API testing, use the Swagger UI:

1. Open http://localhost:8080/swagger-ui.html
2. Click the **"Authorize"** button (top right)
3. Enter credentials:
   - Username: `admin`
   - Password: `admin123`
4. Click **"Authorize"** then **"Close"**
5. Expand any endpoint and click **"Try it out"**
6. Fill in parameters and click **"Execute"**
7. View the response below

**Benefits of Swagger UI:**
- Interactive testing without writing commands
- Automatic request validation
- Response schema documentation
- Built-in authentication support
- Example values for all parameters
- Copy as curl command feature

---

## Additional Resources

- **Main README**: [README.md](README.md) - Project overview and setup
- **Docker Guide**: [DOCKER.md](DOCKER.md) - Deployment and container management
- **Mock Providers**: [MOCK_PROVIDERS.md](MOCK_PROVIDERS.md) - Testing with mock services
- **OpenAPI Spec**: http://localhost:8080/v3/api-docs - Machine-readable API specification
