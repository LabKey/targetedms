/*
 * Copyright (c) 2024 LabKey Corporation
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
package org.labkey.targetedms.parser;

import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.Assert;
import org.junit.Test;
import org.labkey.targetedms.parser.proto.ChromatogramGroupDataOuterClass;
import org.labkey.targetedms.parser.proto.ChromatogramGroupDataOuterClass.ChromatogramGroupIdsProto.FilterOperation;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Specifies that a precursor's chromatogram should be extracted from a subset of the matching spectra.
 * A SpectrumFilter contains a list of {@link FilterClause} objects which are combined with "OR" semantics.
 * The individual {@link FilterPredicate} items with the FilterClause are combined with "AND".
 */
public class SpectrumFilter
{
    private static final Map<String, FilterOperation> _filterOperationMap = Map.ofEntries(
            Map.entry("equals", FilterOperation.FILTER_OP_EQUALS),
            Map.entry("<>", FilterOperation.FILTER_OP_NOT_EQUALS),
            Map.entry("isnullorblank", FilterOperation.FILTER_OP_IS_BLANK),
            Map.entry("isnotnullorblank", FilterOperation.FILTER_OP_IS_NOT_BLANK),
            Map.entry(">", FilterOperation.FILTER_OP_IS_GREATER_THAN),
            Map.entry("<", FilterOperation.FILTER_OP_IS_LESS_THAN),
            Map.entry(">=", FilterOperation.FILTER_OP_IS_GREATER_THAN_OR_EQUAL_TO),
            Map.entry("<=", FilterOperation.FILTER_OP_IS_LESS_THAN_OR_EQUAL_TO),
            Map.entry("contains", FilterOperation.FILTER_OP_CONTAINS),
            Map.entry("notcontains", FilterOperation.FILTER_OP_NOT_CONTAINS),
            Map.entry("startswith", FilterOperation.FITLER_OP_STARTS_WITH),
            Map.entry("notstartswith", FilterOperation.FILTER_OP_NOT_STARTS_WITH));
    private static final Map<FilterOperation, String> _filterOperationReverseMap = _filterOperationMap.entrySet()
            .stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
    private final List<FilterClause> _filterClauses;

    public static Optional<SpectrumFilter> fromFilterClauses(List<FilterClause> filterClauses)
    {
        if (filterClauses == null || filterClauses.isEmpty())
        {
            return Optional.empty();
        }
        return Optional.of(new SpectrumFilter(filterClauses));
    }

    public SpectrumFilter(List<FilterClause> filterClauses)
    {
        _filterClauses = filterClauses;
    }

    public static SpectrumFilter fromProtocolMessage(ChromatogramGroupDataOuterClass.ChromatogramGroupIdsProto protocolMessage)
    {
        List<FilterClause> filterClauses = new ArrayList<>();
        for (ChromatogramGroupDataOuterClass.ChromatogramGroupIdsProto.SpectrumFilter clause : protocolMessage.getFiltersList())
        {
            filterClauses.add(FilterClause.fromProtocolMessage(clause));
        }
        return new SpectrumFilter(filterClauses);
    }

    /**
     * Constructs a SpectrumFilter from the bytes that are stored in the {@link GeneralPrecursor#getSpectrumFilter()}.
     */
    public static SpectrumFilter fromByteArray(byte[] byteArray) throws InvalidProtocolBufferException
    {
        if (byteArray == null || byteArray.length == 0)
        {
            return null;
        }
        return fromProtocolMessage(ChromatogramGroupDataOuterClass.ChromatogramGroupIdsProto.parseFrom(byteArray));
    }

    public List<FilterClause> getFilterClauses()
    {
        return _filterClauses;
    }

    public ChromatogramGroupDataOuterClass.ChromatogramGroupIdsProto toProtocolMessage()
    {
        return ChromatogramGroupDataOuterClass.ChromatogramGroupIdsProto.newBuilder().addAllFilters(_filterClauses.stream().map(FilterClause::toProtocolMessage).collect(Collectors.toList())).build();
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
        private final List<FilterPredicate> _predicates;

        public FilterClause(List<FilterPredicate> predicates)
        {
            _predicates = predicates;
        }

        public static FilterClause fromProtocolMessage(ChromatogramGroupDataOuterClass.ChromatogramGroupIdsProto.SpectrumFilter protocolMessage)
        {
            List<FilterPredicate> predicates = new ArrayList<>();
            for (ChromatogramGroupDataOuterClass.ChromatogramGroupIdsProto.SpectrumFilter.Predicate predicate : protocolMessage.getPredicatesList())
            {
                predicates.add(new FilterPredicate(predicate.getPropertyPath(), _filterOperationReverseMap.get(predicate.getOperation()), predicate.getOperand()));
            }
            return new FilterClause(predicates);
        }

        public static FilterClause parse(XMLStreamReader reader) throws XMLStreamException
        {
            String elementName = reader.getLocalName();
            List<FilterPredicate> predicates = new ArrayList<>();
            while (reader.hasNext())
            {
                int evtType = reader.next();

                if (evtType == XMLStreamReader.END_ELEMENT && elementName.equals(reader.getLocalName())) break;

                if (XmlUtil.isStartElement(reader, evtType, "filter"))
                {
                    predicates.add(new FilterPredicate(XmlUtil.readAttribute(reader, "column"),
                            XmlUtil.readAttribute(reader, "opname"),
                            XmlUtil.readAttribute(reader, "operand")));
                }
            }
            return new FilterClause(predicates);
        }

        public List<FilterPredicate> getPredicates()
        {
            return _predicates;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FilterClause that = (FilterClause) o;
            return Objects.equals(_predicates, that._predicates);
        }

        @Override
        public int hashCode()
        {
            return Objects.hashCode(_predicates);
        }

        public ChromatogramGroupDataOuterClass.ChromatogramGroupIdsProto.SpectrumFilter toProtocolMessage()
        {

            return ChromatogramGroupDataOuterClass.ChromatogramGroupIdsProto.SpectrumFilter.newBuilder()
                    .addAllPredicates(_predicates.stream().map(FilterPredicate::toProtocolMessage)
                            .collect(Collectors.toList())).build();
        }
    }

    public static class FilterPredicate
    {
        private final String _column;
        private final String _operation;
        private final String _operand;

        public FilterPredicate(String column, String operation, String operand)
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
            return ChromatogramGroupDataOuterClass.ChromatogramGroupIdsProto.SpectrumFilter.Predicate.newBuilder()
                    .setPropertyPath(getColumn()).setOperation(_filterOperationMap.get(getOperation()))
                    .setOperand(_operand).build();
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FilterPredicate that = (FilterPredicate) o;
            return Objects.equals(_column, that._column) &&
                    Objects.equals(_operation, that._operation) &&
                    Objects.equals(_operand, that._operand);
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
        public void testParse() throws Exception
        {
            String xml = String.join("\n",
                    "      <precursor charge=\"2\" modified_sequence=\"ESDTSYVSLK\">",
                    "        <spectrum_filter>",
                    "          <filter column=\"MsLevel\" opname=\"isNotEmpty\" />",
                    "          <filter column=\"MsLevel\" opname=\">\" operand=\"3\" />",
                    "        </spectrum_filter>",
                    "        <spectrum_filter>",
                    "          <filter column=\"MsLevel\" opname=\"isNotEmpty\" />",
                    "          <filter column=\"MsLevel\" opname=\"&lt;\" operand=\"2\" />",
                    "        </spectrum_filter>",
                    "        <bibliospec_spectrum_info count_measured=\"1\" />",
                    "      </precursor");
            XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(new StringReader(xml));
            int bibliospecSpectrumInfoCount = 0;
            List<FilterClause> clauses = new ArrayList<>();
            while (reader.hasNext())
            {
                int event = reader.next();
                if (XmlUtil.isEndElement(reader, event, "precursor"))
                {
                    break;
                }
                if (XmlUtil.isStartElement(reader, event, "spectrum_filter"))
                {
                    clauses.add(FilterClause.parse(reader));
                }
                else if (XmlUtil.isStartElement(reader, event, "bibliospec_spectrum_info"))
                {
                    bibliospecSpectrumInfoCount++;
                }
            }
            Assert.assertEquals(2, clauses.size());
            SpectrumFilter spectrumFilter = new SpectrumFilter(clauses);
            Assert.assertEquals(1, bibliospecSpectrumInfoCount);
            Assert.assertNotNull(spectrumFilter);
            Assert.assertEquals(3, spectrumFilter.getFilterClauses().size());
            byte[] bytes = spectrumFilter.toByteArray();
            SpectrumFilter spectrumFilterRoundTrip = SpectrumFilter.fromByteArray(bytes);
            Assert.assertNotNull(spectrumFilterRoundTrip);
            Assert.assertEquals(spectrumFilter.getFilterClauses().size(), spectrumFilterRoundTrip.getFilterClauses().size());
            Assert.assertEquals(Arrays.asList(spectrumFilter.toByteArray()), Arrays.asList(spectrumFilterRoundTrip.toByteArray()));
        }
    }
}
