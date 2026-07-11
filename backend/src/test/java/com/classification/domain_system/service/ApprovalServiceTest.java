package com.classification.domain_system.service;

import com.classification.domain_system.base.BaseServiceTest;
import com.classification.domain_system.dto.RecordRequest;
import com.classification.domain_system.entity.ApprovalRequest;
import com.classification.domain_system.entity.ApprovalStep;
import com.classification.domain_system.entity.ClassificationNode;
import com.classification.domain_system.entity.Domain;
import com.classification.domain_system.entity.Record;
import com.classification.domain_system.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

class ApprovalServiceTest extends BaseServiceTest {

    @Mock private ApprovalRequestRepository approvalRepository;
    @Mock private ApprovalStepRepository stepRepository;
    @Mock private RecordRepository recordRepository;
    @Mock private ClassificationNodeRepository nodeRepository;
    @Mock private WorkflowConfigRepository workflowConfigRepository;
    @Mock private DataQualityService dqService;
    @Mock private RecordHistoryRepository recordHistoryRepository;
    @Mock private FieldDefinitionService fieldDefinitionService;
    @Mock private MatchingService matchingService;

    @InjectMocks
    private ApprovalService approvalService;

    private RecordRequest createRecordRequest(String data, UUID requesterId) {
        RecordRequest req = new RecordRequest();
        req.setData(data);
        req.setRequesterId(requesterId);
        req.setComment("테스트 요청");
        return req;
    }

    @Nested
    @DisplayName("requestRecordCreation")
    class RequestRecordCreation {

        @Test
        @DisplayName("성공 - 레코드 생성 결재를 요청한다")
        void success() {
            // given
            UUID nodeId = UUID.randomUUID();
            UUID requesterId = UUID.randomUUID();
            UUID domainId = UUID.randomUUID();

            Domain domain = createTestDomain(domainId, "인사", "HR");
            domain.setIdentifierFieldId(UUID.randomUUID());
            domain.setDisplayNameFieldId(UUID.randomUUID());

            ClassificationNode node = createTestNode(nodeId, domain);

            RecordRequest request = createRecordRequest("{\"name\": \"test\"}", requesterId);

            DataQualityService.DQResult dqResult = new DataQualityService.DQResult();
            dqResult.isValid = true;

            Record savedRecord = new Record();
            savedRecord.setId(UUID.randomUUID());
            savedRecord.setNode(node);

            ApprovalRequest savedApproval = new ApprovalRequest();
            savedApproval.setId(UUID.randomUUID());
            savedApproval.setSteps(new ArrayList<>());

            MatchingService.DuplicateResult dupResult = new MatchingService.DuplicateResult();
            dupResult.hasDuplicates = false;
            dupResult.duplicateRecordIds = new ArrayList<>();

            given(nodeRepository.findById(nodeId)).willReturn(Optional.of(node));
            given(dqService.validateData(eq(nodeId), any())).willReturn(dqResult);
            given(fieldDefinitionService.getEffectiveFields(nodeId)).willReturn(Collections.emptyList());
            given(matchingService.checkDuplicates(eq(nodeId), any())).willReturn(dupResult);
            given(recordRepository.save(any(Record.class))).willReturn(savedRecord);
            given(workflowConfigRepository.findByNodeIdAndActionType(any(), eq("CREATE"))).willReturn(Optional.empty());
            given(workflowConfigRepository.findByDomainIdAndNodeIdIsNullAndActionType(any(), eq("CREATE"))).willReturn(Optional.empty());
            given(approvalRepository.save(any(ApprovalRequest.class))).willReturn(savedApproval);

            // when
            ApprovalRequest result = approvalService.requestRecordCreation(nodeId, request);

            // then
            assertThat(result).isNotNull();
            verify(recordRepository).save(any(Record.class));
            verify(approvalRepository).save(any(ApprovalRequest.class));
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 노드 ID로 요청 시 예외 발생")
        void failNodeNotFound() {
            // given
            UUID nodeId = UUID.randomUUID();
            RecordRequest request = createRecordRequest("{}", UUID.randomUUID());
            given(nodeRepository.findById(nodeId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> approvalService.requestRecordCreation(nodeId, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Node not found");
        }

        @Test
        @DisplayName("실패 - 필수 필드 매핑 누락 시 예외 발생")
        void failMissingFieldMapping() {
            // given
            UUID nodeId = UUID.randomUUID();
            Domain domain = createTestDomain(UUID.randomUUID(), "인사", "HR");
            domain.setIdentifierFieldId(null);
            domain.setDisplayNameFieldId(null);
            ClassificationNode node = createTestNode(nodeId, domain);

            RecordRequest request = createRecordRequest("{}", UUID.randomUUID());
            given(nodeRepository.findById(nodeId)).willReturn(Optional.of(node));

            // when & then
            assertThatThrownBy(() -> approvalService.requestRecordCreation(nodeId, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("missing required field mappings");
        }

        @Test
        @DisplayName("실패 - 데이터 품질 검증 실패 시 예외 발생")
        void failDQValidation() {
            // given
            UUID nodeId = UUID.randomUUID();
            Domain domain = createTestDomain(UUID.randomUUID(), "인사", "HR");
            domain.setIdentifierFieldId(UUID.randomUUID());
            domain.setDisplayNameFieldId(UUID.randomUUID());
            ClassificationNode node = createTestNode(nodeId, domain);

            RecordRequest request = createRecordRequest("{}", UUID.randomUUID());

            DataQualityService.DQResult dqResult = new DataQualityService.DQResult();
            dqResult.isValid = false;
            dqResult.errors.add("Field 'name' is required.");

            given(nodeRepository.findById(nodeId)).willReturn(Optional.of(node));
            given(dqService.validateData(eq(nodeId), any())).willReturn(dqResult);

            // when & then
            assertThatThrownBy(() -> approvalService.requestRecordCreation(nodeId, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Data Quality Check Failed");
        }
    }

    @Nested
    @DisplayName("approveStep")
    class ApproveStep {

        @Test
        @DisplayName("실패 - 본인이 아닌 사용자가 승인 시 예외 발생")
        void failNotAssignee() {
            // given
            UUID stepId = UUID.randomUUID();
            UUID assigneeId = UUID.randomUUID();
            UUID otherUserId = UUID.randomUUID();

            ApprovalStep step = new ApprovalStep();
            step.setId(stepId);
            step.setAssigneeId(assigneeId);
            step.setStatus("PENDING");

            given(stepRepository.findById(stepId)).willReturn(Optional.of(step));

            // when & then
            assertThatThrownBy(() -> approvalService.approveStep(stepId, otherUserId, "승인"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not the assignee");
        }

        @Test
        @DisplayName("실패 - 이미 처리된 단계 승인 시 예외 발생")
        void failAlreadyProcessed() {
            // given
            UUID stepId = UUID.randomUUID();
            UUID assigneeId = UUID.randomUUID();

            ApprovalStep step = new ApprovalStep();
            step.setId(stepId);
            step.setAssigneeId(assigneeId);
            step.setStatus("APPROVED");

            given(stepRepository.findById(stepId)).willReturn(Optional.of(step));

            // when & then
            assertThatThrownBy(() -> approvalService.approveStep(stepId, assigneeId, "승인"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not pending");
        }
    }

    @Nested
    @DisplayName("rejectStep")
    class RejectStep {

        @Test
        @DisplayName("실패 - 본인이 아닌 사용자가 반려 시 예외 발생")
        void failNotAssignee() {
            // given
            UUID stepId = UUID.randomUUID();
            UUID assigneeId = UUID.randomUUID();
            UUID otherUserId = UUID.randomUUID();

            ApprovalStep step = new ApprovalStep();
            step.setId(stepId);
            step.setAssigneeId(assigneeId);
            step.setStatus("PENDING");

            given(stepRepository.findById(stepId)).willReturn(Optional.of(step));

            // when & then
            assertThatThrownBy(() -> approvalService.rejectStep(stepId, otherUserId, "반려"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not the assignee");
        }
    }
}
