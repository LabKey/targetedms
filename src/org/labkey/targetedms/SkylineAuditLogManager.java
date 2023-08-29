/*
 * Copyright (c) 2019 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.targetedms;

import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.labkey.api.data.BaseSelector;
import org.labkey.api.data.CompareType;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.DbScope;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.SqlExecutor;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableInfo;
import org.labkey.api.module.ModuleLoader;
import org.labkey.api.module.ModuleProperty;
import org.labkey.api.query.FieldKey;
import org.labkey.api.security.User;
import org.labkey.api.util.GUID;
import org.labkey.api.util.JunitUtil;
import org.labkey.api.util.TestContext;
import org.labkey.api.util.logging.LogHelper;
import org.labkey.targetedms.parser.skyaudit.AuditLogEntry;
import org.labkey.targetedms.parser.skyaudit.AuditLogException;
import org.labkey.targetedms.parser.skyaudit.AuditLogParsingException;
import org.labkey.targetedms.parser.skyaudit.AuditLogTree;
import org.labkey.targetedms.parser.skyaudit.SkylineAuditLogParser;
import org.labkey.targetedms.parser.skyaudit.SkylineAuditLogSecurityManager;
import org.labkey.targetedms.parser.skyaudit.UnitTestUtil;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;


public class SkylineAuditLogManager
{
    private static final Logger _logger = LogHelper.getLogger(SkylineAuditLogManager.class, "Persists Skyline audit logs in database");
    private final SkylineAuditLogSecurityManager _securityMgr;

    private static class AuditLogImportContext
    {
        File _logFile;
        GUID _documentGUID;
        AuditLogTree _logTree;
        Long _runId;
        MessageDigest _rootHash = null;
    }

    public SkylineAuditLogManager(@NotNull Container pContainer, @Nullable Logger jobLogger)
    {
        _securityMgr = new SkylineAuditLogSecurityManager(pContainer, jobLogger);
    }


    public int importAuditLogFile(User user, @NotNull File pAuditLogFile, @NotNull GUID pDocumentGUID, TargetedMSRun run) throws AuditLogException
    {
        AuditLogImportContext context = new AuditLogImportContext();
        context._logFile = pAuditLogFile;
        context._documentGUID = pDocumentGUID;
        context._runId = run.getId();
        try
        {
            context._rootHash = MessageDigest.getInstance("SHA1");
        }
        catch (NoSuchAlgorithmException e)
        {
            _securityMgr.reportErrorForIntegrityLevel("Cannot verify root hash because SHA1 algorithm is not available.",
                    SkylineAuditLogSecurityManager.INTEGRITY_LEVEL.ANY);
        }

        if (verifyPreRequisites(context))
        {
            try (SkylineAuditLogParser parser = new SkylineAuditLogParser(context._logFile, _logger))
            {
                int entriesCount = persistAuditLog(user, context, parser);
                verifyPostRequisites(context, parser);
                return entriesCount;
            }
        }
        else
            return 0;
    }


    /***
     * Method verifies that all conditions required for the audit log upload are satisfied.
     *
     * @return It returns true if so, false if no log upload is required.
     * @throws AuditLogException is thrown if audit log settings prevent the document upload (should rollback document transaction on catch)
     */
    private boolean verifyPreRequisites(AuditLogImportContext pContext) throws AuditLogException
    {
        //verify if log file and GUID are not empty
        if(pContext._logFile == null || !pContext._logFile.exists()){
            if(_securityMgr.getIntegrityLevel() == SkylineAuditLogSecurityManager.INTEGRITY_LEVEL.ANY){
                _logger.warn("Log file is missing. Proceeding without the log.");
                return false;
            }
            else
                throw new AuditLogException("Current log integrity setting do not allow to upload a file without a valid audit log. ");
        }
        if(pContext._documentGUID == null){
            if(_securityMgr.getIntegrityLevel() == SkylineAuditLogSecurityManager.INTEGRITY_LEVEL.ANY){
                _logger.warn("Cannot process the audit log because the document does not have a valid GUID. " +
                        "You are probably using old version if Skyline. Proceeding without the log.");
                return false;
            }
            else
                throw new AuditLogException("Current log integrity setting do not allow to upload a file without a valid document GUID. ");
        }

        //retrieve count of documents with the same GUID
        TableInfo runsTbl = TargetedMSManager.getTableInfoRuns();
        SimpleFilter docFilter = new SimpleFilter();
        docFilter.addCondition(       // get all entries for all versions of this document
                new CompareType.CompareClause(FieldKey.fromParts("documentGuid"), CompareType.EQUAL, pContext._documentGUID.toString())
        );
        docFilter.addCondition(       //except the version we are currently uploading
                new CompareType.CompareClause(FieldKey.fromParts("Id"), CompareType.NEQ, pContext._runId)
        );
        docFilter.addCondition(       //and they are successfully loaded
                new CompareType.CompareClause(FieldKey.fromParts("StatusId"), CompareType.EQUAL, SkylineDocImporter.STATUS_SUCCESS)
        );

        SQLFragment query = new SQLFragment("SELECT count(*) docCount FROM ")
                    .append(runsTbl)
                    .append(" ")
                    .append(docFilter.getSQLFragment(TargetedMSManager.getSqlDialect()));

        Integer docCount = new SqlSelector(TargetedMSManager.getSchema(), query).getObject(Integer.class);
        pContext._logTree = buildLogTree(pContext._documentGUID);

        //verify that the tree is not empty
        if(docCount == 0 || pContext._logTree.getTreeSize() > 1)
        {
            //if document count >0 retrieve audit log tree for this GUID
            return true;
        }
        else
        {
            _securityMgr.reportErrorForIntegrityLevel("Cannot upload the audit log. There are existing versions of this document that do not have audit log information.",
                    SkylineAuditLogSecurityManager.INTEGRITY_LEVEL.ANY);
            return false;
        }
    }

    /***
     * Iterates through the log entries and persists them one by one
     * @return number of audit log entries read from the file.
     */
    private int persistAuditLog(User user, AuditLogImportContext pContext, SkylineAuditLogParser parser) throws AuditLogException
    {
        //since entries in the log file are in reverse chronological order we have to
        //read them in the list and then reverse it before the tree processing
        List<AuditLogEntry> entries = new LinkedList<>();
        int hashValidationFailures = 0;
        AuditLogEntry previousEntry = null;
        //while next entry is not null
        while (parser.hasNextEntry())
        {
            try
            {
                AuditLogEntry ent = parser.parseLogEntry();
                ent.setDocumentGUID(pContext._documentGUID);
                pContext._rootHash.update(ent.getEntryHash().getBytes(StandardCharsets.UTF_8));
                // Insert at the beginning of the list so we can quickly iterate in reverse order for validation
                entries.add(0, ent);
            }
            catch (XMLStreamException e)
            {
                throw new AuditLogException("Error when parsing the audit log file.", e);
            }
            catch(AuditLogParsingException e)
            {
                _securityMgr.reportErrorForIntegrityLevel(
                        "Error when parsing audit log file.",
                        SkylineAuditLogSecurityManager.INTEGRITY_LEVEL.ANY, e);
            }
        }

        for (AuditLogEntry ent : entries)
        {
            if (previousEntry != null)
            {
                ent.setParentEntryHash(previousEntry.getEntryHash());
            }

            //throw or log the results based on the integrity level setting
            if (!ent.verifyHash())
            {
                if (hashValidationFailures == 0)
                {
                    _securityMgr.reportErrorForIntegrityLevel(
                            String.format("Hash value verification failed for the log entry timestamped with %s. This is expected for older Skyline documents with audit logs that do not contain hashes. Suppressing warning for remainder of file", ent.getOffsetCreateTimestamp().toString()),
                            SkylineAuditLogSecurityManager.INTEGRITY_LEVEL.ANY);
                }
                hashValidationFailures++;
            }
            previousEntry = ent;
        }

        if (hashValidationFailures > 0)
        {
            _securityMgr.reportErrorForIntegrityLevel(
                    "Hash value verification failed for " + hashValidationFailures + " of " + entries.size() + " total entries",
                    SkylineAuditLogSecurityManager.INTEGRITY_LEVEL.ANY);
        }

        // Issue 39455 - bail out if there were no audit log entries
        if (entries.isEmpty())
        {
            return 0;
        }

        if (pContext._runId != null)       //set the document version id on the chronologically last log entry.
            entries.get(entries.size() - 1).setVersionId(pContext._runId);

        AuditLogTree treePointer = pContext._logTree;
        int persistedEntriesCount = 0;
        for (AuditLogEntry ent : entries)
        {
            //if log tree has the hash
            if (treePointer.hasChildEntry(ent.getEntryHash()))
            {
                // Set so RunAuditLogEntry table can be updated
                ent.setEntryId(treePointer.getChildEntry(ent.getEntryHash()).getEntryId());

                //advance the tree and proceed to the next entry
                treePointer = treePointer.getChildEntry(ent.getEntryHash());
            }
            else
            {
                if (treePointer.getParentEntryHash() == null && treePointer.getTreeSize() > 1)
                {
                    throw new AuditLogException("Invalid audit log. Documents with same GUID should have same first audit log entry.");
                }
                // persist the entry and add to the tree
                ent.setParentEntryHash(treePointer.getEntryHash());
                ent.persist(user);
                persistedEntriesCount++;
                AuditLogTree newTreeEntry = ent.getTreeEntry();
                assert newTreeEntry != null;
                treePointer.addChild(newTreeEntry);
                //advance the tree
                treePointer = newTreeEntry;
            }
        }
        if (persistedEntriesCount == 0)      //if no entries were actually saved into the database we are uploading an earlier document version
            entries.get(entries.size() - 1).insertRunAuditLogEntry(user, pContext._runId);   //and still need to update the terminal entry with the versionId.

        return entries.size();
    }

    private void verifyPostRequisites(AuditLogImportContext pContext, SkylineAuditLogParser parser) throws AuditLogException
    {
        //verify the document-level hash if required
        byte[] rootHashBytes = pContext._rootHash.digest();
        String rootHashString = new String(Base64.getEncoder().encode(rootHashBytes), StandardCharsets.US_ASCII);
        if(!rootHashString.equals(parser.getEnRootHash()))
        {
            //throw or log the results based on the integrity level setting
            _securityMgr.reportErrorForIntegrityLevel(
                    "Audit log root hash verification failed",
                    SkylineAuditLogSecurityManager.INTEGRITY_LEVEL.ANY);
        }

    }

    /**
     * Builds a tree of audit log entry tokens representing the document versioning tree
     * @return the root node of the tree
     */
    private AuditLogTree buildLogTree(@NotNull GUID pDocumentGUID) throws AuditLogException {

        TableInfo entryTbl = TargetedMSManager.getTableInfoSkylineAuditLogEntry();

        SQLFragment treeQuery = new SQLFragment("");
        if(TargetedMSManager.getSqlDialect().isPostgreSQL()){
            treeQuery.append("WITH RECURSIVE tree(id, hash, parenthash, treedepth) AS ");
        }
        else if(TargetedMSManager.getSqlDialect().isSqlServer()){
            treeQuery.append("WITH tree(id, hash, parenthash, treedepth) AS ");
        }
        else
            throw new AuditLogException("Database type not supported");

        treeQuery.append("( ");

        treeQuery.append("SELECT entryid, entryhash, parententryhash, 1 \n")
                .append("FROM ").append(TargetedMSManager.getTableInfoSkylineAuditLogEntry(), "a").append("\n")
                .append(" WHERE documentGuid = ? ").add(pDocumentGUID).append(" \n")
                .append("  AND parententryhash = ?").add(AuditLogTree.NULL_STRING).append(" \n")
                .append(" UNION ALL \n")
                .append(" SELECT entryid, entryhash, parententryhash, treedepth + 1 \n")
                .append("FROM ").append(TargetedMSManager.getTableInfoSkylineAuditLogEntry(), "e").append("\n")
                .append(" JOIN tree ON tree.hash = e.parententryhash \n")
                .append(" WHERE documentGuid = ? ").add(pDocumentGUID).append(" \n")
                .append(" )\n")
                .append(" SELECT DISTINCT t.*, r.VersionId FROM tree t \n")
                .append(" LEFT JOIN ").append(TargetedMSManager.getTableInfoSkylineRunAuditLogEntry(), "r").append(" ON t.Id = r.AuditLogEntryId \n")
                .append(" ORDER BY treedepth\n");

        if (TargetedMSManager.getSqlDialect().isSqlServer())
        {
            // Issue 45712: SQLServer defaults to a limit of 100 recursion steps. Import verification will prevent
            // loops so don't set a limit
            treeQuery.append("option (maxrecursion 0)");
        }

        BaseSelector.ResultSetHandler<Map<String, AuditLogTree>> resultSetHandler = (rs, conn) -> {
            Map<String, AuditLogTree> result = new HashMap<>(10);

            while(rs.next()){
                String parentHash = rs.getString("parenthash");
                if(rs.wasNull()) parentHash = AuditLogTree.NULL_STRING;
                Long versionId = rs.getLong("versionId");
                if(rs.wasNull()) versionId = null;

                AuditLogTree node = new AuditLogTree(
                        rs.getInt("id"),
                        pDocumentGUID,
                        rs.getString("hash"),
                        parentHash,
                        versionId
                );
                result.put(AuditLogTree.getMapId(node.getEntryHash(), node.getVersionId()), node);
            }
            return  result;
        };

        AuditLogTree root = new AuditLogTree(0, null, AuditLogTree.NULL_STRING, null,null);
        Map<String, AuditLogTree> nodes = new SqlExecutor(entryTbl.getSchema()).executeWithResults(treeQuery, resultSetHandler);
        nodes.put(AuditLogTree.NULL_STRING, root);
        for(Map.Entry<String, AuditLogTree> e : nodes.entrySet()){
            if(e.getValue().getParentEntryHash() != null)
            {
                var entry = nodes.entrySet().stream().filter(n -> n.getKey().startsWith(e.getValue().getParentEntryHash())).findFirst();
                entry.ifPresent(stringAuditLogTreeEntry -> stringAuditLogTreeEntry.getValue().addChild(e.getValue()));
            }
        }
        return root;
    }

    /**
     * Deletes audit log entries belonging to the given document version. It will not delete entries that
     * belong to more than one version. If the given run has no GUID it does nothing.
     * @param pRunId of the document version to delete
     */
    public void deleteDocumentVersionLog(long pRunId) throws AuditLogException
    {
        SQLFragment query = new SQLFragment(String.format("SELECT documentGUID FROM targetedms.Runs WHERE Id = %d", pRunId));
        String objGUID = new SqlSelector(TargetedMSManager.getSchema(), query).getObject(String.class);
        if(objGUID == null) return;     //nothing to delete if there is no document or the document has no GUID
        AuditLogTree root = buildLogTree(new GUID(objGUID));
        Set<Integer> deleteEntryIds = root.deleteList(pRunId).stream().map(AuditLogTree::getEntryId).collect(Collectors.toSet());

        try (DbScope.Transaction t = TargetedMSManager.getSchema().getScope().ensureTransaction())
        {
            SimpleFilter runAuditLogFilter = new SimpleFilter(FieldKey.fromParts("VersionId"), pRunId);

            if (deleteEntryIds.size() > 0)
            {
                SimpleFilter entryFilter = new SimpleFilter(
                        new SimpleFilter.InClause(FieldKey.fromParts("entryId"), deleteEntryIds, false)
                );
                runAuditLogFilter.addCondition(new SimpleFilter.InClause(FieldKey.fromParts("AuditLogEntryId"), deleteEntryIds));
                Table.delete(TargetedMSManager.getTableInfoSkylineRunAuditLogEntry(), runAuditLogFilter);
                Table.delete(TargetedMSManager.getTableInfoSkylineAuditLogMessage(), entryFilter);
                Table.delete(TargetedMSManager.getTableInfoSkylineAuditLogEntry(), entryFilter);
            }
            else
            {
                AuditLogTree versionTree = root.findVersionEntry(pRunId);
                if (versionTree != null)
                {
                    runAuditLogFilter.addCondition(FieldKey.fromParts("AuditLogEntryId"), versionTree.getEntryId());
                    Table.delete(TargetedMSManager.getTableInfoSkylineRunAuditLogEntry(), runAuditLogFilter);
                }
            }
            t.commit();
        }
    }

    public static class TestCase extends Assert
    {
        private static final String FOLDER_NAME = "TargetedMSAuditLogImportFolder";
        private static final String FOLDER_NAME2 = "TargetedMSAuditLogImportFolder2";
        private static final GUID _docGUID = new GUID("50323e78-0e2b-4764-b979-9b71559bbf9f");
        private static User _user;
        private static Container _container;
        private static Container _container2;

        @Before
        public void initTest()
        {
            UnitTestUtil.cleanupDatabase(_docGUID);
            _user = TestContext.get().getUser();
            _container = ContainerManager.ensureContainer(JunitUtil.getTestContainer(), FOLDER_NAME, TestContext.get().getUser());
            _container2 = ContainerManager.ensureContainer(JunitUtil.getTestContainer(), FOLDER_NAME2, TestContext.get().getUser());
        }

        private AuditLogTree persistALogFile(String filePath, TargetedMSRun run) throws IOException, AuditLogException
        {
            File fZip = UnitTestUtil.getSampleDataFile(filePath);
            File logFile = UnitTestUtil.extractLogFromZip(fZip, _logger);
            SkylineAuditLogManager importer = new SkylineAuditLogManager(run.getContainer(), null);

            importer.importAuditLogFile(_user, logFile, _docGUID, run);
            return importer.buildLogTree(_docGUID);
        }

        private TargetedMSRun getNewRun(GUID pDocumentGUID)
        {
            return getNewRun(pDocumentGUID, _container);
        }

        private TargetedMSRun getNewRun(GUID pDocumentGUID, Container container)
        {
            TargetedMSRun run = new TargetedMSRun();
            run.setContainer(container);
            run.setDocumentGUID(pDocumentGUID);
            Table.insert(_user, TargetedMSManager.getTableInfoRuns(), run);
            _logger.info(String.format("new run is inserted with id %d", run.getId()));
            return run;
        }

        @Test
        public void buildTreeTest() throws IOException, AuditLogException
        {
            validateHashIntegrity("AuditLogFiles/MethodEdit_v1.zip");
        }

        @Test
        public void validateSequentialHash() throws IOException, AuditLogException
        {
            validateHashIntegrity("AuditLogFiles/MethodEdit_v1_sequential.zip");
        }

        private void validateHashIntegrity(String fileName) throws AuditLogException, IOException
        {
            enableHashValidation();
            File logFile = UnitTestUtil.extractLogFromZip(UnitTestUtil.getSampleDataFile(fileName), _logger);
            SkylineAuditLogManager importer = new SkylineAuditLogManager(_container, null);
            importer.importAuditLogFile(_user, logFile, _docGUID, getNewRun(_docGUID));
            AuditLogTree tree = importer.buildLogTree(_docGUID);
            assertNotNull(tree);
            assertEquals(4, tree.getTreeSize());
        }

        @Test
        public void rejectTamperedHashSequential() throws IOException
        {
            try
            {
                validateHashIntegrity("AuditLogFiles/MethodEdit_v1_sequential_hacked.zip");
                fail("Import did not fail on tampered hash values");
            }
            catch (AuditLogException ignored) {}
        }

        @Test
        public void rejectTamperedHash() throws IOException
        {
            try
            {
                validateHashIntegrity("AuditLogFiles/MethodEdit_v1_hacked.zip");
                fail("Import did not fail on tampered hash values");
            }
            catch (AuditLogException ignored) {}
        }

        private void enableHashValidation()
        {
            ModuleProperty logLevelProperty = ModuleLoader.getInstance().getModule(TargetedMSModule.class).SKYLINE_AUDIT_LEVEL_PROPERTY;
            logLevelProperty.saveValue(null, _container, Integer.toString(SkylineAuditLogSecurityManager.INTEGRITY_LEVEL.HASH.getValue()));
        }

        @Test
        public void testRepeatedOverlappingImports() throws AuditLogException, IOException
        {
            // Coverage for issue 46081: uix_auditlogentry_document constraint violation importing overlapping Skyline audit logs

            // For reasons that I (jeckels) do not fully understand, the failure with this test isn't fully
            // deterministic, but in my testing 20 attempts is always more than sufficient to repro the failure.
            for (int i = 0; i < 20; i++)
            {
                TargetedMSRun runA = getNewRun(_docGUID);
                persistALogFile("AuditLogFiles/MethodEdit_v1.zip", runA);
                TargetedMSRun runB = getNewRun(_docGUID);
                persistALogFile("AuditLogFiles/MethodEdit_v5.1.zip", runB);
                TargetedMSRun runC = getNewRun(_docGUID);
                persistALogFile("AuditLogFiles/MethodEdit_v2.zip", runC);
            }
        }

        @Test
        public void auditTreeDeleteTest() throws IOException, AuditLogException
        {
            AuditLogTree tree;

            TargetedMSRun run1 = getNewRun(_docGUID, _container);
            TargetedMSRun run2 = getNewRun(_docGUID, _container);
            TargetedMSRun run3 = getNewRun(_docGUID, _container);
            TargetedMSRun run4 = getNewRun(_docGUID, _container2);

            persistALogFile("AuditLogFiles/MethodEdit_v1.zip", run1);
            persistALogFile("AuditLogFiles/MethodEdit_v2.zip", run2);
            persistALogFile("AuditLogFiles/MethodEdit_v3.zip", run3);
            tree = persistALogFile("AuditLogFiles/MethodEdit_v1.zip", run4);

            assertNotNull(tree);
            assertEquals(8, tree.getTreeSize());

            SkylineAuditLogManager importer1 = new SkylineAuditLogManager(_container, null);
            SkylineAuditLogManager importer2 = new SkylineAuditLogManager(_container2, null);

            // Deletes one run-auditlog node
            importer1.deleteDocumentVersionLog(run1.getId());
            tree = importer1.buildLogTree(_docGUID);
            assertEquals(7, tree.getTreeSize());

            // Ensure fk constraint is not hit. The audit log will not be deleted because the same audit log entries
            // are used in MethodEdit_v2, so the number of nodes is the same
            importer2.deleteDocumentVersionLog(run4.getId());
            tree = importer2.buildLogTree(_docGUID);
            assertEquals(7, tree.getTreeSize());

            // Removes one audit log from tree that is not used in run2
            importer1.deleteDocumentVersionLog(run3.getId());
            tree = importer1.buildLogTree(_docGUID);
            assertEquals(6, tree.getTreeSize());

        }

        @Test
        public void addAVersionTest() throws IOException, AuditLogException
        {
            Stack<TargetedMSRun> runs = new Stack<>();
            List<String> testFileNames = new ArrayList<>(
                    List.of("MethodEdit_v2.zip", "MethodEdit_v3.zip", "MethodEdit_v3.zip", "MethodEdit_v4.zip",
                            "MethodEdit_v5.1.zip", "MethodEdit_v5.2.zip", "MethodEdit_v6.2.zip"));

            AuditLogTree tree = null;

            for(String fileName : testFileNames)
            {
                _logger.info("AuditLogFiles/" + fileName);
                runs.push(getNewRun(_docGUID));
                tree = persistALogFile("AuditLogFiles/" + fileName, runs.peek());
            }

            assertNotNull(tree);
            assertEquals(15, tree.getTreeSize());

            SkylineAuditLogManager importer = new SkylineAuditLogManager(_container, null);
            importer.deleteDocumentVersionLog(runs.pop().getId());
            tree = importer.buildLogTree(_docGUID);
            assertEquals(14, tree.getTreeSize());

            importer.deleteDocumentVersionLog(runs.pop().getId());
            tree = importer.buildLogTree(_docGUID);
            assertEquals(13, tree.getTreeSize());

            runs.pop();
            importer.deleteDocumentVersionLog(runs.pop().getId());
            tree = importer.buildLogTree(_docGUID);
            assertEquals(13, tree.getTreeSize());
        }

        @Test
        public void testEntryRetrieval() throws AuditLogException
        {
            AuditLogTree node = new SkylineAuditLogManager(_container, null).buildLogTree(_docGUID);
            while(node.iterator().hasNext()){
                AuditLogEntry ent = AuditLogEntry.retrieve(node.getEntryId());
                assertNotNull(ent);
                node = node.iterator().next();
            }
        }

        @After
        public void cleanup()
        {
            UnitTestUtil.cleanupDatabase(_docGUID);
        }
    }
}
