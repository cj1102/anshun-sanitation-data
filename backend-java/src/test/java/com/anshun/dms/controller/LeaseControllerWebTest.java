package com.anshun.dms.controller;

import com.anshun.dms.common.BusinessException;
import com.anshun.dms.security.JwtTokenService;
import com.anshun.dms.service.LeaseService;
import com.anshun.dms.service.UserTokenStateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebMvcTest(LeaseController.class)
@AutoConfigureMockMvc(addFilters = false)
class LeaseControllerWebTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean LeaseService leaseService;
    @MockitoBean JwtTokenService jwtTokenService;
    @MockitoBean UserTokenStateService userTokenStateService;

    @Test
    void listRejectsInvalidPage() throws Exception {
        mockMvc.perform(get("/api/leases").param("page", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("页码必须大于 0"));
    }

    @Test
    void createRejectsEndDateBeforeStartDate() throws Exception {
        String body = """
                {"contract_code":"CON-TEST","ad_position_code":"POS-001","lessee_code":"ENT-001",
                 "lessee_company":"测试公司","lease_rent":12.5,"lease_start_date":"2026-12-02",
                 "lease_end_date":"2026-12-01","contract_sign_date":"2026-11-20"}
                """;
        when(leaseService.create(any())).thenThrow(BusinessException.badRequest("租期结束日期必须在开始日期之后"));
        mockMvc.perform(post("/api/leases").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("租期结束日期必须在开始日期之后"));
    }

    @Test
    void archiveRequiresAndForwardsTheObservedVersion() throws Exception {
        mockMvc.perform(delete("/api/leases/21")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("合同已归档"));

        verify(leaseService).archive(21, 3);
    }

    @Test
    void archiveRejectsMissingVersion() throws Exception {
        mockMvc.perform(delete("/api/leases/21")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("数据版本不能为空"));
    }
}
