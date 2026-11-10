package com.dhoon.transfertracker.internal.transferpost.translation.batch.writer;

import com.dhoon.transfertracker.external.openai.dto.response.TranslationBatchResult;
import com.dhoon.transfertracker.external.openai.dto.response.TranslationResult;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPost;
import com.dhoon.transfertracker.internal.transferpost.exception.NotFoundTransferPostException;
import com.dhoon.transfertracker.internal.transferpost.repository.TransferPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;

import java.util.List;

@RequiredArgsConstructor
public class TransferPostTranslationItemWriter implements ItemWriter<TranslationBatchResult> {

    private final TransferPostRepository transferPostRepository;


    @Override
    public void write(Chunk<? extends TranslationBatchResult> chunk) throws Exception {
        for (TranslationBatchResult result : chunk) {

            List<TranslationResult> posts = result.getPosts();


            posts.forEach(post -> {

                        TransferPost tp = transferPostRepository.findById(post.getPostId())
                                .orElseThrow(NotFoundTransferPostException::new);

                        tp.translate(post.getTranslatedContent());
                    });

        }






    }
}
