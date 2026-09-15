# Extension Development

ntexup is extensible via Java. Extensions can add new node types, functions, and text flavors.

## Extension Modules

Each extension is a separate Maven module. The repository ships a complete, runnable tutorial:
`tutorials/ntexup-extension-tutorial-myshape/`.

## Node Builder Extension (Recommended)

The modern way to add a node type is with an `NTxNodeBuilder` registered via the Java SPI.

### Example: `my-shape` node

Create `NTxMyShapeBuilder.java`:

```java
package net.thevpc.ntexup.extension.tutorials.myshape;

import net.thevpc.ntexup.api.engine.NTxNodeBuilderContext;
import net.thevpc.ntexup.api.extension.NTxNodeBuilder;
import net.thevpc.ntexup.api.renderer.NTxRendererContext;
import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;

public class NTxMyShapeBuilder implements NTxNodeBuilder {
    @Override
    public void build(NTxNodeBuilderContext builderContext) {
        builderContext
                .id("my-shape")
                .parseParam()
                .matchesNamedPair(NTxPropName.WIDTH, NTxPropName.HEIGHT, "base", "hat")
                .end()
                .renderComponent(this::render);
    }

    public void render(NTxRendererContext rendererContext) {
        NTxBounds2D b = rendererContext.selfBounds2D();
        // read properties, build an AWT shape, then:
        NTxGraphics g = rendererContext.graphics();
        g.draw(shape);
        g.fill(shape);
        rendererContext.drawContour();
    }
}
```

Key APIs:

| API | Purpose |
|-----|---------|
| `.id("my-shape")` | node type name |
| `.parseParam().matchesNamedPair(...)` | accept named parameters |
| `.end()` | finish the parameter declaration |
| `.renderComponent(this::render)` | custom rendering hook |
| `rendererContext.selfBounds2D()` | computed bounds of the node |
| `rendererContext.graphics()` | `NTxGraphics` drawing surface |
| `rendererContext.getForegroundColor(true)` | resolved foreground paint |
| `rendererContext.drawContour()` | draw the contour (if `draw-contour`) |

### Register the SPI service

Create the service file:

```
src/main/resources/META-INF/services/net.thevpc.ntexup.api.extension.NTxNodeBuilder
```

Content:

```
net.thevpc.ntexup.extension.tutorials.myshape.NTxMyShapeBuilder
```

### pom.xml

```xml
<parent>
    <groupId>net.thevpc.xprojects.builders</groupId>
    <artifactId>ntexup-builder</artifactId>
    <version>1.0.0.0</version>
    <relativePath>../../pom.xml</relativePath>
</parent>
<groupId>net.thevpc.ntexup</groupId>
<artifactId>ntexup-extension-tutorial-myshape</artifactId>
<version>1.0.0.0</version>
<dependencies>
    <dependency>
        <groupId>net.thevpc.ntexup</groupId>
        <artifactId>ntexup-api</artifactId>
        <version>1.0.0.0</version>
    </dependency>
</dependencies>
```

## Function Extension

Add functions with `NTxFunction`:

```java
public class MyFunction implements NTxFunction {
    @Override
    public String name() {
        return "myFunction";
    }

    @Override
    public Object invoke(NTxFunctionCallContext ctx) {
        // read arguments via ctx
        return "result";
    }
}
```

Register in:

```
META-INF/services/net.thevpc.ntexup.api.extension.NTxFunction
```

## Text Flavors

Add custom text markup with `NTxTextRendererFlavor`:

```java
public class MyFlavor implements NTxTextRendererFlavor {
    // parse tokens, produce rich-text tokens
}
```

## Using Your Extension

From a document:

```tson
import("myshape")
```

Then use the node:

```tson
my-shape(width: 100, height: 50, base: (80, 20), hat: (-1, -1))
```

Install the extension into Nuts so `import` can resolve it:

```bash
mvn install
nuts install net.thevpc.ntexup:ntexup-extension-tutorial-myshape
```

## Packaging & Versioning

- Target the module version `1.0.0.0` (matching the ntexup API).
- Declare the module in the root `pom.xml` `<modules>`.
- Use the `net.thevpc.ntexup` group and the `/Office/ntexup` Nuts category.

## Debugging

- Use `debug: true` on nodes to visualize bounds while authoring.
- Check workspace logs under `.nuts/ws/...`.
- Set the app trace level (or `NUTS_DEBUG=*`) to see resolution errors.

## Related

- [NTxNodeType reference](27-node-type-reference.md)
- [Components](19-components-reuse.md)
- [Import / extensions](21-multi-file-includes.md)