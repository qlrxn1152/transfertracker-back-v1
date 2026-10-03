package com.dhoon.transfertracker.external.openai.client;

import com.dhoon.transfertracker.external.openai.config.OpenAiConfig;
import com.dhoon.transfertracker.external.openai.dto.request.TranslationTarget;
import com.dhoon.transfertracker.external.openai.dto.response.TranslationBatchResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.test.autoconfigure.RestClientTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;


@RestClientTest(
        components = OpenAiClient.class,
        properties = {
                "openai.api.base-url=https://api.openai.test",
                "openai.api.key=test-api-key",
                "openai.api.model=gpt-5.6-luna"
        }
)
@Import(OpenAiConfig.class)
class OpenAiClientTest {

    @Autowired
    OpenAiClient openAiClient;

    @Autowired
    MockRestServiceServer mockServer;


    // ==================================================
    // 정상 상황
    // ==================================================

    @Test
    @DisplayName("OpenAI 응답의 output_text를 TranslationBatchResult로 변환한다.")
    void translate_success() {

        // given
        String responseBody = """
                {
                  "id": "resp_test",
                  "status": "completed",
                  "output": [
                    {
                      "id": "reasoning_test",
                      "type": "reasoning"
                    },
                    {
                      "id": "message_test",
                      "type": "message",
                      "status": "completed",
                      "content": [
                        {
                          "type": "output_text",
                          "text": "{\\"posts\\":[{\\"postId\\":1,\\"translatedContent\\":\\"맨체스터 유나이티드 번역\\"},{\\"postId\\":2,\\"translatedContent\\":\\"아스널 번역\\"}]}"
                        }
                      ]
                    }
                  ]
                }
                """;

        mockServer.expect(
                        requestTo(
                                "https://api.openai.test/v1/responses"
                        )
                )
                .andExpect(method(HttpMethod.POST))
                .andExpect(
                        header(
                                "Authorization",
                                "Bearer test-api-key"
                        )
                )
                .andExpect(
                        content().contentType(
                                MediaType.APPLICATION_JSON
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "\"model\":\"gpt-5.6-luna\""
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "Manchester United"
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "Arsenal"
                                )
                        )
                )
                .andRespond(
                        withSuccess(
                                responseBody,
                                MediaType.APPLICATION_JSON
                        )
                );


        List<TranslationTarget> targets =
                List.of(
                        new TranslationTarget(
                                1L,
                                "Manchester United"
                        ),
                        new TranslationTarget(
                                2L,
                                "Arsenal"
                        )
                );


        // when
        TranslationBatchResult result =
                openAiClient.translate(targets);


        // then
        assertThat(result.getPosts())
                .hasSize(2);

        assertThat(result.getPosts().get(0).getPostId())
                .isEqualTo(1L);

        assertThat(
                result.getPosts()
                        .get(0)
                        .getTranslatedContent()
        )
                .isEqualTo(
                        "맨체스터 유나이티드 번역"
                );

        assertThat(result.getPosts().get(1).getPostId())
                .isEqualTo(2L);

        assertThat(
                result.getPosts()
                        .get(1)
                        .getTranslatedContent()
        )
                .isEqualTo("아스널 번역");


        mockServer.verify();
    }


    // ==================================================
    // 예외 상황
    // ==================================================

    @Test
    @DisplayName("OpenAI 응답에 output_text가 없으면 예외가 발생한다.")
    void translate_outputTextNotFound_fail() {

        // given
        String responseBody = """
                {
                  "id": "resp_test",
                  "status": "completed",
                  "output": [
                    {
                      "id": "reasoning_test",
                      "type": "reasoning"
                    },
                    {
                      "id": "message_test",
                      "type": "message",
                      "content": [
                        {
                          "type": "something_else"
                        }
                      ]
                    }
                  ]
                }
                """;

        mockServer.expect(
                        requestTo(
                                "https://api.openai.test/v1/responses"
                        )
                )
                .andRespond(
                        withSuccess(
                                responseBody,
                                MediaType.APPLICATION_JSON
                        )
                );


        List<TranslationTarget> targets =
                List.of(
                        new TranslationTarget(
                                1L,
                                "Manchester United"
                        )
                );


        // when & then
        assertThatThrownBy(() ->
                openAiClient.translate(targets)
        )
                .isInstanceOf(
                        IllegalStateException.class
                )
                .hasMessage(
                        "OpenAI 응답에서 output_text를 찾을 수 없습니다."
                );


        mockServer.verify();
    }


    @Test
    @DisplayName("output_text가 올바른 JSON 형식이 아니면 역직렬화에 실패한다.")
    void translate_invalidOutputJson_fail() {

        // given
        String responseBody = """
                {
                  "id": "resp_test",
                  "status": "completed",
                  "output": [
                    {
                      "type": "message",
                      "content": [
                        {
                          "type": "output_text",
                          "text": "this is not json"
                        }
                      ]
                    }
                  ]
                }
                """;

        mockServer.expect(
                        requestTo(
                                "https://api.openai.test/v1/responses"
                        )
                )
                .andRespond(
                        withSuccess(
                                responseBody,
                                MediaType.APPLICATION_JSON
                        )
                );


        List<TranslationTarget> targets =
                List.of(
                        new TranslationTarget(
                                1L,
                                "Manchester United"
                        )
                );


        // when & then
        assertThatThrownBy(() ->
                openAiClient.translate(targets)
        );


        mockServer.verify();
    }


    @Test
    @DisplayName("OpenAI API가 5xx 응답을 반환하면 예외가 발생한다.")
    void translate_openAiServerError_fail() {

        // given
        mockServer.expect(
                        requestTo(
                                "https://api.openai.test/v1/responses"
                        )
                )
                .andExpect(
                        method(HttpMethod.POST)
                )
                .andRespond(
                        withServerError()
                );


        List<TranslationTarget> targets =
                List.of(
                        new TranslationTarget(
                                1L,
                                "Manchester United"
                        )
                );


        // when & then
        assertThatThrownBy(() ->
                openAiClient.translate(targets)
        )
                .isInstanceOf(
                        RestClientResponseException.class
                );


        mockServer.verify();
    }
}