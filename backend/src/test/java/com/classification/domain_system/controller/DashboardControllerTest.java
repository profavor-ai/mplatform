package com.classification.domain_system.controller;

import com.classification.domain_system.security.JwtUtil;
import com.classification.domain_system.service.DashboardService;
import com.classification.domain_system.service.PermissionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DashboardController.class)
@AutoConfigureMockMvc(addFilters = false)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private PermissionService permissionService;

    @MockitoBean
    private com.classification.domain_system.context.AuthContext authContext;

    @Test
    @DisplayName("getStats - DashboardService에서 반환된 통계 데이터 응답")
    void getStats_ReturnsStatsMap() throws Exception {
        Map<String, Long> stats = Map.of(
                "totalDomains", 5L,
                "pendingApprovals", 2L,
                "activeRecords", 100L
        );
        when(dashboardService.getStats()).thenReturn(stats);

        mockMvc.perform(get("/api/dashboard/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDomains").value(5))
                .andExpect(jsonPath("$.pendingApprovals").value(2))
                .andExpect(jsonPath("$.activeRecords").value(100));
    }
}
