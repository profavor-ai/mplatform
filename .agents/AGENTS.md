# Rules

- Always write implementation plans (implementation_plan.md) and tasks in Korean ("계획서는 한글로 표시해줘. 앞으로 쭈욱.").
- DB 작업 시 `TRUNCATE TABLE` 명령어를 절대로 사용하지 말 것. 기존 데이터를 날리지 말고 문제가 있는 레코드/도메인을 명시적으로 조회하여 개별 삭제(Delete)할 것.
- 파일 내용 변경(Multi-replace 등) 후에는 괄호 `}`나 태그가 잘못 남겨지지 않았는지 반드시 Diff를 재차 확인하고, 필요 시 빌드/린트 스크립트를 돌려 Syntax 에러가 없는지 검증할 것.
- 추후 AG-Grid로 화면 개발 시, 대량의 데이터 처리를 위해 반드시 서버 사이드 페이징(Server-Side Pagination)이 가능하도록 개발할 것.
