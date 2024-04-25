/*
 * Copyright (c) 2012-2019 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.labkey.targetedms.query;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.DataColumn;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.data.RenderContext;
import org.labkey.api.query.FieldKey;
import org.labkey.api.util.HtmlString;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSSchema;
import org.labkey.targetedms.chart.LabelFactory;
import org.labkey.targetedms.parser.GeneralTransition.IonType;

import java.util.ArrayList;
import java.util.Set;

/**
 * User: vsharma
 * Date: Apr 13, 2012
 */
public class DocTransitionsTableInfo extends AbstractGeneralTransitionTableInfo
{
    public DocTransitionsTableInfo(final TargetedMSSchema schema, ContainerFilter cf)
    {
        this(schema, cf, false);
    }

    public DocTransitionsTableInfo(final TargetedMSSchema schema, ContainerFilter cf, boolean omitAnnotations)
    {
        super(schema, TargetedMSManager.getTableInfoTransition(), cf, omitAnnotations, TargetedMSSchema.ContainerJoinType.PrecursorFK);

        setDescription(TargetedMSManager.getTableInfoTransition().getDescription());

        var precursorCol = getMutableColumnOrThrow("GeneralPrecursorId");
        precursorCol.setFk(new TargetedMSForeignKey(getUserSchema(), TargetedMSSchema.TABLE_PRECURSOR, cf));
        precursorCol.setHidden(true);

        var precursorIdCol = wrapColumn("PrecursorId", getRealTable().getColumn(precursorCol.getFieldKey()));
        precursorIdCol.setFk(new TargetedMSForeignKey(getUserSchema(), TargetedMSSchema.TABLE_PRECURSOR, cf));

        addColumn(precursorIdCol);

        //Display the fragment as y9 instead of 'y' and '9' in separate columns
        var fragmentCol = wrapColumn("Fragment", getRealTable().getColumn(FieldKey.fromParts("FragmentType")));
        fragmentCol.setDisplayColumnFactory(new FragmentIonDisplayColumnFactory());
        addColumn(fragmentCol);


        ArrayList<FieldKey> visibleColumns = new ArrayList<>();
        visibleColumns.add(FieldKey.fromParts("PrecursorId", "PeptideId", "PeptideGroupId", "Label"));
        visibleColumns.add(FieldKey.fromParts("PrecursorId", "PeptideId", "PeptideGroupId", "Description"));
        visibleColumns.add(FieldKey.fromParts("PrecursorId", "PeptideId", "PeptideGroupId", "Annotations"));

        // Peptide level information
        visibleColumns.add(FieldKey.fromParts("PrecursorId", "PeptideId", ModifiedSequenceDisplayColumn.PEPTIDE_COLUMN_NAME));
        visibleColumns.add(FieldKey.fromParts("PrecursorId", "PeptideId", "Annotations"));
        visibleColumns.add(FieldKey.fromParts("PrecursorId", "PeptideId", "NumMissedCleavages"));
        visibleColumns.add(FieldKey.fromParts("PrecursorId", "PeptideId", "CalcNeutralMass"));
        visibleColumns.add(FieldKey.fromParts("PrecursorId", "PeptideId", "Rank"));

        // Precursor level information
        visibleColumns.add(FieldKey.fromParts("PrecursorId", ModifiedSequenceDisplayColumn.PRECURSOR_COLUMN_NAME));
        visibleColumns.add(FieldKey.fromParts("PrecursorId", "Annotations"));
        visibleColumns.add(FieldKey.fromParts("PrecursorId", "IsotopeLabelId", "Name"));
        visibleColumns.add(FieldKey.fromParts("PrecursorId", "NeutralMass"));
        visibleColumns.add(FieldKey.fromParts("PrecursorId", "Mz"));
        visibleColumns.add(FieldKey.fromParts("PrecursorId", "Charge"));

        // Transition level information
        visibleColumns.add(FieldKey.fromParts("Fragment"));
        visibleColumns.add(FieldKey.fromParts("Mz"));
        visibleColumns.add(FieldKey.fromParts("Charge"));

        setDefaultVisibleColumns(visibleColumns);
    }

    public static class FragmentIonDisplayColumnFactory implements DisplayColumnFactory
    {
        private final FieldKey _fragmentOrdinalFieldKey = FieldKey.fromParts("FragmentOrdinal");
        private final FieldKey _neutralLossMassFieldKey = FieldKey.fromParts("NeutralLossMass");
        private final FieldKey _massIndexFieldKey = FieldKey.fromParts("MassIndex");
        private final FieldKey _measuredIonNameFieldKey = FieldKey.fromParts("MeasuredIonName");

        @Override
        public DisplayColumn createRenderer(ColumnInfo colInfo)
        {
            DataColumn dc = new DataColumn(colInfo) {

                @Override
                public Object getValue(RenderContext ctx)
                {
                    // Return the ion name without any extended characters
                    return getValue(ctx, false);
                }

                private @Nullable String getValue(RenderContext ctx, boolean extendedCharsAllowed)
                {
                    String fragmentType = ctx.get(getColumnInfo().getFieldKey(), String.class);
                    if (fragmentType != null)
                    {
                        IonType ionType = IonType.getType(fragmentType);
                        if (ionType != null)
                        {
                            Integer fragmentOrdinal = ctx.get(_fragmentOrdinalFieldKey, Integer.class);
                            Double neutralLossMass = ctx.get(_neutralLossMassFieldKey, Double.class);
                            Integer massIndex = ctx.get(_massIndexFieldKey, Integer.class);
                            String measuredIonName = ctx.get(_measuredIonNameFieldKey, String.class);

                            return LabelFactory.getProteomicTransitionName(ionType, massIndex, fragmentOrdinal, measuredIonName, neutralLossMass, extendedCharsAllowed);
                        }
                        else
                        {
                            Integer fragmentOrdinal = ctx.get(_fragmentOrdinalFieldKey, Integer.class);
                            Double neutralLossMass = ctx.get(_neutralLossMassFieldKey, Double.class);
                            return LabelFactory.getProteomicTransitionName(fragmentType, fragmentOrdinal, neutralLossMass);
                        }
                    }
                    return null;
                }

                @Override
                public Object getDisplayValue(RenderContext ctx)
                {
                    return getValue(ctx);
                }

                @Override
                public @Nullable String getFormattedText(RenderContext ctx)
                {
                    Object o = getValue(ctx);
                    return o == null ? null : o.toString();
                }

                @Override
                public Object getExcelCompatibleValue(RenderContext ctx)
                {
                    // Return the fragment ion name with extended characters, if any. Example: z• (z+1) and z′ (z+2) ions.
                    return getValue(ctx, true);
                }

                @Override @NotNull
                public HtmlString getFormattedHtml(RenderContext ctx)
                {
                    // Display the fragment ion name with extended characters, if any. Example: z• (z+1) and z′ (z+2) ions.
                    return HtmlString.of(getValue(ctx, true));
                }

                @Override
                public void addQueryFieldKeys(Set<FieldKey> keys)
                {
                    super.addQueryFieldKeys(keys);
                    keys.add(_fragmentOrdinalFieldKey);
                    keys.add(_neutralLossMassFieldKey);
                    keys.add(_massIndexFieldKey);
                    keys.add(_measuredIonNameFieldKey);
                }
            };
            return dc;
        }
    }
}
