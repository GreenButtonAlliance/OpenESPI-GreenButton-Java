/*
 * Copyright 2013 EnergyOS.org
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.energyos.espi.thirdparty.domain;

import org.junit.Test;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import static org.energyos.espi.thirdparty.utils.TestUtils.assertAnnotationPresent;

public class ReadingQualityPersistenceTests {
    @Test
    public void persistence() {
        assertAnnotationPresent(ReadingQuality.class, Entity.class);
        assertAnnotationPresent(ReadingQuality.class, Table.class);
    }

    @Test
    public void intervalReadings() {
        assertAnnotationPresent(ReadingQuality.class, "intervalReading", ManyToOne.class);
        assertAnnotationPresent(ReadingQuality.class, "intervalReading", JoinColumn.class);
    }

}