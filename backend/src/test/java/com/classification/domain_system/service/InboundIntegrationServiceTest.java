package com.classification.domain_system.service;

import com.classification.domain_system.entity.IntegrationChannel;
import com.classification.domain_system.integration.DataMappingTransformer;
import com.classification.domain_system.integration.InboundIntegrationService;
import com.classification.domain_system.integration.IntegrationLogService;
import com.classification.domain_system.repository.IntegrationChannelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InboundIntegrationServiceTest {

    @Mock
    private IntegrationChannelRepository channelRepository;

    @Mock
    private DataMappingTransformer mappingTransformer;

    @Mock
    private IntegrationLogService logService;

    @InjectMocks
    private InboundIntegrationService inboundIntegrationService;

    private UUID channelId;
    private IntegrationChannel inboundChannel;

    @BeforeEach
    void setUp() {
        channelId = UUID.randomUUID();
        inboundChannel = new IntegrationChannel();
        inboundChannel.setId(channelId);
        inboundChannel.setName("Test Inbound Webhook");
        inboundChannel.setType("WEB_SERVICE");
        inboundChannel.setDirection("INBOUND");
        inboundChannel.setActive(true);
        inboundChannel.setConfigJson("{\"authType\":\"BEARER_TOKEN\",\"secretToken\":\"valid-token-123\"}");
        inboundChannel.setMappingConfigJson("{\"mappings\":[{\"targetField\":\"internalName\",\"sourceExpression\":\"payload['externalName']\"}]}");
    }

    @Test
    @DisplayName("유효한 Bearer 토큰으로 Inbound 데이터 수신 성공 테스트")
    void processInboundData_ValidBearerToken_Success() {
        // given
        String rawPayload = "{\"externalName\":\"Sample Data\"}";
        String transformedPayload = "{\"internalName\":\"Sample Data\"}";
        String authHeader = "Bearer valid-token-123";

        given(channelRepository.findById(channelId)).willReturn(Optional.of(inboundChannel));
        given(mappingTransformer.transformPayload(rawPayload, inboundChannel.getMappingConfigJson()))
                .willReturn(transformedPayload);

        // when
        String result = inboundIntegrationService.processInboundData(channelId, rawPayload, authHeader, null, null);

        // then
        assertThat(result).isEqualTo(transformedPayload);
        verify(logService).logSuccess(eq(channelId), any(), eq("INBOUND_RECEIVE"), eq(rawPayload), eq(transformedPayload));
    }

    @Test
    @DisplayName("잘못된 Bearer 토큰 전달 시 SecurityException 예외 발생 검증")
    void processInboundData_InvalidBearerToken_ThrowsException() {
        // given
        String rawPayload = "{\"externalName\":\"Sample Data\"}";
        String invalidAuthHeader = "Bearer invalid-token-xyz";

        given(channelRepository.findById(channelId)).willReturn(Optional.of(inboundChannel));

        // when & then
        assertThatThrownBy(() -> inboundIntegrationService.processInboundData(channelId, rawPayload, invalidAuthHeader, null, null))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("유효하지 않은 인증 토큰");

        verify(logService).logError(eq(channelId), any(), eq("INBOUND_RECEIVE"), eq(rawPayload), any(), any(), any(), eq(1));
    }

    @Test
    @DisplayName("API Key 헤더 인증 성공 테스트")
    void processInboundData_ValidApiKey_Success() {
        // given
        inboundChannel.setConfigJson("{\"authType\":\"API_KEY\",\"secretToken\":\"my-secret-key\"}");
        String rawPayload = "{\"externalName\":\"Sample Data\"}";
        String transformedPayload = "{\"internalName\":\"Sample Data\"}";

        given(channelRepository.findById(channelId)).willReturn(Optional.of(inboundChannel));
        given(mappingTransformer.transformPayload(rawPayload, inboundChannel.getMappingConfigJson()))
                .willReturn(transformedPayload);

        // when
        String result = inboundIntegrationService.processInboundData(channelId, rawPayload, null, "my-secret-key", null);

        // then
        assertThat(result).isEqualTo(transformedPayload);
    }

    @Test
    @DisplayName("비활성화된 Inbound 채널 요청 시 예외 발생 검증")
    void processInboundData_InactiveChannel_ThrowsException() {
        // given
        inboundChannel.setActive(false);
        given(channelRepository.findById(channelId)).willReturn(Optional.of(inboundChannel));

        // when & then
        assertThatThrownBy(() -> inboundIntegrationService.processInboundData(channelId, "{}", "Bearer valid-token-123", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("비활성화");
    }

    @Test
    @DisplayName("Outbound 방향 채널에 Inbound 요청 시 예외 발생 검증")
    void processInboundData_OutboundChannel_ThrowsException() {
        // given
        inboundChannel.setDirection("OUTBOUND");
        given(channelRepository.findById(channelId)).willReturn(Optional.of(inboundChannel));

        // when & then
        assertThatThrownBy(() -> inboundIntegrationService.processInboundData(channelId, "{}", "Bearer valid-token-123", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Inbound 연계 채널이 아닙니다");
    }
}
