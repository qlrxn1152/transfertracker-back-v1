# Member 도메인
[ 해당 프로젝트를 사용하는 유저를 나타냅니다. ]

## 1. 책임 

Member 도메인은 사용자의 계정 정보(`loginId` , `password`) 와 권한 ( `memberGrade` ) 을 관리한다.

- 회원 가입시, 일반 사용자 ( MemberGrade = USER ) 계정 생성
- 로그인 ID 와, 암호화된 비밀번호 관리
- 사용자 권한 ( USER, ADMIN ) 관리

회원 인증 , JWT 발급은 애플리케이션 레벨에서 처리합니다.

## 2. 비즈니스 규칙

1. 로그인 ID 는 중복될 수 없다. ( UNIQUE )
2. 회원가입 시 비밀번호는 암호화해서 저장합니다.
3. 회원가입 시 회원의 권한은 USER 로 설정됩니다.
4. 일반 사용자는 권한을 변경할 수 없으며, ADMIN 권한은 운영자가 MySQL 에서 직접 부여합니다.
5. 회원의 권한 변경 API 는 제공하지 않습니다.

## 3. 데이터베이스 제약조건

| 컬럼 | 제약조건                                  |
|---|---------------------------------------|
| member_id | PRIMARY KEY, AUTO_INCREMENT, NOT NULL |
| login_id | NOT NULL, UNIQUE                      |
| password | NOT NULL                              |
| member_grade | NOT NULL, ENUM(USER, ADMIN)           |

---



# Player 도메인
[ 실제 선수들를 표현하기 위한 도메인입니다. ]