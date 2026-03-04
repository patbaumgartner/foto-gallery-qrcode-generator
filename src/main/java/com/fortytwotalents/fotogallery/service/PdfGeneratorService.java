package com.fortytwotalents.fotogallery.service;

import com.fortytwotalents.fotogallery.model.GalleryCode;
import com.fortytwotalents.fotogallery.model.PdfOptions;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;

@Service
public class PdfGeneratorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PdfGeneratorService.class);

    private static final float MARGIN = 30f;

    private static final float CELL_PADDING = 14f;

    private static final float TEXT_HEIGHT = 40f;

    private static final float CODE_FONT_SIZE = 14f;

    private static final float EVENT_NAME_FONT_SIZE = 12f;

    private static final float EVENT_NAME_GAP = 3f;

    private static final float CUTTING_LINE_DASH = 4f;

    private static final float CUTTING_LINE_GAP = 4f;

    private static final float CUTTING_LINE_WIDTH = 0.5f;

    private static final float CUTTING_LINE_GRAY = 0.7f;

    public int createPdf(List<GalleryCode> codes, LinkedHashMap<GalleryCode, BufferedImage> qrImages, String baseUrl,
            PdfOptions options) throws IOException {

        int gridColumns = options.gridColumns();
        int gridRows = options.gridRows();
        boolean showCuttingLines = options.showCuttingLines();
        String eventName = options.eventName();
        Path outputPath = options.outputPath();

        int codesPerPage = gridColumns * gridRows;

        float pageWidth = PDRectangle.A4.getWidth();
        float pageHeight = PDRectangle.A4.getHeight();
        float usableWidth = pageWidth - 2 * MARGIN;
        float usableHeight = pageHeight - 2 * MARGIN;
        float cellWidth = usableWidth / gridColumns;
        float cellHeight = usableHeight / gridRows;
        float innerWidth = cellWidth - 2 * CELL_PADDING;
        float innerHeight = cellHeight - 2 * CELL_PADDING;
        float qrSize = Math.min(innerWidth, innerHeight - TEXT_HEIGHT);

        int totalPages = 0;

        try (PDDocument document = new PDDocument()) {
            PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            boolean hasEventName = eventName != null && !eventName.isBlank();

            for (int i = 0; i < codes.size(); i++) {
                int indexOnPage = i % codesPerPage;

                if (indexOnPage == 0) {
                    PDPage page = new PDPage(PDRectangle.A4);
                    document.addPage(page);
                    totalPages++;
                }

                PDPage currentPage = document.getPage(document.getNumberOfPages() - 1);
                GalleryCode code = codes.get(i);
                BufferedImage qrImage = qrImages.get(code);

                int col = indexOnPage % gridColumns;
                int row = indexOnPage / gridColumns;

                float cellX = MARGIN + col * cellWidth;
                float cellY = pageHeight - MARGIN - (row + 1) * cellHeight;

                // Inner area starts at cellX + CELL_PADDING, cellY + CELL_PADDING
                float innerX = cellX + CELL_PADDING;
                float innerY = cellY + CELL_PADDING;

                float qrX = innerX + (innerWidth - qrSize) / 2;
                float qrY = innerY + TEXT_HEIGHT;

                byte[] imageBytes = toByteArray(qrImage);
                PDImageXObject pdImage = PDImageXObject.createFromByteArray(document, imageBytes, "qr-" + code.code());

                try (PDPageContentStream content = new PDPageContentStream(document, currentPage,
                        PDPageContentStream.AppendMode.APPEND, true, true)) {
                    content.drawImage(pdImage, qrX, qrY, qrSize, qrSize);

                    String codeLabel = code.code();
                    float codeLabelWidth = fontBold.getStringWidth(codeLabel) / 1000 * CODE_FONT_SIZE;
                    float codeLabelX = innerX + (innerWidth - codeLabelWidth) / 2;
                    float codeLabelY = innerY + (TEXT_HEIGHT - CODE_FONT_SIZE) / 2;

                    if (hasEventName) {
                        // Center both lines as a block
                        float combinedHeight = CODE_FONT_SIZE + EVENT_NAME_GAP + EVENT_NAME_FONT_SIZE;
                        float blockStartY = innerY + (TEXT_HEIGHT - combinedHeight) / 2;

                        content.beginText();
                        content.setFont(fontBold, CODE_FONT_SIZE);
                        content.newLineAtOffset(codeLabelX, blockStartY);
                        content.showText(codeLabel);
                        content.endText();

                        float eventNameWidth = fontRegular.getStringWidth(eventName) / 1000 * EVENT_NAME_FONT_SIZE;
                        float eventNameX = innerX + (innerWidth - eventNameWidth) / 2;
                        float eventNameY = blockStartY + CODE_FONT_SIZE + EVENT_NAME_GAP;

                        content.beginText();
                        content.setFont(fontRegular, EVENT_NAME_FONT_SIZE);
                        content.newLineAtOffset(eventNameX, eventNameY);
                        content.showText(eventName);
                        content.endText();
                    } else {
                        content.beginText();
                        content.setFont(fontBold, CODE_FONT_SIZE);
                        content.newLineAtOffset(codeLabelX, codeLabelY);
                        content.showText(codeLabel);
                        content.endText();
                    }
                }
            }

            if (showCuttingLines) {
                for (int p = 0; p < document.getNumberOfPages(); p++) {
                    PDPage page = document.getPage(p);
                    drawCuttingLines(document, page, pageWidth, pageHeight, gridColumns, gridRows, cellWidth,
                            cellHeight);
                }
            }

            document.save(outputPath.toFile());
        }

        LOGGER.atInfo()
                .addArgument(outputPath)
                .addArgument(totalPages)
                .addArgument(() -> codes.size())
                .log("Generated PDF: {} ({} pages, {} codes)");
        return totalPages;
    }

    private void drawCuttingLines(PDDocument document, PDPage page, float pageWidth, float pageHeight, int gridColumns,
            int gridRows, float cellWidth, float cellHeight) throws IOException {
        try (PDPageContentStream content = new PDPageContentStream(document, page,
                PDPageContentStream.AppendMode.APPEND, true, true)) {
            content.setStrokingColor(CUTTING_LINE_GRAY, CUTTING_LINE_GRAY, CUTTING_LINE_GRAY);
            content.setLineWidth(CUTTING_LINE_WIDTH);
            content.setLineDashPattern(new float[] { CUTTING_LINE_DASH, CUTTING_LINE_GAP }, 0);

            // Vertical lines between columns
            for (int col = 1; col < gridColumns; col++) {
                float x = MARGIN + col * cellWidth;
                content.moveTo(x, MARGIN);
                content.lineTo(x, pageHeight - MARGIN);
                content.stroke();
            }

            // Horizontal lines between rows
            for (int row = 1; row < gridRows; row++) {
                float y = pageHeight - MARGIN - row * cellHeight;
                content.moveTo(MARGIN, y);
                content.lineTo(pageWidth - MARGIN, y);
                content.stroke();
            }
        }
    }

    private byte[] toByteArray(BufferedImage image) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "PNG", baos);
            return baos.toByteArray();
        }
    }

}
