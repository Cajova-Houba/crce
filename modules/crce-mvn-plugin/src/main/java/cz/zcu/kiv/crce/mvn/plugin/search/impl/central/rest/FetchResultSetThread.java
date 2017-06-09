package cz.zcu.kiv.crce.mvn.plugin.search.impl.central.rest;

import cz.zcu.kiv.crce.mvn.plugin.search.FoundArtifact;
import cz.zcu.kiv.crce.mvn.plugin.search.impl.SimpleFoundArtifact;
import cz.zcu.kiv.crce.mvn.plugin.search.impl.central.rest.json.CentralRepoJsonResponse;
import cz.zcu.kiv.crce.mvn.plugin.search.impl.central.rest.json.JsonArtifactDescriptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Downloads and parses a set of search results.
 *
 * @author Zdenek Vales
 */
public class FetchResultSetThread extends Thread {

    private final int start;

    private final int rows;

    private QueryBuilder queryBuilder;

    private List<FoundArtifact> foundArtifacts;

    /**
     * Constructor.
     * @param queryBuilder Query builder containing the original query.
     * @param start Number of the starting result.
     * @param rows How many results should be fetched.
     */
    public FetchResultSetThread(QueryBuilder queryBuilder, int start, int rows) {
        this.start = start;
        this.rows = rows;
        this.queryBuilder = queryBuilder;
        foundArtifacts = new ArrayList<>();
    }

    @Override
    public void run() {
        // download results
        List<JsonArtifactDescriptor> jsonArtifactDescriptors = new ArrayList<>();
        List<FoundArtifact> foundArtifactsTmp = new ArrayList<>();
        CentralRepoRestConsumer restConsumer = new CentralRepoRestConsumer();
        queryBuilder = queryBuilder.addAdditionalParameter(AdditionalQueryParam.ROWS, Integer.toString(rows));
        queryBuilder = queryBuilder.addAdditionalParameter(AdditionalQueryParam.START, Integer.toString(start));
        CentralRepoJsonResponse jsonResponse = restConsumer.getJson(queryBuilder);
        if(jsonResponse.getResponse().getNumFound() > 0) {
            jsonArtifactDescriptors.addAll(Arrays.asList(jsonResponse.getResponse().getDocs()));
        }

        // parse results
        for(JsonArtifactDescriptor ad : jsonArtifactDescriptors) {
            foundArtifactsTmp.add(new SimpleFoundArtifact(ad.getG(),
                    ad.getA(),
                    ad.getV(),
                    ad.jarDownloadLink(),
                    ad.pomDownloadLink()));
        }
        setFoundArtifacts(foundArtifactsTmp);
    }

    public synchronized void setFoundArtifacts(List<FoundArtifact> foundArtifacts) {
        this.foundArtifacts = foundArtifacts;
    }

    public synchronized List<FoundArtifact> getFoundArtifacts() {
        return foundArtifacts;
    }
}
