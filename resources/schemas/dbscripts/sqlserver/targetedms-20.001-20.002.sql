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

-- Drop the FK constraint and index before dropping the column
IF OBJECTPROPERTY(OBJECT_ID('targetedms.fk_spectrumlibrary_librarysourceid'), 'IsConstraint') = 1
    BEGIN
EXEC('ALTER TABLE targetedms.spectrumlibrary DROP CONSTRAINT fk_spectrumlibrary_librarysourceid')
END

DROP INDEX targetedms.spectrumlibrary.IX_SpectrumLibrary_LibrarySourceId;
GO

ALTER TABLE targetedms.spectrumlibrary DROP COLUMN librarysourceid;
DROP TABLE targetedms.librarysource;
