# Task: Generic layout hints (weight / span) on any ntexup node

## Goal

Today, grid column weights are declared on the grid itself:

```
grid(4,2,columns-weight:[1,2]){ ... }
```

Change this so that **any node can carry layout hints, and its parent container is solely responsible for interpreting them** (CSS / Android LayoutParams / WPF attached-property model). The node does not know or care what contains it.

Target syntax:

```
content-slide(title:"Arrows",variant:5){
    body{
        grid(5,2){
            miniPage(column-weight:1){
                line(from:(10,10), to:(80,90), end-arrow:simple(), color:red, stroke:3)
            }
            miniCodeNTexup(
                """
                line(from:(10,10), to:(80,90), end-arrow:simple())
                """
                column-weight:2
            )
            // ... remaining rows declare no hints
        }
    }
}
```

Note that only row 1 declares the hints, yet they must apply to the whole column.

## Step 0: Investigate before changing anything

1. Locate: the node base class / property model, the grid component (and any other containers: row/column boxes, miniPage, etc.), the property resolution mechanism (APRS/SFRS ancestor-proximity), and the style system (`styles:{ "*": {...} }`).
2. Report back briefly how `columns-weight` is currently parsed and consumed, and which containers exist.
3. Then implement per the spec below. If the codebase contradicts an assumption here, say so and propose the smallest adjustment rather than silently diverging.

## Spec

### 1. Layout hints are generic node properties
- Parsed once in the base node (not redeclared in each component's property schema), stored as a separate layout-hint bag distinct from the node's own properties.
- Container API: something like `child.layoutHint(name)` returning an optional typed value. Containers query only the hints they understand.
- Hints must also be settable through the style system (e.g. a style rule targeting a node type or id can set `column-weight`), with normal specificity rules.

### 2. Hint vocabulary
| Hint | Meaning | Understood by |
|---|---|---|
| `column-weight` | share of free horizontal space for the column | grid |
| `row-weight` | share of free vertical space for the row | grid |
| `column-span` | number of columns occupied | grid |
| `row-span` | number of rows occupied | grid |
| `weight` | share of free space along the container's main axis | linear containers (row/column boxes) |

- Default weight is 1; default span is 1.
- Weight values are positive numbers (not necessarily integers).
- Do **not** add a separate `width-weight`; one name per concept.

### 3. Resolution must NOT use ancestor inheritance
Layout hints are non-inherited. They resolve only from (a) the node's own declaration, (b) style rules that match the node itself. They must never be picked up from an ancestor via APRS/SFRS, otherwise a grid's hint would leak into descendants and nested grids.

### 4. Grid track weight resolution
Weight belongs to a track (column/row), not to a cell:
1. If the grid declares explicit track weights (legacy `columns-weight:[...]`), they win. Keep that syntax working as an override.
2. Otherwise the weight of a column = **max** of the `column-weight` values declared by non-spanning cells in that column; undeclared cells contribute nothing; if no cell declares one, the weight is 1.
3. If two cells in the same column declare *different* values, emit a diagnostic (warning, with source location) and apply the max.
4. Cells with `column-span > 1` do **not** contribute to track weights. Same logic for rows.
5. Free space is distributed proportionally to weights after fixed-size content is accounted for, consistent with the current `columns-weight` behaviour (preserve existing rendering for existing documents).

### 5. Diagnostics
- A container that receives a hint it does not understand (e.g. `column-span` on a child of a linear container) emits a warning with source location, once per node.
- Unknown hint-like names are not detected generically (they are ordinary properties), but make sure ordinary unknown-property diagnostics still fire for typos if the system has them.
- Invalid values (zero, negative, non-numeric weight; span exceeding the grid size) produce an error/warning, never a silent fallback.

### 6. Grid sizing
The example uses `grid(5,2)` for 5 rows of 2 cells; the original used `grid(4,2)` with 5 rows. Determine how the grid currently handles children beyond rows×columns (auto-grow, overflow, silently dropped?) and report it. Do not change that behaviour unless it is clearly a bug; if so, flag it and propose a fix separately.

## Backward compatibility
- `grid(..., columns-weight:[1,2])` and any existing `.ntx` documents must render identically.
- Migrate the "Arrows" slide to the new syntax as a test fixture and confirm identical output to the old version.

## Acceptance tests
1. Old syntax (`columns-weight:[1,2]`) and new syntax (`column-weight` on row-1 cells) produce identical layout.
2. Conflicting `column-weight` in two rows of the same column: warning + max applied.
3. Grid-level `columns-weight` overrides cell-level hints.
4. `column-span:2` cell does not alter track weights.
5. A nested grid inside a cell with `column-weight:3` does not inherit that hint.
6. `column-weight` on a child of a linear container: warning, no crash.
7. Hints set via a style rule apply as if declared inline.
8. `weight` on children of a linear container distributes main-axis space proportionally.

## Out of scope
- New container types, alignment/gravity hints, min/max size constraints (can be follow-ups using the same hint mechanism).

## Deliverables
- Implementation + tests above.
- A short design note (a few paragraphs) added to the docs describing the hint model and the track-weight resolution rules.
- A summary of any deviations from this spec and why.