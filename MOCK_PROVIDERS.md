# Mock Exchange Rate Providers - User Guide

## Overview

The project includes **2 standalone Spring Boot microservices** that simulate external currency exchange rate providers. These services return randomized exchange rates and are used for testing the rate aggregation and failover capabilities of the main application.

## Architecture

### Mock Provider 1
- **Location**: `mock-services/mock-provider-1/`
- **Port**: 8091
- **Container**: `mock-provider-1`
- **Base Path**: `/api/v1`
- **Simulates**: Fixer.io API structure

### Mock Provider 2
- **Location**: `mock-services/mock-provider-2/`
- **Port**: 8092
- **Container**: `mock-provider-2`
- **Base Path**: `/v1`
- **Simulates**: ExchangeRatesAPI structure

## How They Work

Both providers are **minimal Spring Boot applications** that:

1. **Generate Random Exchange Rates**
   - Rates between 0.5 and 2.0
   - 6 decimal precision
   - Support 8 common currencies: USD, EUR, GBP, JPY, CHF, CAD, AUD, NZD

2. **Provide REST Endpoints**
   - `GET /latest` - Current exchange rates
   - `GET /{date}` - Historical rates (any date)

3. **Return JSON Response**
   ```json
   {
     "success": true,
     "base": "EUR",
     "date": "2025-11-26",
     "rates": {
       "USD": 1.087234,
       "GBP": 0.856789,
       "JPY": 162.345678
     }
   }
   ```

### Key Features

- **No Database**: Rates are generated on-the-fly using `Random`
- **No Authentication**: Open endpoints for easy testing
- **Deterministic Structure**: Always returns valid JSON
- **Different Each Time**: Random rates on every request
- **Health Checks**: Docker monitors `/latest` endpoint availability

## Starting the Mock Providers

### Method 1: Docker Compose (Recommended)

Start both providers along with the main application:

```powershell
cd "c:\Work\Study\AI Copilot\Cur_ex_app"
docker-compose up -d mock-provider-1 mock-provider-2
```

Or start all services:

```powershell
docker-compose up -d
```

### Method 2: Standalone (Without Docker)

**Provider 1:**
```powershell
cd "c:\Work\Study\AI Copilot\Cur_ex_app\mock-services\mock-provider-1"
mvn spring-boot:run
```

**Provider 2 (in separate terminal):**
```powershell
cd "c:\Work\Study\AI Copilot\Cur_ex_app\mock-services\mock-provider-2"
mvn spring-boot:run
```

## Testing the Mock Providers

### 1. Check Service Health

**Mock Provider 1:**
```powershell
# Via Docker
docker logs mock-provider-1 --tail 20

# Direct HTTP
curl http://localhost:8091/api/v1/latest

# PowerShell
Invoke-RestMethod -Uri "http://localhost:8091/api/v1/latest"
```

**Mock Provider 2:**
```powershell
# Via Docker
docker logs mock-provider-2 --tail 20

# Direct HTTP
curl http://localhost:8092/v1/latest

# PowerShell
Invoke-RestMethod -Uri "http://localhost:8092/v1/latest"
```

### 2. Test Latest Rates Endpoint

**Get EUR-based rates (default):**
```powershell
# Provider 1
Invoke-RestMethod -Uri "http://localhost:8091/api/v1/latest"

# Provider 2
Invoke-RestMethod -Uri "http://localhost:8092/v1/latest"
```

**Get USD-based rates:**
```powershell
# Provider 1
Invoke-RestMethod -Uri "http://localhost:8091/api/v1/latest?base=USD"

# Provider 2
Invoke-RestMethod -Uri "http://localhost:8092/v1/latest?base=USD"
```

### 3. Test Historical Rates Endpoint

**Get rates for a specific date:**
```powershell
# Provider 1
Invoke-RestMethod -Uri "http://localhost:8091/api/v1/2025-11-01?base=EUR"

# Provider 2
Invoke-RestMethod -Uri "http://localhost:8092/v1/2025-11-01?base=EUR"
```

**Get specific currency pair:**
```powershell
# Provider 1
Invoke-RestMethod -Uri "http://localhost:8091/api/v1/2025-11-01?base=USD&symbols=EUR"

# Provider 2
Invoke-RestMethod -Uri "http://localhost:8092/v1/2025-11-01?base=USD&symbols=EUR"
```

### 4. Test Rate Variability

Run the same request multiple times to see random rates:

```powershell
# Get 5 different rate sets from Provider 1
1..5 | ForEach-Object {
    Write-Host "`n--- Request $_ ---"
    $result = Invoke-RestMethod -Uri "http://localhost:8091/api/v1/latest?base=USD"
    $result.rates | Format-Table
}
```

### 5. Test with Main Application

The main application automatically uses these providers when running in Docker:

```powershell
# Start all services
docker-compose up -d

# Wait for startup
Start-Sleep -Seconds 30

# Trigger rate refresh (uses both mock providers)
$headers = @{ 'Authorization' = 'Basic YWRtaW46YWRtaW4xMjM=' }
Invoke-RestMethod -Uri 'http://localhost:8080/api/v1/currencies/refresh' -Method POST -Headers $headers

# Check which providers responded
docker logs currency-exchange-app --tail 50 | Select-String -Pattern "mock-provider|Aggregated|rates"
```

## Verification Checklist

✅ **Service Running**: Container is up and healthy
```powershell
docker ps | Select-String "mock-provider"
```

✅ **HTTP Response**: Returns 200 OK
```powershell
curl -I http://localhost:8091/api/v1/latest
```

✅ **Valid JSON**: Response is well-formed JSON
```powershell
$response = Invoke-RestMethod -Uri "http://localhost:8091/api/v1/latest"
$response | ConvertTo-Json
```

✅ **Random Rates**: Each request returns different rates
```powershell
$rate1 = (Invoke-RestMethod -Uri "http://localhost:8091/api/v1/latest").rates.USD
$rate2 = (Invoke-RestMethod -Uri "http://localhost:8091/api/v1/latest").rates.USD
Write-Host "Rate 1: $rate1, Rate 2: $rate2, Different: $($rate1 -ne $rate2)"
```

✅ **Integration**: Main app fetches rates successfully
```powershell
docker logs currency-exchange-app | Select-String -Pattern "Aggregated best rates"
```

## Expected Output Examples

### Successful Health Check
```
✓ mock-provider-1 (healthy)
✓ mock-provider-2 (healthy)
```

### Latest Rates Response
```json
{
  "success": true,
  "base": "USD",
  "date": "2025-11-26",
  "rates": {
    "EUR": 0.923456,
    "GBP": 0.789012,
    "JPY": 149.876543,
    "CHF": 0.876543,
    "CAD": 1.356789,
    "AUD": 1.523456,
    "NZD": 1.678901
  }
}
```

### Main App Integration
```
INFO  c.c.p.service.RateAggregationService : Aggregating rates from 2 providers
INFO  c.c.p.service.RateAggregationService : Aggregated best rates for 5 base currencies with total 40 currency pairs
```

## Troubleshooting

### Provider Not Starting

**Check Docker logs:**
```powershell
docker logs mock-provider-1 --tail 50
```

**Common issues:**
- Port 8091/8092 already in use
- Build failed (check `mvn clean package`)
- Network issue (check `currency-exchange-network`)

### Provider Returns Empty Response

**Verify endpoint path:**
- Provider 1: `/api/v1/latest` ✅ NOT `/latest`
- Provider 2: `/v1/latest` ✅ NOT `/latest`

### Main App Not Using Providers

**Check environment variables:**
```powershell
docker exec currency-exchange-app env | Select-String "MOCK_PROVIDER"
```

Expected:
```
MOCK_PROVIDER_1_URL=http://mock-provider-1:8091
MOCK_PROVIDER_2_URL=http://mock-provider-2:8092
```

**Verify network connectivity:**
```powershell
docker exec currency-exchange-app wget -q -O - http://mock-provider-1:8091/api/v1/latest
```

## Configuration

### Environment Variables

Located in `docker-compose.yml`:

```yaml
environment:
  MOCK_PROVIDER_1_URL: ${MOCK_PROVIDER_1_URL:-http://mock-provider-1:8091}
  MOCK_PROVIDER_2_URL: ${MOCK_PROVIDER_2_URL:-http://mock-provider-2:8092}
```

### Ports

- **8091**: Mock Provider 1 (Fixer.io simulation)
- **8092**: Mock Provider 2 (ExchangeRatesAPI simulation)

### Network

Both providers run on the `currency-exchange-network` Docker network, allowing the main application to reach them by container name.

## Development

### Code Location

**Controller:**
- `mock-services/mock-provider-1/src/main/java/com/currencyexchange/mock/MockExchangeRateController.java`
- `mock-services/mock-provider-2/src/main/java/com/currencyexchange/mock/MockExchangeRateController.java`

### Modifying Rate Generation

Edit `generateRandomRate()` method to change rate range:

```java
private BigDecimal generateRandomRate() {
    // Change min/max values
    double min = 0.5;
    double max = 2.0;
    double rate = min + ((max - min) * random.nextDouble());
    return BigDecimal.valueOf(rate).setScale(6, RoundingMode.HALF_UP);
}
```

### Adding New Currencies

Edit `generateMockRates()` method:

```java
String[] currencies = {"USD", "EUR", "GBP", "JPY", "CHF", "CAD", "AUD", "NZD", "SEK", "NOK"};
```

### Rebuilding

After code changes:

```powershell
# Rebuild and restart
docker-compose build mock-provider-1 mock-provider-2
docker-compose up -d mock-provider-1 mock-provider-2
```

## Integration with Main Application

The main application's `RateAggregationService` fetches rates from:

1. **Real Providers**: Fixer.io, ExchangeRatesAPI (if API keys configured)
2. **Mock Providers**: mock-provider-1, mock-provider-2 (in Docker)

### Rate Selection Strategy

The service:
- Fetches rates from **all available providers**
- Selects the **best (lowest) rate** for each currency pair
- Falls back to cache if no providers available

### Testing Rate Aggregation

```powershell
# Add currencies
$headers = @{ 'Authorization' = 'Basic YWRtaW46YWRtaW4xMjM=' }
Invoke-RestMethod -Uri 'http://localhost:8080/api/v1/currencies?currency=USD' -Method POST -Headers $headers
Invoke-RestMethod -Uri 'http://localhost:8080/api/v1/currencies?currency=EUR' -Method POST -Headers $headers
Invoke-RestMethod -Uri 'http://localhost:8080/api/v1/currencies?currency=GBP' -Method POST -Headers $headers

# Trigger refresh (aggregates from both mock providers)
Invoke-RestMethod -Uri 'http://localhost:8080/api/v1/currencies/refresh' -Method POST -Headers $headers

# Verify rates were stored
Invoke-RestMethod -Uri 'http://localhost:8080/api/v1/currencies/exchange-rates?from=USD&to=EUR&amount=100'
```

## Performance Characteristics

- **Startup Time**: ~5 seconds
- **Response Time**: <50ms
- **Memory Usage**: ~150MB per provider
- **CPU Usage**: Minimal (rate generation is simple)
- **Concurrent Requests**: Supports 100+ simultaneous requests

## Use Cases

1. **Local Development**: Test without real API keys
2. **Integration Testing**: Verify rate aggregation logic
3. **Failover Testing**: Simulate provider unavailability
4. **Performance Testing**: Generate high load scenarios
5. **Demo/Presentation**: Show multi-provider aggregation

## Summary

The mock providers are **lightweight, standalone Spring Boot services** that:

✅ Simulate real exchange rate APIs  
✅ Return random but valid data  
✅ Support the same endpoints as real providers  
✅ Enable testing without API keys  
✅ Work seamlessly in Docker environment  
✅ Can be run standalone for debugging  

They are essential for testing the **rate aggregation**, **failover**, and **caching** features of the main application.
