# DEVOPS PRACTICES

### 1. Observability

**Front-End**

*Grafana Faro*

Set ```FARO_ENABLED: "true"``` to enable Grafana Faro.
Faro will send data to Grafana Cloud. (or Alloy for local deployment)

**Spring Boot** *Actuator*
##### Server
- Health: http://localhost:8080/actuator/health
- Metrics: http://localhost:8080/actuator/metrics
- Transaction Metrics:
    - http://localhost:8080/actuator/metrics/iso8583.transactions.successful
    - http://localhost:8080/actuator/metrics/iso8583.transactions.failed

- When server receive the transaction from *simulator*, the simer start (using field 37 as key). The timer end when a 210 resonse is processed
- Transaction successed: response time < ```iso8583.transaction.timeout``` (usually 7 seconds)
- Transaction failed: Invalid message or Timeout

### 2. Deployment

**High Availability**

This project focus on the high availability in the Issuer Side (usually a Bank).

##### Scale Authorize service
- All services use ```group-id=authorize-service```
- Topic has 3 partitions (support up to 3 consumer instance)
- Each partition assigned to different consumer instance
- Each message goes to only one consumer in the group

**Kubernetes** K3s for testing purpose


#### Downtime Scenerio

#### Maintainance

**Kubernetes**

### 3. CICD

**Jenkins**
