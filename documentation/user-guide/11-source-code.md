# Source Code

## The `source` Element

Renders syntax-highlighted source code.

```tson
source(java
    """
    public static class MyClass {
        int value = 10;
        int add(int b) { value++; }
    }
    """
    font-size: 2%P,
    font-family: Monospaced
)
```

Note the structure: `source(<lang>` followed by the triple-quoted code, then style properties.

## Properties

| Property | Description | Example |
|----------|-------------|---------|
| (positional) | language | `source(java ...)` |
| `font-size` | code size | `font-size: 2%P` |
| `font-family` | code font | `font-family: Monospaced` |
| `at` | placement | `at: left` |
| `file` | load from file | `source(ntexup, file: "0030-example.tson", position: (2,2))` |

## From a File

```tson
source(ntexup, file: "0030-example.tson", position: (2, 2), font-size: 2%P)
```

## Supported Languages

From the doc-slides:

```text
text, java, c#, c++, xml, html, json
bash, fish, cmd, sql
hd, ntf, hadra, tson, ntexup
```

Common: `java`, `sql`, `xml`, `python`, `json`, `ts`, `kotlin`, `go`, `c`, etc.

## In Grid Setup

```tson
grid(4, 2, columns-weight: [1, 2]) {
    styles {
        "*": { margin: 2 }
        source: { at: left }
    }
    miniPage {
        source(java
            """
            System.out.println("hi");
            """
            font-size: 2%P,
            font-family: Monospaced
        )
    }
}
```

## Example: Language Listing

```tson
miniPage {
    source(text
        """
        text, java, c#, c++
        xml, html, json
        bash, fish, cmd, sql
        hd, ntf, hadra, tson, ntexup
        """
        font-size: 10%P
    )
}
text("Supported Languages", font-size: 3%P)
```