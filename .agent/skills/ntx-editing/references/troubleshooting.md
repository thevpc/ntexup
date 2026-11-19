# ntexup troubleshooting

The engine usually **keeps going** after an error: it logs `[SEVERE]` and still writes the
PDF/PNG with the faulty element missing. ntexup ≥ 1.2 ends every run with an error summary
and exits 1 on errors (`--strict`: also on warnings); older versions exit 0 regardless.

| Log / symptom | Cause | Fix |
|---|---|---|
| `syntax error : missing rbrace` / pages missing with no error | unclosed `{`, typically `¶ a ¶ b }` on one line (`¶` eats to end of line) | one `¶` per line or quoted strings; count braces |
| `undefined node ul` (or any node) | extension not imported, or typo | add `import("layout")` (lists/grids), `"shapes2d"`, `"latex"`, `"text"`, `"plot2d"`, `"plantuml"`, `"image"`, `"svg"`, `"shapes3d"`; or include a theme |
| `No parser for node type X` | same as above | same |
| `invalid style rule ".x"` | dot selector | `class-x: {...}` + `@(x) node(...)` |
| `invalid argument """...` in `source(...)` | comma between language and code | `source(java """...""")` |
| `unexpected for(i,in[...])` | `in` loop syntax | `for(i: [...]){...}` |
| body rendered once, `${i}` printed literally | `@for(...)` annotation form | drop the `@` |
| `invalid argument down:10` (or up/leftOf/rightOf) | no anchor-offset props exist | `position: (x, y), origin: top` |
| `unsupported operator INFIX in ...` | operator not implemented (e.g. ternary `?:`), or engine < 1.2 for comparisons/`&&`/`||` | rewrite with `if`; upgrade ntexup |
| `cannot compare X and Y` | `<`/`>` between a number and a string | compare like with like |
| `invalid expression ... : Empty token` | malformed operator expression (`1 +`) | fix the expression |
| `**`, `__`, `~~` printed literally | markup outside `text()`, or unsupported marker | use `text("...")`; italic is `*x*`, no `__x__`/`~~x~~` |
| `\n` printed as two chars | TSON strings are literal | real newline or `"""..."""` |
| LaTeX broken | doubled backslashes | write `\frac` not `\\frac` |
| image missing | path relative to wrong file | relative to the `.ntx` that contains `image(...)` |
| component not found | `@define` included after its use | include styles/components before pages |
| identity variable ignored | set after the theme include, or with `:=` | `documentTitle = "..."` **before** the theme include |
| `NoSuchMethodError` / stack trace from nuts boot | stale nuts workspace | run with `nuts -Zy ...` |
| `NoSuchElementException: No line found` | nuts prompting without a TTY | always pass `-y` |
| `repository template not found 'local'…` | first-run info | ignore |
| `took Ns to clone repo …ntexup-templates` | first-run template clone | ignore (needs network once) |
| `StackOverflowError` in `NTxNodeEval.eval` | engine bug (fixed for `[a -> b]` ranges in 1.2) | reduce to a 1-page repro; report/fix in ntexup |
| viewer won't open | no display | use `image`/`pdf` (headless) |
| GIF static in PDF | expected | GIFs animate only in the viewer |
| text overflows its box | box too small / font too big | lower `font-size` (`%P`), add `text-wrap: wrap`, enlarge container; `debug: true` on a node shows bounds |

Minimal repro deck for isolating a problem:

```tson
import("stdlib" "layout" "text" "shapes2d" "latex")
page {
    // paste the suspicious snippet here
}
```
