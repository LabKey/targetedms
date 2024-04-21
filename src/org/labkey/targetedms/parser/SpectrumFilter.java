package org.labkey.targetedms.parser;

import com.google.protobuf.InvalidProtocolBufferException;
import org.apache.commons.text.StringEscapeUtils;
import org.junit.Assert;
import org.junit.Test;
import org.labkey.targetedms.parser.proto.ChromatogramGroupDataOuterClass;
import org.labkey.targetedms.parser.proto.ChromatogramGroupDataOuterClass.ChromatogramGroupIdsProto.FilterOperation;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class SpectrumFilter
{
    private static final Map<String, FilterOperation> _filterOperationMap = Map.ofEntries(Map.entry("equals", FilterOperation.FILTER_OP_EQUALS), Map.entry("<>", FilterOperation.FILTER_OP_NOT_EQUALS), Map.entry("isnullorblank", FilterOperation.FILTER_OP_IS_BLANK), Map.entry("isnotnullorblank", FilterOperation.FILTER_OP_IS_NOT_BLANK), Map.entry(">", FilterOperation.FILTER_OP_IS_GREATER_THAN), Map.entry("<", FilterOperation.FILTER_OP_IS_LESS_THAN), Map.entry(">=", FilterOperation.FILTER_OP_IS_GREATER_THAN_OR_EQUAL_TO), Map.entry("<=", FilterOperation.FILTER_OP_IS_LESS_THAN_OR_EQUAL_TO), Map.entry("contains", FilterOperation.FILTER_OP_CONTAINS), Map.entry("notcontains", FilterOperation.FILTER_OP_NOT_CONTAINS), Map.entry("startswith", FilterOperation.FITLER_OP_STARTS_WITH), Map.entry("notstartswith", FilterOperation.FILTER_OP_NOT_STARTS_WITH));
    private static final Map<FilterOperation, String> _filterOperationReverseMap = _filterOperationMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
    private final List<FilterClause> _filterClauses;

    public SpectrumFilter(List<FilterClause> filterClauses)
    {
        _filterClauses = filterClauses;
    }

    public static SpectrumFilter parse(XMLStreamReader reader) throws XMLStreamException
    {
        String elementName = reader.getLocalName();
        List<FilterClause> clauses = new ArrayList<>();
        while (reader.hasNext())
        {
            int evtType = reader.next();

            if (evtType == XMLStreamReader.END_ELEMENT && elementName.equals(reader.getLocalName())) break;

            if (XmlUtil.isStartElement(reader, evtType, "filter"))
            {
                clauses.add(new FilterClause(XmlUtil.readAttribute(reader, "column"), XmlUtil.readAttribute(reader, "opname"), XmlUtil.readAttribute(reader, "operand")));
            }
        }
        return new SpectrumFilter(clauses);
    }

    public static SpectrumFilter fromProtocolMessage(ChromatogramGroupDataOuterClass.ChromatogramGroupIdsProto.SpectrumFilter protocolMessage)
    {
        List<FilterClause> filterClauses = new ArrayList<>();
        for (ChromatogramGroupDataOuterClass.ChromatogramGroupIdsProto.SpectrumFilter.Predicate predicate : protocolMessage.getPredicatesList())
        {
            filterClauses.add(new FilterClause(predicate.getPropertyPath(), _filterOperationReverseMap.get(predicate.getOperation()), predicate.getOperand()));
        }
        return new SpectrumFilter(filterClauses);
    }

    public static SpectrumFilter fromByteArray(byte[] byteArray) throws InvalidProtocolBufferException
    {
        if (byteArray == null || byteArray.length == 0)
        {
            return null;
        }
        return fromProtocolMessage(ChromatogramGroupDataOuterClass.ChromatogramGroupIdsProto.SpectrumFilter.parseFrom(byteArray));
    }

    public List<FilterClause> getFilterClauses()
    {
        return _filterClauses;
    }

    public String toXml()
    {
        if (_filterClauses == null || _filterClauses.size() == 0)
        {
            return "";
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<spectrum_filter>");
        for (FilterClause filterClause : _filterClauses)
        {
            stringBuilder.append("<filter column=\"");
            stringBuilder.append(StringEscapeUtils.escapeXml11(filterClause.getColumn()));
            stringBuilder.append("\" opname=\"");
            stringBuilder.append(StringEscapeUtils.escapeXml11(filterClause.getOperation()));
            if (null != filterClause.getOperand())
            {
                stringBuilder.append("\" operand=\"");
                stringBuilder.append(StringEscapeUtils.escapeXml11(filterClause.getOperand()));
            }
            stringBuilder.append("\"/>");
        }
        stringBuilder.append("</spectrum_filter>");
        return stringBuilder.toString();
    }

    public ChromatogramGroupDataOuterClass.ChromatogramGroupIdsProto.SpectrumFilter toProtocolMessage()
    {
        var builder = ChromatogramGroupDataOuterClass.ChromatogramGroupIdsProto.SpectrumFilter.newBuilder();
        for (FilterClause filterClause : _filterClauses)
        {

            builder.addPredicates(filterClause.toProtocolMessage());
        }
        return builder.build();
    }

    public byte[] toByteArray()
    {
        return toProtocolMessage().toByteArray();
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SpectrumFilter that = (SpectrumFilter) o;
        return Objects.equals(_filterClauses, that._filterClauses);
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(_filterClauses);
    }

    public static class FilterClause
    {
        private final String _column;
        private final String _operation;
        private final String _operand;

        public FilterClause(String column, String operation, String operand)
        {
            _column = column;
            _operation = operation;
            _operand = "".equals(operand) ? null : operand;
        }

        public String getColumn()
        {
            return _column;
        }

        public String getOperation()
        {
            return _operation;
        }

        public String getOperand()
        {
            return _operand;
        }

        public ChromatogramGroupDataOuterClass.ChromatogramGroupIdsProto.SpectrumFilter.Predicate toProtocolMessage()
        {
            return ChromatogramGroupDataOuterClass.ChromatogramGroupIdsProto.SpectrumFilter.Predicate.newBuilder().setPropertyPath(getColumn()).setOperation(_filterOperationMap.get(getOperation())).setOperand(_operand).build();
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FilterClause that = (FilterClause) o;
            return Objects.equals(_column, that._column) && Objects.equals(_operation, that._operation) && Objects.equals(_operand, that._operand);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(_column, _operation, _operand);
        }
    }

    public static class TestCase
    {
        @Test
        public void TestParse() throws Exception
        {
            String xml = String.join("\n", "      <precursor charge=\"2\" modified_sequence=\"ESDTSYVSLK\">", "        <spectrum_filter>", "          <filter column=\"MsLevel\" opname=\"isNotEmpty\" />", "          <filter column=\"MsLevel\" opname=\">\" operand=\"1\" />", "          <filter column=\"MsLevel\" opname=\"&lt;\" operand=\"3\" />", "        </spectrum_filter>", "        <bibliospec_spectrum_info count_measured=\"1\" />", "      </precursor");
            XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(new StringReader(xml));
            SpectrumFilter spectrumFilter = null;
            int bibliospecSpectrumInfoCount = 0;
            while (reader.hasNext())
            {
                int event = reader.next();
                if (XmlUtil.isEndElement(reader, event, "precursor"))
                {
                    break;
                }
                if (XmlUtil.isStartElement(reader, event, "spectrum_filter"))
                {
                    spectrumFilter = SpectrumFilter.parse(reader);
                }
                else if (XmlUtil.isStartElement(reader, event, "bibliospec_spectrum_info"))
                {
                    bibliospecSpectrumInfoCount++;
                }
            }
            Assert.assertEquals(1, bibliospecSpectrumInfoCount);
            Assert.assertNotNull(spectrumFilter);
            Assert.assertEquals(3, spectrumFilter.getFilterClauses().size());
            XMLStreamReader readerRoundTrip = XMLInputFactory.newInstance().createXMLStreamReader(new StringReader(spectrumFilter.toXml()));
            SpectrumFilter spectrumFilterRoundTrip = null;
            while (readerRoundTrip.hasName())
            {
                if (XmlUtil.isStartElement(readerRoundTrip, readerRoundTrip.next(), "spectrum_filter"))
                {
                    break;
                }
                spectrumFilterRoundTrip = SpectrumFilter.parse(readerRoundTrip);
            }
            Assert.assertNotNull(spectrumFilterRoundTrip);
            Assert.assertEquals(spectrumFilter.getFilterClauses().size(), spectrumFilterRoundTrip.getFilterClauses().size());
            Assert.assertEquals(spectrumFilter.toXml(), spectrumFilterRoundTrip.toXml());
            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(spectrumFilter.toXml());
            Assert.assertEquals("spectrum_filter", document.getDocumentElement().getLocalName());
            Assert.assertEquals(spectrumFilter.getFilterClauses().size(), document.getDocumentElement().getChildNodes().getLength());
        }
    }
}
