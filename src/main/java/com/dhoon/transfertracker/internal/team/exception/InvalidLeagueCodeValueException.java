package com.dhoon.transfertracker.internal.team.exception;

import com.dhoon.transfertracker.global.exception.BusinessException;


public class InvalidLeagueCodeValueException extends BusinessException {

    public InvalidLeagueCodeValueException() {
        super(TeamErrorCode.INVALID_LEAGUE_CODE);
    }
}
