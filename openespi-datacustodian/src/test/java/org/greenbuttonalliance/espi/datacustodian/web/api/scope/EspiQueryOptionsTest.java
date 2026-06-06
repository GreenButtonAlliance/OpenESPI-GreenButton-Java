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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link EspiQueryOptions} — REQ.21.6.2.8 feed query parameters (all optional) (#119).
 */
@DisplayName("EspiQueryOptions — REQ.21.6.2.8 (#119)")
class EspiQueryOptionsTest {

    @Test
    @DisplayName("none() is empty and has no timestamp filter")
    void noneIsEmpty() {
        EspiQueryOptions opts = EspiQueryOptions.none();
        assertThat(opts.isEmpty()).isTrue();
        assertThat(opts.hasTimestampFilter()).isFalse();
    }

    @Test
    @DisplayName("a timestamp filter is detected and not considered empty")
    void timestampFilterDetected() {
        EspiQueryOptions opts = new EspiQueryOptions(
                OffsetDateTime.parse("2025-01-01T00:00:00Z"), null, null, null,
                null, null, null, null);
        assertThat(opts.isEmpty()).isFalse();
        assertThat(opts.hasTimestampFilter()).isTrue();
    }

    @Test
    @DisplayName("optional DB knobs alone are not empty but are not timestamp filters")
    void optionalKnobsNotTimestamp() {
        EspiQueryOptions opts = new EspiQueryOptions(
                null, null, null, null, 100, 0, "cursor-1", 2);
        assertThat(opts.isEmpty()).isFalse();
        assertThat(opts.hasTimestampFilter()).isFalse();
        assertThat(opts.maxResults()).isEqualTo(100);
        assertThat(opts.startIndex()).isZero();
        assertThat(opts.startAfter()).isEqualTo("cursor-1");
        assertThat(opts.depth()).isEqualTo(2);
    }
}
