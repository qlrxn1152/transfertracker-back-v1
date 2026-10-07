controller -> FootballSyncService ( Orchestration ) -> ApiFootballHttpClient ( 외부 HTTP 호출담당 )  [ Transaction x ]
                                                    -> FootballSyncTxService ( Transaction 담당 ) -> Repository ( DB ) [ Transaction o ]

ApiFootballHttpClient -> FootballSyncService ( JsonNode 타입의 응답 데이터 반환) -> 필요한 데이터들을 FootballSyncTxService 로 전달.

FootballSyncTxService -> FootballSyncService ( Entity 를 반환하는게아니라, 전용 DTO 를 만들어서 반환 ) -> Controller 로 전달



