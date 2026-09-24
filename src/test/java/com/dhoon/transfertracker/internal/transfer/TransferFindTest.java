package com.dhoon.transfertracker.internal.transfer;

import com.dhoon.transfertracker.integrate.TeamCreateFixture;
import com.dhoon.transfertracker.internal.service.TransferService;
import com.dhoon.transfertracker.internal.team.TeamsFindTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
public class TransferFindTest {

    @Autowired TeamCreateFixture teamCreateFixture;

    @Autowired TransferService transferService;

    @Test
    @DisplayName(value = "")
    void getAllTransfers() throws Exception {
        // given
        teamCreateFixture.saveTeams(10);

        // when
        transferService.getTransfers();

        // then

    }

}
