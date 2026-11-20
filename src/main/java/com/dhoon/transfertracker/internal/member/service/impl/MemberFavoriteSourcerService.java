package com.dhoon.transfertracker.internal.member.service.impl;

import com.dhoon.transfertracker.internal.member.domain.Member;
import com.dhoon.transfertracker.internal.member.domain.MemberFavoriteSourcer;
import com.dhoon.transfertracker.internal.member.dto.request.MemberFavoriteSourcerRegisterRequestDto;
import com.dhoon.transfertracker.internal.member.dto.response.MemberFavoriteSourcersAllPostsResponseDto;
import com.dhoon.transfertracker.internal.member.dto.response.MemberFollowJournalistDetailResponseDto;
import com.dhoon.transfertracker.internal.member.dto.response.MemberFollowJournalistsResponseDto;
import com.dhoon.transfertracker.internal.member.exception.AlreadyExistMemberFavoriteSourcerException;
import com.dhoon.transfertracker.internal.member.exception.NotFoundMemberException;
import com.dhoon.transfertracker.internal.member.exception.NotFoundMemberFavoriteSourcerException;
import com.dhoon.transfertracker.internal.member.repository.MemberFavoriteSourcerRepository;
import com.dhoon.transfertracker.internal.member.repository.MemberRepository;
import com.dhoon.transfertracker.internal.post.domain.Post;
import com.dhoon.transfertracker.internal.post.domain.PostJournalist;
import com.dhoon.transfertracker.internal.post.dto.response.PostItemResponseDto;
import com.dhoon.transfertracker.internal.post.dto.response.PostsResponseDto;
import com.dhoon.transfertracker.internal.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Transactional
@Service
public class MemberFavoriteSourcerService {

    private final MemberFavoriteSourcerRepository memberFavoriteSourcerRepository;
    private final PostRepository postRepository;

    private final MemberRepository memberRepository;

    public MemberFollowJournalistDetailResponseDto followJournalist(Long memberId, MemberFavoriteSourcerRegisterRequestDto requestDto) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(NotFoundMemberException::new);
        PostJournalist journalist = requestDto.getJournalist();

        memberFavoriteSourcerRepository.findByMemberIdAndJournalist(memberId, journalist)
                .ifPresentOrElse(
                        favorite -> {
                            throw new AlreadyExistMemberFavoriteSourcerException();
                        },
                        () -> memberFavoriteSourcerRepository.save(MemberFavoriteSourcer.of(member, journalist))
                );

        return MemberFollowJournalistDetailResponseDto.of(MemberFavoriteSourcer.of(member, journalist));
    }

    public void unFollowJournalist(Long memberId, MemberFavoriteSourcerRegisterRequestDto requestDto) {
        memberRepository.findById(memberId)
                .orElseThrow(NotFoundMemberException::new);
        PostJournalist journalist = requestDto.getJournalist();

        memberFavoriteSourcerRepository.findByMemberIdAndJournalist(memberId, journalist)
                .ifPresentOrElse(
                        memberFavoriteSourcerRepository::delete,
                        () -> {throw new NotFoundMemberFavoriteSourcerException();}
                );
    }

    public MemberFollowJournalistsResponseDto getMemberFollowJournalists(Long memberId) {
        memberRepository.findById(memberId)
                .orElseThrow(NotFoundMemberException::new);

        List<MemberFollowJournalistDetailResponseDto> memberFollowJournalists = memberFavoriteSourcerRepository.findAllByMemberId(memberId)
                .stream()
                .map(MemberFollowJournalistDetailResponseDto::of)
                .toList();

        return MemberFollowJournalistsResponseDto.of(memberFollowJournalists);
    }

    public PostsResponseDto getJournalistPosts(Long memberId, PostJournalist journalist, int page) {
        memberRepository.findById(memberId)
                .orElseThrow(NotFoundMemberException::new);

        memberFavoriteSourcerRepository.findByMemberIdAndJournalist(memberId, journalist)
                .orElseThrow(NotFoundMemberFavoriteSourcerException::new);

        PageRequest pageable = PageRequest.of(
                page,
                20,
                Sort.by(Sort.Order.desc("contentCreatedAt"), Sort.Order.desc("id"))
        );


        Page<Post> journalistAllPosts = postRepository.findAllByJournalist(journalist, pageable);

        long totalPostCount = journalistAllPosts.getTotalElements();
        int totalPageCount = journalistAllPosts.getTotalPages();
        boolean hasNext = journalistAllPosts.hasNext();
        boolean hasPrevious = journalistAllPosts.hasPrevious();

        List<PostItemResponseDto> posts = journalistAllPosts.getContent()
                .stream()
                .map(PostItemResponseDto::createPost)
                .toList();

        return PostsResponseDto.of(posts, totalPostCount, totalPageCount, hasNext, hasPrevious);
    }

    public MemberFavoriteSourcersAllPostsResponseDto getAllJournalistPosts(Long memberId, int page) {
        memberRepository.findById(memberId)
                .orElseThrow(NotFoundMemberException::new);

        List<MemberFavoriteSourcer> memberFollowingJournalists = memberFavoriteSourcerRepository.findAllByMemberId(memberId);

        PageRequest pageable = PageRequest.of(
                page,
                20,
                Sort.by(Sort.Order.desc("contentCreatedAt"), Sort.Order.desc("id"))
        );

        List<PostsResponseDto> allJournalistsPosts = new ArrayList<>();


        for (MemberFavoriteSourcer favoriteSourcer : memberFollowingJournalists) {
            PostJournalist journalist = favoriteSourcer.getJournalist();

            Page<Post> journalistAllPosts = postRepository.findAllByJournalist(journalist, pageable);

            long totalPostCount = journalistAllPosts.getTotalElements();
            int totalPageCount = journalistAllPosts.getTotalPages();
            boolean hasNext = journalistAllPosts.hasNext();
            boolean hasPrevious = journalistAllPosts.hasPrevious();

            List<PostItemResponseDto> posts = journalistAllPosts.getContent()
                    .stream()
                    .map(PostItemResponseDto::createPost)
                    .toList();

            allJournalistsPosts.add(PostsResponseDto.of(posts, totalPostCount, totalPageCount, hasNext, hasPrevious));
        }

        return MemberFavoriteSourcersAllPostsResponseDto.of(allJournalistsPosts);
    }



}
