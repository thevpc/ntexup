# ntexup-extension-scoreboard

Animated, customizable scoreboard / leaderboard component for ntexup.

```ntexup
import("scoreboard")

scoreboard(
    title: "ENISo IoT Challenge",
    subtitle: "Final results",
    scores: [
        {name: "DagDeg",  sub-name: "Vers",          score: 62.5},
        {name: "Drive X", sub-name: "Alert Guard",   score: 49},
        {name: "Next Opta", sub-name: "RoadC",       score: 79.5}
    ]
)
```

The component sorts itself, animates the score count-up and the rank
reordering, and draws a polished glass-card dashboard. In PDF/image export the
final sorted frame is rendered.

> Deck convention: if the deck includes a theme (`include(".../theme")`), put
> `import("scoreboard")` **before** the theme include. Imports are processed in
> source order, and the theme templates must already see the node type.

## Data sources

All of these are accepted by `scores:` / `data:` / `entries:` / `items:` /
`rows:` / `results:` / `ranking:`:

### 1. Inline TSON objects

```ntexup
scoreboard(scores: [
    {name: "Alice", sub-name: "Team A", score: 42, color: "#6366f1"},
    {name: "Bob",   sub-name: "Team B", score: 30}
])
```

Recognized keys: `name`, `sub-name`/`subname`, `disclosed-name`/`disclosed`,
`score`/`points`/`value`, `color`, `unit`.

### 2. Inline positional rows

```ntexup
scoreboard(scores: [
    ["Alice", 42],
    ["Bob", "Team B", 30],
    ["Carol", "Team C", "Sector 7", 27]
])
```

`[name, score]`, `[name, sub-name, score]` and
`[name, sub-name, disclosed-name, score]` are all supported.

A plain name → score map is also accepted:

```ntexup
scoreboard(scores: {Alice: 42, Bob: 30, Carol: 27})
```

### 3. Inline CSV text

```ntexup
scoreboard(scores: """
    name,sub-name,score
    Alice,Team A,42
    Bob,Team B,30
    """)
```

Comma, semicolon and tab separators are auto-detected. A header row is
optional; without one the positional rules above apply.

### 4. External file

```ntexup
scoreboard(file: "scores.csv")
scoreboard(file: "scores.tson")
scoreboard(file: "scores.json")
scoreboard(file: "scores.yaml")
```

The path is resolved relative to the deck file. TSON files may also carry
`title` / `subtitle` metadata:

```tson
{
    title: "ENISo IoT Challenge",
    scores: [
        {name: "DagDeg", score: 62.5},
        {name: "Drive X", score: 49}
    ]
}
```

## Customization

| Property | Default | Meaning |
|---|---|---|
| `title`, `subtitle` | – | Header text |
| `icon` / `logo` | – | Header image (relative path) |
| `theme` | `dark` | `dark`, `light`, `neon`, `minimal` |
| `palette` / `colors` | theme palette | List of row colours |
| `bar-color` | – | Force one colour for all bars |
| `track-color` | theme | Unfilled bar colour |
| `accent` | first palette colour | Header accent |
| `title-color`, `subtitle-color`, `name-color`, `score-color`, `rank-color` | theme | Text colours |
| `duration` | `2.6` | Score reveal duration in seconds |
| `stagger` | `0.07` | Per-row delay in seconds |
| `max` | auto | Fixed score axis maximum |
| `limit` / `top` | – | Show only the top N entries |
| `sort` | `true` | Sort by score (false keeps input order) |
| `show-title`, `show-subtitle`, `show-rank`, `show-score`, `show-unit` | mixed | Toggle parts (`show-unit` defaults to true when a unit is set) |
| `gap` | `0.16` | Row gap as a fraction of row height |
| `radius` | `0.34` | Row corner radius as a fraction of row height |
| `score-format` | `0.##` | `DecimalFormat` pattern |
| `unit` | – | Appended to the score when `show-unit` is on |
| `pulse` | `false` | Gently pulse the leader row for a short celebration |
| `pulse-duration` | `2.5` | Seconds the leader pulse lasts after the reveal |

Common ntexup properties (`at`, `size`, `background-color`, `color`,
`font-family`, `draw-contour`, ...) work as usual.
