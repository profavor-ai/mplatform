package com.classification.domain_system.repository;

import com.classification.domain_system.entity.Domain;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.*;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class DomainRepositoryTest {

    @Autowired
    private DomainRepository domainRepository;

    private Domain createTestDomain(String koName, String enName) {
        Domain domain = new Domain();
        Map<String, String> name = new HashMap<>();
        name.put("ko", koName);
        name.put("en", enName);
        domain.setName(name);
        Map<String, String> desc = new HashMap<>();
        desc.put("ko", koName + " 설명");
        desc.put("en", enName + " description");
        domain.setDescription(desc);
        return domain;
    }

    @Test
    @DisplayName("성공 - 도메인을 저장하고 조회한다")
    void saveAndFindById() {
        // given
        Domain domain = createTestDomain("인사", "HR");

        // when
        Domain saved = domainRepository.save(domain);
        Optional<Domain> found = domainRepository.findById(saved.getId());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getName().get("ko")).isEqualTo("인사");
        assertThat(found.get().getId()).isEqualTo(saved.getId());
    }

    @Test
    @DisplayName("성공 - 전체 도메인 목록을 조회한다")
    void findAll() {
        // given
        domainRepository.save(createTestDomain("인사", "HR"));
        domainRepository.save(createTestDomain("재무", "Finance"));

        // when
        List<Domain> all = domainRepository.findAll();

        // then
        assertThat(all).hasSize(2);
    }

    @Test
    @DisplayName("실패 - 존재하지 않는 ID로 조회 시 빈 결과")
    void findByIdNotFound() {
        // when
        Optional<Domain> found = domainRepository.findById(UUID.randomUUID());

        // then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("성공 - 도메인을 수정한다")
    void update() {
        // given
        Domain domain = createTestDomain("인사", "HR");
        Domain saved = domainRepository.save(domain);

        // when
        saved.getName().put("ko", "인사관리");
        domainRepository.save(saved);
        Optional<Domain> updated = domainRepository.findById(saved.getId());

        // then
        assertThat(updated).isPresent();
        assertThat(updated.get().getName().get("ko")).isEqualTo("인사관리");
    }
}
