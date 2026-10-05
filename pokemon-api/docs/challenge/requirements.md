# Challenge requirements

> **Single source:** `tech-challenge.pdf` ("Java - Technical Interview Exercise").
> The **PDF text** column is a literal copy. The **Interpretation** column only exists where the PDF is ambiguous; every interpretation is marked and can be revisited by the developer.
> This page lists the backend **and** frontend requirements. The backend is detailed in `docs/`; the frontend in `frontend/docs/`. If something is not on this page, **it is not a requirement**. See also [Not asked for by the PDF](#not-asked-for-by-the-pdf).

## Functional

| ID | PDF text | Interpretation |
|---|---|---|
| REQ-F01 | "Construct a RESTful API using Spring Boot that communicates with the external PokeAPI." | — |
| REQ-US01 | "The system should allow users to browse Pokemon via paginated results, displaying each entry's sprite, category, mass, and a collection of their skills." | *sprite* = `sprites.front_default`; *category* = species `genera[].genus` (en); *mass* = `weight`; *skills* = `abilities`. See `pokeapi.md`. |
| REQ-US01-NTH | "Nice to have: Implement caching for service responses." | Optional. |
| REQ-US02 | "Users must be able to access comprehensive data for a chosen Pokemon, specifically viewing its image, core statistics, narrative description, and evolutionary lineage." | *image* = the documented sprite (`front_default`); *core statistics* = `stats`; *narrative description* = `flavor_text_entries` (en); *evolutionary lineage* = `evolution-chain`. |
| REQ-US03 | "Develop a mechanism to persist Pokemon data into a local relational store. This replication layer is intended to facilitate the addition of proprietary fields. Use cases include localized nomenclature, geographical metadata, or internal classification tags." | Proprietary fields: `localizedName`, `region`, `internalTags`. |
| REQ-US04 | "Enable update operations for any Pokemon currently stored within the local database. Ensure robust validation: provide 404 responses for missing records, 400 status codes for malformed payloads, and incorporate further defensive logic as required." | — |

## Technical

| ID | PDF text |
|---|---|
| REQ-T01 | "Host the code in a public Git repository" |
| REQ-T02 | "Include tests" |
| REQ-T03 | "Proper error handling" |
| REQ-T04 | "Nice to have: Implement a caching layer for PokeAPI responses" |
| REQ-T05 | "Front-end that consumes the API (language of your choice)" (listed under **Mandatory**; see [Frontend](#frontend)) |
| REQ-T06 | "Any additional functionality is welcome" (optional) |
| REQ-DB | "Establish a relational database or appropriate data storage solution containing a primary entity and a secondary collection for user management. Records must include a unique primary key and a minimum of two descriptive attributes." |
| REQ-API01 | "Construct an Java Web API facilitating comprehensive CRUD operations for the defined dataset. Ensure all endpoints utilize standard HTTP verbs, required parameters, and consistent return structures." |
| REQ-API02 | "Implement an auxiliary API for user registration, authentication, and the management of protected versus public routes." |
| REQ-DATA | "Design a specialized data access layer to manage interactions with the persistence store, providing the foundational logic for the API controllers." |
| REQ-CORE | "Develop a dedicated business logic layer to encapsulate all domain rules and data validation procedures. This layer should maintain architectural independence from both the API and the data access components." |
| REQ-TEST | "Provide thorough unit test coverage for every core component within the application suite." |

## Delivery

| ID | PDF text |
|---|---|
| REQ-DEL01 | "Provide a README file containing environment setup procedures and technical documentation." |
| REQ-DEL02 | "The solution must be pre-populated with relevant demonstration data." / "pre-populated with seeded data or mock credentials for demonstration purposes." |
| REQ-DEL03 | "Supply a Dockerfile for containerized execution." |

## Frontend

Implemented in `frontend/`; screens and rules are detailed in `frontend/docs/`.

| ID | PDF text |
|---|---|
| REQ-FE01 | "Integrate your established backend service with a modern frontend framework of your choosing (e.g., React or Vue)." |
| REQ-FE02 | "The interface must exhibit responsiveness and a user-centric design." |
| REQ-FE03 | "Execute standard CRUD operations corresponding to the defined functional use cases." |
| REQ-FE04 | "Architectural integrity: Maintain clean component organization and efficient state management." |
| REQ-FE05 | "Optional but desired: no warnings in the browser console." |

## Evaluation criteria

Clean Architecture (separation of concerns, independent components) · testing (TDD preferred) · code quality · functionality without errors.

## Not asked for by the PDF

Do not implement any of these without an explicit decision in `docs/decisions.md`:

- User roles (USER/ADMIN). The PDF only asks for **public vs protected** routes.
- Refresh tokens, logout, password recovery, a `/me` endpoint.
- OpenAPI/Swagger documentation (the README covers "technical documentation").
- CI, architecture tests, coverage thresholds.
- Height, types, moves or any PokéAPI data beyond what US01–US04 list.
- HATEOAS, search/filters, batch or scheduled sync. (API versioning was decided in D-30: `/api/v1`.)
