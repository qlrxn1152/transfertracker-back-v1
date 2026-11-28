package com.dhoon.transfertracker.internal.team.exception;

import com.dhoon.transfertracker.global.exception.BusinessException;

public class InvalidTeamIdException extends BusinessException {
    public InvalidTeamIdException() {
        super(TeamErrorCode.INVALID_TEAM_ID);
    }
}
