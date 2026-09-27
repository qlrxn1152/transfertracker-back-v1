package com.dhoon.transfertracker.internal.service.impl;

import com.dhoon.transfertracker.internal.domain.TransferPostSource;
import com.dhoon.transfertracker.internal.dto.response.TransferPostItemResponseDto;
import com.dhoon.transfertracker.internal.dto.response.TransferPostsResponseDto;
import com.dhoon.transfertracker.internal.repository.TransferPostRepository;
import com.dhoon.transfertracker.internal.service.TransferPostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class TransferPostServiceImpl implements TransferPostService {

    private final TransferPostRepository transferPostRepository;


    @Override
    public TransferPostsResponseDto getSourcerAllPosts(TransferPostSource sourcer) {
        List<TransferPostItemResponseDto> posts = transferPostRepository.findAllBySourcer(sourcer)
                .stream()
                .map(TransferPostItemResponseDto::of)
                .toList();

        return TransferPostsResponseDto.of(posts);
    }
}
