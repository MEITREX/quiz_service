ProjectName := gits

start-docker:
	docker compose --project-name $(ProjectName) up -d
build-docker:
	docker compose --project-name $(ProjectName) build
stop-docker:
	docker compose --project-name $(ProjectName) down
status-docker:
	docker compose --project-name $(ProjectName) ps
