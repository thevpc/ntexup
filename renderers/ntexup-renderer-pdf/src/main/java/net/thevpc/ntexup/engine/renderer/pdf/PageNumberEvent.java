package net.thevpc.ntexup.engine.renderer.pdf;

//import com.lowagie.text.pdf.PdfPageEventHelper;
//import com.lowagie.text.pdf.PdfWriter;
//import com.lowagie.text.pdf.PdfContentByte;
//import com.lowagie.text.pdf.BaseFont;
//import com.lowagie.text.Document;

import org.openpdf.text.Document;
import org.openpdf.text.pdf.BaseFont;
import org.openpdf.text.pdf.PdfContentByte;
import org.openpdf.text.pdf.PdfPageEventHelper;
import org.openpdf.text.pdf.PdfWriter;

public class PageNumberEvent extends PdfPageEventHelper {
    private BaseFont baseFont;

    public PageNumberEvent() {
        try {
            baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onEndPage(PdfWriter writer, Document document) {
        PdfContentByte cb = writer.getDirectContent();
        cb.beginText();
        cb.setFontAndSize(baseFont, 12);
        cb.showTextAligned(PdfContentByte.ALIGN_CENTER, "Page " + document.getPageNumber(),
                (document.right() + document.left()) / 2, document.bottom() - 20, 0);
        cb.endText();
    }
}
