package com.dhoon.transfertracker.internal.fixture.repository;

import com.dhoon.transfertracker.internal.fixture.domain.Fixture;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface FixtureRepository extends JpaRepository<Fixture, Long> {

    List<Fixture> findAllByApiFootballIdIn(Collection<Long> apiFootballIds);
}
