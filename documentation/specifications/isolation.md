# NTexUp Structural Definitions: Scoping vs. Containment
In NTexUp, every container node is defined by two dimensions:
- Context (Logic): Does this node create a new variable scope?
- Inflation (Rendering): Does this node remain in the final tree, or does it "dissolve" its children into its parent?

1. The Fragment (fragment)
- Definition: A transparent, non-scoping aggregator.
- Logic: It shares the parent's NTxResolutionContext. Any variable defined or modified inside a fragment persists in the parent's scope after the fragment is processed.
- Rendering: It inflates (dissolves). It disappears during compilation, leaving its children as direct siblings of the parent.
- Use Case: Purely organizational. Use it to group nodes for better TSON readability without affecting logic or layout.

2. The Group (group)
- Definition: A standalone, scoping unit.
- Logic: It creates a new NTxResolutionContext. It inherits from the parent, but local assignments are "trapped" inside.
- Rendering: It is retained. It remains as a node in the final Render Tree. It acts as a layout container (like a div or a Box).
- Use Case: Layout isolation. Use it when you need a visual container that also protects the external scope from internal variable changes.

3. The Block (block)
- Definition: A lexical scope boundary with transparent rendering.
- Logic: It creates a new NTxResolutionContext. Variables defined inside the block are local and discarded once the block ends.
- Rendering: It inflates (dissolves). Like the fragment, the block node itself is removed, and its compiled children are promoted to the parent.
- Use Case: Logical Isolation. Use it when you need to perform temporary calculations, loops, or conditional logic that requires a "clean" local environment, but you don't want to introduce an extra nesting layer in the final Render Tree.


| Feature          | Fragment          | Group          | Block          |
|:-----------------|:------------------|:---------------|:---------------|
| New Context?     | No (Transparent)  | Yes (Isolated) | Yes (Isolated) |
| Render Node?     | No (Dissolves)    | Yes (Retained) | No (Dissolves) |
| Variable Leakage | Leaks to Parent   | Blocked        | Blocked        |
| Impact on Tree   | Flat              | Hierarchical   | Flat           |



Implementation Note for the Peer Developer
- "The Block is essentially a Group that forgets its own name and hands its children to its parent once the work is done."

When implementing the visitor for block:
- Spawn a newContext (Child of Parent).
- Compile all children into this context.
- Add the resulting nodes directly to the parent container.
- Discard the context.