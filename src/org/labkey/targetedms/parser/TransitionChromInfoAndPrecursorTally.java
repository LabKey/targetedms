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

public class TransitionChromInfoAndPrecursorTally implements AutoCloseable
{
    private final XMLStreamReader _reader;
    private final FileInputStream _inputStream;
    private final Logger _log;

    private final int _maxTransitionChromInfos; // max allowed count for TransitionChromInfos
    private final int _maxPrecursors; // max allowed count for precursors

    // Tally the precursor and TransitionChromInfo counts in the document.
    private int _precursorCount;
    private int _transitionChromInfoCount;


    public TransitionChromInfoAndPrecursorTally(File file, Logger log, int maxTransitionChromInfos, int maxPrecursors) throws XMLStreamException, IOException
    {
        _log = log;
        _maxTransitionChromInfos = maxTransitionChromInfos;
        _maxPrecursors = maxPrecursors;

        _inputStream = new FileInputStream(file);
        XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        _reader = inputFactory.createXMLStreamReader(_inputStream);
        getCounts(_reader);
    }

    /** @return false if we've equalled or exceeded the maximum allowed counts for TransitionChromInfo and precursors. */
    public boolean isWithinLimits()
    {
         // To prevent giant DIA documents from overwhelming the DB, we skip importing TransitionChromInfos if the document
         // has more than 100,000 AND has more than 1,000 precursors. We use both because a document may have a lot of
         // replicates, so the TransitionChromInfo count by itself isn't sufficient to do the desired screening
        return _transitionChromInfoCount < _maxTransitionChromInfos || _precursorCount < _maxPrecursors;
    }

    private void getCounts(XMLStreamReader reader) throws XMLStreamException
    {
        // Tally the precursor and TransitionChromInfo counts in the document until the end of the document
        // or until we exceed the thresholds.
        while(reader.hasNext()) {

            int evtType = _reader.next();

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
            for (SkylineDocument.SkylineDocumentProto.Transition transitionProto : transitionData.getTransitionsList())
            {
                _transitionChromInfoCount += transitionProto.getResults().getPeaksCount();
            }
        }
        catch (Exception e)
        {
            throw UnexpectedException.wrap(e);
        }
    }

    private void readTransitionResultsData(XMLStreamReader reader)
    {
        try {
            String strContent = reader.getElementText();
            byte[] data = Base64.getDecoder().decode(strContent);
            SkylineDocument.SkylineDocumentProto.TransitionResults transitionResults
                    = SkylineDocument.SkylineDocumentProto.TransitionResults.parseFrom(data);
            _transitionChromInfoCount += transitionResults.getPeaksCount();
        }
        catch (Exception e) {
            throw UnexpectedException.wrap(e);
        }
    }

    @Override
    public void close()
    {
        if (_reader != null) try
        {
            _reader.close();
        }
        catch (XMLStreamException e)
        {
            _log.error(e);
        }
        if(_inputStream != null) try
        {
            _inputStream.close();
        }
        catch(IOException e)
        {
            _log.error(e);
        }
    }
}
