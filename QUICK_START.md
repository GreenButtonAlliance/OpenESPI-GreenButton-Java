# OpenESPI Authorization Server - Quick Start Guide

Get the OpenESPI Authorization Server running in under 5 minutes with Docker.

## Prerequisites

- Docker 20.10+ and Docker Compose 2.0+
- Git
- 4GB+ available RAM

## Quick Start (Docker)

1. **Clone the repository:**
```bash
git clone https://github.com/GreenButtonAlliance/OpenESPI-AuthorizationServer-java.git
cd OpenESPI-AuthorizationServer-java
```

2. **Start the development environment:**
```bash
# Simple development setup
docker-compose up -d

# Or use the deployment script
./scripts/deploy.sh -e development
```

3. **Access the application:**
- **Authorization Server**: http://localhost:8080
- **Management Endpoints**: http://localhost:8081/actuator
- **Health Check**: http://localhost:8081/actuator/health

## Quick Start (Advanced)

### With Monitoring Stack
```bash
./scripts/deploy.sh -e development -p monitoring
```
Access Grafana at http://localhost:3000 (admin/admin)

### With PostgreSQL
```bash
./scripts/deploy.sh -e development -p postgresql
```

### Full Stack (PostgreSQL + Monitoring + Redis)
```bash
./scripts/deploy.sh -e development -p postgresql,monitoring,redis -b
```

## Test the Installation

1. **Check health status:**
```bash
curl http://localhost:8081/actuator/health
```

2. **View OIDC discovery document:**
```bash
curl http://localhost:8080/.well-known/openid_configuration
```

3. **Access Swagger UI (development only):**
```bash
open http://localhost:8080/swagger-ui.html
```

## Default Configuration

### Database
- **Type**: MySQL 8.4
- **Host**: localhost:3306
- **Database**: espi_authserver
- **Username**: espi_user
- **Password**: espi_password

### Application
- **Port**: 8080 (HTTP)
- **Management Port**: 8081
- **Profile**: docker
- **HTTPS**: Disabled (development)

## Development Workflow

### View Logs
```bash
docker-compose logs -f authserver
```

### Restart Services
```bash
docker-compose restart authserver
```

### Stop Services
```bash
docker-compose down
```

### Clean Reset
```bash
./scripts/deploy.sh -e development -c -b
```

## Production Deployment

For production deployment, see the [Deployment Guide](DEPLOYMENT_GUIDE.md).

### Quick Production Setup
```bash
# Copy environment template
cp docker/.env.example docker/.env

# Edit production configuration
nano docker/.env

# Deploy with monitoring
./scripts/deploy.sh -e production -p monitoring -b
```

## Default Test Clients

The development environment includes pre-configured OAuth2 clients:

| Client ID | Type | Grant Types | Scopes |
|-----------|------|-------------|---------|
| `test_client_auth_code` | Web Application | authorization_code, refresh_token | openid, profile, ESPI scopes |
| `test_client_credentials` | Service Account | client_credentials | Admin scopes |
| `datacustodian_admin` | DataCustodian | client_credentials | DataCustodian_Admin_Access |

## API Testing

### Get Access Token (Client Credentials)
```bash
curl -X POST http://localhost:8080/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials&client_id=test_client_credentials&client_secret=test_secret&scope=openid"
```

### Use Token for API Access
```bash
# Get client list
curl -H "Authorization: Bearer YOUR_TOKEN" \
  http://localhost:8080/api/v1/oauth2/clients

# Check user info
curl -H "Authorization: Bearer YOUR_TOKEN" \
  http://localhost:8080/userinfo
```

## Common Issues

### Port Already in Use
```bash
# Check what's using port 8080
lsof -i :8080

# Stop conflicting services
sudo kill -9 $(lsof -t -i:8080)
```

### Database Connection Issues
```bash
# Check MySQL container
docker-compose logs mysql

# Reset database
docker-compose down -v
docker-compose up -d
```

### Memory Issues
```bash
# Check container resources
docker stats

# Increase Docker memory limit to 4GB+
```

## Next Steps

1. **Read the [Deployment Guide](DEPLOYMENT_GUIDE.md)** for production setup
2. **Review [Certificate Authentication](CERTIFICATE_AUTHENTICATION.md)** for mTLS setup
3. **Check the [API Documentation](docs/)** for integration details
4. **Configure DataCustodian integration** for full ESPI functionality

## Support

- **Issues**: [GitHub Issues](https://github.com/GreenButtonAlliance/OpenESPI-AuthorizationServer-java/issues)
- **Documentation**: [Project Wiki](https://github.com/GreenButtonAlliance/OpenESPI-AuthorizationServer-java/wiki)
- **Community**: [Green Button Alliance](https://www.greenbuttonalliance.org/)

---

**Happy coding! 🚀**