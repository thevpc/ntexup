# Feature Request / Bug Fix: Support Variable Definitions (`=`, `:=`) at Any Level Inside Node Bodies

## 1. Problem Description

Currently in `ntexup`, variable assignments (`name = value` and `name := value`) work only when placed at the file / page top-level or as macro parameters:
```ntx
// Top level - WORKS
h = 1.6mm
w = 3.1mm

content-slide(title: "My Slide") {
    body {
        scene3d(...) {
            box(size: (w, l, h))
        }
    }
}
```

However, if a user writes variable definitions **inside** a component or container body (such as inside `body { ... }`, `group { ... }`, `col { ... }`, `page { ... }`, etc.):
```ntx
content-slide(title: "My Slide") {
    body {
        // Variable definitions inside container body - CURRENTLY FAILS
        h = 1.6mm
        w = 3.1mm

        scene3d(...) {
            box(size: (w, l, h))
        }
    }
}
```

### Symptoms:
1. **Engine Warning**: The compiler or evaluator logs:
   ```
   [WARNING] [classic.ntx] unsupported operator infix in h=1.6mm
   [WARNING] [classic.ntx] unsupported operator infix in w=3.1mm
   ```
2. **Undefined Variables**: `h` and `w` are not registered into the local resolution context (`context.setVar(...)`).
3. **Rendering Failures**: Subsequent children in the same body that reference `h` and `w` receive `null` or 0, resulting in collapsed geometry, broken bounds, or default fallback shapes.

---

## 2. Root Cause Analysis

### A. How Assignments Are Parsed
In [`DefaultNTxDocumentItemParserFactory.java`](file:///home/vpc/xprojects/nuts-world/nuts-productivity/ntexup/ntexup/core/ntexup-engine/src/main/java/net/thevpc/ntexup/engine/parser/DefaultNTxDocumentItemParserFactory.java):
- Binary operators `=` and `:=` are correctly mapped to `NTxNodeType.CTRL_ASSIGN` and `NTxNodeType.CTRL_ASSIGN_DEFAULT`:
  ```java
  case BINARY_OPERATOR: {
      NBinaryOperatorElement bo = c.asBinaryOperator().get();
      switch (bo.operatorSymbol()) {
          case EQ: return parseNodeAsOpEq(c, context);
          case COLON_EQ: return parseNodeAsOpColonEq(c, context);
      }
      return parseNodeAsOpSpecial(c, context);
  }
  ```

### B. How Macro Call Bodies and Containers Are Processed
When a macro like `content-slide(...)` or `body { ... }` is invoked:
1. In `NTxCompiler.java`, `_process_call_node` packs the uncompiled body elements into `c.getCallBody()`, which is assigned to the `slot` variable:
   ```java
   assigns.add(DefaultNTxNode.ofAssign(NTxUtils.COMPONENT_SLOT_VAR_NAME, NElement.ofArray(c.getCallBody().toArray(new NElement[0])), context.source()));
   ```
2. In the template definition (e.g. `classic.ntx` or `eniso.ntx`):
   ```ntx
   @define body {
       group(position: (3, 14), size: (94, 82), ...) {
           slot
       }
   }
   ```
3. When `slot` is referenced in `compileNodeTree_name`:
   ```java
   for (NElement e : all) {
       engine.parseNode(e, context, n -> {
           new DispatchCompileNodeVisitor(visitor).visitItem(n.get(), context);
       });
   }
   ```
4. The children of `group` are collected into `node.children()`.
5. In `NTxCompiler.compileNodeTree_default` (or during container layout compilation), child nodes are compiled in child contexts. But unlike root/page-level statements or `block` scopes, `CTRL_ASSIGN` nodes inside arbitrary container children either:
   - Run in an isolated or discarded visitor scope, or
   - If container children are evaluated directly by `NTxNodeEval` without going through `compileNodeTree_assign`, `_evalBinaryOperator` in `NTxNodeEval.java` receives `h=1.6mm`, sees that `operatorSymbol() == EQ`, which is unhandled in `_evalBinaryOperator`, and logs:
     ```java
     context.engine().log().log(NMsg.ofC("unsupported operator %s in %s", elem.asOperator().get().fixity(), NTxUtils.snippet(elem)).asWarning(), ...);
     ```
   - Furthermore, `CTRL_ASSIGN` nodes must not be treated as visible layout items (they should produce 0-size / void nodes, so they don't corrupt flow layout, flex weights, or grid row/col counts).

---

## 3. Requirements & Expected Behavior

1. **Scope and Visibility**:
   - Variable declarations (`var = value` or `var := value`) placed inside any container body (e.g., `body { ... }`, `group { ... }`, `div { ... }`, `col { ... }`, `scene3d { ... }`) MUST be evaluated and added to the current `NTxResolutionContext`.
   - The variable MUST be visible to all sibling nodes and descendant nodes that follow the declaration in source order within that scope (and nested child scopes).
   - Variables declared inside a block or container must not leak outside their enclosing scope if that container defines a new scope.

2. **No Layout Footprint**:
   - `CTRL_ASSIGN` and `CTRL_ASSIGN_DEFAULT` nodes inside a visual container MUST NOT render as visual shapes, bullets, or take up space in layouts (such as grid cells, column rows, or group bounds).

3. **Infix Warning Eliminated**:
   - Assignments inside blocks or element lists must never fall through to `NTxNodeEval._evalBinaryOperator` as unsupported infix operators.

4. **Consistency**:
   - Both `=` (override/assign) and `:=` (default assign if not already set) should be supported identically at all levels.

---

## 4. Key Files Involved

| File | Purpose / Role |
|---|---|
| [`NTxCompiler.java`](file:///home/vpc/xprojects/nuts-world/nuts-productivity/ntexup/ntexup/core/ntexup-engine/src/main/java/net/thevpc/ntexup/engine/eval/NTxCompiler.java) | Compiles nodes, manages resolution contexts, processes macro calls (`_process_call_node`), slot expansion, and assignments (`compileNodeTree_assign`). |
| [`DefaultNTxDocumentItemParserFactory.java`](file:///home/vpc/xprojects/nuts-world/nuts-productivity/ntexup/ntexup/core/ntexup-engine/src/main/java/net/thevpc/ntexup/engine/parser/DefaultNTxDocumentItemParserFactory.java) | Converts parsed TSON AST elements (`NElement`) into `NTxItem` / `NTxNode` instances, including `parseNodeAsOpEq`, `parseNodeAsOpColonEq`, and `parseNodeAsNoNameBloc`. |
| [`NTxNodeEval.java`](file:///home/vpc/xprojects/nuts-world/nuts-productivity/ntexup/ntexup/core/ntexup-engine/src/main/java/net/thevpc/ntexup/engine/eval/NTxNodeEval.java) | Evaluates expressions and binary operators (`_evalBinaryOperator`). |
| [`NTxAssignRenderer.java`](file:///home/vpc/xprojects/nuts-world/nuts-productivity/ntexup/ntexup/core/ntexup-engine/src/main/java/net/thevpc/ntexup/engine/renderer/special/NTxAssignRenderer.java) | Rendering handler for assignment nodes (must remain invisible/void in rendering). |
| [`NTxVoidBuilder.java`](file:///home/vpc/xprojects/nuts-world/nuts-productivity/ntexup/ntexup/extensions/ntexup-extension-layout/src/main/java/net/thevpc/ntexup/extension/layout/filler/NTxVoidBuilder.java) | Layout builder for invisible nodes. |

---

## 5. Verification & Test Plan

1. **Unit Test**:
   - Add a test case in `ntexup/test/ntexup-test/src/test/java/net/thevpc/ntexup/test/` with:
     ```ntx
     content-slide(title: "Test") {
         body {
             x = 10mm
             y = 20mm
             rectangle(size: (x, y))
         }
     }
     ```
   - Assert that:
     - No warning `unsupported operator infix in x=10mm` is logged.
     - The inner rectangle has computed size `(10mm, 20mm)`.
     - Only the rectangle is rendered (no extra empty child or artifact from `x = 10mm`).

2. **Integration Verification with Existing Decks**:
   - Re-test with the antenna models in `showcase` and `test-3d` where dimension variables are written directly inside `body { ... }`:
     ```bash
     nuts -e --java-version=21 -Ny ntexup image -p=15 -o target/images/
     ```
   - Confirm that the output images render accurately without having to lift variables outside the slide or page blocks.
