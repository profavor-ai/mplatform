# 4. 비즈니스 로직 및 생명주기

### 4.1 필드 상속 알고리즘 (EffectiveFields 계산)
특정 노드 N의 유효 필드 목록을 구하는 절차:
1. 루트(도메인)부터 N까지의 조상 경로를 구한다: [root, ..., parent, N]
2. 경로를 따라 내려가며 각 노드에 정의된 field_definition을 순서대로 누적한다.
3. 동일 key가 다시 나타나면 가장 가까운(하위) 정의로 교체(Override) 한다.
4. is_removed = true인 정의가 나타나면 해당 key를 결과 집합에서 제외한다.
5. 최종적으로 남은 필드 집합 = N의 EffectiveFields.

### 4.2 노드 이동(Move) 시 데이터 처리 정책
노드를 다른 부모 아래로 이동시킬 경우, 이동 자체는 허용(유연성)하되 기존 데이터는 보호한다.
1. 노드의 parent_id 및 path, depth를 업데이트한다.
2. 이동된 노드의 새로운 EffectiveFields를 계산한다.
3. 해당 노드에 속한 기존 레코드들을 새로운 EffectiveFields와 비교한다.
4. 필수 필드 누락 등 스키마 불일치가 발생한 레코드들의 status를 ACTIVE에서 **MISMATCHED**로 변경한다.
5. 기본 조회 API에서는 WHERE status = 'ACTIVE' 조건을 주어 불일치 레코드를 자연스럽게 숨김 처리한다. 관리자는 별도 관리 화면에서 MISMATCHED 레코드를 보정할 수 있다.

### 4.3 노드 삭제(Delete) 정책
하위 레코드가 존재하더라도 데이터는 삭제하지 않고 연쇄 논리 삭제(Cascade Soft Delete) 를 수행한다.
1. 삭제 대상 노드의 is_deleted = true, deleted_at = 현재시간으로 업데이트.
2. path 컬럼을 활용하여 하위 노드를 효율적으로 탐색 후 모두 is_deleted = true로 변경.
   ```sql
   UPDATE classification_node 
   SET is_deleted = true, deleted_at = NOW() 
   WHERE path LIKE '/삭제노드경로/%' OR id = '삭제노드ID';
   ```
3. 하위 노드에 속한 레코드 데이터(record 등)는 물리적 삭제하지 않으나, 소속 노드가 논리 삭제되었으므로 자연스럽게 조회에서 제외된다.

### 4.4 도메인 참조(REFERENCE) 해석 로직
다른 도메인을 참조할 때, domain 테이블에 정의된 식별 속성을 활용하여 UI와 검색 쿼리를 동적으로 구성한다.
- 검색 키: 대상 도메인의 identifier_field_id가 가리키는 필드의 key
- 표시 라벨: 대상 도메인의 display_name_field_id가 가리키는 필드의 key
- 부가 설명: 대상 도메인의 description_field_id가 가리키는 필드의 key
- 동적 쿼리 예시: 사용자가 "홍"으로 검색 시, 백엔드는 display_name_field_id의 key를 찾아 `WHERE record.data->>'성명' LIKE '%홍%'` 쿼리를 동적으로 생성한다.

---

# 5. 암호화 방식
1차 구현은 자체 암복호화 로직 기준으로 한다.
- 알고리즘: AES-256-GCM (인증 태그 포함, 필드별 nonce 매번 새로 생성)
- 키 관리: 애플리케이션 서버 환경변수/Secret 저장소에 마스터 키 보관 (DB 하드코딩 금지)
- 시점: 저장 시 앱 레이어에서 암호화 후 DB 저장, 조회 시 앱 레이어에서 복호화
- 검색 제약: DB에서 직접 검색 불가. 동일 값 일치 검색이 필요한 필드는 평문을 HMAC 해시한 blind_index를 저장하여 동등 비교만 지원. (범위/부분 일치 검색 불가)

---

# 6. 데이터 품질 및 유효성 검증 (Data Quality & Validation)

데이터 오염(예: 숫자가 문자로 저장되는 현상) 방지 및 무결성을 보장하기 위해 프론트엔드와 백엔드에서 엄격한 검증을 수행한다.

### 6.1 UI 입력 제어 (프론트엔드)
- 동적 폼 생성 시 필드 타입(`nodeFields`)에 따라 적절한 컴포넌트로 렌더링.
- **숫자형 강제 차단**: `NUMBER`, `DECIMAL`, `FLOAT`, `INTEGER` 타입은 브라우저 네이티브 `<va-input type="number">`로 렌더링되어 텍스트 입력을 원천 차단.

### 6.2 데이터 타입 직렬화 (Type Casting)
- **저장 전 형변환 (`formatDataForSave`)**: 문자열로 넘어올 수 있는 폼 데이터를 실제 데이터 타입에 맞게 캐스팅.
  - 숫자형 데이터: `Number(val)`
  - 논리형 데이터: `Boolean(val)`
- **AG-Grid 호환성 최적화**: 동적 컬럼 바인딩으로 인한 "Invalid Number" 오류를 방지하기 위해 `cellDataType: false` 속성을 기본 설정으로 적용.

### 6.3 외부 서비스 연동 (Integration)
- **사용자 매핑**: UUID로 저장된 식별자(`requesterId`, `assigneeId` 등)를 인증/사용자 서비스 API(`/api/auth/users`)와 통신하여 실제 이름으로 매핑.
- **파일 업로드**: 파일(`FILE`) 필드 타입의 데이터는 전용 스토리지 API(`/api/files/upload`)를 통해 선업로드 후 발급받은 다운로드 경로만 JSON에 저장.
