package com.dhoon.transfertracker.etc.guide.service;

import com.dhoon.transfertracker.etc.guide.domain.GuidePost;
import com.dhoon.transfertracker.etc.guide.dto.request.GuidePostCreateRequestDto;
import com.dhoon.transfertracker.etc.guide.dto.response.GuidePostItemResponseDto;
import com.dhoon.transfertracker.etc.guide.dto.response.GuidePostsResponseDto;
import com.dhoon.transfertracker.etc.guide.repository.GuidePostRepository;
import com.dhoon.transfertracker.internal.member.domain.Member;
import com.dhoon.transfertracker.internal.member.exception.NotFoundMemberException;
import com.dhoon.transfertracker.internal.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class GuidePostService {

    private static final String GUIDE_ADMIN_LOGIN_ID = "qlrxn1152";

    private final GuidePostRepository guidePostRepository;
    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
    public GuidePostsResponseDto getGuidePosts() {
        return GuidePostsResponseDto.of(
                guidePostRepository.findTop20ByActiveTrueOrderByCreatedAtDescIdDesc()
                        .stream()
                        .map(GuidePostItemResponseDto::of)
                        .toList()
        );
    }

    @Transactional(readOnly = true)
    public Optional<GuidePostItemResponseDto> findGuidePost(Long guidePostId) {
        return guidePostRepository.findByIdAndActiveTrue(guidePostId)
                .map(GuidePostItemResponseDto::of);
    }

    public GuidePostItemResponseDto createGuidePost(Long memberId, GuidePostCreateRequestDto request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(NotFoundMemberException::new);

        validateGuideAdmin(member);

        GuidePost guidePost = guidePostRepository.save(GuidePost.of(
                request.getTitle().trim(),
                request.getContent().trim(),
                normalizeImageUrls(request.getImageUrls()),
                member.getLoginId()
        ));

        return GuidePostItemResponseDto.of(guidePost);
    }

    public GuidePostItemResponseDto updateGuidePost(Long memberId, Long guidePostId, GuidePostCreateRequestDto request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(NotFoundMemberException::new);

        validateGuideAdmin(member);

        GuidePost guidePost = guidePostRepository.findByIdAndActiveTrue(guidePostId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "이용방법 게시물을 찾을 수 없습니다."));

        guidePost.update(
                request.getTitle().trim(),
                request.getContent().trim(),
                normalizeImageUrls(request.getImageUrls())
        );

        return GuidePostItemResponseDto.of(guidePost);
    }

    public void deleteGuidePost(Long memberId, Long guidePostId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(NotFoundMemberException::new);

        validateGuideAdmin(member);

        GuidePost guidePost = guidePostRepository.findByIdAndActiveTrue(guidePostId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "이용방법 게시물을 찾을 수 없습니다."));

        guidePost.deactivate();
    }

    private void validateGuideAdmin(Member member) {
        if (!GUIDE_ADMIN_LOGIN_ID.equals(member.getLoginId())) {
            throw new IllegalArgumentException("이용방법 게시물은 관리자만 작성할 수 있습니다.");
        }
    }

    private List<String> normalizeImageUrls(List<String> imageUrls) {
        if (imageUrls == null) {
            return List.of();
        }

        return imageUrls.stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .limit(8)
                .toList();
    }
}
