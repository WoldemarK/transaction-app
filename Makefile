# ==================== Configuration ====================
COMPOSE_FILE ?= docker-compose.yml
PROJECT_NAME ?= transaction-platform

# Services
POSTGRES_0      ?= postgres-0
POSTGRES_1      ?= postgres-1
SHARDING_PROXY  ?= shardingsphere-proxy
ZOOKEEPER       ?= zookeeper
KAFKA           ?= kafka1
SCHEMA_REGISTRY ?= schema-registry
KAFKA_EXPORTER  ?= kafka-exporter
PROMETHEUS      ?= prometheus
GRAFANA         ?= grafana
TEMPO           ?= tempo
LOKI            ?= loki
ALLOY           ?= alloy

# Ports
PG_PORT_0       ?= 5433
PG_PORT_1       ?= 5434
SHARDING_PORT   ?= 3307
KAFKA_PORT      ?= 9092
SCHEMA_PORT     ?= 8081
EXPORTER_PORT   ?= 9308
PROM_PORT       ?= 9090
GRAFANA_PORT    ?= 3000
TEMPO_PORT      ?= 3200
LOKI_PORT       ?= 3100
ALLOY_PORT      ?= 9080

# URLs
SCHEMA_URL      ?= http://localhost:$(SCHEMA_PORT)/subjects
EXPORTER_URL    ?= http://localhost:$(EXPORTER_PORT)/metrics
PROM_URL        ?= http://localhost:$(PROM_PORT)
GRAFANA_URL     ?= http://localhost:$(GRAFANA_PORT)
TEMPO_URL       ?= http://localhost:$(TEMPO_PORT)
LOKI_URL        ?= http://localhost:$(LOKI_PORT)
ALLOY_URL       ?= http://localhost:$(ALLOY_PORT)

# Colors
RESET   = \033[0m
GREEN   = \033[32m
YELLOW  = \033[33m
RED     = \033[31m
BLUE    = \033[34m
CYAN    = \033[36m
MAGENTA = \033[35m

# ==================== Helpers ====================
define print_info
	@printf '$(BLUE)➤ $(1)$(RESET)\n'
endef

define print_success
	@printf '$(GREEN)✓ $(1)$(RESET)\n'
endef

define print_warning
	@printf '$(YELLOW)⚠ $(1)$(RESET)\n'
endef

define print_error
	@printf '$(RED)✗ $(1)$(RESET)\n'
endef

define print_header
	@printf '\n$(CYAN)$(1)$(RESET)\n'
	@printf '$(CYAN)$(shell printf '=%.0s' {1..$(words $(1))})$(RESET)\n'
endef

# ==================== Core Commands ====================
.PHONY: help
help:
	@printf '$(CYAN)Transaction Platform — Development Stack$(RESET)\n'
	@printf '$(CYAN)========================================$(RESET)\n\n'
	@printf ' $(GREEN)make up$(RESET)               Запустить весь стек\n'
	@printf ' $(GREEN)make down$(RESET)             Остановить стек\n'
	@printf ' $(GREEN)make clean$(RESET)            Полная очистка (данные + образы)\n'
	@printf ' $(GREEN)make status$(RESET)           Статус всех сервисов\n'
	@printf ' $(GREEN)make health$(RESET)           Проверить здоровье критичных сервисов\n'
	@printf '\n $(MAGENTA)Database$(RESET)\n'
	@printf ' $(GREEN)make db-up$(RESET)            Запустить только БД + прокси\n'
	@printf ' $(GREEN)make psql-0$(RESET)           Подключиться к postgres-0\n'
	@printf ' $(GREEN)make psql-1$(RESET)           Подключиться к postgres-1\n'
	@printf ' $(GREEN)make psql-sharding$(RESET)    Подключиться через ShardingSphere\n'
	@printf ' $(GREEN)make db-init$(RESET)          Применить миграции (если есть)\n'
	@printf '\n $(MAGENTA)Kafka$(RESET)\n'
	@printf ' $(GREEN)make kafka-up$(RESET)         Запустить только Kafka экосистему\n'
	@printf ' $(GREEN)make topic-list$(RESET)       Список топиков\n'
	@printf ' $(GREEN)make topic-create NAME=test$(RESET)  Создать топик\n'
	@printf ' $(GREEN)make topic-describe NAME=test$(RESET) Описание топика\n'
	@printf ' $(GREEN)make producer TOPIC=test$(RESET)     Консольный продюсер\n'
	@printf ' $(GREEN)make consumer TOPIC=test$(RESET)     Консольный консьюмер (с начала)\n'
	@printf ' $(GREEN)make schema-list$(RESET)      Список схем в Registry\n'
	@printf '\n $(MAGENTA)Monitoring$(RESET)\n'
	@printf ' $(GREEN)make monitor-up$(RESET)       Запустить только мониторинг\n'
	@printf ' $(GREEN)make ui-grafana$(RESET)       Открыть Grafana в браузере\n'
	@printf ' $(GREEN)make ui-prometheus$(RESET)    Открыть Prometheus в браузере\n'
	@printf ' $(GREEN)make metrics$(RESET)          Показать метрики Kafka Exporter\n'
	@printf '\n $(MAGENTA)Application$(RESET)\n'
	@printf ' $(GREEN)make app-up$(RESET)           Запустить приложение (если раскомментировано)\n'
	@printf ' $(GREEN)make logs-app$(RESET)         Логи приложения\n'
	@printf '\n $(MAGENTA)Utilities$(RESET)\n'
	@printf ' $(GREEN)make shell-kafka$(RESET)      Bash в контейнере Kafka\n'
	@printf ' $(GREEN)make shell-proxy$(RESET)      Bash в контейнере ShardingSphere\n'
	@printf ' $(GREEN)make wait-ready$(RESET)       Дождаться готовности всех сервисов\n'

# ==================== Stack Management ====================
.PHONY: up
up:
	$(call print_header, Starting Full Platform Stack)
	@docker compose -f $(COMPOSE_FILE) up -d --wait 2>/dev/null || \
		docker-compose -f $(COMPOSE_FILE) up -d --wait
	@sleep 3
	@make health

.PHONY: down
down:
	$(call print_header, Stopping Platform Stack)
	@docker compose -f $(COMPOSE_FILE) down 2>/dev/null || \
		docker-compose -f $(COMPOSE_FILE) down

.PHONY: clean
clean:
	$(call print_warning, WARNING: This will DELETE ALL data including databases!)
	@read -p "Are you absolutely sure? Type 'yes' to confirm: " confirm && [ "$$confirm" = "yes" ] || (echo "Aborted." && exit 1)
	@echo ""
	$(call print_info,Stopping and removing containers...)
	@docker compose -f $(COMPOSE_FILE) down -v --remove-orphans 2>/dev/null || \
		docker-compose -f $(COMPOSE_FILE) down -v --remove-orphans
	$(call print_info,Pruning unused volumes...)
	@docker volume prune -f
	$(call print_success,✓ All data purged successfully!)

.PHONY: status
status:
	$(call print_header, Service Status)
	@docker compose -f $(COMPOSE_FILE) ps --format "table {{.Names}}\t{{.State}}\t{{.Ports}}"

.PHONY: health
health:
	$(call print_header, Health Check)
	@echo ""
	@docker compose -f $(COMPOSE_FILE) ps --format "table {{.Names}}\t{{.State}}\t{{.Health}}" | grep -v "Name"
	@echo ""
	$(call print_info,Checking critical services...)
	@echo ""
	@echo "   ShardingSphere Proxy:"
	@timeout 2 bash -c "cat < /dev/null > /dev/tcp/localhost/$(SHARDING_PORT)" 2>/dev/null && \
		$(call print_success,"    ✓ Proxy is accepting connections") || \
		$(call print_error,"    ✗ Proxy is NOT ready")
	@echo ""
	@echo "   Schema Registry:"
	@curl -s -f $(SCHEMA_URL) > /dev/null 2>&1 && \
		$(call print_success,"    ✓ Schema Registry API is healthy") || \
		$(call print_error,"    ✗ Schema Registry API is NOT responding")
	@echo ""
	@echo "   Kafka Exporter:"
	@curl -s -f $(EXPORTER_URL) > /dev/null 2>&1 && \
		$(call print_success,"    ✓ Kafka Exporter is healthy") || \
		$(call print_error,"    ✗ Kafka Exporter is NOT responding")
	@echo ""
	@echo "   Prometheus:"
	@curl -s -f $(PROM_URL)/-/healthy > /dev/null 2>&1 && \
		$(call print_success,"    ✓ Prometheus is healthy") || \
		$(call print_error,"    ✗ Prometheus is NOT responding")
	@echo ""
	@echo "   Grafana:"
	@curl -s -f $(GRAFANA_URL)/api/health > /dev/null 2>&1 && \
		$(call print_success,"    ✓ Grafana is healthy") || \
		$(call print_error,"    ✗ Grafana is NOT responding")

.PHONY: wait-ready
wait-ready:
	$(call print_info,Waiting for all services to become healthy...)
	@timeout 120 bash -c 'while ! docker compose -f $(COMPOSE_FILE) ps -q 2>/dev/null | xargs docker inspect --format "{{.State.Health.Status}}" 2>/dev/null | grep -q "healthy"; do sleep 2; done' || \
		(echo "" && $(call print_error,"Timeout waiting for services to become healthy") && exit 1)
	$(call print_success,All services are healthy!)

# ==================== Database Commands ====================
.PHONY: db-up
db-up:
	$(call print_header,🗃️ Starting Database Stack)
	@docker compose -f $(COMPOSE_FILE) up -d $(POSTGRES_0) $(POSTGRES_1) $(SHARDING_PROXY) --wait 2>/dev/null || \
		docker-compose -f $(COMPOSE_FILE) up -d $(POSTGRES_0) $(POSTGRES_1) $(SHARDING_PROXY) --wait
	@sleep 5
	@make health-db

.PHONY: health-db
health-db:
	@echo ""
	@echo "  🔹 postgres-0 (port $(PG_PORT_0)):"
	@PGPASSWORD=SecurePass123! psql -h localhost -p $(PG_PORT_0) -U sharding_user -d sharding_db -c "SELECT 1" > /dev/null 2>&1 && \
		$(call print_success,"    ✓ Ready") || $(call print_error,"    ✗ Not ready")
	@echo ""
	@echo "  🔹 postgres-1 (port $(PG_PORT_1)):"
	@PGPASSWORD=SecurePass123! psql -h localhost -p $(PG_PORT_1) -U sharding_user -d sharding_db -c "SELECT 1" > /dev/null 2>&1 && \
		$(call print_success,"    ✓ Ready") || $(call print_error,"    ✗ Not ready")
	@echo ""
	@echo "  🔹 ShardingSphere Proxy (port $(SHARDING_PORT)):"
	@timeout 2 bash -c "cat < /dev/null > /dev/tcp/localhost/$(SHARDING_PORT)" 2>/dev/null && \
		$(call print_success,"    ✓ Ready") || $(call print_error,"    ✗ Not ready")

.PHONY: psql-0
psql-0:
	@PGPASSWORD=SecurePass123! psql -h localhost -p $(PG_PORT_0) -U sharding_user -d sharding_db

.PHONY: psql-1
psql-1:
	@PGPASSWORD=SecurePass123! psql -h localhost -p $(PG_PORT_1) -U sharding_user -d sharding_db

.PHONY: psql-sharding
psql-sharding:
	@PGPASSWORD=SecurePass123! psql -h localhost -p $(SHARDING_PORT) -U sharding_user -d sharding_db

.PHONY: db-init
db-init:
	$(call print_info,Applying database migrations...)
	@echo "  Implement your migration tool here (Flyway/Liquibase)"
	@echo "Example: docker run --network=$(PROJECT_NAME)_app-network flyway/flyway ..."
	@sleep 1
	@make health-db

# ==================== Kafka Commands ====================
.PHONY: kafka-up
kafka-up:
	$(call print_header, Starting Kafka Ecosystem)
	@docker compose -f $(COMPOSE_FILE) up -d $(ZOOKEEPER) $(KAFKA) $(SCHEMA_REGISTRY) $(KAFKA_EXPORTER) --wait 2>/dev/null || \
		docker-compose -f $(COMPOSE_FILE) up -d $(ZOOKEEPER) $(KAFKA) $(SCHEMA_REGISTRY) $(KAFKA_EXPORTER) --wait
	@sleep 10
	@make health-kafka

.PHONY: health-kafka
health-kafka:
	@echo ""
	@echo "   Kafka Broker:"
	@docker exec $(KAFKA) kafka-broker-api-versions --bootstrap-server localhost:9092 > /dev/null 2>&1 && \
		$(call print_success,"    ✓ Broker is ready") || $(call print_error,"    ✗ Broker is NOT ready")
	@echo ""
	@echo "   Schema Registry:"
	@curl -s -f $(SCHEMA_URL) > /dev/null 2>&1 && \
		$(call print_success,"    ✓ Schema Registry is ready") || $(call print_error,"    ✗ Schema Registry is NOT ready")
	@echo ""
	@echo "   Kafka Exporter:"
	@curl -s -f $(EXPORTER_URL) > /dev/null 2>&1 && \
		$(call print_success,"    ✓ Exporter is ready") || $(call print_error,"    ✗ Exporter is NOT ready")

.PHONY: topic-list
topic-list:
	@docker exec $(KAFKA) kafka-topics --bootstrap-server localhost:9092 --list

.PHONY: topic-create
topic-create:
ifndef NAME
	$(error NAME parameter is required. Usage: make topic-create NAME=my-topic [PARTITIONS=3] [REPLICATION=1])
endif
	$(call print_info,Creating topic '$(NAME)'...)
	@docker exec $(KAFKA) kafka-topics \
		--bootstrap-server localhost:9092 \
		--create \
		--topic $(NAME) \
		--partitions $(or $(PARTITIONS),3) \
		--replication-factor $(or $(REPLICATION),1)
	$(call print_success,Topic '$(NAME)' created successfully!)

.PHONY: topic-describe
topic-describe:
ifndef NAME
	$(error NAME parameter is required. Usage: make topic-describe NAME=my-topic)
endif
	@docker exec $(KAFKA) kafka-topics \
		--bootstrap-server localhost:9092 \
		--describe \
		--topic $(NAME)

.PHONY: topic-delete
topic-delete:
ifndef NAME
	$(error NAME parameter is required. Usage: make topic-delete NAME=my-topic)
endif
	$(call print_warning,⚠ Deleting topic '$(NAME)'...)
	@docker exec $(KAFKA) kafka-topics \
		--bootstrap-server localhost:9092 \
		--delete \
		--topic $(NAME)
	$(call print_success,Topic '$(NAME)' deleted)

.PHONY: producer
producer:
ifndef TOPIC
	$(error TOPIC parameter is required. Usage: make producer TOPIC=my-topic)
endif
	$(call print_info,Starting console producer for topic '$(TOPIC)'...)
	@echo "Type messages below (Ctrl+D to exit):"
	@docker exec -i $(KAFKA) kafka-console-producer \
		--bootstrap-server localhost:9092 \
		--topic $(TOPIC)

.PHONY: consumer
consumer:
ifndef TOPIC
	$(error TOPIC parameter is required. Usage: make consumer TOPIC=my-topic)
endif
	$(call print_info,Starting console consumer for topic '$(TOPIC)' from beginning...)
	@docker exec -i $(KAFKA) kafka-console-consumer \
		--bootstrap-server localhost:9092 \
		--topic $(TOPIC) \
		--from-beginning \
		--property print.key=true \
		--property key.separator=": "

.PHONY: schema-list
schema-list:
	@curl -s $(SCHEMA_URL) | python3 -m json.tool 2>/dev/null || \
		curl -s $(SCHEMA_URL)

.PHONY: shell-kafka
shell-kafka:
	@docker exec -it $(KAFKA) bash

# ==================== Monitoring Commands ====================
.PHONY: monitor-up
monitor-up:
	$(call print_header, Starting Monitoring Stack)
	@docker compose -f $(COMPOSE_FILE) up -d $(PROMETHEUS) $(GRAFANA) $(TEMPO) $(LOKI) $(ALLOY) --wait 2>/dev/null || \
		docker-compose -f $(COMPOSE_FILE) up -d $(PROMETHEUS) $(GRAFANA) $(TEMPO) $(LOKI) $(ALLOY) --wait
	@sleep 5
	@make health-monitor

.PHONY: health-monitor
health-monitor:
	@echo ""
	@echo "   Prometheus: $(PROM_URL)"
	@curl -s -f $(PROM_URL)/-/healthy > /dev/null 2>&1 && \
		$(call print_success,"    ✓ Ready") || $(call print_error,"    ✗ Not ready")
	@echo ""
	@echo "   Grafana: $(GRAFANA_URL) (admin/admin)"
	@curl -s -f $(GRAFANA_URL)/api/health > /dev/null 2>&1 && \
		$(call print_success,"    ✓ Ready") || $(call print_error,"    ✗ Not ready")
	@echo ""
	@echo "   Tempo: $(TEMPO_URL)"
	@curl -s -f $(TEMPO_URL)/ready > /dev/null 2>&1 && \
		$(call print_success,"    ✓ Ready") || $(call print_error,"    ✗ Not ready")
	@echo ""
	@echo "   Loki: $(LOKI_URL)"
	@curl -s -f $(LOKI_URL)/ready > /dev/null 2>&1 && \
		$(call print_success,"    ✓ Ready") || $(call print_error,"    ✗ Not ready")

.PHONY: metrics
metrics:
	@curl -s $(EXPORTER_URL) | head -50

.PHONY: ui-grafana
ui-grafana:
	@echo "Opening Grafana UI: $(GRAFANA_URL)"
	@open $(GRAFANA_URL) 2>/dev/null || xdg-open $(GRAFANA_URL) 2>/dev/null || \
		(echo "  Could not open browser. Visit manually: $(GRAFANA_URL)")

.PHONY: ui-prometheus
ui-prometheus:
	@echo "Opening Prometheus UI: $(PROM_URL)"
	@open $(PROM_URL) 2>/dev/null || xdg-open $(PROM_URL) 2>/dev/null || \
		(echo "  Could not open browser. Visit manually: $(PROM_URL)")

# ==================== Application Commands ====================
.PHONY: app-up
app-up:
	$(call print_info,Starting application services...)
	@echo "  Uncomment transaction-service in docker-compose.yml first!"
	@docker compose -f $(COMPOSE_FILE) up -d --wait 2>/dev/null || \
		docker-compose -f $(COMPOSE_FILE) up -d --wait

.PHONY: logs-app
logs-app:
	@docker compose -f $(COMPOSE_FILE) logs -f transaction-service 2>/dev/null || \
		docker-compose -f $(COMPOSE_FILE) logs -f transaction-service 2>&1 | grep -q "No such service" && \
		echo "  transaction-service is commented out in docker-compose.yml"

# ==================== Utilities ====================
.PHONY: shell-proxy
shell-proxy:
	@docker exec -it $(SHARDING_PROXY) bash

.PHONY: logs
logs:
	@docker compose -f $(COMPOSE_FILE) logs -f 2>/dev/null || \
		docker-compose -f $(COMPOSE_FILE) logs -f

.PHONY: logs-db
logs-db:
	@docker compose -f $(COMPOSE_FILE) logs -f $(POSTGRES_0) $(POSTGRES_1) $(SHARDING_PROXY) 2>/dev/null || \
		docker-compose -f $(COMPOSE_FILE) logs -f $(POSTGRES_0) $(POSTGRES_1) $(SHARDING_PROXY)

.PHONY: logs-kafka
logs-kafka:
	@docker compose -f $(COMPOSE_FILE) logs -f $(ZOOKEEPER) $(KAFKA) $(SCHEMA_REGISTRY) 2>/dev/null || \
		docker-compose -f $(COMPOSE_FILE) logs -f $(ZOOKEEPER) $(KAFKA) $(SCHEMA_REGISTRY)

.PHONY: logs-monitor
logs-monitor:
	@docker compose -f $(COMPOSE_FILE) logs -f $(PROMETHEUS) $(GRAFANA) $(TEMPO) $(LOKI) $(ALLOY) 2>/dev/null || \
		docker-compose -f $(COMPOSE_FILE) logs -f $(PROMETHEUS) $(GRAFANA) $(TEMPO) $(LOKI) $(ALLOY)

# ==================== Aliases ====================
.PHONY: start stop restart
start: up
stop: down
restart: down up