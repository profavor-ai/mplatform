# 6. API 스펙 (제안)

| Method | Endpoint | 설명 |
|---|---|---|
| POST | `/domains` | 도메인 생성 (식별자 필드 ID들 포함) |
| PUT | `/domains/{id}` | 도메인 스키마 수정 (다국어 지원) |
| GET | `/domains` | 도메인 목록 |
| POST | `/domains/{id}/nodes` | 특정 도메인 하위에 노드 생성 (parent_id 지정) |
| PUT | `/domains/{domainId}/nodes/{nodeId}` | 분류 노드 메타데이터 수정 (다국어 지원) |
| GET | `/nodes/{id}/tree` | 해당 노드 기준 하위 트리 조회 |
| POST | `/nodes/{id}/fields` | 해당 노드에 필드 정의 추가 |
| PUT | `/nodes/{nodeId}/fields/{fieldId}` | 필드 정의 수정 (다국어 지원) |
| DELETE | `/fields/{id}` | 필드 정의 논리 삭제 |
| GET | `/nodes/{id}/fields/effective` | 해당 노드의 EffectiveFields 조회 |
| POST | `/nodes/{id}/move` | 노드 이동 (순환 참조 검증, MISMATCHED 상태 자동 부여 로직 포함) |
| DELETE | `/nodes/{id}` | 노드 연쇄 논리 삭제 |
| POST | `/nodes/{id}/records` | 레코드 생성 (5-way 라우팅 로직 수행) |
| GET | `/nodes/{id}/records` | 레코드 목록 조회 (동적 쿼리 빌더, status=ACTIVE 필터 기본 적용) |
| POST | `/domains/{id}/references/search` | 도메인 참조 시 사용할 검색/조회 API (4.4 로직 적용) |
| GET | `/approval-requests` | 대기 중인 승인 요청 목록 조회 (권한에 따라 필터링) |
| POST | `/approval-requests/{id}/approve` | 승인 요청 승인 처리 (변경사항 실제 DB 반영) |
| POST | `/approval-requests/{id}/reject` | 승인 요청 반려 처리 |
