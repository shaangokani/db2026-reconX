```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
-H "Content-Type: application/json" \
-d '{"email":"admin@db.com","password":"admin123"}' | jq -r '.token')

echo "$TOKEN"

```
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \    
-H "Content-Type: application/json" \
-d '{"email":"admin@db.com","password":"admin123"}' | jq -r '.token')

echo "$TOKEN"

```bash
curl -i -X POST http://localhost:8080/api/v1/trades \         
  -H "Content-Type: application/json" \
  -d '{"tradeRef":"TRD-20260315-0001","instrumentId":1,"counterpartyId":1,"assetClass":"EQUITY","side":"BUY","quantity":100.0,"price":245.50,"tradeDate":"2026-03-15"}'
```

With auth 
```bash
curl -i -X POST http://localhost:8080/api/v1/trades \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"tradeRef":"TRD-20260315-0001","instrumentId":1,"counterpartyId":1,"assetClass":"EQUITY","side":"BUY","quantity":100.0,"price":245.50,"tradeDate":"2026-03-15"}'
  
```

curl -i -X PATCH http://localhost:8080/api/v1/trades/1/status \
-H "Content-Type: application/json" \
-d '{"status":"MATCHED"}'

curl -i -X DELETE http://localhost:8080/api/v1/trades/1   

curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/instruments/1
curl -i -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/instruments/SAP.DE
  