# AGENTS.md

## Always load ponytail skill

You MUST load the `ponytail` skill at the start of every session. This defines your coding approach: lazy senior dev mode with minimal changes, YAGNI, and root cause fixes.

When you begin working on any task, first call:
```
skill({ name: "ponytail" })
```

## Use codebase-memory MCP

This project is indexed in `codebase-memory` as `eventos-culturales-Vue.js`. Use its graph tools (search_graph, get_code_snippet, trace_path, get_architecture) for structural code discovery before falling back to grep/glob.
