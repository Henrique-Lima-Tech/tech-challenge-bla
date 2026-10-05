# Tech Challenge

Two independent Java 25 + Spring Boot 4 projects, both built with Clean Architecture and TDD. Each one has its own README with the full details.

| Project | What it is | Run it |
|---|---|---|
| [`pokemon-api/`](pokemon-api/README.md) | REST API that consumes the [PokéAPI](https://pokeapi.co/docs/v2): paginated catalog and Pokémon details, a local copy of Pokémon with proprietary fields, full CRUD and JWT authentication. Includes a React frontend. | `cd pokemon-api && docker compose up --build`, then open http://localhost:3000 |
| [`task-management/`](task-management/README.md) | REST API for task management: user registration, JWT login and a CRUD of each user's own tasks. Built by an AI agent from a written prompt, with every step recorded in `docs/` (starter prompt, meta-prompt, plan, answered questions and validation report). | `cd task-management && ./mvnw -pl web spring-boot:run`, then call http://localhost:8080 (demo user `demo@example.com` / `demo1234`) |

Both backends listen on port 8080, so run one at a time or start the second one on another port (see its README).

`CLAUDE.md` (and `AGENTS.md`, which links to it) and `.claude/` hold the rules and commands used by the AI agent that implemented the Pokémon API.
