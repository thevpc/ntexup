# Troubleshooting

## Install / Setup

### "repository template not found 'local' / 'user' / 'system'"

- **Cause**: benign "first run" warnings before any template repository is cloned.
- **Fix**: none required — they are informational.

### "took Nms to clone repo github.com:thevpc/ntexup-templates"

- **Cause**: the first run clones the templates repository over network.
- **Fix**: wait; ensure network access and `git` installed. Subsequent runs are faster (cached).

### Theme not found / `include` fails for `github://` URLs

- **Cause**: no network, no git, or wrong path.
- **Fix**: check `git --version`; verify the URL matches `<user>/<repo>/<path>`.

### `nuts ntexup` command not found

- **Cause**: `nuts` not on PATH or ntexup not installed.
- **Fix**: restart terminal after Nuts install; run `nuts -y install ntexup`.

## Rendering

### Viewer opens blank / nothing renders

- Run `nuts ntexup documentation` first — if that works, your project has an error. Check the console output for a stack trace.
- Look for unmatched braces `{}`, missing comma/term.

### PDF generated but blank pages

- Use the on-screen viewer to find the wrong page. Check `page size`, ensures content placed within page bounds.
- For large `n-up` rasters, increase margins/quality.

### GIF not animating

- Animated GIFs animate **on-screen only**. PDF captures a static frame.
- Ensure `import("animated-gif")` (or `animate`) is present.

### SVG not rendering

- Requires the `svg` extension: `import("svg")`.

### 3D scene missing

- Requires `import("shapes3d")`.
- Camera settings `{azimuth, elevation}` wildly off can place objects outside view.

### Plot shows nothing / all curves missing

- Check the sampler: `plot2d(x: dtimes(-5, 5, 100)) { curve { f(x): sin(x) title: 'sin' } }`.
- Verify the function expression syntax (`sin(x)`, `pow(x, n)`, quoted strings allowed).

## Syntax Issues

### "No parser for node type X"

- The node type is not registered: you forgot `import(...)` or the extension isn't installed.
- Typos: `retangle` vs `rectangle`.

### Unexpected token errors

- TSON is literal-first. A bare word that isn't a keyword/function config can confuse parsing — quote strings: `color: "red"` if needed (bare `red` usually works as a color).
- Missing closing brace.

### `@for` loop doesn't produce slides

- Ranges use `[1 -> 5]` not `[1,5]` (that's a 2-element list).

## Style Issues

### Colors look different between viewer and PDF

- Font/color rendering can differ. Use standard color names or `#rgb`.
- Check `foreground` vs `line-color` vs `color` — they differ by element type.

### Text overflow / clipping

- Reduce `font-size` or give the text node a `size`/`wrap`.
- Check the parent container size (use `debug: true`).

## Editor / Integration

### Syntax highlighting not applied

- Re-run `nuts ntexup install-editor-syntax=vscode` and restart the editor.
- Confirm `.ntx` file type is recognized (see [Editor Integration](24-editor-integration.md)).

## Performance

### Large projects slow to render

- Reduce `segment-count` on 3D solids.
- Lower `surface` `grid-size`.
- Reduce `grid`/`n-up` pages per sheet.
- Cache remote includes by pinning branch URLs.

## Common Error Messages

| Message | Likely cause |
|---------|--------------|
| `No parser for node type ...` | missing extension/import or typo |
| `Unknown property ...` | property name typo |
| `Invalid path ...` | file/glob path wrong in `include` |
| `Cannot resolve ...` | variable/function not defined before use |
| `Number expected ...` | wrong value type for a numeric property |
| `Color expected ...` | unsupported color name |

## Useful Debugging

- `page(debug: true)` — visualize element bounds.
- Console stack traces during `show`/`pdf` point to the failing `.ntx` file and node.
- Test snippet in a minimal single-file project:

```tson
import("shapes2d")
page {
    circle(size: 100, color: red, at: center)
}
```

Run `nuts ntexup show test.ntx` and incrementally add complexity.
