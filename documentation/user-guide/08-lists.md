# Lists

Lists come from the **TSON bullet markers** and ntexup renders them as `ul`/`ol` nodes.

A **bare list** needs no component — a run of bullet markers at any level is a list:

```tson
• Item one
• Item two
```

Each item can be a raw `¶` line (`•¶ ...`), a quoted string (`• "..."`), or any node (`• text("...")`):

```tson
•¶ raw single-line item
• "quoted item"
• text("**markers work**")
```

The `ul { ... }` / `ol { ... }` wrapper makes the scope explicit — equivalent to the bare form:

```tson
ul {
    • "Item one"
    • "Item two"
}
```

## Bullet Marker Characters

| Markers | List | Rendered as |
|---------|------|-------------|
| `•`, `●` (also `[.]`, `[..]`, …) | unordered | `ul` |
| `▪`, `■` (also `[#]`, `[##]`, …) | ordered | `ol` |

## Unordered Lists (`ul`)

```tson
ul {
    ¶ Hello World
    ¶ Hello World again
}
```

Items are plain strings — raw `¶` lines or bare quoted strings — rendered as plain text. For rich-text markers, wrap the item in `text()`; any node also works as an item:

```tson
ul {
    ¶ Hello world             // plain paragraph
    "Hello plain quoted"      // plain quoted string
    text("Hello **World**")   // child node -> markers processed
}
```

### Nested Unordered Lists

Nest by repeating the marker — `••` is a sub-item of `•` (in bare form), or nest `ul` inside `ul`:

```tson
• Fruit
•• Apple
•• Banana
• Vegetable
```

```tson
ul {
    text("Hello ##World##")
    ul {
        ¶ Sub 1
        ¶ Sub 2
    }
    ¶ Hello World
}
```

## Ordered Lists (`ol`)

Ordered markers are `▪`/`■`; nesting repeats the marker (`▪▪`, `▪▪▪`):

```tson
▪ Step 1
▪▪ Sub-step A
▪▪ Sub-step B
▪ Step 2
```

Or the `ol` wrapper — cross-nesting works, an ordered item may contain unordered sub-items and vice-versa:

```tson
ol {
    text("Hello ##World##")
    ol {
        ¶ Sub 1
        ¶ Sub 2
    }
    ¶ Hello World
}
```

## List with Rich Content

Items can be full nodes — text, shapes, code:

```tson
ul {
    text("First item with a long description that wraps onto several lines.", text-wrap: wrap)
    rectangle(size: (15%P, 4.5%P), background: documentColors[4])
}
```

## Bullet Alignment

### `bullet-align: top` (multi-line text)

```tson
ul(bullet-align: top) {
    text("First item with a long multi-line description that wraps onto several lines cleanly.", text-wrap: wrap)
    text("Second item also spanning multiple lines.", text-wrap: wrap)
}
```

### `bullet-align: center` (large objects)

```tson
ul(bullet-align: center) {
    rectangle(size: (15%P, 4.5%P), background: documentColors[4])
    rectangle(size: (15%P, 8.5%P), background: documentColors[1])
}
```

## List with Shape Bullets

Anything can serve between items — theme examples place small shapes:

```tson
ul(bullet-align: center) {
    rectangle(size: (32%P, 3.5%P), background: documentColors[4], color: documentColors[0])
    rectangle(size: (32%P, 3.5%P), background: documentColors[4], color: documentColors[1])
}
```

## Example: Agenda Slide

```tson
agenda-slide(title: "Rationale") {
    body {
        ul(bullet-align: top) {
            •¶ Text-based, declarative, and intuitive syntax
            •¶ Readable by humans, writable with ease
            •¶ Designed for long-lived documents
            •¶ Unmatched control over rendering
            •¶ Seamless multi-file support
            •¶ Version-control friendly (Git & more)
        }
    }
}
```

## Example: Feature List in a Card

```tson
card(accentColor: documentColors[0]) {
    kicker("CARD COMPONENT")
    ul {
        • "Defined via `@define name(args){ ... }`"
        • "Embeds caller content using `body`"
        • "Reusable across slides and layouts"
    }
}
```

## Styling Lists

Lists respond to font and margin styles:

```tson
styles {
    ul: { font-size: 2%P, margin: 2 }
    ol: { font-size: 2%P, margin: 2 }
}
```