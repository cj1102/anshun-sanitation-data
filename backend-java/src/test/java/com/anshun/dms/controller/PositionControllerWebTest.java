package com.anshun.dms.controller;

import com.anshun.dms.security.JwtTokenService;
import com.anshun.dms.service.PositionService;
import com.anshun.dms.service.UserTokenStateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PositionController.class)
@AutoConfigureMockMvc(addFilters = false)
class PositionControllerWebTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean PositionService positionService;
    @MockitoBean JwtTokenService jwtTokenService;
    @MockitoBean UserTokenStateService userTokenStateService;

    @Test
    void archiveRequiresObservedVersion() throws Exception {
        mockMvc.perform(delete("/api/positions/POS-001"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("缺少必填参数：version"));
    }

    @Test
    void archiveForwardsObservedVersion() throws Exception {
        mockMvc.perform(delete("/api/positions/POS-001").param("version", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("点位已归档"));

        verify(positionService).archive("POS-001", 3);
    }
}
