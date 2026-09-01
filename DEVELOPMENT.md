# Development Guide

This guide provides instructions for developing and extending the Striim Splunk Connector application.

## Development Environment Setup

### Prerequisites

- Java 17 or higher
- Maven 3.8.1 or higher
- Node.js 18 or higher
- PostgreSQL 15
- Docker and Docker Compose (for containerized development)

### Backend Development

#### 1. Setup PostgreSQL Locally

```bash
# Create database
createdb striim_connector

# Create user (if needed)
createuser -U postgres striim_user
```

#### 2. Build and Run Backend

```bash
# Build the project
mvn clean install

# Run the application
mvn spring-boot:run

# Or run the JAR directly
java -jar target/striim-splunk-connector-1.0.0.jar
```

The backend will start on `http://localhost:8080`

#### 3. Backend Project Structure

```
src/main/java/com/striim/
├── StriimSplunkConnectorApplication.java  # Main Spring Boot class
├── config/                                # Configuration classes
│   └── SecurityConfig.java               # Security and CORS config
├── controller/                            # REST API controllers
│   ├── ConfigController.java
│   ├── CollectController.java
│   └── HistoryController.java
├── entity/                                # JPA entities
│   ├── SystemConfig.java
│   └── ExecutionHistory.java
├── repository/                            # Data access layer
│   ├── SystemConfigRepository.java
│   └── ExecutionHistoryRepository.java
├── service/                               # Business logic
│   ├── ConfigService.java
│   ├── MetricsCollectionService.java
│   ├── StriimApiClient.java
│   └── SplunkHecClient.java
└── util/                                  # Utility classes
    └── EncryptionUtil.java
```

### Frontend Development

#### 1. Setup Frontend

```bash
cd frontend

# Install dependencies
npm install

# Start development server
npm start
```

The frontend will start on `http://localhost:3000`

#### 2. Frontend Project Structure

```
frontend/
├── src/
│   ├── api/
│   │   └── apiClient.js                 # Axios API client
│   ├── components/
│   │   ├── AppHeader.js
│   │   ├── NavigationBar.js
│   │   ├── ConfigPanel.js
│   │   ├── DashboardSummary.js
│   │   └── ExecutionHistoryTable.js
│   ├── App.js                           # Main App component
│   ├── App.css                          # Global styles
│   └── index.js                         # React entry point
├── public/
│   └── index.html                       # HTML template
├── package.json
├── Dockerfile
└── nginx.conf
```

## Code Architecture

### Backend Architecture

#### Service Layer
- `ConfigService`: Handles configuration management
- `MetricsCollectionService`: Orchestrates metrics collection and publishing
- `StriimApiClient`: Communicates with Striim REST API
- `SplunkHecClient`: Publishes metrics to Splunk

#### Controller Layer
- `ConfigController`: `/api/v1/config` endpoints
- `CollectController`: `/api/v1/collect` endpoints
- `HistoryController`: `/api/v1/history` endpoints

#### Data Layer
- `SystemConfig`: Configuration entity
- `ExecutionHistory`: Execution history entity
- Repositories: Database access using Spring Data JPA

### Frontend Architecture

#### Component Hierarchy
```
App
├── AppHeader
├── NavigationBar
└── Active Component (based on activeTab)
    ├── ConfigPanel
    ├── DashboardSummary
    └── ExecutionHistoryTable
```

## Development Workflow

### Adding a New API Endpoint

1. **Create a DTO** (if needed):
```java
// src/main/java/com/striim/dto/YourRequest.java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class YourRequest {
    private String field;
}
```

2. **Create/Update Service**:
```java
// src/main/java/com/striim/service/YourService.java
@Service
public class YourService {
    public void processYourRequest(YourRequest request) {
        // Implementation
    }
}
```

3. **Create/Update Controller**:
```java
// src/main/java/com/striim/controller/YourController.java
@RestController
@RequestMapping("/v1/your-endpoint")
public class YourController {
    @Autowired
    private YourService service;

    @PostMapping
    public ResponseEntity<?> handle(@RequestBody YourRequest request) {
        service.processYourRequest(request);
        return ResponseEntity.ok().build();
    }
}
```

4. **Add Tests** (optional):
```java
@SpringBootTest
public class YourControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testEndpoint() throws Exception {
        // Test implementation
    }
}
```

### Adding a New Frontend Component

1. **Create Component File**:
```javascript
// frontend/src/components/YourComponent.js
import React, { useState } from 'react';

function YourComponent() {
  const [state, setState] = useState(null);

  return (
    <div className="card">
      <h2 className="card-title">Your Component</h2>
      {/* Component JSX */}
    </div>
  );
}

export default YourComponent;
```

2. **Update App.js** to integrate the component:
```javascript
import YourComponent from './components/YourComponent';

// In App component
{activeTab === 'your-tab' && <YourComponent />}
```

3. **Update NavigationBar.js** to add tab button:
```javascript
<button
  className={`nav-tab ${activeTab === 'your-tab' ? 'active' : ''}`}
  onClick={() => setActiveTab('your-tab')}
>
  Your Tab
</button>
```

## Database Migrations

Database schema is managed via SQL files in `src/main/resources/db/migration/`:

```sql
-- V1__initial_schema.sql
CREATE TABLE your_table (
    id VARCHAR(50) PRIMARY KEY,
    field VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

## Testing

### Backend Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=YourControllerTest

# Run with coverage
mvn test jacoco:report
```

### Frontend Tests

```bash
cd frontend

# Run tests
npm test

# Run with coverage
npm test -- --coverage
```

## Building for Production

### Build Backend Docker Image

```bash
docker build -t striim-splunk-connector:latest .
```

### Build Frontend Docker Image

```bash
docker build -t striim-splunk-connector-ui:latest -f frontend/Dockerfile .
```

### Build All Services

```bash
docker-compose build
```

## Common Development Tasks

### Adding a New Metric

1. Update `MetricsCollectionService`:
```java
// Add metric extraction logic
Map<String, Object> metrics = new HashMap<>();
metrics.put("new_metric", extractNewMetric());
```

2. Update `SplunkHecClient` if metric format differs

3. Document the metric in `README.md`

### Modifying Database Schema

1. Create new migration file in `src/main/resources/db/migration/`:
```sql
-- V2__add_new_column.sql
ALTER TABLE system_config ADD COLUMN new_column VARCHAR(255);
```

2. Restart the application to apply migration

### Updating Dependencies

**Backend (Maven)**:
```bash
# Check for updates
mvn versions:display-dependency-updates

# Update plugin versions
mvn versions:display-plugin-updates
```

**Frontend (npm)**:
```bash
cd frontend

# Check for updates
npm outdated

# Update specific package
npm update package-name

# Update all packages (carefully)
npm update
```

## Debugging

### Backend Debugging

1. **Using IDE (IntelliJ)**:
   - Set breakpoints in code
   - Run -> Debug 'StriimSplunkConnectorApplication'

2. **Using Command Line**:
```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--debug"
```

3. **Using Logs**:
```bash
# Tail logs
tail -f logs/application.log

# Change log level in application.yml
logging:
  level:
    com.striim: DEBUG
```

### Frontend Debugging

1. **Browser DevTools**:
   - Open Chrome DevTools (F12)
   - Check Network tab for API calls
   - Check Console for JavaScript errors

2. **React Developer Tools**:
   - Install React Developer Tools extension
   - Inspect component hierarchy and state

3. **API Testing**:
```bash
# Test API endpoint directly
curl -X GET http://localhost:8080/api/v1/config
```

## Performance Optimization

### Backend

1. **Connection Pooling**: Adjust in `application.yml`:
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
```

2. **Query Optimization**: Add indexes to frequently queried columns:
```sql
CREATE INDEX idx_execution_status ON execution_history(status);
```

### Frontend

1. **Code Splitting**: Use React.lazy for route-based splitting
2. **Memoization**: Use React.memo to prevent unnecessary re-renders
3. **API Caching**: Implement response caching in apiClient.js

## Security Best Practices

1. **Never commit secrets**: Use environment variables
2. **Validate inputs**: Always validate user input on backend
3. **Use HTTPS**: Enable SSL/TLS in production
4. **Update dependencies**: Regularly check for security updates
5. **Rate limiting**: Implement rate limiting for API endpoints
6. **SQL Injection prevention**: Use parameterized queries (JPA handles this)

## Useful Commands

```bash
# Docker Compose
docker-compose up -d                 # Start all services
docker-compose down                  # Stop all services
docker-compose logs -f               # View logs
docker-compose exec backend sh        # Access backend container

# Maven
mvn clean install                    # Clean build
mvn spring-boot:run                  # Run application
mvn test                            # Run tests

# npm/React
npm install                          # Install dependencies
npm start                            # Start dev server
npm run build                        # Build for production
npm test                            # Run tests
```

## Troubleshooting Development Issues

### Port Already in Use

```bash
# Find process using port
lsof -i :8080

# Kill process
kill -9 <PID>
```

### Database Connection Error

```bash
# Check PostgreSQL
psql -U postgres -c "SELECT version();"

# Check credentials in application.yml
grep "datasource" src/main/resources/application.yml
```

### CORS Errors

- Update `SecurityConfig.java` with frontend URL
- Check frontend proxy settings in `frontend/package.json`

## Contributing

1. Create a feature branch: `git checkout -b feature/your-feature`
2. Make your changes
3. Write/update tests
4. Commit with clear messages: `git commit -m "feat: add your feature"`
5. Push and create a Pull Request

## Resources

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [React Documentation](https://react.dev)
- [PostgreSQL Documentation](https://www.postgresql.org/docs)
- [Docker Documentation](https://docs.docker.com)
