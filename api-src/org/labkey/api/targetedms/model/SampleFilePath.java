/*
 * Copyright (c) 2022-2026 LabKey Corporation
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
package org.labkey.api.targetedms.model;

import org.jetbrains.annotations.Nullable;
import org.labkey.api.targetedms.ISampleFile;

import java.nio.file.Path;

// Class encapsulating the information for a sample file from a Skyline document, and its path in the "RawFiles" directory
// in the file root of the container where the Skyline document lives.
public class SampleFilePath
{
    private final ISampleFile _sampleFile;
    private Path _path;

    public SampleFilePath(ISampleFile sampleFile)
    {
        _sampleFile = sampleFile;
    }

    public ISampleFile getSampleFile()
    {
        return _sampleFile;
    }

    /**
     * @return the path of the sample file on the server if it exists, null otherwise.
     */
    public @Nullable Path getPath()
    {
        return _path;
    }

    public void setPath(Path path)
    {
        _path = path;
    }
}
