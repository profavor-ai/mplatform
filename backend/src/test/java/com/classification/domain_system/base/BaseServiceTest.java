package com.classification.domain_system.base;

import com.classification.domain_system.dto.DomainRequest;
import com.classification.domain_system.entity.Domain;
import com.classification.domain_system.entity.ClassificationNode;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Mockito 기반 서비스 단위 테스트의 공통 Base 클래스.
 * 테스트에서 자주 사용되는 유틸리티 메서드를 제공합니다.
 */
@ExtendWith(MockitoExtension.class)
public abstract class BaseServiceTest {

    /**
     * 다국어 이름 Map을 생성합니다.
     */
    protected Map<String, String> createI18nMap(String ko, String en) {
        Map<String, String> map = new HashMap<>();
        map.put("ko", ko);
        map.put("en", en);
        return map;
    }

    /**
     * 테스트용 Domain 엔티티를 생성합니다.
     */
    protected Domain createTestDomain(UUID id, String koName, String enName) {
        Domain domain = new Domain();
        domain.setId(id);
        domain.setName(createI18nMap(koName, enName));
        domain.setDescription(createI18nMap(koName + " 설명", enName + " description"));
        return domain;
    }

    /**
     * 테스트용 DomainRequest를 생성합니다.
     */
    protected DomainRequest createTestDomainRequest(String koName, String enName) {
        DomainRequest request = new DomainRequest();
        request.setName(createI18nMap(koName, enName));
        request.setDescription(createI18nMap(koName + " 설명", enName + " description"));
        request.setIdentifierFieldId(UUID.randomUUID());
        request.setDisplayNameFieldId(UUID.randomUUID());
        return request;
    }

    /**
     * 테스트용 ClassificationNode를 생성합니다.
     */
    protected ClassificationNode createTestNode(UUID id, Domain domain) {
        ClassificationNode node = new ClassificationNode();
        node.setId(id);
        node.setDomain(domain);
        node.setName(createI18nMap("테스트 노드", "Test Node"));
        node.setPath("/test/node");
        node.setDepth(1);
        node.setOrder(0);
        node.setIsDeleted(false);
        return node;
    }
}
