# PlantUML Diagrams

The `plantuml` extension integrates [PlantUML](https://plantuml.com/) for diagrams. Import it:

```tson
import("plantuml")
```

## UML (Use Case / Sequence / Class / Activity)

```tson
uml(at: center, size: (100, 50)
    """
    left to right direction
    actor "Food Critic" as fc
    rectangle Restaurant {
      usecase "Eat Food" as UC1
      usecase "Pay for Food" as UC2
    }
    fc --> UC1
    fc --> UC2
    """
)
```

### Class Diagram

```tson
uml(at: center, size: (100, 50)
    """
    Object <|-- ArrayList
    List <|.. ArrayList
    interface List
    Object : equals()
    ArrayList : Object[] elementData
    ArrayList : size()
    """
)
```

### Sequence Diagram

```tson
uml(at: center, size: (100, 50)
    """
    actor Bob #red
    participant Alice
    participant "I have a really\nlong name" as L #99FF99

    Alice->Bob: Authentication Request
    Bob->Alice: Authentication Response
    Bob->L: Log transaction
    """
)
```

## Gantt Charts

```tson
gantt(size: (95, 70), at: center
    """
    [Task1] requires 4 days
    then [Task1.1] requires 4 days
    [Task1.2] starts at [Task1]'s end and requires 7 days

    [Task2] requires 5 days
    then [Task2.1] requires 4 days
    """
)

// with colors
gantt(size: (95, 40), at: center
    """
    [Prototype design] is colored in Fuchsia/FireBrick
    [Test prototype] is colored in GreenYellow/Green
    """
)
```

## Network Diagrams (nwdiag)

```tson
nwdiag(size: (100, 50), at: center
    """
    network dmz {
      web01;
      web02;
    }
    network internal {
      web01;
      web02;
      db01;
      db02;
    }
    """
)

// with groups & addresses
nwdiag(size: (100, 50), at: center
    """
    network Sample_front {
        address = "192.168.10.0/24"
        color = "red"
        group web {
          web01 [address = ".1, .2", shape = "node"]
          web02 [address = ".2, .3"]
        }
    }
    network Sample_back {
        address = "192.168.20.0/24"
        color = "palegreen"
        db01 [address = ".101", shape = database]
        db02 [address = ".102"]
        group db {
          db01;
          db02;
        }
    }
    """
)
```

## Wireframes (salt)

```tson
wireframe(size: (90, 30), at: center
    """
    {+
      Login    | "MyName   "
      Password | "****     "
      [Cancel] | [  OK   ]
    }
    """
)

wireframe(size: (90, 30), at: center
    """
    {+
    {/ <b>General | Fullscreen | Behavior | Saving }
    [X] Smooth images when zoomed
    [X] Confirm image deletion
    [ ] Show hidden images
    [Close]
    }
    """
)
```

## Diagram Element Reference

| Element | Diagram type |
|---------|--------------|
| `uml` | PlantUML generic (use-case, class, sequence, activity) |
| `gantt` | Gantt chart |
| `nwdiag` | network diagram |
| `wireframe` | UI wireframe (salt) |
| `mindmap` | mind maps |
| `wbs` | work breakdown structure |
| `regex` | regex diagram |
| `ebnf` | grammar diagram |
| `chronology` | timeline |
| `chen` | ER diagram |
| `ditaa` | ASCII art |

Each takes a `size` and inline PlantUML source string.

## Styling

```tson
uml(at: center, size: (100, 50))
```

`at` and `size` control placement. Colors follow the PlantUML source itself (e.g., `#red`, `#99FF99`).

## Example: Whole Slide

```tson
content-slide(title: "UML Use Case", variant: 7) {
    body {
        grid(4, 1, columns-weight: [2, 3]) {
            styles {
                "*": { margin: 2 }
                source: { at: left }
            }
            miniPage {
                uml(at: center, size: (100, 50)
                    """
                        left to right direction
                        actor "Food Critic" as fc
                        rectangle Restaurant {
                          usecase "Eat Food" as UC1
                          usecase "Pay for Food" as UC2
                        }
                        fc --> UC1
                        fc --> UC2
                    """
                )
            }
        }
    }
}
```