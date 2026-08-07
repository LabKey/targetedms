/*
 * Copyright (c) 2019-2026 LabKey Corporation
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
package org.labkey.targetedms.parser.skyaudit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.labkey.api.module.Module;
import org.labkey.api.module.ModuleLoader;
import org.labkey.api.resource.FileResource;
import org.labkey.api.util.ExternalReferenceProbe;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.GUID;
import org.labkey.api.util.Path;
import org.labkey.api.util.XmlBeansUtil;
import org.labkey.targetedms.TargetedMSModule;
import org.labkey.targetedms.parser.XmlUtil;
import org.xml.sax.SAXException;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;


/***
 * Reads the audit log file, validates it and converts into a sequence
 * of AuditLogEntry instances that can be persisted into the database
 */
public class SkylineAuditLogParser implements AutoCloseable
{
    //------ log root
    public static final String AUDIT_LOG_ROOT = "audit_log_root";
    public static final String FORMAT_VERSION = "format_version";
    public static final String DOCUMENT_HASH = "document_hash";
    public static final String EN_ROOT_HASH = "root_hash";
    public static final String AUDIT_LOG = "audit_log";
    public static final String AUDIT_LOG_ENTRY = "audit_log_entry";
    //--------- log entry
    private static final String SKYLINE_VERSION = "skyline_version";
    private static final String TIME_STAMP = "time_stamp";
    private static final String USER = "user";
    private static final String REASON = "reason";
    private static final String EXTRA_INFO = "extra_info";
    private static final String EXTRA_INFO_ENGLISH = "en_extra_info";
    private static final String UNDO_REDO_MSG = "undo_redo";
    private static final String SUMMARY_MSG = "summary";
    private static final String MESSAGE = "all_info";
    private static final String EN_HASH = "hash";
    //--------- log message
    private static final String MESSAGE_TYPE = "type";
    private static final String MESSAGE_NAME = "name";
    private static final String MESSAGE_TEXT = "en_expanded";
    private static final String MESSAGE_REASON = "reason";


    private static final String SCHEMA_FILE = "schemas/Skyl.xsd";

    private final File _file;
    private final Logger _logger;
    private XMLStreamReader _stream;
    private FileInputStream _fileStream;

    private String _documentHash;
    private String _enRootHash;
    private BigDecimal _formatVersion;

    public SkylineAuditLogParser(File logFile, Logger logger)  throws AuditLogException{

        _file = logFile;
        _logger = logger;

        try
        {
            validateXml();
            parseLogHeader();
        }
        catch(Exception e){
            throw new AuditLogException("Error when parsing audit log file.", e);
        }
    }


    private void validateXml() throws IOException, SAXException, AuditLogParsingException
    {
        try (InputStream schemaStream = new BufferedInputStream(openSchemaInputStream());
             InputStream auditLogStream = new BufferedInputStream(new FileInputStream(_file)))
            {
                // Use a factory Hardened against XXE
                SchemaFactory schemaFactory = XmlBeansUtil.schemaFactory();
                Schema schema = schemaFactory.newSchema(new StreamSource(schemaStream));
                Validator validator = XmlBeansUtil.hardenValidator(schema.newValidator());
                validator.validate(new StreamSource(auditLogStream));
            }
    }

    @NotNull
    private InputStream openSchemaInputStream() throws AuditLogParsingException, FileNotFoundException
    {
        if (ModuleLoader.getInstance() != null)
        {   //if we are running web test
            Module module = ModuleLoader.getInstance().getModule(TargetedMSModule.class);
            FileResource schemaResource = (FileResource) module.getModuleResolver().lookup(Path.parse(SCHEMA_FILE));
            if (schemaResource == null)
            {
                throw new AuditLogParsingException("Schema file not found in the module resources.");
            }
            return new FileInputStream(schemaResource.getFile());
        }

        //this is for unit testing
        return new FileInputStream(UnitTestUtil.getResourcesFile(SCHEMA_FILE));
    }

    /***
     * This method parses the beginning of the log: the hashes and audit_log tag and stops at
     * the first log entry, ready to proceed with read/save loop
     */
    private void parseLogHeader() throws IOException, XMLStreamException
    {
        _fileStream = new FileInputStream(_file);
        _stream = XmlBeansUtil.XML_INPUT_FACTORY.createXMLStreamReader(_fileStream);

        //Skipping most XML structure validation since the file passed the schema validation
        int evtType = _stream.nextTag();     //log root element read

        if (evtType != XMLStreamReader.START_ELEMENT || !_stream.getLocalName().equals(AUDIT_LOG_ROOT))
        {
            throw new IllegalStateException("Root element was not " + AUDIT_LOG_ROOT);
        }

        String formatVersion = _stream.getAttributeValue(null, FORMAT_VERSION);
        if (formatVersion == null)
        {
            throw new IllegalStateException("Could not find " + FORMAT_VERSION + " attribute on " + AUDIT_LOG_ROOT);
        }
        _formatVersion = new BigDecimal(formatVersion);

        while(_stream.hasNext()){
            evtType = _stream.nextTag();
            if(evtType == XMLStreamReader.START_ELEMENT){
                switch(_stream.getLocalName()){
                    case EN_ROOT_HASH:
                        this._enRootHash = _stream.getElementText();
                        break;
                    case DOCUMENT_HASH:
                        this._documentHash = _stream.getElementText();
                        break;
                    case AUDIT_LOG:
                        _stream.nextTag();
                        return;
                }
            }
        }
    }

    public AuditLogEntry parseLogEntry() throws XMLStreamException, AuditLogParsingException{

        //XmlUtil.skip(_stream, XMLStreamReader.START_ELEMENT, AuditLog.AUDIT_LOG_ENTRY );
        //the _stream must be at the correct position to parse an entry
        assert (_stream.getLocalName().equals(AUDIT_LOG_ENTRY) && _stream.getEventType() == XMLStreamReader.START_ELEMENT) :
                    "Parser is at a wrong position to parse a log entry";

        AuditLogEntry result = new AuditLogEntry(_formatVersion);

        result.setFormatVersion(_stream.getAttributeValue(null, SKYLINE_VERSION));

        String timeStamp = _stream.getAttributeValue(null, TIME_STAMP);
        try
        {
            result.parseCreateTimestamp(timeStamp);
        }
        catch(DateTimeParseException e){
            throw new AuditLogParsingException(String.format("Invalid date/time format in audit log file: %s", timeStamp), e);
        }
        result.setUserName(_stream.getAttributeValue(null, USER));

        int messageCount = 0;
        while(_stream.hasNext()){
            switch(_stream.nextTag()){
                case XMLStreamReader.START_ELEMENT:
                    switch(_stream.getLocalName()){
                        case REASON:
                            result.setReason(_stream.getElementText());
                            break;
                        case EXTRA_INFO_ENGLISH:
                            result.setExtraInfo(_stream.getElementText().replace("\n", "\r\n"));
                        case EXTRA_INFO:
                            if(result.getExtraInfo() == null)
                                result.setExtraInfo(_stream.getElementText().replace("\n", "\r\n"));
                            break;
                        case UNDO_REDO_MSG:
                        case SUMMARY_MSG:
                        case MESSAGE:
                            AuditLogMessage msg = this.parseAuditLogMessage();
                            msg.setOrderNumber(messageCount);
                            result._allInfoMessage.add(msg);
                            messageCount++;
                            break;
                        case EN_HASH:
                            result.setEntryHash(_stream.getElementText());
                            break;
                        default:
                            throw new AuditLogParsingException("Unexpected tag encountered:" + _stream.getLocalName());
                    }
                    //XmlUtil.skip(_stream, _stream.END_ELEMENT);
                    break;
                case XMLStreamReader.END_ELEMENT:
                    _stream.nextTag();
                    return result;
            }
        }
        throw new AuditLogParsingException("Element end expected.");
    }

    public boolean hasNextEntry()
    {
        return !XmlUtil.isEndElement(_stream, XMLStreamReader.END_ELEMENT, AUDIT_LOG);
    }

    private AuditLogMessage parseAuditLogMessage() throws XMLStreamException, AuditLogParsingException{
        List<String> names = new LinkedList<>();
        AuditLogMessage result = new AuditLogMessage();
        while(_stream.hasNext()){
            switch(_stream.nextTag()){
                case XMLStreamReader.START_ELEMENT:
                    switch(_stream.getLocalName()){
                        case MESSAGE_TYPE:
                            result._messageType = _stream.getElementText();
                            break;
                        case MESSAGE_TEXT:
                            result._enText = _stream.getElementText();
                            break;
                        case MESSAGE_NAME:
                            names.add(_stream.getElementText());
                            break;
                        case MESSAGE_REASON:
                            result._reason = _stream.getElementText();
                            break;
                    }
                    //XmlUtil.skip(reader, XMLStreamReader.END_ELEMENT);
                    break;
                case XMLStreamReader.END_ELEMENT:
                    result._names = Collections.unmodifiableList(names);
                    return result;
            }
        }
        throw new AuditLogParsingException("Element end expected.");

    }

    @Override
    //cleanup method to use in exception handlers
    public void close(){
        try {
            _stream.close();
            _fileStream.close();
        }
        catch(IOException | XMLStreamException e){
            _logger.warn("Exception when trying to close audit log XML stream.", e);
        }
    }

    public String getDocumentHash()
    {
        return _documentHash;
    }

    public String getEnRootHash()
    {
        return _enRootHash;
    }


    //--------------------------------------------
    public static class TestCase extends Assert{

        private static final Logger _logger = LogManager.getLogger(TestCase.class);
        public final static String SYS_PROPERTY_CWD = "user.dir";
        public final static String SKYLINE_LOG_EXTENSION = "skyl";

        private static final GUID _docGUID = new GUID("add8ea9c-0b32-1037-a00c-1e459cb1acac");

        @Before
        public void init()
        {
            UnitTestUtil.cleanupDatabase(_docGUID);
        }

        @Test
        public void testLogParser()  throws XMLStreamException, AuditLogException, AuditLogParsingException, IOException
        {
            List<AuditLogEntry> entries = new LinkedList<>();

            File fZip = UnitTestUtil.getSampleDataFile("AuditLogFiles/MethodEdit_v6.2.zip");
            File logFile = UnitTestUtil.extractLogFromZip(fZip, _logger);
            try (SkylineAuditLogParser parser = new SkylineAuditLogParser(logFile, _logger))
            {
                Assert.assertNotNull(parser.getEnRootHash());

                AuditLogEntry prevEntry = null;

                while (parser.hasNextEntry())
                {
                    AuditLogEntry ent = parser.parseLogEntry();
                    ent.setDocumentGUID(_docGUID);
                    if (prevEntry != null)
                        ent.setParentEntryHash(prevEntry.getEntryHash());
                    entries.add(ent);
                    _logger.debug(ent.toString());
                    //all messages in this file should have expanded text
                    //ent.persist();

                    for (AuditLogMessage msg : ent.getAllInfoMessage())
                    {
                        Assert.assertNotNull(msg.getEnText());
                    }
                    prevEntry = ent;
                }

                Assert.assertNotNull(entries.get(2).getExtraInfo());
                Assert.assertNull(entries.get(1).getExtraInfo());

                Assert.assertEquals(11, entries.size());
                Assert.assertEquals(6, entries.get(5).getAllInfoMessage().size());
                Assert.assertTrue(entries.get(0).canBeHashed());
            }
        }

        @Test
        public void testInvalidXmlFile() throws IOException
        {
            SkylineAuditLogParser parser = null;
            try
            {
                File logFile = UnitTestUtil.getSampleDataFile("AuditLogFiles/InvalidSchemaTest.skyl");
                parser = new SkylineAuditLogParser(logFile, _logger);
                Assert.fail("Expected file validation failure but it succeeded.");
            }
            catch (AuditLogException _) {}
            finally
            {
                if (parser != null)
                    parser.close();
            }
        }

        //TODO: Validate against different files.
    }

    /**
     * XXE (CWE-611) coverage for {@link SkylineAuditLogParser#validateXml()}, where the hardening carries the most
     * weight: unlike the SAML path nothing guards the uploaded .skyl before it reaches the validator, so if the
     * hardening doesn't hold an uploader can make the server fetch a URL of their choosing.
     *
     * <p>Separate from {@link TestCase}, which needs a database for its {@code @Before} cleanup.
     */
    public static class XxeTestCase extends Assert
    {
        /**
         * {@link #validateXml} resolves the schema first, so an unavailable module resource means it throws before
         * parsing any XML and every probe-based assertion below goes green while proving nothing.
         */
        @Before
        public void schemaMustBeResolvable()
        {
            Module module = ModuleLoader.getInstance().getModule(TargetedMSModule.class);
            assertNotNull("TargetedMS module must be registered, otherwise validateXml() never validates", module);
            assertNotNull("Bundled " + SCHEMA_FILE + " must be resolvable, otherwise validateXml() never validates",
                module.getModuleResolver().lookup(Path.parse(SCHEMA_FILE)));
        }

        /** Conforming apart from the injected reference, so validation gets far enough to matter. */
        private static final String AUDIT_LOG =
            "<audit_log_root format_version=\"1.0\">" +
            "<document_hash>hash</document_hash>" +
            "<audit_log/>" +
            "</audit_log_root>";

        /**
         * {@code XmlBeansUtil.TestCase} does primary XXE validation. Just prove that validateXml() gets the hardened factory.
         */
        @Test
        public void validateXmlRefusesExternalDtdSubset() throws Exception
        {
            // Qualified because org.labkey.api.util.Path wins the simple name in this file
            java.nio.file.Path dir = Files.createTempDirectory("skylineAuditLogXxe");

            try (ExternalReferenceProbe probe = ExternalReferenceProbe.start())
            {
                File logFile = dir.resolve("audit.skyl").toFile();
                Files.writeString(logFile.toPath(),
                    "<!DOCTYPE audit_log_root SYSTEM \"" +
                    probe.url("/external.dtd", ExternalReferenceProbe.DTD_BODY) + "\">" + AUDIT_LOG,
                    StandardCharsets.UTF_8);

                // Expected to fail on this input; the assertion is about the fetch on the way there
                try (SkylineAuditLogParser ignored = new SkylineAuditLogParser(logFile, LogManager.getLogger(XxeTestCase.class)))
                {
                    fail("Should have failed");
                }
                catch (Exception _) {}

                probe.assertNotContacted("Validating an uploaded Skyline audit log must not resolve an external DTD subset");
            }
            finally
            {
                FileUtil.deleteDir(dir.toFile());
            }
        }
    }
}
