## 1. 문제

- 기존 구조에서는, 외부 API 를 호출하는 부분과 DB 작업이 하나의 서비스에서 모든것을 담당했었음

## 2. 왜 문제가 되는건가 ?

외부 HTTP 응답 시간이 길어질경우, Transaction 과 DB Connection 의 생명주기가 불필요하게 길어질 수 있음.

즉, Transaction 입장에서는, 외부 HTTP 응답을 받기위해 같이 기다리는것은 불필요함.

## 3. 기존구조 

Controller -> Service ( 외부 HTTP 호출 / Transaction ) -> Repository

## 4. 개선 구조

Controller -> Service -> HTTP 호출담당 Service [ Transaction x ]
                      -> Transaction 담당 Service -> Repository  [ Transaction o ]

---

## 5. 구조를 변경하면서 생긴 문제점

- `LazyInitializationException` -> LAZY 로 로딩되어져있는 객체의 Proxy 초기화문제.
- `Entity Transaction Boundary leakage`  
 
---

## 6. 최종 원칙

- 외부 I / O 는 Transaciton 밖
- Entity 사용은 Transaction 안에서 만들어서 반환.
- TxService 외부에는 DTO 반환. 즉, 큰 단위 Service 쪽에서는 그냥 받아서 사용. 해당 Service 계층에서는 DTO 를 생성하는것은 제한.

---





