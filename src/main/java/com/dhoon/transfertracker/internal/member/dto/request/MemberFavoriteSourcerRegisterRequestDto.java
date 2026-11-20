package com.dhoon.transfertracker.internal.member.dto.request;

import com.dhoon.transfertracker.internal.post.domain.PostJournalist;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MemberFavoriteSourcerRegisterRequestDto {

    private PostJournalist journalist;

}
