package com.consulos.document;

import com.consulos.document.dto.DocumentDTO;
import com.consulos.document.dto.DownloadUrlDTO;
import com.consulos.testsupport.ConsulosWebMvcTest;
import com.consulos.testsupport.WithConsulosUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ConsulosWebMvcTest(controllers = DocumentController.class)
class DocumentControllerMoreWebMvcTest {

    private static final UUID DOC_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final UUID ORG_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DocumentService documentService;

    @Test
    @WithConsulosUser(role = "ROLE_CAISSIER")
    void getDownloadUrl_asCaissier_returnsOk() throws Exception {
        when(documentService.getDownloadUrl(eq(DOC_ID), eq(ORG_ID)))
                .thenReturn(new DownloadUrlDTO("https://minio/file", OffsetDateTime.parse("2026-01-01T12:00:00Z")));

        mockMvc.perform(get("/documents/{id}/download-url", DOC_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.url").value("https://minio/file"));
    }

    @Test
    @WithConsulosUser(role = "ROLE_CAISSIER")
    void getById_asCaissier_returnsForbidden() throws Exception {
        mockMvc.perform(get("/documents/{id}", DOC_ID))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
        verifyNoInteractions(documentService);
    }

    @Test
    @WithConsulosUser(role = "ROLE_AGENT")
    void upload_asAgent_returnsCreated() throws Exception {
        DocumentDTO dto = new DocumentDTO(
                DOC_ID, null, null, "scan.pdf", "scan.pdf", "PASSEPORT",
                "application/pdf", 4L, null, 1, null, null);
        when(documentService.upload(
                any(MultipartFile.class), isNull(), isNull(), eq("PASSEPORT"), isNull(), eq(USER_ID), eq(ORG_ID)))
                .thenReturn(dto);

        MockMultipartFile file = new MockMultipartFile(
                "file", "scan.pdf", "application/pdf", "data".getBytes());

        mockMvc.perform(multipart("/documents/upload").file(file).param("typeDocument", "PASSEPORT"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.nomFichier").value("scan.pdf"));
    }

    @Test
    @WithConsulosUser(role = "ROLE_ACCUEIL")
    void upload_asAccueil_returnsForbidden() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "scan.pdf", "application/pdf", "data".getBytes());

        mockMvc.perform(multipart("/documents/upload").file(file).param("typeDocument", "PASSEPORT"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
        verifyNoInteractions(documentService);
    }
}
