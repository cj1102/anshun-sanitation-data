package com.anshun.dms.controller;

import com.anshun.dms.common.BusinessException;
import com.anshun.dms.security.JwtTokenService;
import com.anshun.dms.service.LeaseAttachmentService;
import com.anshun.dms.service.UserTokenStateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mock.web.MockMultipartFile;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LeaseAttachmentController.class)
@AutoConfigureMockMvc(addFilters = false)
class LeaseAttachmentControllerWebTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean LeaseAttachmentService attachmentService;
    @MockitoBean JwtTokenService jwtTokenService;
    @MockitoBean UserTokenStateService userTokenStateService;

    @Test
    void listRejectsInvalidLeaseId() throws Exception {
        mockMvc.perform(get("/api/leases/0/attachments"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void uploadReturnsNotFoundWhenLeaseDoesNotExist() throws Exception {
        doThrow(BusinessException.notFound("合同不存在")).when(attachmentService).upload(eq(99L), any(), any());
        MockMultipartFile file = new MockMultipartFile("file", "contract.pdf", "application/pdf", "test".getBytes());
        mockMvc.perform(multipart("/api/leases/99/attachments").file(file))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("合同不存在"));
    }
}
