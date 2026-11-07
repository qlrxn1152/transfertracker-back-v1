package com.dhoon.transfertracker.internal.transferpost.sync.batch.writer;


import com.dhoon.transfertracker.internal.transferpost.domain.TransferPost;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPostSource;
import com.dhoon.transfertracker.internal.transferpost.repository.TransferPostRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.BDDMockito.*;


@ExtendWith(MockitoExtension.class)
public class TransferPostItemWriterTest {


    @Mock
    private TransferPostRepository transferPostRepository;

    @InjectMocks
    private TransferPostItemWriter writer;

    @Test
    @DisplayName(value = "이미 존재하는 게시물은 저장하지 않는다.")
    void skipDuplicatePost() throws Exception {
        // given
        TransferPost existingPost = mock(TransferPost.class);

        given(existingPost.getExternalPostId()).willReturn("external_post_id");

        given(transferPostRepository.findByExternalPostId("external_post_id"))
                .willReturn(Optional.of(existingPost));

        // when
        writer.write(new Chunk<>(List.of(existingPost)));

        // then
        verify(transferPostRepository, never()).save(any(TransferPost.class));
        verify(transferPostRepository, times(1)).findByExternalPostId("external_post_id");
    }

    @Test
    @DisplayName(value = "존재하지 않는 게시물은 저장한다")
    void save_notExistPost() throws Exception {
        // given
        TransferPost existingPost = mock(TransferPost.class);

        given(existingPost.getExternalPostId()).willReturn("external_post_id");

        given(transferPostRepository.findByExternalPostId("external_post_id"))
                .willReturn(Optional.empty());

        // when
        writer.write(new Chunk<>(List.of(existingPost)));

        // then
        verify(transferPostRepository, times(1)).save(existingPost);
        verify(transferPostRepository, times(1)).findByExternalPostId("external_post_id");
    }

    @Test
    @DisplayName("기존 게시물과 신규 게시물이 혼합되어 있으면 신규 게시물만 저장한다")
    void saveOnlyNewPosts() throws Exception {
        // given
        TransferPost existingPost = mock(TransferPost.class);

        TransferPost newPost1 = mock(TransferPost.class);
        TransferPost newPost2 = mock(TransferPost.class);

        given(existingPost.getExternalPostId()).willReturn("external_post_id-1");
        given(newPost1.getExternalPostId()).willReturn("external_post_id-2");
        given(newPost2.getExternalPostId()).willReturn("external_post_id-3");

        given(transferPostRepository.findByExternalPostId("external_post_id-1"))
                .willReturn(Optional.of(existingPost));

        given(transferPostRepository.findByExternalPostId("external_post_id-2"))
                .willReturn(Optional.empty());

        given(transferPostRepository.findByExternalPostId("external_post_id-3"))
                .willReturn(Optional.empty());


        // when
        writer.write(new Chunk<>(List.of(existingPost, newPost1, newPost2)));

        // then
        verify(transferPostRepository, times(1)).save(newPost1);
        verify(transferPostRepository, times(1)).save(newPost2);
        verify(transferPostRepository, times(2)).save(any(TransferPost.class));

        verify(transferPostRepository, never()).save(existingPost);

        verify(transferPostRepository, times(1)).findByExternalPostId("external_post_id-1");
        verify(transferPostRepository, times(1)).findByExternalPostId("external_post_id-2");
        verify(transferPostRepository, times(1)).findByExternalPostId("external_post_id-3");
    }








}
