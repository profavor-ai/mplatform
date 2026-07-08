# 7. 예시 시나리오
1. 관리자가 도메인 `임직원` 생성. 이때 identifier_field_id에 "사번" 필드를, display_name_field_id에 "성명" 필드를 매핑.
2. 하위 노드 `정규직`, `비정규직` 생성.
3. `비정규직` 하위에 `계약직`, `파견직` 생성.
4. `임직원` 노드에 필드 `입사일(DATE)`, `이름(TEXT)` 추가.
5. `계약직` 노드에 필드 `계약종료일(DATE)` 추가.
6. `비정규직` 노드에서 `입사일` 필드를 is_removed=true로 오버라이드.
   → 결과: 정규직은 이름, 입사일 보유. 계약직은 이름, 계약종료일 보유. 파견직은 이름만 보유.
7. (이동 시나리오) `파견직`을 비정규직에서 정규직 아래로 이동.
   → 파견직의 EffectiveField에 입사일이 다시 생김. 기존 파견직 레코드들에 입사일 값이 없으므로 status='MISMATCHED'로 변경되어 기본 조회에서 숨김 처리됨.
8. (참조 시나리오) 프로젝트 도메인에서 임직원 도메인을 참조하는 담당자(REFERENCE) 필드 생성.
   → 프론트엔드에서 담당자 검색 팝업 호출 시, 백엔드는 임직원 도메인의 display_name_field_id(성명)를 기준으로 동적 쿼리를 만들어 검색 결과를 제공함.

---

# 8. 요약 ERD
```text
domain (1) ──< classification_node (1, self-referencing tree via parent_id)
domain (1) ──< field_definition (N)  // 도메인 식별자/표시명 필드 매핑 (FK)
classification_node (1) ──< field_definition (N)

classification_node (1) ──< record (N)
record (1) ──< record_table_field (N)      [type='TABLE']
record (1) ──< record_encrypted_value (N)  [is_encrypted=true]
record (1) ──< record_file (N)             [type='FILE']
record (1) ──< record_relation (N)         [type='REFERENCE']

field_definition (1) ──< record_table_field (N)
field_definition (1) ──< record_encrypted_value (N)
field_definition (1) ──< record_file (N)
field_definition (1) ──< record_relation (N)
```

---

# 9. 향후 확장 고려사항 (스펙 범위 외, 참고용)
- 다축 분류(예: 고용형태 축 + 부서 축 동시 적용)가 필요해지면 노드를 다중 트리(DAG)로 확장하거나, 분류축(Axis) 개념을 별도로 두는 설계 검토 필요
- 암호화 필드의 키 분리(필드별/도메인별 키, KMS 연동)는 1차 구현 이후 보안 요구가 커지면 검토
- 검색 패턴이 다양한 필드 조합으로 확장되면 is_searchable 기반 표현식 인덱스만으로 부족할 수 있어, 별도 검색 엔진(Elasticsearch 등) 도입을 검토
- 권한 관리(어떤 관리자가 어떤 노드/필드를 수정할 수 있는지)는 별도 스펙 필요
