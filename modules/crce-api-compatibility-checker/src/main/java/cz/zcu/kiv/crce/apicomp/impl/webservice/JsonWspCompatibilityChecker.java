package cz.zcu.kiv.crce.apicomp.impl.webservice;

import cz.zcu.kiv.crce.metadata.AttributeType;

import java.util.Collections;
import java.util.List;

/**
 * Contains logic for comparing apis described by Json-WSP.
 *
 * Possible final diffs:
 * NON - no difference between APIs.
 * GEN - API 2 is more generic than API 1. Could be because of endpoint parameter GEN or
 *       there is at least one optional endpoint parameter in API 2 that was required in API 1.
 * SPE - API 2 is less generic than API 1. Caused by data types in endpoint responses and/or parameters.
 * INS - API 2 contains endpoint that API 1 does not.
 * DEL - API 1 contains endpoint that API 2 does not.
 * MUT - Combination of INS/DEL and/or GEN/SPEC.
 * UNK - Could not determine diff, could be because of uncomparable data types (endpoint parameters/responses)
 *       or communication patterns do not match.
 */
public class JsonWspCompatibilityChecker extends WadlCompatibilityChecker {

    @Override
    protected List<AttributeType> getEndpointMetadataAttributeTypes() {
        // only name is indexed for JSON-WSP endpoints
        return Collections.singletonList(WebserviceIndexerConstants.ATTRIBUTE__WEBSERVICE_ENDPOINT__NAME);
    }
}