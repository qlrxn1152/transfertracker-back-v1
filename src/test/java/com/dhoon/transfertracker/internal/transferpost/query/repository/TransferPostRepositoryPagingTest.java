package com.dhoon.transfertracker.internal.transferpost.query.repository;

import com.dhoon.transfertracker.TransferTrackerApplication;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPost;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPostSource;
import com.dhoon.transfertracker.internal.transferpost.dto.response.TransferPostItemResponseDto;
import com.dhoon.transfertracker.internal.transferpost.repository.TransferPostRepository;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.Assertions;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Testcontainers
@Transactional
@ContextConfiguration(classes = TransferTrackerApplication.class)
@ActiveProfiles("test")
public class TransferPostRepositoryPagingTest {

    @Container
    static MySQLContainer mysql =
            new MySQLContainer("mysql:8.4");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @Autowired
    private TransferPostRepository transferPostRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private EntityManager em;

    @Test
    @DisplayName("특정 기자의 게시물만 페이지 단위로 조회한다")
    void findAllBySourcerWithTeam_paging() {
        // given
        Instant now = Instant.parse("2026-11-08T12:00:00Z");

        TransferPost romanoPost1 = TransferPost.of(
                TransferPostSource.FABRIZIO_ROMANO,
                "Romano transfer news 1",
                "post-100",
                now.minusSeconds(3600),
                true
        );

        TransferPost romanoPost2 = TransferPost.of(
                TransferPostSource.FABRIZIO_ROMANO,
                "Romano transfer news 2",
                "post-101",
                now.minusSeconds(1800),
                true
        );

        TransferPost romanoPost3 = TransferPost.of(
                TransferPostSource.FABRIZIO_ROMANO,
                "Romano transfer news 3",
                "post-102",
                now,
                false
        );

        TransferPost ornsteinPost1 = TransferPost.of(
                TransferPostSource.DAVID_ORNSTEIN,
                "Ornstein transfer news 1",
                "post-200",
                now.minusSeconds(2400),
                true
        );

        TransferPost ornsteinPost2 = TransferPost.of(
                TransferPostSource.DAVID_ORNSTEIN,
                "Ornstein transfer news 2",
                "post-201",
                now.minusSeconds(600),
                false
        );

        transferPostRepository.saveAll(List.of(
                romanoPost1,
                romanoPost2,
                romanoPost3,
                ornsteinPost1,
                ornsteinPost2
        ));

        em.flush();
        em.clear();

        Pageable pageable = PageRequest.of(
                0,
                20,
                Sort.by(
                        Sort.Order.desc("contentCreatedAt"),
                        Sort.Order.desc("id")
                )
        );

        // when
        Page<TransferPost> response = transferPostRepository.findAllBySourcerWithTeam(TransferPostSource.FABRIZIO_ROMANO, pageable);

        // then
        assertThat(response.getTotalPages()).isEqualTo(1);
        assertThat(response.getTotalElements()).isEqualTo(3);
        assertThat(response.getNumberOfElements()).isEqualTo(3);

        assertThat(response.getContent()).extracting(TransferPost::getExternalPostId)
                .containsExactly("post-102", "post-101", "post-100");

        assertThat(response.hasNext()).isFalse();
        assertThat(response.hasPrevious()).isFalse();

    }

    @Test
    @DisplayName("기자별 게시물 조회 시 페이지 분할과 마지막 페이지를 검증한다")
    void findAllBySourcerWithTeam_lastPage() {
        // given
        Instant now = Instant.parse("2026-11-08T12:00:00Z");

        TransferPost romanoPost1 = TransferPost.of(
                TransferPostSource.FABRIZIO_ROMANO,
                "Romano transfer news 1",
                "post-100",
                now.minusSeconds(3600),
                true
        );

        TransferPost romanoPost2 = TransferPost.of(
                TransferPostSource.FABRIZIO_ROMANO,
                "Romano transfer news 2",
                "post-101",
                now.minusSeconds(1800),
                true
        );

        TransferPost romanoPost3 = TransferPost.of(
                TransferPostSource.FABRIZIO_ROMANO,
                "Romano transfer news 3",
                "post-102",
                now,
                false
        );

        TransferPost ornsteinPost = TransferPost.of(
                TransferPostSource.DAVID_ORNSTEIN,
                "Ornstein transfer news",
                "post-200",
                now.minusSeconds(600),
                true
        );

        transferPostRepository.saveAll(List.of(
                romanoPost1,
                romanoPost2,
                romanoPost3,
                ornsteinPost
        ));

        em.flush();
        em.clear();

        Sort sort = Sort.by(
                Sort.Order.desc("contentCreatedAt"),
                Sort.Order.desc("id")
        );

        Pageable firstPageable = PageRequest.of(0, 2, sort); // 1,2
        Pageable secondPageable = PageRequest.of(1, 2, sort); // 3

        // when
        Page<TransferPost> romanoFirstResponse = transferPostRepository.findAllBySourcerWithTeam(TransferPostSource.FABRIZIO_ROMANO, firstPageable);
        Page<TransferPost> romanoSecondResponse = transferPostRepository.findAllBySourcerWithTeam(TransferPostSource.FABRIZIO_ROMANO, secondPageable);

        Page<TransferPost> ornsteinFirstResponse = transferPostRepository.findAllBySourcerWithTeam(TransferPostSource.DAVID_ORNSTEIN, firstPageable);


        // then
        assertThat(romanoFirstResponse.getTotalElements()).isEqualTo(3);
        assertThat(romanoFirstResponse.getContent()).hasSize(2);
        assertThat(romanoFirstResponse.getContent()).extracting(TransferPost::getExternalPostId)
                .containsExactly("post-102", "post-101");
        assertThat(romanoFirstResponse.hasPrevious()).isFalse();
        assertThat(romanoFirstResponse.hasNext()).isTrue();

        assertThat(romanoSecondResponse.getTotalElements()).isEqualTo(3);
        assertThat(romanoSecondResponse.getContent()).hasSize(1);
        assertThat(romanoSecondResponse.getContent()).extracting(TransferPost::getExternalPostId)
                .containsExactly("post-100");
        assertThat(romanoSecondResponse.hasPrevious()).isTrue();
        assertThat(romanoSecondResponse.hasNext()).isFalse();

        assertThat(ornsteinFirstResponse.getTotalElements()).isEqualTo(1);
        assertThat(ornsteinFirstResponse.getContent()).hasSize(1);
        assertThat(ornsteinFirstResponse.getContent()).extracting(TransferPost::getExternalPostId)
                .containsExactly("post-200");
        assertThat(ornsteinFirstResponse.hasPrevious()).isFalse();
        assertThat(ornsteinFirstResponse.hasNext()).isFalse();
    }

    @Test
    @DisplayName("기자별 조회 시 Team이 없는 게시물도 포함하고 Team을 함께 로딩한다")
    void findAllBySourcerWithTeam_fetchJoin() {
        // given
        Team savedTeam = teamRepository.save(Team.of("testTeam", 111L));

        Instant now = Instant.parse("2026-11-08T12:00:00Z");

        TransferPost romanoPost1 = TransferPost.of(
                TransferPostSource.FABRIZIO_ROMANO,
                "Romano transfer news 1",
                "post-100",
                now.minusSeconds(1000),
                true
        );

        romanoPost1.assignTeam(savedTeam);
        transferPostRepository.save(romanoPost1);

        TransferPost romanoPost2 = TransferPost.of(
                TransferPostSource.FABRIZIO_ROMANO,
                "Romano transfer news 2",
                "post-101",
                now.minusSeconds(1800),
                true
        );

        transferPostRepository.save(romanoPost2);

        em.flush();
        em.clear();

        Pageable pageable = PageRequest.of(
                0,
                20,
                Sort.by(
                        Sort.Order.desc("contentCreatedAt"),
                        Sort.Order.desc("id")
                )
        );

        // when
        Page<TransferPost> response = transferPostRepository.findAllBySourcerWithTeam(TransferPostSource.FABRIZIO_ROMANO, pageable);


        // then
        assertThat(response.getTotalElements()).isEqualTo(2);

        TransferPost romanoFirstPost = response.getContent().getFirst();
        assertThat(Hibernate.isInitialized(romanoFirstPost.getTeam())).isTrue();
        assertThat(romanoFirstPost.getExternalPostId()).isEqualTo("post-100");
        assertThat(romanoFirstPost.getTeam().getId()).isEqualTo(savedTeam.getId());


        TransferPost romanoSecondPost = response.getContent().get(1);
        assertThat(romanoSecondPost.getTeam()).isNull();
        assertThat(romanoSecondPost.getExternalPostId()).isEqualTo("post-101");
    }




}
