/*
 * Copyright (c) 2019 LabKey Corporation. All rights reserved. No portion of this work may be reproduced in
 * any form or by any electronic or mechanical means without written permission from LabKey Corporation.
 */

package org.labkey.panoramapremium;

import org.apache.logging.log4j.Logger;
import org.labkey.api.action.ApiSimpleResponse;
import org.labkey.api.action.MutatingApiAction;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.collections.CaseInsensitiveHashMap;
import org.labkey.api.data.CompareType;
import org.labkey.api.data.DataRegionSelection;
import org.labkey.api.data.DbScope;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableSelector;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.query.BatchValidationException;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryForm;
import org.labkey.api.query.QueryService;
import org.labkey.api.query.QueryUpdateService;
import org.labkey.api.query.UserSchema;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.security.permissions.UpdatePermission;
import org.labkey.api.util.logging.LogHelper;
import org.springframework.validation.BindException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class PanoramaPremiumController extends SpringActionController
{
    private static final DefaultActionResolver _actionResolver = new DefaultActionResolver(PanoramaPremiumController.class);
    public static final String NAME = "panoramapremium";
    private static final Logger LOG = LogHelper.getLogger(PanoramaPremiumController.class, "PanoramaPremiumController requests");

    public PanoramaPremiumController()
    {
        setActionResolver(_actionResolver);
    }

    @RequiresPermission(UpdatePermission.class)
    public class MarkPrecursorAsIncludedAction extends MutatingApiAction<QueryForm>
    {
        @Override
        public Object execute(QueryForm queryForm, BindException errors) throws Exception
        {
            //get selected rows
            Set<Integer> selectedRowsKeys = DataRegionSelection.getSelectedIntegers(queryForm.getViewContext(),true);

            UserSchema us = QueryService.get().getUserSchema(getUser(), getContainer(), "targetedms");
            TableInfo excludedPrecursorsTableInfo = us.getTable("ExcludedPrecursors");
            assert excludedPrecursorsTableInfo != null;

            //remove selected precursors/molecules from targetedms.ExcludedPrecursors table (to be considered "Included")
            List<Map<String, Object>> precursorsToBeConsideredIncluded = getPrecursorsToExcludeOrInclude(selectedRowsKeys, queryForm.getQueryName(), false, true);

            try (DbScope.Transaction transaction = ExperimentService.get().ensureTransaction())
            {
                QueryUpdateService qus = excludedPrecursorsTableInfo.getUpdateService();
                assert qus != null;
                qus.deleteRows(getUser(), getContainer(), precursorsToBeConsideredIncluded, null, null);

                transaction.commit();
            }
            catch (Exception e)
            {
                String errMsg = "Error deleting row from targetedms.ExcludedPrecursors table";
                LOG.error(errMsg, e);
                errors.reject(ERROR_MSG, errMsg + ": " + e.getMessage());
                return null;
            }
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            return new ApiSimpleResponse(result);
        }
    }

    public List<Map<String, Object>> getPrecursorsToExcludeOrInclude(Set<Integer> selectedRowsKeys, String query, boolean isExcluding, boolean isIncluding)
    {
        final boolean isPeptide = query.equalsIgnoreCase("QCGroupingPrecursorPeptides");
        final boolean isMolecule = query.equalsIgnoreCase("QCGroupingPrecursorMolecules");

        TableInfo tableInfo = null;
        String fieldKey = "Id";
        List<Map<String, Object>> selectedRows = new ArrayList<>();

        UserSchema us = QueryService.get().getUserSchema(getUser(), getContainer(), "targetedms");

        if (isPeptide)
        {
            tableInfo = us.getTable("Precursor");
        }
        else if(isMolecule)
        {
            tableInfo = us.getTable("MoleculePrecursor");
        }

        assert tableInfo != null;

        //get peptide or molecule precursor data from Precursor or MoleculePrecursor table for peptide or molecule ids of the selected rows
        SimpleFilter precursorIdentifierFilter = new SimpleFilter(FieldKey.fromString(fieldKey), selectedRowsKeys, CompareType.IN);
        TableSelector precursors = new TableSelector(tableInfo, precursorIdentifierFilter, null);
        List<ExcludedPrecursor> precursorsList = precursors.getArrayList(ExcludedPrecursor.class);

        //get peptide or molecule precursors that are already excluded
        TableInfo excludedPrecursorsTableInfo = us.getTable("ExcludedPrecursors");
        TableSelector excludedPrecursors = new TableSelector(excludedPrecursorsTableInfo);
        List<ExcludedPrecursor> excludedPrecursorsList = excludedPrecursors.getArrayList(ExcludedPrecursor.class);

        //skip selected precursors that are already excluded
        selectedRowsKeys.forEach(id -> {
            List<ExcludedPrecursor> prec = precursorsList.stream().filter(p -> p.getId() == id).collect(Collectors.toList());
            ExcludedPrecursor precursor = prec.get(0);
            precursor.setIsPeptide(isPeptide);

            if (isExcluding)
            {
                if (excludedPrecursorsList.stream().noneMatch(exclPrec -> exclPrec.equals(precursor)))
                {
                    Map<String, Object> row = new CaseInsensitiveHashMap<>();

                    row.put("ModifiedSequence", precursor.getModifiedSequence());
                    row.put("Mz", precursor.getMz());
                    row.put("Charge", precursor.getCharge());
                    row.put("CustomIonName", precursor.getCustomIonName());
                    row.put("IonFormula", precursor.getIonFormula());
                    row.put("MassMonoisotopic", precursor.getMassMonoisotopic());
                    row.put("MassAverage", precursor.getMassAverage());
                    selectedRows.add(row);
                }
            }
            else if (isIncluding)
            {
                excludedPrecursorsList.forEach(ep -> {
                    if (ep.equals(precursor))
                    {
                        Map<String, Object> row = new CaseInsensitiveHashMap<>();
                        row.put("rowId", ep.getRowId());
                        selectedRows.add(row);
                    }
                });
            }
        });

        return selectedRows;
    }

    @RequiresPermission(UpdatePermission.class)
    public class MarkPrecursorAsExcludedAction extends MutatingApiAction<QueryForm>
    {
        @Override
        public Object execute(QueryForm queryForm, BindException errors) throws Exception
        {
            BatchValidationException batchValidationErrors = new BatchValidationException();

            //get selected rows
            Set<Integer> selectedRowsKeys = DataRegionSelection.getSelectedIntegers(queryForm.getViewContext(), true);

            //get selected precursors that are not already excluded (i.e. not in targetedms.excludedPrecursors table)
            List<Map<String, Object>> precursorsToExclude = getPrecursorsToExcludeOrInclude(selectedRowsKeys, queryForm.getQueryName(), true, false);

            //add selected precursors/molecules to targetedms.ExcludedPrecursors table
            try (DbScope.Transaction transaction = ExperimentService.get().ensureTransaction())
            {
                UserSchema us = QueryService.get().getUserSchema(getUser(), getContainer(), "targetedms");
                TableInfo excludedPrecursorsTableInfo = us.getTable("ExcludedPrecursors");
                assert excludedPrecursorsTableInfo != null;

                QueryUpdateService qus = excludedPrecursorsTableInfo.getUpdateService();
                assert qus != null;

                qus.insertRows(getUser(), getContainer(), precursorsToExclude, batchValidationErrors, null, null);

                if (batchValidationErrors.hasErrors())
                {
                    errors.reject(ERROR_MSG, batchValidationErrors.getMessage());
                    return null;
                }
                transaction.commit();
            }
            catch (Exception e)
            {
                String errMsg = "Error inserting row into targetedms.ExcludedPrecursors table";
                LOG.error(errMsg, e);
                errors.reject(ERROR_MSG, errMsg + ": " + e.getMessage());
                return null;
            }
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            return new ApiSimpleResponse(result);
        }
    }

    public static class ExcludedPrecursor
    {
        int rowId;
        int id;
        String modifiedSequence;
        String customIonName;
        String ionFormula;
        Double massMonoisotopic;
        Double massAverage;
        Integer charge;
        Double mz;

        boolean isPeptide;

        public boolean equals (ExcludedPrecursor p)
        {
            if (p.isPeptide)
                return Objects.equals(this.modifiedSequence, p.modifiedSequence) &&
                        Objects.equals(this.charge, p.charge) &&
                        Objects.equals(this.mz, p.mz);
            else //if its a molecule
                return Objects.equals(this.customIonName, p.customIonName) &&
                        Objects.equals(this.ionFormula, p.ionFormula) &&
                        Objects.equals(this.massMonoisotopic, p.massMonoisotopic) &&
                        Objects.equals(this.massAverage, p.massAverage) &&
                        Objects.equals(this.charge, p.charge) &&
                        Objects.equals(this.mz, p.mz);
        }

        public int getRowId()
        {
            return rowId;
        }

        public void setRowId(int rowId)
        {
            this.rowId = rowId;
        }

        public int getId()
        {
            return id;
        }

        public void setId(int id)
        {
            this.id = id;
        }

        public String getModifiedSequence()
        {
            return modifiedSequence;
        }

        public void setModifiedSequence(String modifiedSequence)
        {
            this.modifiedSequence = modifiedSequence;
        }

        public String getCustomIonName()
        {
            return customIonName;
        }

        public void setCustomIonName(String customIonName)
        {
            this.customIonName = customIonName;
        }

        public String getIonFormula()
        {
            return ionFormula;
        }

        public void setIonFormula(String ionFormula)
        {
            this.ionFormula = ionFormula;
        }

        public Double getMassMonoisotopic()
        {
            return massMonoisotopic;
        }

        public void setMassMonoisotopic(Double massMonoisotopic)
        {
            this.massMonoisotopic = massMonoisotopic;
        }

        public Double getMassAverage()
        {
            return massAverage;
        }

        public void setMassAverage(Double massAverage)
        {
            this.massAverage = massAverage;
        }

        public Integer getCharge()
        {
            return charge;
        }

        public void setCharge(Integer charge)
        {
            this.charge = charge;
        }

        public Double getMz()
        {
            return mz;
        }

        public void setMz(Double mz)
        {
            this.mz = mz;
        }

        public boolean isPeptide()
        {
            return isPeptide;
        }

        public void setIsPeptide(boolean peptide)
        {
            isPeptide = peptide;
        }
    }
}