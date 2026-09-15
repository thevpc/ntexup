# ntexup Developer Guide

Developer-facing documentation for the **ntexup** codebase: architecture, engine internals, extension development, and automated testing. For end-user authoring topics see the [User Guide](../user-guide/00-index.md).

---

## Index

1. [Repository Architecture & Design Choices](01-repository-architecture.md)
   - What ntexup is, how the repo is organized, every module explained, architecture styles & decisions.

2. [The Engine Pipeline: Parse, Compile, Render](02-engine-pipeline.md)
   - TSON → raw element (lazy parse) → node tree → compiled document; **how lazy & incremental compilation works**; **how included chunks are compiled and when** (`include`, `import`); the render stage.

3. [Developing ntexup Extensions](03-extension-development.md)
   - Module skeleton, SPI registration & runtime discovery, `NTxNodeBuilder` (new component), `NTxFunction` (new function), text flavors, explicit parser/renderer registration, **common-libraries-not-chained-deps rule**, **common properties** reference.

4. [Programmatic API, Rendering & Automated PDF Testing](04-programmatic-api-testing.md)
   - The 5-line engine recipe, renderer acquisition, stream config, **headless automated tests by rendering PDFs/PNGs**, golden-image checks, CI/Docker notes.

5. [Troubleshooting](05-troubleshooting.md)
   - Debug toolkit, common `ntexup`/Maven/engine/extension problems and fixes, source map for stack traces.

---

## Cheatsheet — where the important files live

| Topic | Location |
|-------|----------|
| `NTxEngine` contract | `core/ntexup-api/.../api/engine/NTxEngine.java` |
| Engine implementation | `core/ntexup-engine/.../engine/impl/DefaultNTxEngine.java` |
| Document compiler | `core/ntexup-engine/.../engine/eval/NTxCompiler.java` |
| Lazy page materialization | `core/ntexup-engine/.../engine/impl/NTxCompiledDocumentImpl.java` (`readMore`) |
| TSON → element parser | `core/ntexup-engine/.../engine/parser/NTxDocStreamParser.java` |
| Node builder DSL | `core/ntexup-api/.../api/engine/NTxNodeBuilderContext.java` + `engine/ext/NTxNodeBuilderContextImpl.java` |
| Builder → parser/renderer/flavor | `core/ntexup-engine/.../engine/ext/CustomNTxNodeParserFromBuilder.java`, `NTxNodeRendererAsDefault.java`, `CustomNTxTextRendererFlavorFromBuilder.java` |
| Service registration & reload | `core/ntexup-engine/.../engine/impl/NtxServiceListImpl2.java`, `NTxNodeBuilderList.java` |
| Common property vocabulary | `api/document/style/NTxPropName.java` + `engine/parser/NTxStyleParser.java` (`COMMON_STYLE_PROPS`) |
| Isolation semantics (fragment/group/block) | `../specifications/isolation.md` |
| Minimal extension example | `../../tutorials/ntexup-extension-tutorial-myshape/` |
| Render samples & PDF harness | `../../test/ntexup-examples/src/main/java/net/thevpc/ntexup/examples/` |
| Node types & user properties | `../user-guide/27-node-type-reference.md`, `26-property-reference.md` |

All relative paths in this guide are from this directory (`documentation/developer-guide/`).