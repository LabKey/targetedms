package org.labkey.targetedms.parser;

import org.apache.logging.log4j.Logger;
import org.labkey.api.util.UnexpectedException;
import org.labkey.targetedms.parser.proto.SkylineDocument;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Base64;

public class TransitionChromInfoAndPrecursorTally
{
    private final int _maxTransitionChromInfos; // max allowed count for TransitionChromInfos
    private final int _maxPrecursors; // max allowed count for precursors

    // Tally of the precursors and TransitionChromInfos in the document.
    private int _precursorCount;
    private int _transitionChromInfoCount;


    public TransitionChromInfoAndPrecursorTally(int maxTransitionChromInfos, int maxPrecursors)
    {
        _maxTransitionChromInfos = maxTransitionChromInfos;
        _maxPrecursors = maxPrecursors;
    }

    // Reads the given file and returns true if the Precursor and TransitionChromInfo counts in the file are within
    // the limits.
    public boolean isWithinLimits(File file, Logger log) throws XMLStreamException, IOException
    {
        _precursorCount = 0;
        _transitionChromInfoCount = 0;

        XMLStreamReader reader;
        try (FileInputStream inputStream = new FileInputStream(file))
        {
            reader = XMLInputFactory.newInstance().createXMLStreamReader(inputStream);
            try
            {
                readCounts(reader);
            }
            finally
            {
                try
                {
                    reader.close();
                }
                catch (XMLStreamException e)
                {
                    log.warn("An exception was thrown while trying to close the XML reader", e);
                }
            }
        }

        return isWithinLimits();
    }

    // Return false if we've equalled or exceeded the maximum allowed counts for TransitionChromInfos AND precursors.
    private boolean isWithinLimits()
    {
        return _transitionChromInfoCount < _maxTransitionChromInfos || _precursorCount < _maxPrecursors;
    }

    private void readCounts(XMLStreamReader reader) throws XMLStreamException
    {
        // Tally the precursor and TransitionChromInfo counts in the document. Read until the end of the document
        // or until we exceed the thresholds.
        while(reader.hasNext()) {

            int evtType = reader.next();

            if (XmlUtil.isStartElement(reader, evtType, SkylineDocumentParser.PRECURSOR))
            {
                _precursorCount++;
                if (!isWithinLimits()) break;
            }
            else if (XmlUtil.isStartElement(reader, evtType, SkylineDocumentParser.TRANSITION_PEAK))
            {
                _transitionChromInfoCount++;
                if (!isWithinLimits()) break;
            }
            else if (XmlUtil.isStartElement(reader, evtType, SkylineDocumentParser.TRANSITION_DATA))
            {
                readTransitionData(reader);
                if (!isWithinLimits()) break;
            }
            else if (XmlUtil.isStartElement(reader, evtType, SkylineDocumentParser.RESULTS_DATA))
            {
                readTransitionResultsData(reader);
                if (!isWithinLimits()) break;
            }
        }
    }

    private void readTransitionData(XMLStreamReader reader)
    {
        try
        {
            String elementText = reader.getElementText();
            byte[] bytes = Base64.getDecoder().decode(elementText);
            SkylineDocument.SkylineDocumentProto.TransitionData transitionData = SkylineDocument.SkylineDocumentProto.TransitionData.parseFrom(bytes);
            transitionData.getTransitionsList().forEach(transition -> _transitionChromInfoCount += transition.getResults().getPeaksCount());
        }
        catch (Exception e)
        {
            throw UnexpectedException.wrap(e);
        }
    }

    private void readTransitionResultsData(XMLStreamReader reader)
    {
        try
        {
            String strContent = reader.getElementText();
            byte[] bytes = Base64.getDecoder().decode(strContent);
            SkylineDocument.SkylineDocumentProto.TransitionResults transitionResults = SkylineDocument.SkylineDocumentProto.TransitionResults.parseFrom(bytes);
            _transitionChromInfoCount += transitionResults.getPeaksCount();
        }
        catch (Exception e)
        {
            throw UnexpectedException.wrap(e);
        }
    }
}
