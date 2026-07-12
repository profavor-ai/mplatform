package com.classification.domain_system.service;

import com.classification.domain_system.dto.FieldDefinitionRequest;
import com.classification.domain_system.entity.ClassificationNode;
import com.classification.domain_system.entity.Domain;
import com.classification.domain_system.entity.FieldDefinition;
import com.classification.domain_system.repository.ClassificationNodeRepository;
import com.classification.domain_system.repository.DomainRepository;
import com.classification.domain_system.repository.FieldDefinitionRepository;
import com.classification.domain_system.repository.FieldGroupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FieldDefinitionServiceTest {

    @Mock private FieldDefinitionRepository fieldRepository;
    @Mock private ClassificationNodeRepository nodeRepository;
    @Mock private DomainRepository domainRepository;
    @Mock private FieldGroupRepository fieldGroupRepository;
    @Mock private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private FieldDefinitionService fieldDefinitionService;

    private UUID domainId;
    private UUID nodeId;
    private UUID fieldId;
    private ClassificationNode node;
    private Domain domain;

    @BeforeEach
    void setUp() {
        domainId = UUID.randomUUID();
        nodeId = UUID.randomUUID();
        fieldId = UUID.randomUUID();

        domain = new Domain();
        domain.setId(domainId);
        domain.setName(Map.of("ko", "테스트 도메인"));

        node = new ClassificationNode();
        node.setId(nodeId);
        node.setDomain(domain);
    }

    // ─────────────────────────────────────────────────────────────────
    // normalizeJsonStr 테스트 (private → addField를 통해 간접 검증)
    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("normalizeJsonStr (addField의 options/defaultValue를 통해 검증)")
    class NormalizeJsonStr {

        private FieldDefinition makeSavedField(String key) {
            FieldDefinition f = new FieldDefinition();
            f.setId(UUID.randomUUID());
            f.setKey(key);
            f.setIsSearchable(false);
            return f;
        }

        @Test
        @DisplayName("null 값은 null로 반환")
        void null_ReturnsNull() {
            when(nodeRepository.findById(nodeId)).thenReturn(Optional.of(node));
            FieldDefinition saved = makeSavedField("f1");
            when(fieldRepository.save(any())).thenReturn(saved);

            FieldDefinitionRequest req = new FieldDefinitionRequest();
            req.setKey("f1");
            req.setType("TEXT");
            req.setOptions(null);

            fieldDefinitionService.addField(nodeId, req);

            ArgumentCaptor<FieldDefinition> captor = ArgumentCaptor.forClass(FieldDefinition.class);
            verify(fieldRepository).save(captor.capture());
            assertThat(captor.getValue().getOptions()).isNull();
        }

        @Test
        @DisplayName("빈 문자열은 null로 반환")
        void emptyString_ReturnsNull() {
            when(nodeRepository.findById(nodeId)).thenReturn(Optional.of(node));
            FieldDefinition saved = makeSavedField("f1");
            when(fieldRepository.save(any())).thenReturn(saved);

            FieldDefinitionRequest req = new FieldDefinitionRequest();
            req.setKey("f1");
            req.setType("TEXT");
            req.setOptions("   ");

            fieldDefinitionService.addField(nodeId, req);

            ArgumentCaptor<FieldDefinition> captor = ArgumentCaptor.forClass(FieldDefinition.class);
            verify(fieldRepository).save(captor.capture());
            assertThat(captor.getValue().getOptions()).isNull();
        }

        @Test
        @DisplayName("JSON 배열은 그대로 반환")
        void jsonArray_ReturnedAsIs() {
            when(nodeRepository.findById(nodeId)).thenReturn(Optional.of(node));
            FieldDefinition saved = makeSavedField("f1");
            when(fieldRepository.save(any())).thenReturn(saved);

            FieldDefinitionRequest req = new FieldDefinitionRequest();
            req.setKey("f1");
            req.setType("SELECT");
            req.setOptions("[\"A\",\"B\"]");

            fieldDefinitionService.addField(nodeId, req);

            ArgumentCaptor<FieldDefinition> captor = ArgumentCaptor.forClass(FieldDefinition.class);
            verify(fieldRepository).save(captor.capture());
            assertThat(captor.getValue().getOptions()).isEqualTo("[\"A\",\"B\"]");
        }

        @Test
        @DisplayName("CSV 문자열은 JSON 배열로 변환")
        void csvString_ConvertedToJsonArray() {
            when(nodeRepository.findById(nodeId)).thenReturn(Optional.of(node));
            FieldDefinition saved = makeSavedField("f1");
            when(fieldRepository.save(any())).thenReturn(saved);

            FieldDefinitionRequest req = new FieldDefinitionRequest();
            req.setKey("f1");
            req.setType("SELECT");
            req.setOptions("A, B, C");

            fieldDefinitionService.addField(nodeId, req);

            ArgumentCaptor<FieldDefinition> captor = ArgumentCaptor.forClass(FieldDefinition.class);
            verify(fieldRepository).save(captor.capture());
            assertThat(captor.getValue().getOptions()).isEqualTo("[\"A\",\"B\",\"C\"]");
        }

        @Test
        @DisplayName("JSON 객체는 그대로 반환")
        void jsonObject_ReturnedAsIs() {
            when(nodeRepository.findById(nodeId)).thenReturn(Optional.of(node));
            FieldDefinition saved = makeSavedField("f1");
            when(fieldRepository.save(any())).thenReturn(saved);

            FieldDefinitionRequest req = new FieldDefinitionRequest();
            req.setKey("f1");
            req.setType("TEXT");
            req.setDefaultValue("{\"key\":\"val\"}");

            fieldDefinitionService.addField(nodeId, req);

            ArgumentCaptor<FieldDefinition> captor = ArgumentCaptor.forClass(FieldDefinition.class);
            verify(fieldRepository).save(captor.capture());
            assertThat(captor.getValue().getDefaultValue()).isEqualTo("{\"key\":\"val\"}");
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // addField 테스트
    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("addField")
    class AddField {

        @Test
        @DisplayName("존재하지 않는 nodeId 요청 시 RuntimeException 발생")
        void nodeNotFound_ThrowsException() {
            when(nodeRepository.findById(nodeId)).thenReturn(Optional.empty());

            FieldDefinitionRequest req = new FieldDefinitionRequest();
            assertThatThrownBy(() -> fieldDefinitionService.addField(nodeId, req))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Node not found");
        }

        @Test
        @DisplayName("isSearchable=true 이면 index CREATE SQL 실행")
        void isSearchable_True_CreatesIndex() {
            when(nodeRepository.findById(nodeId)).thenReturn(Optional.of(node));
            FieldDefinition saved = new FieldDefinition();
            saved.setId(UUID.randomUUID());
            saved.setKey("ticker");
            saved.setIsSearchable(true);
            when(fieldRepository.save(any())).thenReturn(saved);

            FieldDefinitionRequest req = new FieldDefinitionRequest();
            req.setKey("ticker");
            req.setType("TEXT");
            req.setIsSearchable(true);

            fieldDefinitionService.addField(nodeId, req);

            verify(jdbcTemplate).execute(contains("CREATE INDEX"));
        }

        @Test
        @DisplayName("isSearchable=false 이면 index SQL 미실행")
        void isSearchable_False_NoIndex() {
            when(nodeRepository.findById(nodeId)).thenReturn(Optional.of(node));
            FieldDefinition saved = new FieldDefinition();
            saved.setId(UUID.randomUUID());
            saved.setKey("ticker");
            saved.setIsSearchable(false);
            when(fieldRepository.save(any())).thenReturn(saved);

            FieldDefinitionRequest req = new FieldDefinitionRequest();
            req.setKey("ticker");
            req.setType("TEXT");
            req.setIsSearchable(false);

            fieldDefinitionService.addField(nodeId, req);

            verify(jdbcTemplate, never()).execute(anyString());
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // getEffectiveFields 테스트
    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("getEffectiveFields")
    class GetEffectiveFields {

        @Test
        @DisplayName("도메인 필드 + 노드 필드 순서로 합산하여 반환")
        void returnsDomainFieldsThenNodeFields() {
            FieldDefinition domainField = new FieldDefinition();
            domainField.setKey("domain_field");
            FieldDefinition nodeField = new FieldDefinition();
            nodeField.setKey("node_field");

            when(nodeRepository.findById(nodeId)).thenReturn(Optional.of(node));
            when(fieldRepository.findByDomainIdOrderByOrderAsc(domainId)).thenReturn(List.of(domainField));
            when(fieldRepository.findByDefinedAtNodeIdOrderByOrderAsc(nodeId)).thenReturn(List.of(nodeField));

            List<FieldDefinition> result = fieldDefinitionService.getEffectiveFields(nodeId);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getKey()).isEqualTo("domain_field");
            assertThat(result.get(1).getKey()).isEqualTo("node_field");
        }

        @Test
        @DisplayName("존재하지 않는 nodeId 요청 시 RuntimeException 발생")
        void nodeNotFound_ThrowsException() {
            when(nodeRepository.findById(nodeId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> fieldDefinitionService.getEffectiveFields(nodeId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Node not found");
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // updateField - index 관리 분기 테스트
    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("updateField - index 분기")
    class UpdateFieldIndex {

        private FieldDefinition existingField(boolean currentSearchable) {
            FieldDefinition f = new FieldDefinition();
            f.setId(fieldId);
            f.setKey("ticker");
            f.setIsSearchable(currentSearchable);
            f.setDefinedAtNode(node);
            return f;
        }

        @Test
        @DisplayName("isSearchable false→true 변경 시 CREATE INDEX 실행")
        void falseToTrue_CreatesIndex() {
            FieldDefinition field = existingField(false);
            when(fieldRepository.findById(fieldId)).thenReturn(Optional.of(field));
            when(fieldRepository.save(any())).thenReturn(field);

            FieldDefinitionRequest req = new FieldDefinitionRequest();
            req.setIsSearchable(true);
            req.setKey("ticker");

            fieldDefinitionService.updateField(nodeId, fieldId, req);

            verify(jdbcTemplate).execute(contains("CREATE INDEX"));
        }

        @Test
        @DisplayName("isSearchable true→false 변경 시 DROP INDEX 실행")
        void trueToFalse_DropsIndex() {
            FieldDefinition field = existingField(true);
            when(fieldRepository.findById(fieldId)).thenReturn(Optional.of(field));
            when(fieldRepository.save(any())).thenReturn(field);

            FieldDefinitionRequest req = new FieldDefinitionRequest();
            req.setIsSearchable(false);
            req.setKey("ticker");

            fieldDefinitionService.updateField(nodeId, fieldId, req);

            verify(jdbcTemplate).execute(contains("DROP INDEX"));
        }

        @Test
        @DisplayName("isSearchable 변경 없을 시 index SQL 미실행")
        void noChange_NoIndexOperation() {
            FieldDefinition field = existingField(true);
            when(fieldRepository.findById(fieldId)).thenReturn(Optional.of(field));
            when(fieldRepository.save(any())).thenReturn(field);

            FieldDefinitionRequest req = new FieldDefinitionRequest();
            req.setIsSearchable(true); // 기존과 동일
            req.setKey("ticker");

            fieldDefinitionService.updateField(nodeId, fieldId, req);

            verify(jdbcTemplate, never()).execute(anyString());
        }

        @Test
        @DisplayName("field가 해당 노드에 속하지 않을 시 RuntimeException 발생")
        void fieldNotBelongsToNode_ThrowsException() {
            UUID otherNodeId = UUID.randomUUID();
            FieldDefinition field = existingField(false);
            // field는 nodeId 노드에 속하지만, 다른 nodeId로 요청
            when(fieldRepository.findById(fieldId)).thenReturn(Optional.of(field));

            FieldDefinitionRequest req = new FieldDefinitionRequest();
            req.setIsSearchable(false);

            assertThatThrownBy(() -> fieldDefinitionService.updateField(otherNodeId, fieldId, req))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Field does not belong to the specified node");
        }
    }
}
