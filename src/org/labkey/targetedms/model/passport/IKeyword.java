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
package org.labkey.targetedms.model.passport;

public class IKeyword
{
    public static final String BIOLOGICAL_PROCESS_CATEGORY = "KW-9999";
    public static final String MOLECULAR_FUNCTION_CATEGORY = "KW-9992";

    public final String id;
    public final String categoryId;
    public final String label;
    public final String category;

    public IKeyword(String id, String categoryId, String label, String category)
    {
        this.id = id;
        this.categoryId = categoryId;
        this.label = label;
        this.category = category;
    }
}
