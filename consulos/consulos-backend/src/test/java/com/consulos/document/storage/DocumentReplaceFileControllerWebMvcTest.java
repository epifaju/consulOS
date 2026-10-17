package com.consulos.document.storage;

import com.consulos.common.NotFoundResourceExceptionHandler;
import com.consulos.document.dto.DocumentDTO;
import com.consulos.testsupport.ConsulosWebMvcTest;
import com.consulos.testsupport.WithConsulosUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ConsulosWebMvcTest(controllers = DocumentReplaceFileController.class)
@Import(NotFoundResourceExceptionHandler.class)
class DocumentReplaceFileControllerWebMvcTest {

    private static final UUID DOC_ID = UUID.fromString("2b436033-f2ab-4fe2-82ff-be48c5e40b8d");
    private static final UUID ORG_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DocumentReplaceFileService replaceFileService;

    @Test
    @WithConsulosUser(role = "ROLE_AGENT")
    void replace_asAgent_multipart_returnsOk() throws Exception {
        DocumentDTO dto = new DocumentDTO(
                DOC_ID, null, null, "scan.pdf", "scan.pdf", "PASSEPORT",
                "application/pdf", 4L, null, 2, null, null
        );
        when(replaceFileService.replaceFile(eq(DOC_ID), any(MultipartFile.class), eq(ORG_ID))).thenReturn(dto);

        MockMultipartFile file = new MockMultipartFile(
                "file", "scan.pdf", "application/pdf", "data".getBytes());

        mockMvc.perform(multipart("/documents/{id}/replace", DOC_ID).file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(DOC_ID.toString()))
                .andExpect(jsonPath("$.data.nomFichier").value("scan.pdf"));

        verify(replaceFileService).replaceFile(eq(DOC_ID), any(MultipartFile.class), eq(ORG_ID));
    }

    @Test
    @WithConsulosUser(role = "ROLE_AGENT")
    void replace_asAgent_json_returns415() throws Exception {
        mockMvc.perform(post("/documents/{id}/replace", DOC_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.errorCode").value("UNSUPPORTED_MEDIA_TYPE"));

        verifyNoInteractions(replaceFileService);
    }

    @Test
    @WithConsulosUser(role = "ROLE_CAISSIER")
    void replace_asCaissier_returnsForbidden() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "scan.pdf", "application/pdf", "data".getBytes());

        mockMvc.perform(multipart("/documents/{id}/replace", DOC_ID).file(file))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));

        verifyNoInteractions(replaceFileService);
    }
}
