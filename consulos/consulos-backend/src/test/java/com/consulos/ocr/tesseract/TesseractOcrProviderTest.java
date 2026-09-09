package com.consulos.ocr.tesseract;

import com.consulos.ocr.OcrExtractResult;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedConstruction;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

class TesseractOcrProviderTest {

    @TempDir
    Path tempDir;

    @Test
    void providerName_shouldBeTesseract() {
        TesseractOcrProvider provider = newProviderWithIsolatedEmptyTessdata();
        assertThat(provider.providerName()).isEqualTo("tesseract");
    }

    @Test
    void extract_whenContentNull_shouldFail() {
        TesseractOcrProvider provider = newProviderWithIsolatedEmptyTessdata();

        OcrExtractResult result = provider.extract("scan.png", "image/png", null);

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).contains("vide");
    }

    @Test
    void extract_whenContentEmpty_shouldFail() {
        TesseractOcrProvider provider = newProviderWithIsolatedEmptyTessdata();

        OcrExtractResult result = provider.extract("scan.png", "image/png", new byte[0]);

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).contains("vide");
    }

    @Test
    void extract_whenDatapathHasNoTraineddata_shouldFail() {
        TesseractOcrProvider provider = newProviderWithIsolatedEmptyTessdata();

        OcrExtractResult result = provider.extract("scan.png", "image/png", new byte[]{1, 2, 3});

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).contains("Tessdata introuvable");
    }

    @Test
    void extract_whenInvalidImageBytes_shouldFailUnsupportedFormat() throws Exception {
        Path tessdata = createTessdataDir();
        TesseractOcrProvider provider = newProvider(tessdata);

        try (MockedConstruction<Tesseract> ignored = mockConstruction(Tesseract.class)) {
            OcrExtractResult result = provider.extract(
                    "broken.bin",
                    "image/png",
                    new byte[]{0x00, 0x01, 0x02, 0x03}
            );

            assertThat(result.success()).isFalse();
            assertThat(result.errorMessage()).contains("Format image non supporté");
        }
    }

    @Test
    void extract_whenTesseractReturnsText_shouldSucceed() throws Exception {
        Path tessdata = createTessdataDir();
        TesseractOcrProvider provider = newProvider(tessdata);
        byte[] png = tinyPng();

        try (MockedConstruction<Tesseract> mocked = mockConstruction(
                Tesseract.class,
                (mock, context) -> when(mock.doOCR(any(BufferedImage.class))).thenReturn("NOM: CAMARA")
        )) {
            OcrExtractResult result = provider.extract("passeport.png", "image/png", png);

            assertThat(result.success()).isTrue();
            assertThat(result.text()).contains("NOM:");
            assertThat(mocked.constructed()).hasSize(1);
        }
    }

    @Test
    void extract_whenTesseractThrows_shouldFailWithMessage() throws Exception {
        Path tessdata = createTessdataDir();
        TesseractOcrProvider provider = newProvider(tessdata);
        byte[] png = tinyPng();

        try (MockedConstruction<Tesseract> ignored = mockConstruction(
                Tesseract.class,
                (mock, context) -> when(mock.doOCR(any(BufferedImage.class)))
                        .thenThrow(new TesseractException("engine down"))
        )) {
            OcrExtractResult result = provider.extract("scan.png", "image/png", png);

            assertThat(result.success()).isFalse();
            assertThat(result.errorMessage()).contains("Échec OCR Tesseract");
            assertThat(result.errorMessage()).contains("engine down");
        }
    }

    @Test
    void extract_whenTesseractReturnsBlank_shouldFail() throws Exception {
        Path tessdata = createTessdataDir();
        TesseractOcrProvider provider = newProvider(tessdata);
        byte[] png = tinyPng();

        try (MockedConstruction<Tesseract> ignored = mockConstruction(
                Tesseract.class,
                (mock, context) -> when(mock.doOCR(any(BufferedImage.class))).thenReturn("   ")
        )) {
            OcrExtractResult result = provider.extract("scan.png", "image/png", png);

            assertThat(result.success()).isFalse();
            assertThat(result.errorMessage()).contains("aucun texte");
        }
    }

    private Path createTessdataDir() throws IOException {
        Path tessdata = tempDir.resolve("tessdata-ok");
        Files.createDirectories(tessdata);
        Files.writeString(tessdata.resolve("eng.traineddata"), "dummy-traineddata");
        return tessdata;
    }

    private TesseractOcrProvider newProvider(Path tessdata) {
        TesseractOcrProperties properties = new TesseractOcrProperties();
        properties.setDatapath(tessdata.toAbsolutePath().toString());
        properties.setLanguage("eng");
        return new TesseractOcrProvider(properties);
    }

    /**
     * Isolates {@code user.dir} so fallback tessdata candidates under the real project root
     * cannot satisfy {@code resolveDatapath}.
     */
    private TesseractOcrProvider newProviderWithIsolatedEmptyTessdata() {
        String originalUserDir = System.getProperty("user.dir");
        Path isolatedRoot = tempDir.resolve("isolated-user-dir");
        Path emptyTessdata = isolatedRoot.resolve("empty-tessdata");
        try {
            Files.createDirectories(emptyTessdata);
            System.setProperty("user.dir", isolatedRoot.toAbsolutePath().toString());
            TesseractOcrProperties properties = new TesseractOcrProperties();
            properties.setDatapath(emptyTessdata.toAbsolutePath().toString());
            return new TesseractOcrProvider(properties);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        } finally {
            System.setProperty("user.dir", originalUserDir);
        }
    }

    private static byte[] tinyPng() throws IOException {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }
}
