package com.classification.domain_system.service;

import com.classification.domain_system.base.BaseServiceTest;
import com.classification.domain_system.dto.DomainRequest;
import com.classification.domain_system.entity.Domain;
import com.classification.domain_system.repository.DomainRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

class DomainServiceTest extends BaseServiceTest {

    @Mock
    private DomainRepository domainRepository;

    @InjectMocks
    private DomainService domainService;

    @Nested
    @DisplayName("createDomain")
    class CreateDomain {

        @Test
        @DisplayName("성공 - 도메인을 정상적으로 생성한다")
        void success() {
            // given
            DomainRequest request = createTestDomainRequest("인사", "HR");
            Domain saved = createTestDomain(UUID.randomUUID(), "인사", "HR");
            given(domainRepository.save(any(Domain.class))).willReturn(saved);

            // when
            Domain result = domainService.createDomain(request);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getName().get("ko")).isEqualTo("인사");
            verify(domainRepository).save(any(Domain.class));
        }
    }

    @Nested
    @DisplayName("getAllDomains")
    class GetAllDomains {

        @Test
        @DisplayName("성공 - 전체 도메인 목록을 반환한다")
        void success() {
            // given
            List<Domain> domains = List.of(
                createTestDomain(UUID.randomUUID(), "인사", "HR"),
                createTestDomain(UUID.randomUUID(), "재무", "Finance")
            );
            given(domainRepository.findAll()).willReturn(domains);

            // when
            List<Domain> result = domainService.getAllDomains();

            // then
            assertThat(result).hasSize(2);
            verify(domainRepository).findAll();
        }
    }

    @Nested
    @DisplayName("getDomain")
    class GetDomain {

        @Test
        @DisplayName("성공 - ID로 도메인을 조회한다")
        void success() {
            // given
            UUID id = UUID.randomUUID();
            Domain domain = createTestDomain(id, "인사", "HR");
            given(domainRepository.findById(id)).willReturn(Optional.of(domain));

            // when
            Domain result = domainService.getDomain(id);

            // then
            assertThat(result.getId()).isEqualTo(id);
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 ID로 조회 시 예외 발생")
        void failNotFound() {
            // given
            UUID id = UUID.randomUUID();
            given(domainRepository.findById(id)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> domainService.getDomain(id))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Domain not found");
        }
    }

    @Nested
    @DisplayName("updateDomain")
    class UpdateDomain {

        @Test
        @DisplayName("성공 - 도메인을 정상적으로 수정한다")
        void success() {
            // given
            UUID id = UUID.randomUUID();
            Domain existing = createTestDomain(id, "인사", "HR");
            DomainRequest request = createTestDomainRequest("인사관리", "HR Management");
            given(domainRepository.findById(id)).willReturn(Optional.of(existing));
            given(domainRepository.save(any(Domain.class))).willReturn(existing);

            // when
            Domain result = domainService.updateDomain(id, request);

            // then
            assertThat(result).isNotNull();
            verify(domainRepository).save(any(Domain.class));
        }

        @Test
        @DisplayName("실패 - identifierFieldId 누락 시 예외 발생")
        void failMissingIdentifierFieldId() {
            // given
            UUID id = UUID.randomUUID();
            DomainRequest request = createTestDomainRequest("인사", "HR");
            request.setIdentifierFieldId(null);

            // when & then
            assertThatThrownBy(() -> domainService.updateDomain(id, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Identifier Field");
        }

        @Test
        @DisplayName("실패 - displayNameFieldId 누락 시 예외 발생")
        void failMissingDisplayNameFieldId() {
            // given
            UUID id = UUID.randomUUID();
            DomainRequest request = createTestDomainRequest("인사", "HR");
            request.setDisplayNameFieldId(null);

            // when & then
            assertThatThrownBy(() -> domainService.updateDomain(id, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Display Name Field");
        }
    }
}
