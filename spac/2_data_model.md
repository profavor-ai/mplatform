# 3. 데이터 모델 (PostgreSQL 기준)

### 3.1 domain
도메인 자체의 정보뿐만 아니라, **이 도메인에 속하는 레코드(Record)를 대표하는 식별 필드**를 매핑한다. (도메인 참조 시 UI 렌더링 및 검색 키로 사용됨)

| 컬럼 | 타입 | 설명 |
|---|---|---|
| id | UUID/PK | 도메인 ID |
| name | JSONB | 도메인명 다국어 맵 (예: `{"ko":"임직원", "en":"Employee"}`) |
| description | JSONB | 도메인 설명 다국어 맵 |
| identifier_field_id | UUID/FK, nullable | 레코드 식별자 필드 (예: 사번 필드의 ID). 참조 시 검색용 키로 사용됨. |
| display_name_field_id | UUID/FK, nullable | 레코드 표시명 필드 (예: 성명 필드의 ID). 참조 시 드롭다운에 표시됨. |
| description_field_id | UUID/FK, nullable | 레코드 설명 필드 (예: 직급 필드의 ID). 참조 시 부가 정보로 표시됨. |
| created_at / updated_at | datetime | |

> **무결성 규칙:** 위 3개의 FK가 가리키는 필드 정의는 반드시 해당 도메인의 루트 노드(모든 레코드가 상속받는)에 정의되어야 한다.

### 3.2 classification_node
도메인을 루트로 하는 트리. **본 스펙에서는 도메인=root node(parent_id = null)** 로 통합 관리한다.

| 컬럼 | 타입 | 설명 |
|---|---|---|
| id | UUID/PK | 노드 ID |
| domain_id | UUID/FK | 소속 도메인 |
| parent_id | UUID/FK, nullable | 부모 노드 (null이면 도메인 루트) |
| name | JSONB | 노드명 다국어 맵 (예: `{"ko":"정규직", "en":"Regular"}`) |
| path | string | 조상 경로 캐시 (예: `/임직원/정규직/수습`), 조회 및 연쇄 작업 최적화용 |
| depth | int | 루트로부터의 깊이 |
| order | int | 형제 노드 간 정렬 순서 |
| is_deleted | boolean, default false | 논리적 삭제 여부 |
| deleted_at | datetime, nullable | 삭제 일시 |
| created_at / updated_at | datetime | |

> **무결성 규칙 (PostgreSQL 구현체):**
> 1. 순환 참조 금지(자기 자신 또는 자손을 parent로 지정 불가)
> 2. 동일 부모 하위 노드명 중복 금지 → **Partial Unique Index** 사용:
>    `CREATE UNIQUE INDEX idx_node_unique_name_active ON classification_node (domain_id, parent_id, name) WHERE is_deleted = false;`

### 3.3 field_definition
필드는 **특정 classification_node에 정의**되며, 그 노드와 모든 하위 노드에서 유효하다.

| 컬럼 | 타입 | 설명 |
|---|---|---|
| id | UUID/PK | 필드 ID |
| defined_at_node_id | UUID/FK | 이 필드를 최초로 정의한 노드 |
| name | JSONB | 필드명 다국어 맵 (예: `{"ko":"입사일", "en":"Hire Date"}`) |
| key | string | 시스템용 식별자 (snake_case, 노드 내 유일) |
| type | enum | 필드 타입 (아래 3.3.1 참조) |
| options | JSON, nullable | 타입별 세부 설정 및 UI/Validation 힌트 (아래 3.3.2 참조) |
| required | boolean | 필수 여부 |
| default_value | JSON, nullable | 기본값 |
| order | int | 표시 순서 |
| is_removed | boolean, default false | 상속된 필드를 이 노드부터 제외시킬지 여부 (Soft delete형 오버라이드) |
| is_multi_value | boolean, default false | 하나의 필드에 여러 값(배열)을 허용할지 여부 (FILE, REFERENCE 타입은 별도 테이블로 관리하므로 불필요) |
| is_table | boolean, default false | 필드 값이 테이블형(행x열) 데이터인지 여부 |
| is_encrypted | boolean, default false | 값을 암호화하여 저장해야 하는지 여부 (FILE, REFERENCE 타입에는 적용 불가) |
| is_searchable | boolean, default false | 검색/필터링 대상 필드로 지정할지 여부 (인덱싱 대상) |
| is_highlighted | boolean, default false | 필드 표시 시 강조(Highlight) 여부 |
| created_at / updated_at | datetime | |

#### 3.3.1 필드 타입 (type Enum)
`TEXT`, `NUMBER`, `DATE`, `TIME`, `I18N`, `RICH_TEXT`, `SELECT`, `MULTI_SELECT`, `TABLE`, `FILE`, `REFERENCE`

#### 3.3.2 필드 옵션 (options JSON) 권장 스펙
프론트엔드 UI 렌더링 및 백엔드 Validation을 위해 타입별로 아래 구조를 준수한다.

| type | options JSON 스펙 예시 |
| :--- | :--- |
| **TEXT** | `{ "max_length": 50, "placeholder": "이름 입력" }` |
| **NUMBER** | `{ "min": 0, "max": 100, "step": 0.1, "unit": "kg" }` |
| **DATE** | `{ "format": "YYYY-MM-DD" }` |
| **TIME** | `{ "format": "HH:mm" }` |
| **I18N** | `{ "supported_locales": ["ko", "en", "ja"] }` |
| **RICH_TEXT**| `{ "max_length": 5000, "toolbar": ["bold", "image"] }` |
| **SELECT** | `{ "choices": [ {"value": "male", "label": "남성"}, {"value": "female", "label": "여성"} ] }` |
| **MULTI_SELECT**| `{ "choices": [ {"value": "reading", "label": "독서"} ], "max_count": 3 }` |
| **TABLE** | `{ "columns": [ {"key": "company", "type": "TEXT", "required": true}, {"key": "start_date", "type": "DATE"} ] }` *(행 내부 스키마 정의)* |
| **FILE** | `{ "allowed_extensions": ["jpg", "pdf"], "max_size_mb": 10, "max_count": 5 }` |
| **REFERENCE**| `{ "target_domain_ids": ["참조가능도메인_UUID"], "is_multi": false }` |

### 3.4 실제 데이터 저장 구조 (5-way Routing)

필드 정의는 메타데이터일 뿐이며, 실제 값은 필드의 속성에 따라 **5개의 저장소로 라우팅**된다. (EAV 패턴 및 필드별 테이블 생성 방식 지양)

#### (1) record — 기본 레코드 + 일반 필드 (JSONB)
스칼라 값, 다중값(배열), 다국어, 선택형 등을 저장한다.

| 컬럼 | 타입 | 설명 |
|---|---|---|
| id | UUID/PK | 레코드 ID |
| node_id | UUID/FK | 어떤 분류 노드에 속하는 데이터인지 |
| status | enum | 레코드 상태 (`DRAFT`, `PENDING_APPROVAL`, `ACTIVE`, `INACTIVE`, `MISMATCHED`, `REJECTED`) |
| data | JSONB | 일반 필드 값 모음. 예: `{"이름":"홍길동", "보유자격증":["정보처리기사"]}` |
| created_at / updated_at | datetime | |

> **검색 최적화:** `is_searchable=true`인 필드는 표현식 인덱스 생성. (예: `CREATE INDEX idx_record_name ON record ((data->>'이름'));`)

#### (1-2) approval_request — 거버넌스 승인 워크플로우 관리 테이블
데이터 및 스키마 변경 사항에 대한 결재/승인 상태를 관리한다.

| 컬럼 | 타입 | 설명 |
|---|---|---|
| id | UUID/PK | 승인 요청 ID |
| target_type | enum | 변경 대상 유형 (`SCHEMA`, `RECORD`) |
| target_id | UUID | 변경 대상 ID (노드 ID 또는 레코드 ID) |
| requester_id | UUID | 요청자 사용자 ID |
| approver_id | UUID, nullable | 승인자 사용자 ID |
| status | enum | 승인 상태 (`PENDING`, `APPROVED`, `REJECTED`) |
| changes | JSONB | 변경될 내용 (예: 레코드 수정 시 새로운 data JSON, 스키마 변경 시 필드 정의) |
| created_at / updated_at | datetime | |

#### (2) record_table_field — 테이블형 필드 (`type='TABLE'`)
한 필드가 여러 행 x 여러 컬럼 구조를 가질 때 사용.

| 컬럼 | 타입 | 설명 |
|---|---|---|
| id | UUID/PK | |
| record_id | UUID/FK | 소속 레코드 |
| field_id | UUID/FK | 어떤 필드 정의인지 |
| row_order | int | 행 순서 |
| row_data | JSONB | 한 행의 데이터 (예: `{"회사":"A","기간":"2020-2022"}`) |

#### (3) record_encrypted_value — 암호화 필드 (`is_encrypted=true`)
암호화 대상 값은 평문을 암호화하여 저장하며 동등 비교 검색을 위한 해시값을 포함할 수 있다.
