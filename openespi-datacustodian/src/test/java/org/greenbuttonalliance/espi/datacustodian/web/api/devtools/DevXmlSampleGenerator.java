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

package org.greenbuttonalliance.espi.datacustodian.web.api.devtools;

import org.greenbuttonalliance.espi.common.dto.atom.UsageAtomEntryDto;
import org.greenbuttonalliance.espi.common.dto.usage.UsagePointDto;
import org.greenbuttonalliance.espi.common.service.impl.UsageExportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Dev-only, on-demand generator that marshals a sample ESPI resource through the real (tested)
 * {@link UsageExportService} and writes the Atom XML to the repo-root {@code dev-xml-capture/sample/}
 * so a developer can view the actual ESPI XML <b>without</b> standing up the full OAuth-secured
 * server + database (#119).
 *
 * <p>This class is intentionally <b>not</b> named {@code *Test}/{@code *Tests}, so the normal CI
 * Surefire run does not execute it (no files written in CI). Run it on demand:</p>
 * <pre>
 *   mvn test -pl openespi-datacustodian -Dtest=DevXmlSampleGenerator -Dsurefire.failIfNoSpecifiedTests=false
 * </pre>
 * <p>Output (git-ignored): {@code dev-xml-capture/sample/UsagePoint-sample.xml}.</p>
 */
@DisplayName("Dev ESPI XML sample generator (#119)")
class DevXmlSampleGenerator {

    @Test
    @DisplayName("write a sample UsagePoint Atom XML to dev-xml-capture/sample")
    void generateUsagePointSample() throws Exception {
        UsageExportService usageExportService = new UsageExportService();
        usageExportService.init(); // not using Spring context here

        UsagePointDto usagePoint = new UsagePointDto(
                new byte[]{0x01, 0x02},  // roleFlags
                null,                    // serviceCategory
                (short) 1,               // status
                null, null, null, null, null,
                null, null, null, null,
                null, null, null, null, null,
                null, null, null, null,
                null, null, null, null, null);

        UsageAtomEntryDto entry = new UsageAtomEntryDto(
                "urn:uuid:550e8400-e29b-51d4-a716-446655440000",
                "Residential Electric Service - Usage Domain (dev sample)",
                usagePoint);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        usageExportService.exportDto(entry, out);
        byte[] xml = out.toByteArray();

        Path dir = repoRoot().resolve("dev-xml-capture").resolve("sample");
        Files.createDirectories(dir);
        Path file = dir.resolve("UsagePoint-sample.xml");
        Files.write(file, xml);

        System.out.println("\n[dev] wrote sample ESPI XML -> " + file.toAbsolutePath() + "\n");
        System.out.println(new String(xml));

        assertThat(Files.readString(file)).contains("<espi:UsagePoint");
    }

    /** Walk up from the working directory to the repository root (the dir containing {@code .git}). */
    private static Path repoRoot() {
        Path dir = Path.of("").toAbsolutePath();
        while (dir != null) {
            if (Files.isDirectory(dir.resolve(".git"))) {
                return dir;
            }
            dir = dir.getParent();
        }
        // Fallback: when run from the module dir, the repo root is the parent.
        return Path.of("").toAbsolutePath().getParent();
    }
}
