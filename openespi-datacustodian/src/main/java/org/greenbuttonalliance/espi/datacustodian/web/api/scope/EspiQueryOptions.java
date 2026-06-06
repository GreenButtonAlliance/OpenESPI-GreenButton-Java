/*
 *
 *        Copyright (c) 2025 Green Button Alliance, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package org.greenbuttonalliance.espi.datacustodian.web.api.scope;

import java.time.OffsetDateTime;

/**
 * The ESPI feed query parameters of REQ.21.6.2.8, as bound from a resource request (#119).
 *
 * <p><b>All parameters are optional on the request</b> — a feed request need supply none of them.
 * The standard states only {@code published-min/max} and {@code updated-min/max} are
 * <em>required to be supported</em> by the Data Custodian (they filter by the Atom
 * {@code published}/{@code updated} timestamps); {@code max-results}, {@code start-index},
 * {@code start-after} and {@code depth} are optional DB-query knobs that certification checks only
 * for data-format acceptance, not filtering behavior — they are accepted (never a {@code 400}) and
 * applied best-effort.</p>
 *
 * <p>ESPI does not define {@code limit}/{@code offset} pagination; CMD/DMD are machine-to-machine.</p>
 *
 * @param publishedMin filter: Atom {@code published} &ge; this instant (required-to-support)
 * @param publishedMax filter: Atom {@code published} &le; this instant (required-to-support)
 * @param updatedMin   filter: Atom {@code updated} &ge; this instant (required-to-support)
 * @param updatedMax   filter: Atom {@code updated} &le; this instant (required-to-support)
 * @param maxResults   optional cap on results (best-effort)
 * @param startIndex   optional 0-based start offset (best-effort)
 * @param startAfter   optional id cursor (best-effort; accepted, no-op until needed)
 * @param depth        optional feed-expansion depth (best-effort; sandbox returns natural depth)
 */
public record EspiQueryOptions(
        OffsetDateTime publishedMin,
        OffsetDateTime publishedMax,
        OffsetDateTime updatedMin,
        OffsetDateTime updatedMax,
        Integer maxResults,
        Integer startIndex,
        String startAfter,
        Integer depth) {

    /** An options object with nothing set — i.e. return the full (scoped) feed. */
    public static EspiQueryOptions none() {
        return new EspiQueryOptions(null, null, null, null, null, null, null, null);
    }

    /** True when no parameter is set (the request supplied no query filters). */
    public boolean isEmpty() {
        return publishedMin == null && publishedMax == null && updatedMin == null && updatedMax == null
                && maxResults == null && startIndex == null && startAfter == null && depth == null;
    }

    /** True when any of the required-to-support timestamp filters is present. */
    public boolean hasTimestampFilter() {
        return publishedMin != null || publishedMax != null || updatedMin != null || updatedMax != null;
    }
}
