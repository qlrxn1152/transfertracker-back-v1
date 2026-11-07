package com.dhoon.transfertracker.internal.transferpost.sync.batch;

import com.dhoon.transfertracker.external.x.client.XClient;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPostSource;
import com.dhoon.transfertracker.internal.transferpost.repository.TransferPostRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;

@SpringBootTest
@SpringBatchTest
@Testcontainers
@ActiveProfiles("test")
public class TransferPostBatchIntegrationTest {

    @Container
    static MySQLContainer mysql = new MySQLContainer("mysql:8.4");

    @DynamicPropertySource
    static void configureProperties(
            DynamicPropertyRegistry registry
    ) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @MockitoBean
    private XClient xClient;

    @Autowired
    private JobOperator jobOperator;

    @Autowired
    @Qualifier("transferPostSyncJob")
    private Job transferPostSyncJob;

    @Autowired
    private TransferPostRepository transferPostRepository;

    private final JsonMapper jsonMapper = new JsonMapper();

    @Test
    @DisplayName("수집 Batch가 게시물을 저장하고 정상 종료한다")
    void syncTransferPosts() throws Exception {
        // given
        JsonNode response = jsonMapper.readTree("""
                {
                  "data": [
                    {
                      "id": "post-100",
                      "text": "Manchester United transfer deal agreed",
                      "created_at": "2026-11-07T10:00:00Z"
                    }
                  ],
                  "meta": {
                        "result_count": 1
                    }
                }
                """);

        given(xClient.callExternalApi(
                TransferPostSource.FABRIZIO_ROMANO, null
        )).willReturn(response);

        JobParameters parameters = new JobParametersBuilder()
                .addString("test.id", UUID.randomUUID().toString())
                .toJobParameters();

        // when
        JobExecution execution = jobOperator.start(transferPostSyncJob, parameters);

        // then
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        assertThat(transferPostRepository.findByExternalPostId("post-100")).isPresent();
    }

    @Test
    @DisplayName("X API 호출에 실패하면 Batch는 FAILED 상태로 종료된다")
    void failWhenXApiThrowsException() throws Exception {
        // given
        given(xClient.callExternalApi(
                TransferPostSource.FABRIZIO_ROMANO, null
        )).willThrow(new RuntimeException("X API 호출 실패"));

        JobParameters parameters = new JobParametersBuilder()
                .addString("test.id", UUID.randomUUID().toString())
                .toJobParameters();

        // when
        JobExecution execution =
                jobOperator.start(transferPostSyncJob, parameters);

        // then
        assertThat(execution.getStatus())
                .isEqualTo(BatchStatus.FAILED);
    }






}
