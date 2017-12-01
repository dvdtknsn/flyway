/*
 * Copyright 2010-2017 Boxfuse GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flywaydb.core;

import org.flywaydb.core.internal.util.FileCopyUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.sql.SQLException;
import java.util.Collections;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@RunWith(Parameterized.class)
public class FlywayPerformanceMediumTest {
    private static final String DIR = "target/performance/" + System.currentTimeMillis();

    @Parameterized.Parameter
    public int scriptCount;

    @Parameterized.Parameters(name = "scriptCount={0}")
    public static Iterable<?> data() {
        return Collections.singletonList(1000);
    }

    @Rule
    public Timeout globalTimeout = new Timeout(180, TimeUnit.SECONDS);

    @Before
    public void generateLotsOfInstallerScripts() throws IOException {
        //noinspection ResultOfMethodCallIgnored
        new File(DIR).mkdirs();
        Executor executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);
        for (int i = (scriptCount - 500); i < scriptCount; i++) {
            final int j = i;
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        FileCopyUtils.copy(new StringReader("SELECT " + j + " FROM DUAL"), new FileWriter(DIR + "/V" + j + "__Test.sql"));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    @Test
    public void testPerformance() throws IOException, SQLException {
        Flyway flyway = new Flyway();
        flyway.setDataSource("jdbc:h2:mem:flyway_db_perf;DB_CLOSE_DELAY=-1", "sa", "");
        flyway.setLocations("filesystem:" + DIR);
        flyway.migrate();
    }
}