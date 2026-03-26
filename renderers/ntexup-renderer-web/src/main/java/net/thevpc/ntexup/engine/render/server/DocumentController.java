package net.thevpc.ntexup.engine.render.server;

import net.thevpc.ntexup.api.engine.NTxCompiledDocument;
import net.thevpc.ntexup.api.engine.NTxCompiledPage;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.api.document.NTxDocument;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.engine.impl.DefaultNTxEngine;
import net.thevpc.ntexup.api.renderer.NTxNodeRendererConfig;
import net.thevpc.nuts.io.NPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.List;

@RestController
@RequestMapping("/api/document")
@CrossOrigin(origins = "http://localhost:4200")
public class DocumentController {

    private final NTxEngine engine;

    @Autowired
    public DocumentController(NTxEngine engine) {
        this.engine = engine;
    }

    @GetMapping(value = "/images", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getDocumentImages(@RequestParam(name = "pageNumber", defaultValue = "1") int pageNumber) {
        try {
            //TODO FIX ME
            NPath file = NPath.of("/home/mohamed/Desktop/stage/ntexup/documentation/tson-doc/main.ntx")
                    .toAbsolute()
                    .normalize();

            int sizeWidth = 1200;
            int sizeHeight = 1000;
            NTxEngine e = new DefaultNTxEngine();
            NTxCompiledDocument doc = e.loadDocument(file);

            List<NTxCompiledPage> pages = doc.pages();

            if (pageNumber >= 0 && pageNumber < pages.size()) {
                byte[] imageData = engine.renderImageBytes(
                        pages.get(pageNumber),
                        new NTxNodeRendererConfig(sizeWidth, sizeHeight)
                                .withAnimate(false)
                );
                return ResponseEntity
                        .ok()
                        .contentType(MediaType.IMAGE_PNG)
                        .body(imageData);
            } else {
                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(null);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}
