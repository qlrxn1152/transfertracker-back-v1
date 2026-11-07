import com.dhoon.transfertracker.external.x.client.XClient;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPost;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPostSource;
import com.dhoon.transfertracker.internal.transferpost.sync.batch.dto.TransferPostRawItem;
import com.dhoon.transfertracker.internal.transferpost.sync.batch.reader.TransferPostItemReader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class TransferPostItemReaderTest {

    @Mock
    private XClient xClient;

    @InjectMocks
    private TransferPostItemReader reader;

    JsonMapper jsonMapper = new JsonMapper();

    @Test
    @DisplayName("첫 페이지의 게시물을 순서대로 반환하고 다음 페이지가 없으면 종료한다")
    void readFirstPage() throws Exception {
        // given

        JsonNode response = jsonMapper.readTree("""
        {
          "data": [
            {"id": "post-1", "text": "Post 1"},
            {"id": "post-2", "text": "Post 2"},
            {"id": "post-3", "text": "Post 3"}
          ],
          "meta": {
            "result_count": 3
          }
        }
        """);

        given(xClient.callExternalApi(TransferPostSource.FABRIZIO_ROMANO, null))
                .willReturn(response);

        // when
        TransferPostRawItem first = reader.read();
        TransferPostRawItem second = reader.read();
        TransferPostRawItem third = reader.read();

        TransferPostRawItem fourth = reader.read();

        // then
        assertThat(first.post().get("id").asString()).isEqualTo("post-1");
        assertThat(second.post().get("id").asString()).isEqualTo("post-2");
        assertThat(third.post().get("id").asString()).isEqualTo("post-3");

        assertThat(fourth).isNull();

        verify(xClient, times(1)).callExternalApi(TransferPostSource.FABRIZIO_ROMANO, null);
    }

    @Test
    @DisplayName("첫 페이지를 모두 읽으면 nextToken으로 다음 페이지를 조회한다")
    void readNextPage() throws Exception {
        // given
        JsonNode firstResponse = jsonMapper.readTree("""
            {
              "data": [
                {"id": "post-1"},
                {"id": "post-2"}
              ],
              "meta": {
                "next_token": "NEXT_TOKEN"
              }
            }
            """);

        JsonNode secondResponse = jsonMapper.readTree("""
            {
              "data": [
                {"id": "post-3"}
              ],
              "meta": {
                "result_count": 1
              }
            }
            """);

        given(xClient.callExternalApi(
                TransferPostSource.FABRIZIO_ROMANO, null
        )).willReturn(firstResponse);

        given(xClient.callExternalApi(
                TransferPostSource.FABRIZIO_ROMANO, "NEXT_TOKEN"
        )).willReturn(secondResponse);

        // when
        TransferPostRawItem first = reader.read();
        TransferPostRawItem second = reader.read();
        TransferPostRawItem third = reader.read();
        TransferPostRawItem fourth = reader.read();

        // then
        assertThat(first.post().get("id").asString()).isEqualTo("post-1");
        assertThat(second.post().get("id").asString()).isEqualTo("post-2");
        assertThat(third.post().get("id").asString()).isEqualTo("post-3");
        assertThat(fourth).isNull();

        verify(xClient, times(1)).callExternalApi(TransferPostSource.FABRIZIO_ROMANO, null);
        verify(xClient, times(1)).callExternalApi(TransferPostSource.FABRIZIO_ROMANO, "NEXT_TOKEN");
    }

    @Test
    @DisplayName("최대 페이지 수에 도달하면 다음 페이지를 요청하지 않는다")
    void stopAtMaxPages() throws Exception {
        // given
        JsonNode firstResponse = jsonMapper.readTree("""
            {
              "data": [
                {"id": "post-1"}
              ],
              "meta": {
                "next_token": "NEXT_TOKEN2"
              }
            }
            """);

        JsonNode secondResponse = jsonMapper.readTree("""
            {
              "data": [
                {"id": "post-2"}
              ],
              "meta": {
                "next_token": "NEXT_TOKEN3"
              }
            }
            """);

        given(xClient.callExternalApi(TransferPostSource.FABRIZIO_ROMANO, null))
                .willReturn(firstResponse);

        given(xClient.callExternalApi(TransferPostSource.FABRIZIO_ROMANO, "NEXT_TOKEN2"))
                .willReturn(secondResponse);

        // when
        TransferPostRawItem first = reader.read();
        TransferPostRawItem second = reader.read();
        TransferPostRawItem third = reader.read();

        // then
        assertThat(first.post().get("id").asString()).isEqualTo("post-1");
        assertThat(second.post().get("id").asString()).isEqualTo("post-2");

        assertThat(third).isNull();

        verify(xClient, times(1)).callExternalApi(TransferPostSource.FABRIZIO_ROMANO, null);
        verify(xClient, times(1)).callExternalApi(TransferPostSource.FABRIZIO_ROMANO, "NEXT_TOKEN2");
        verify(xClient, never()).callExternalApi(TransferPostSource.FABRIZIO_ROMANO, "NEXT_TOKEN3");
    }




}