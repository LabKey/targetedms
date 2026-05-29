/*
 * Copyright (c) 2020-2026 LabKey Corporation
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
package org.labkey.targetedms.datasource;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExperimentService;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class MsMultiDataSource extends MsDataSource
{
    private final List<MsDataDirSource> _dirSources;
    private final List<MsDataFileSource> _fileSources;

    MsMultiDataSource()
    {
        super("unknown", Collections.emptyList());
        _fileSources = new ArrayList<>();
        _dirSources = new ArrayList<>();
    }

    public List<MsDataDirSource> getDirSources()
    {
        return _dirSources;
    }

    public List<MsDataFileSource> getFileSources()
    {
        return _fileSources;
    }

    void addSource(MsDataSource source)
    {
        if(source instanceof MsDataFileSource)
        {
            _fileSources.add((MsDataFileSource) source);
        }
        else if(source instanceof  MsDataDirSource)
        {
            _dirSources.add((MsDataDirSource) source);
        }
    }

    @Override
    public boolean isFileSource()
    {
        return _dirSources.isEmpty();
    }

    @Override
    public boolean isValidNameAndPath(@NotNull Path path)
    {
        return _dirSources.stream().anyMatch(s -> s.isValidNameAndPath(path)) ||
                _fileSources.stream().anyMatch(s -> s.isValidNameAndPath(path));
    }

    @Override
    boolean isValidPath(@NotNull Path path)
    {
        return _dirSources.stream().anyMatch(s -> s.isValidPath(path)) ||
                _fileSources.stream().anyMatch(s -> s.isValidPath(path));
    }

    @Override
    public boolean isValidNameAndData(@NotNull ExpData data, @NotNull ExperimentService expSvc)
    {
        return _dirSources.stream().anyMatch(s -> s.isValidNameAndData(data, expSvc)) ||
                _fileSources.stream().anyMatch(s -> s.isValidNameAndData(data, expSvc));
    }

    @Override
    boolean isValidData(@NotNull ExpData data, ExperimentService expSvc)
    {
        return _dirSources.stream().anyMatch(s -> s.isValidData(data, expSvc)) ||
                _fileSources.stream().anyMatch(s -> s.isValidData(data, expSvc));
    }

    @Override
    public String name()
    {
        List<String> names = new ArrayList<>();
        _dirSources.forEach(s -> names.add(s.name()));
        _fileSources.forEach(s -> names.add(s.name()));
        return StringUtils.join(names, ",");
    }

    @Override
    public List<String> getExtensions()
    {
        List<String> allExtensions = new ArrayList<>();
        _dirSources.forEach(s -> allExtensions.addAll(s.getExtensions()));
        _fileSources.forEach(s -> allExtensions.addAll(s.getExtensions()));
        return allExtensions;
    }
}
