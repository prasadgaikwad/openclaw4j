package dev.prasadgaikwad.openclaw4j.rag;

import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.request.conversations.ConversationsHistoryRequest;
import com.slack.api.model.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Periodically indexes configured Slack channels into the RAG knowledge base.
 */
@Service
public class ChannelIndexer {

    private static final Logger log = LoggerFactory.getLogger(ChannelIndexer.class);

    private final MethodsClient slack;
    private final RAGService ragService;

    @Value("${openclaw4j.rag.channels:}")
    private List<String> channelIds;

    public ChannelIndexer(MethodsClient slack, RAGService ragService) {
        this.slack = slack;
        this.ragService = ragService;
    }

    /**
     * Scheduled task to index channel history.
     * Runs every 30 minutes by default.
     */
    @Scheduled(fixedRateString = "${openclaw4j.rag.indexing-interval:PT30M}")
    public void run() {
        if (channelIds == null || channelIds.isEmpty()) {
            log.debug("No channels configured for RAG indexing.");
            return;
        }

        for (String channelId : channelIds) {
            indexChannel(channelId);
        }
    }

    private void indexChannel(String channelId) {
        log.info("Starting indexing for channel: {}", channelId);
        try {
            var result = slack.conversationsHistory(ConversationsHistoryRequest.builder()
                    .channel(channelId)
                    .limit(100)
                    .build());

            if (!result.isOk()) {
                log.error("Failed to fetch history for channel {}: {}", channelId, result.getError());
                return;
            }

            List<Document> docs = new ArrayList<>();
            for (Message msg : result.getMessages()) {
                if (msg.getText() == null || msg.getText().isBlank())
                    continue;

                Document doc = new Document(
                        msg.getText(),
                        Map.of(
                                "channelId", channelId,
                                "userId", msg.getUser() != null ? msg.getUser() : "unknown",
                                "timestamp", msg.getTs(),
                                "source", "slack"));
                docs.add(doc);
            }

            ragService.indexDocuments(docs);
            log.info("Successfully indexed {} messages from channel {}", docs.size(), channelId);

        } catch (Exception e) {
            log.error("Error during channel indexing for {}", channelId, e);
        }
    }
}
