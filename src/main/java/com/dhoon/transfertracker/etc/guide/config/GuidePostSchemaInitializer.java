package com.dhoon.transfertracker.etc.guide.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("prod")
@RequiredArgsConstructor
public class GuidePostSchemaInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute("ALTER TABLE guide_post MODIFY COLUMN content LONGTEXT NOT NULL");
            jdbcTemplate.execute("ALTER TABLE guide_post MODIFY COLUMN image_urls_text LONGTEXT");
        } catch (Exception exception) {
            log.warn("Guide post long text schema update was skipped.", exception);
        }
    }
}
