# AGENTS.md

Guidance for AI coding assistants working on the Apache Camel Examples repository.

## Start here

- Apache Camel LLM index: https://camel.apache.org/llms.txt
- Camel MCP server: https://camel.apache.org/manual/camel-jbang-mcp.html — integrates with Claude Code, GitHub Copilot, Cursor, Gemini CLI and others to provide full Camel catalog context (components, endpoint options, EIPs).
- Any Apache Camel documentation page is available as LLM-friendly Markdown by replacing `.html` with `.md` in its URL.
- Prefer the Camel CLI and the Camel MCP server to inspect components, their endpoint options, and the catalog. Do not invent component URIs or options — verify them against the catalog or documentation.

## Repository layout

This repository contains standalone runnable examples for Apache Camel. Each subdirectory is a self-contained Maven project:

- `<example>/` — example root; contains its own `pom.xml` and `README.adoc` (or `README.md`)
- `<example>/src/main/java` — Java route builders and beans
- `<example>/src/main/resources` — Camel routes (YAML/XML), `application.properties`, and other resources

## Build and run

The root `pom.xml` aggregates all examples. Main branch targets the next SNAPSHOT release.
To run examples against a released version, check out the matching tag first:

```bash
git checkout tags/camel-examples-4.8.0
mvn install -DskipTests          # install root POM
cd <example> && mvn package      # build a specific example
```

See each example's own `README` for how to run and configure it.

## Writing and modifying examples

- **Do not invent component URIs or options.** Use the Camel MCP server or the catalog at https://camel.apache.org/components/latest/ to verify endpoint syntax.
- **YAML DSL**: write routes in the canonical YAML DSL format; use the Camel MCP server to validate their structure.
- **Dependencies**: use managed versions from the parent BOM (`camel-spring-boot-bom` or `camel-bom`); do not pin explicit versions unless strictly necessary.
- **Tests**: if you add or modify an example, verify it still compiles and its tests pass: `mvn verify -pl <example> -am`.

## Conventions

- Commit messages: `CAMEL-XXXX: Brief description` when linked to a JIRA issue.
- PR titles follow the same `CAMEL-XXXX: ...` pattern.
- All AI-generated contributions must identify themselves and the human operator in the PR description.
  Example: `_Hermes Agent (Claude ...) on behalf of Guillaume Nodet_`

## Resources

- Main Camel repository (components, core): https://github.com/apache/camel
- Component documentation: https://camel.apache.org/components/latest/
- Camel CLI (JBang): https://camel.apache.org/manual/camel-jbang.html
- Community support: https://camel.apache.org/community/support/
