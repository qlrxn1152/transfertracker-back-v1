package com.dhoon.transfertracker.internal.transferpost.sync.batch.writer;

import com.dhoon.transfertracker.internal.transferpost.domain.TransferPost;
import com.dhoon.transfertracker.internal.transferpost.repository.TransferPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TransferPostItemWriter implements ItemWriter<TransferPost> {

    private final TransferPostRepository transferPostRepository;


    @Override
    public void write(Chunk<? extends TransferPost> chunk) throws Exception {
        for (TransferPost transferPost : chunk) {

            transferPostRepository.findByExternalPostId(transferPost.getExternalPostId())
                    .orElseGet(() -> transferPostRepository.save(transferPost));

        }
    }
}
