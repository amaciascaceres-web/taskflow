# Required environment variables in AWS

## task-service (port 8080)
DB_HOST=                    # RDS endpoint
DB_PORT=5432
DB_NAME=taskflow
DB_USER=taskflow
DB_PASSWORD=                # NEVER in the repository — use Secrets Manager
SPRING_PROFILES_ACTIVE=prod
USER_SERVICE_URL=           # Internal URL of user-service (e.g. http://user-service:8081)
NOTIFICATION_SERVICE_URL=   # Internal URL of notification-service (e.g. http://notification-service:8082)
REDIS_HOST=                 # ElastiCache endpoint or Redis on the instance

## user-service (port 8081)
DB_HOST=                    # RDS endpoint
DB_PORT=5432
DB_NAME=taskflow_users
DB_USER=taskflow
DB_PASSWORD=                # NEVER in the repository — use Secrets Manager
SPRING_PROFILES_ACTIVE=prod

## notification-service (port 8082)
SPRING_PROFILES_ACTIVE=prod

## api-gateway (port 8090)
SPRING_PROFILES_ACTIVE=prod
TASK_SERVICE_URL=           # Internal URL of task-service (e.g. http://task-service:8080)
USER_SERVICE_URL=           # Internal URL of user-service (e.g. http://user-service:8081)
