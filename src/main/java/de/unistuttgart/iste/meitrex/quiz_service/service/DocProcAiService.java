package de.unistuttgart.iste.meitrex.quiz_service.service;

import de.unistuttgart.iste.meitrex.quiz_service.config.DocProcConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.client.GraphQlClient;
import org.springframework.graphql.client.HttpGraphQlClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import javax.xml.datatype.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * This service is used as a wrapper to communicate with the ProcAi Service
 */
@Service
public class DocProcAiService {


    final GraphQlClient graphQlClient;
    final DocProcConfig docProcConfig;

    /**
     * Constructor for the DocProcAiService
     *
     */
    public DocProcAiService(@Autowired DocProcConfig docProcConfig) {
        this.docProcConfig = docProcConfig;
        this.graphQlClient = HttpGraphQlClient.builder()
                .url(docProcConfig.getUrl())
                .build();

    }

    /**
     * Fetch the summary of a document by its id using the docproc service.
     *
     * @param docId the id of the document
     * @return the summary of the document
     */
    Mono<String> getSummaryByDocId(final UUID docId) {

        final String query = "query GetRecord($mediaRecordId: UUID!){_internal_noauth_getMediaRecordSummary(mediaRecordId: $mediaRecordId)}";
        final Mono<List<String>> response = graphQlClient.document(query)
                .variable("mediaRecordId", docId)
                .execute()
                .map(response1 -> response1.field("_internal_noauth_getMediaRecordSummary").toEntityList(String.class));


        // concat all summaries in mono
        return response
                .map(summaryList -> summaryList != null ? String.join("\n", summaryList) : "")
                .defaultIfEmpty("");

    }

}
