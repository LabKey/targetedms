/*
 * Copyright (c) 2012-2019 LabKey Corporation
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

import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.JdbcType;
import org.labkey.targetedms.parser.list.ListData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * User: vsharma
 * Date: 10/23/12
 * Time: 9:56 PM
 */
public class DataSettings
{
    public enum AnnotationType {
        text(JdbcType.VARCHAR,false, false),
        number(JdbcType.DOUBLE, true, false),
        true_false(JdbcType.BOOLEAN, false, true),
        value_list(JdbcType.VARCHAR, false, true);

        private JdbcType _dataType;
        private boolean _isMeasure;
        private boolean _isDimension;

        AnnotationType(JdbcType dataType, boolean isMeasure, boolean isDimension)
        {

            _dataType = dataType;
            _isMeasure = isMeasure;
            _isDimension = isDimension;
        }

        public JdbcType getDataType()
        {
            return _dataType;
        }
        
        @Nullable
        public static AnnotationType fromString(@Nullable String value)
        {
            for (AnnotationType annotationType : values())
                if (annotationType.name().equals(value))
                    return annotationType;

            return null;
        }

        public boolean isMeasure()
        {
            return _isMeasure;
        }

        public boolean isDimension()
        {
            return _isDimension;
        }
    }

    public enum AnnotationTarget {
        protein,
        peptide,
        precursor,
        transition,
        replicate,
        precursor_result,
        transition_result
    }

    private Map<String, AnnotationDefinition> _annotationDefinitions = new HashMap<>();
    private Map<AnnotationTarget, List<AnnotationDefinition>> _targetAnnotationsMap =
                        new HashMap<>();
    private List<GroupComparisonSettings> _groupComparisons = new ArrayList<>();
    private List<ListData> _listDatas = new ArrayList<>();

    public void addAnnotations(String name, String targetsString, String type, String lookup)
    {
        String[] targetsArr = targetsString.replaceAll("\\s", "").split(",");
        if(targetsArr.length == 0)
        {
            throw new IllegalStateException("No targets found for annotation "+name);
        }
        List<AnnotationTarget> targets = new ArrayList<>(targetsArr.length);
        for(String targetStr: targetsArr)
        {
            targets.add(AnnotationTarget.valueOf(targetStr));
        }

        AnnotationType annotationType;
        if(type.equals("-1"))
        {
            // Current version of Skyline (Skyline-daily 3.5.1.9283) allows users to create a new annotation
            // without selecting a valid "type". This will be fixed in a future release.
            // We will assume "text" type for such annotations.
            annotationType = AnnotationType.text;
        }
        else
        {
            annotationType = AnnotationType.valueOf(type);
        }

        AnnotationDefinition annot = new AnnotationDefinition(
                name,
                targets,
                annotationType, lookup);
        _annotationDefinitions.put(name, annot);

        for(AnnotationTarget target: annot.getTargets())
        {
            List<AnnotationDefinition> targetAnnotations = _targetAnnotationsMap.computeIfAbsent(target, k -> new ArrayList<>());
            targetAnnotations.add(annot);
        }
    }

    public void addGroupComparison(GroupComparisonSettings groupComparison)
    {
        _groupComparisons.add(groupComparison);
    }

    public List<GroupComparisonSettings> getGroupComparisons()
    {
        return _groupComparisons;
    }

    public void addListData(ListData listData)
    {
        _listDatas.add(listData);
    }

    public List<ListData> getListDatas()
    {
        return _listDatas;
    }

    public boolean isBooleanAnnotation(String name) {

        AnnotationDefinition annot = _annotationDefinitions.get(name);
        return annot != null && annot.getType() == AnnotationType.true_false;
    }

    public boolean annotationExists(String name)
    {
        return _annotationDefinitions.get(name) != null;
    }

    public <AnnotationTargetType extends AbstractAnnotation> List<String> getMissingBooleanAnnotations(List<AnnotationTargetType> annotations, AnnotationTarget target)
    {
        Set<String> annotNames = new HashSet<>();
        for(AnnotationTargetType annot: annotations)
        {
            annotNames.add(annot.getName());
        }

        List<AnnotationDefinition> annotDefs = _targetAnnotationsMap.get(target);
        if(annotDefs == null)
            return Collections.emptyList();

        List<String> missingAnnotations = new ArrayList<>();
        for(AnnotationDefinition def: annotDefs)
        {
            if(def.getType() == AnnotationType.true_false && !annotNames.contains(def.getName()))
            {
                missingAnnotations.add(def.getName());
            }
        }
        return missingAnnotations;
    }

    public List<AnnotationSetting> getAnnotationSettings()
    {
        List<AnnotationSetting> settingsList = new ArrayList<>();
        for(AnnotationDefinition annotDef: _annotationDefinitions.values())
        {
            for(AnnotationTarget target: annotDef.getTargets())
            {
                AnnotationSetting setting = new AnnotationSetting();
                setting.setName(annotDef.getName());
                setting.setTargets(target.toString());
                setting.setType(annotDef.getType().toString());
                setting.setLookup(annotDef.getLookup());
                settingsList.add(setting);
            }
        }
        return settingsList;
    }

    private static class AnnotationDefinition
    {
        private final String _name;
        private final AnnotationType _type;
        private final List<AnnotationTarget> _targetList;
        private final String _lookup;

        AnnotationDefinition(String name, List<AnnotationTarget> targets, AnnotationType type, String lookup)
        {
            _name = name;
            _type = type;
            _targetList = targets;
            _lookup = lookup;
        }

        public String getName()
        {
            return _name;
        }

        public AnnotationType getType()
        {
            return _type;
        }

        public List<AnnotationTarget> getTargets()
        {
            return _targetList;
        }

        public String getLookup()
        {
            return _lookup;
        }
    }
}
