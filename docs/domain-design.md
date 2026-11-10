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


인덱스
- uk_member : 로그인 id 유니크 

---



# Player 도메인
[ 실제 선수들를 표현하기 위한 도메인입니다. ]

## 1. 책임

Player 도메인은 축구 선수의 기본정보를 관리한다.

- Player 내부 식별자 관리 ( id )
- API-Football 의 고유 선수 식별자 ( apiFootballId ) 관리
- 선수의 영문이름 ( playerName ) 과 한글이름 ( playerNameKo ) 관리

## 2. 비즈니스 규칙
1. 동일한 apiFootballId 를 가진 선수는 존재할 수 없다. ( UNIQUE )
2. 영문이름 ( playerName ) 과, apiFootballId ( 외부 API 선수 ID ) 는 필수이다.
3. 한국어 이름은 번역 전까지는 NULL 을 허용한다.
4. 사용자가 확인하는 선수이름은 한국어 이름이 존재하는경우, 한국어 이름을 표시하고 존재하지않는다면 영어 이름을 표시한다.
5. 선수 동기화 ( syncPlayer ) 시 선수가 존재하는경우, 추가로 저장하지않고 해당 선수 정보를 그대로 반환한다.
6. 선수 동기화 ( syncPlayer ) 시 선수가 존재하지않는경우, 해당 선수를 저장하고 선수 정보를 반환한다.
7. 선수 이름 번역과정은 player.translation 을 통해서 진행된다.

## 3. 데이터베이스 제약조건

| 컬럼 | 제약조건 |
|---|---|
| player_id | PRIMARY KEY, AUTO_INCREMENT, NOT NULL |
| player_name | NOT NULL |
| api_football_id | NOT NULL, UNIQUE |
| player_name_ko | NULL 허용 |

인덱스 
- idx_player_player_name: 영문 선수 이름 검색용 인덱스
- ft_player_name: 영문·한국어 이름 FULLTEXT 인덱스


---


