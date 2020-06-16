/*
 * Copyright (c) 2020 LabKey Corporation
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

import org.labkey.api.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ComplexFragmentIonName
{
    private final String _ionType;
    private final int _ordinal;
    private final List<Pair<ModificationSite, ComplexFragmentIonName>> _children = new ArrayList<>();
    private final List<Pair<ModificationSite, String>> _losses = new ArrayList<>();
    public ComplexFragmentIonName(String ionType, Integer ionOrdinal)
    {
        _ionType = ionType;
        _ordinal = ionOrdinal == null ? 0 : ionOrdinal;
    }

    public static class ModificationSite {
        private final int _indexAa;
        private final String _modificationName;
        public ModificationSite(int indexAa, String modificationName) {
            _indexAa = indexAa;
            _modificationName = modificationName;
        }

        @Override
        public String toString()
        {
            return (_indexAa + 1) + ":" + _modificationName;
        }
    }

    public String getIonType()
    {
        return _ionType;
    }

    public boolean isOrphan()
    {
        return _ionType == null;
    }

    public int getOrdinal()
    {
        return _ordinal;
    }

    public void addChild(Pair<ModificationSite, ComplexFragmentIonName> child)
    {
        _children.add(child);
    }

    public void addLoss(int aaIndex, String modificationName, String description)
    {
        _losses.add(Pair.of(new ModificationSite(aaIndex, modificationName), description));
    }

    @Override
    public String toString()
    {
        if (isOrphan() && _children.isEmpty())
        {
            return "-";
        }
        StringBuilder stringBuilder = new StringBuilder();
        if (!isOrphan()) {
            if ("precursor".equals(getIonType()))
            {
                stringBuilder.append("p");
            }
            else
            {
                stringBuilder.append(_ionType);
                stringBuilder.append(_ordinal);
            }
        }
        for (Pair<ModificationSite, String> loss : _losses)
        {
            stringBuilder.append("(");
            stringBuilder.append(loss.first);
            stringBuilder.append("[");
            stringBuilder.append(loss.second);
            stringBuilder.append("])");
        }

        if (!_children.isEmpty()){
            stringBuilder.append("-");
            if (_children.size() != 1) {
                stringBuilder.append("[");
            }
            stringBuilder.append(_children.stream().map(this::childToString)
                    .collect(Collectors.joining(",")));

            if (_children.size() != 1) {
                stringBuilder.append("]");
            }
        }
        return stringBuilder.toString();
    }

    private String childToString(Pair<ModificationSite, ComplexFragmentIonName> child)
    {
        return "{" + child.first + ":" + child.second + "}";
    }

    public boolean hasChildren()
    {
        return !_children.isEmpty();
    }
}
