package com.dhoon.transfertracker.internal.team.exception;

import com.dhoon.transfertracker.global.exception.BusinessException;

public class NotFoundTeamException extends BusinessException {
    public NotFoundTeamException() {
        super(TeamErrorCode.TEAM_NOT_FOUND);
    }
}
