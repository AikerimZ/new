/*
 * Copyright 2018 Google LLC
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.cloud.pontem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link BaseCloudSpannerDatabaseRestore}. */
@RunWith(JUnit4.class)
public class BaseCloudSpannerDatabaseRestoreTest {

  @Test
  public void testQueryListOfTablesToRestore_validWithSimpleTableSchema() throws Exception {
    String projectId = "cloud-project-id";
    String inputGcsPath = "gs://my-bucket/myPath";
    String[] emptyTableNamesToIncludeInRestore = new String[0];
    String[] emptyTableNamesToExcludeFromRestore = new String[0];

    Util mockUtil = mock(Util.class);
    when(mockUtil.getContentsOfFileFromGcs(
            eq(projectId),
            eq(Util.getGcsBucketNameFromDatabaseBackupLocation(inputGcsPath)),
            eq(Util.getGcsFolderPathFromDatabaseBackupLocation(inputGcsPath)),
            eq(Util.FILE_PATH_FOR_DATABASE_TABLE_NAMES)))
        .thenReturn("MyTableName1,\nMyTableName2,\nMyTableName3,");

    LinkedHashMap<String, LinkedList<String>> parsedTablesToRestore =
        BaseCloudSpannerDatabaseRestore.queryListOfTablesToRestore(
            projectId,
            inputGcsPath,
            emptyTableNamesToIncludeInRestore,
            emptyTableNamesToExcludeFromRestore,
            mockUtil);
    assertEquals("Number of root tables to restore is invalid", 3, parsedTablesToRestore.size());
    assertTrue("Root table is not present", parsedTablesToRestore.containsKey("MyTableName1"));
    assertTrue("Root table is not present", parsedTablesToRestore.containsKey("MyTableName2"));
    assertTrue("Root table is not present", parsedTablesToRestore.containsKey("MyTableName3"));
    assertFalse("Invalid root table is present", parsedTablesToRestore.containsKey("MyTableName4"));
  }

  @Test
  public void testQueryListOfTablesToRestore_validWithParentChildTableSchema() throws Exception {
    String projectId = "cloud-project-id";
    String inputGcsPath = "gs://my-bucket/myPath";
    String[] emptyTableNamesToIncludeInRestore = new String[0];
    String[] emptyTableNamesToExcludeFromRestore = new String[0];

    Util mockUtil = mock(Util.class);
    when(mockUtil.getContentsOfFileFromGcs(
            eq(projectId),
            eq(Util.getGcsBucketNameFromDatabaseBackupLocation(inputGcsPath)),
            eq(Util.getGcsFolderPathFromDatabaseBackupLocation(inputGcsPath)),
            eq(Util.FILE_PATH_FOR_DATABASE_TABLE_NAMES)))
        .thenReturn("RootTable1,\nChildTable1,RootTable1\nChildTable2,ChildTable1");

    LinkedHashMap<String, LinkedList<String>> parsedTablesToRestore =
        BaseCloudSpannerDatabaseRestore.queryListOfTablesToRestore(
            projectId,
            inputGcsPath,
            emptyTableNamesToIncludeInRestore,
            emptyTableNamesToExcludeFromRestore,
            mockUtil);
    assertEquals("Number of root tables to restore is invalid", 1, parsedTablesToRestore.size());
    assertTrue("Root table is not present", parsedTablesToRestore.containsKey("RootTable1"));
    assertFalse("Child table is present as root", parsedTablesToRestore.containsKey("ChildTable1"));

    assertEquals(
        "Unexpected table hierarchy", "RootTable1", parsedTablesToRestore.get("RootTable1").pop());
    assertEquals(
        "Unexpected table hierarchy", "ChildTable1", parsedTablesToRestore.get("RootTable1").pop());
    assertEquals(
        "Unexpected table hierarchy", "ChildTable2", parsedTablesToRestore.get("RootTable1").pop());
    assertEquals("Unexpected table hierarchy", 0, parsedTablesToRestore.get("RootTable1").size());
  }

  @Test
  public void testQueryListOfTablesToRestore_validWithIncludeList() throws Exception {
    String projectId = "cloud-project-id";
    String inputGcsPath = "gs://my-bucket/myPath";
    String[] emptyTableNamesToIncludeInRestore = {"RootTable1", "ChildTable1"};
    String[] emptyTableNamesToExcludeFromRestore = new String[0];

    Util mockUtil = mock(Util.class);
    when(mockUtil.getContentsOfFileFromGcs(
            eq(projectId),
            eq(Util.getGcsBucketNameFromDatabaseBackupLocation(inputGcsPath)),
            eq(Util.getGcsFolderPathFromDatabaseBackupLocation(inputGcsPath)),
            eq(Util.FILE_PATH_FOR_DATABASE_TABLE_NAMES)))
        .thenReturn("RootTable1,\nChildTable1,RootTable1\nChildTable2,ChildTable1");

    LinkedHashMap<String, LinkedList<String>> parsedTablesToRestore =
        BaseCloudSpannerDatabaseRestore.queryListOfTablesToRestore(
            projectId,
            inputGcsPath,
            emptyTableNamesToIncludeInRestore,
            emptyTableNamesToExcludeFromRestore,
            mockUtil);
    assertEquals("Number of root tables to restore is invalid", 1, parsedTablesToRestore.size());
    assertTrue("Root table is not present", parsedTablesToRestore.containsKey("RootTable1"));
    assertFalse("Child table is present as root", parsedTablesToRestore.containsKey("ChildTable1"));

    assertEquals(
        "Unexpected table hierarchy", "RootTable1", parsedTablesToRestore.get("RootTable1").pop());
    assertEquals(
        "Unexpected table hierarchy", "ChildTable1", parsedTablesToRestore.get("RootTable1").pop());
    assertEquals("Unexpected table hierarchy", 0, parsedTablesToRestore.get("RootTable1").size());
  }

  @Test
  public void testQueryListOfTablesToRestore_validWithExcludeList() throws Exception {
    String projectId = "cloud-project-id";
    String inputGcsPath = "gs://my-bucket/myPath";
    String[] emptyTableNamesToIncludeInRestore = new String[0];
    String[] emptyTableNamesToExcludeFromRestore = {"RootTable1", "RootTable2", "RootTable3"};

    Util mockUtil = mock(Util.class);
    when(mockUtil.getContentsOfFileFromGcs(
            eq(projectId),
            eq(Util.getGcsBucketNameFromDatabaseBackupLocation(inputGcsPath)),
            eq(Util.getGcsFolderPathFromDatabaseBackupLocation(inputGcsPath)),
            eq(Util.FILE_PATH_FOR_DATABASE_TABLE_NAMES)))
        .thenReturn("RootTable1,\nRootTable2,\nRootTable3,\nRootTable4,\nChildTable1,RootTable4");

    LinkedHashMap<String, LinkedList<String>> parsedTablesToRestore =
        BaseCloudSpannerDatabaseRestore.queryListOfTablesToRestore(
            projectId,
            inputGcsPath,
            emptyTableNamesToIncludeInRestore,
            emptyTableNamesToExcludeFromRestore,
            mockUtil);
    assertEquals("Number of root tables to restore is invalid", 1, parsedTablesToRestore.size());
    assertTrue("Root table is not present", parsedTablesToRestore.containsKey("RootTable4"));
    assertFalse("Root table is not present", parsedTablesToRestore.containsKey("RootTable5"));
    assertEquals(
        "Unexpected table hierarchy", "RootTable4", parsedTablesToRestore.get("RootTable4").pop());
    assertEquals(
        "Unexpected table hierarchy", "ChildTable1", parsedTablesToRestore.get("RootTable4").pop());
    assertEquals("Unexpected table hierarchy", 0, parsedTablesToRestore.get("RootTable4").size());
  }

  @Test(expected = Exception.class)
  public void testQueryListOfTablesToRestore_invalidExcludeList() throws Exception {
    String projectId = "cloud-project-id";
    String inputGcsPath = "gs://my-bucket/myPath";
    String[] emptyTableNamesToIncludeInRestore = new String[0];
    String[] emptyTableNamesToExcludeFromRestore = {"RootTable1"};

    Util mockUtil = mock(Util.class);
    when(mockUtil.getContentsOfFileFromGcs(
            eq(projectId),
            eq(Util.getGcsBucketNameFromDatabaseBackupLocation(inputGcsPath)),
            eq(Util.getGcsFolderPathFromDatabaseBackupLocation(inputGcsPath)),
            eq(Util.FILE_PATH_FOR_DATABASE_TABLE_NAMES)))
        .thenReturn("RootTable1,\nChildTable1,RootTable1\nRootTable2,");

    LinkedHashMap<String, LinkedList<String>> parsedTablesToRestore =
        BaseCloudSpannerDatabaseRestore.queryListOfTablesToRestore(
            projectId,
            inputGcsPath,
            emptyTableNamesToIncludeInRestore,
            emptyTableNamesToExcludeFromRestore,
            mockUtil);
  }

  @Test(expected = Exception.class)
  public void testQueryListOfTablesToRestore_invalidIncludeList() throws Exception {
    String projectId = "cloud-project-id";
    String inputGcsPath = "gs://my-bucket/myPath";
    String[] emptyTableNamesToIncludeInRestore = {"ChildTable1"};
    String[] emptyTableNamesToExcludeFromRestore = new String[0];

    Util mockUtil = mock(Util.class);
    when(mockUtil.getContentsOfFileFromGcs(
            eq(projectId),
            eq(Util.getGcsBucketNameFromDatabaseBackupLocation(inputGcsPath)),
            eq(Util.getGcsFolderPathFromDatabaseBackupLocation(inputGcsPath)),
            eq(Util.FILE_PATH_FOR_DATABASE_TABLE_NAMES)))
        .thenReturn("RootTable1,\nChildTable1,RootTable1\nRootTable2,");

    LinkedHashMap<String, LinkedList<String>> parsedTablesToRestore =
        BaseCloudSpannerDatabaseRestore.queryListOfTablesToRestore(
            projectId,
            inputGcsPath,
            emptyTableNamesToIncludeInRestore,
            emptyTableNamesToExcludeFromRestore,
            mockUtil);
  }
}