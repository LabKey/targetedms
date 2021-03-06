/*
 * Copyright (c) 2013-2019 LabKey Corporation
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
package org.labkey.targetedms.chromlib;

import org.labkey.targetedms.chromlib.Constants.PrecursorColumn;
import org.labkey.targetedms.chromlib.Constants.Table;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * User: vsharma
 * Date: 1/2/13
 * Time: 10:15 PM
 */
public class LibPrecursorDao extends BaseDaoImpl<LibPrecursor>
{
    private final Dao<LibPrecursorIsotopeModification> _precIsotopeModDao;
    private final Dao<LibPrecursorRetentionTime> _precRetentionTimeDao;
    private final Dao<LibTransition> _transitionDao;

    public LibPrecursorDao(Dao<LibPrecursorIsotopeModification> precIsotopeModDao,
                           Dao<LibPrecursorRetentionTime> precRetentionTimeDao,
                           Dao<LibTransition> transitionDao)
    {
        _precIsotopeModDao = precIsotopeModDao;
        _precRetentionTimeDao = precRetentionTimeDao;
        _transitionDao = transitionDao;
    }

    @Override
    public void save(LibPrecursor precursor, Connection connection) throws SQLException
    {
        if(precursor != null)
        {
            super.save(precursor, connection);

            if(_precIsotopeModDao != null)
            {
                for(LibPrecursorIsotopeModification isotopeMod: precursor.getIsotopeModifications())
                {
                    isotopeMod.setPrecursorId(precursor.getId());
                }
                _precIsotopeModDao.saveAll(precursor.getIsotopeModifications(), connection);
            }
            if(_precRetentionTimeDao != null)
            {
                for(LibPrecursorRetentionTime precRetentionTime: precursor.getRetentionTimes())
                {
                    precRetentionTime.setPrecursorId(precursor.getId());
                }
                _precRetentionTimeDao.saveAll(precursor.getRetentionTimes(), connection);
            }
            if(_transitionDao != null)
            {
                for(LibTransition transition: precursor.getTransitions())
                {
                    transition.setPrecursorId(precursor.getId());
                }
                _transitionDao.saveAll(precursor.getTransitions(), connection);
            }
        }
    }

    @Override
    protected void setValuesInStatement(LibPrecursor precursor, PreparedStatement stmt) throws SQLException
    {
        int colIndex = 1;
        stmt.setLong(colIndex++, precursor.getPeptideId());
        stmt.setString(colIndex++, precursor.getIsotopeLabel());
        stmt.setDouble(colIndex++, precursor.getMz());
        stmt.setInt(colIndex++, precursor.getCharge());
        stmt.setObject(colIndex++, precursor.getNeutralMass(), Types.DOUBLE);
        stmt.setString(colIndex++, precursor.getModifiedSequence());
        stmt.setObject(colIndex++, precursor.getCollisionEnergy(), Types.DOUBLE);
        stmt.setObject(colIndex++, precursor.getDeclusteringPotential(), Types.DOUBLE);
        stmt.setObject(colIndex++, precursor.getTotalArea(), Types.DOUBLE);
        stmt.setObject(colIndex++, precursor.getNumTransitions(), Types.INTEGER);
        stmt.setObject(colIndex++, precursor.getNumPoints(), Types.INTEGER);
        stmt.setObject(colIndex++, precursor.getAverageMassErrorPPM(), Types.DOUBLE);
        stmt.setLong(colIndex++, precursor.getSampleFileId());
        stmt.setObject(colIndex++, precursor.getChromatogram(), Types.BLOB);
        stmt.setInt(colIndex++, precursor.getUncompressedSize());
        stmt.setInt(colIndex++, precursor.getChromatogramFormat());

        stmt.setObject(colIndex++, precursor.getExplicitIonMobility(), Types.DOUBLE);
        stmt.setObject(colIndex++, precursor.getCcs(), Types.DOUBLE);
        stmt.setObject(colIndex++, precursor.getIonMobilityMS1(), Types.DOUBLE);
        stmt.setObject(colIndex++, precursor.getIonMobilityFragment(), Types.DOUBLE);
        stmt.setObject(colIndex++, precursor.getIonMobilityWindow(), Types.DOUBLE);
        stmt.setString(colIndex++, precursor.getIonMobilityType());
        stmt.setString(colIndex++, precursor.getExplicitIonMobilityUnits());
        stmt.setObject(colIndex++, precursor.getExplicitCcsSqa(), Types.DOUBLE);
        stmt.setObject(colIndex++, precursor.getExplicitCompensationVoltage(), Types.DOUBLE);
        stmt.setObject(colIndex++, precursor.getQValue(), Types.DOUBLE);

        stmt.setString(colIndex++, precursor.getAdduct());
    }

    @Override
    public String getTableName()
    {
        return Table.Precursor.name();
    }

    @Override
    protected Constants.ColumnDef[] getColumns()
    {
        return PrecursorColumn.values();
    }

    @Override
    public void saveAll(Collection<LibPrecursor> precursors, Connection connection) throws SQLException
    {
        if(precursors.size() > 0)
        {
            super.saveAll(precursors, connection);

            List<LibPrecursorIsotopeModification> precIsotopeMods = new ArrayList<>();
            List<LibPrecursorRetentionTime> precRetentionTimes = new ArrayList<>();
            List<LibTransition> transitions = new ArrayList<>();

            if(_precIsotopeModDao != null)
            {
                for(LibPrecursor precursor: precursors)
                {
                    for(LibPrecursorIsotopeModification isotopeMod: precursor.getIsotopeModifications())
                    {
                        isotopeMod.setPrecursorId(precursor.getId());
                        precIsotopeMods.add(isotopeMod);
                    }
                }
                _precIsotopeModDao.saveAll(precIsotopeMods, connection);
            }
            if(_precRetentionTimeDao != null)
            {
                for(LibPrecursor precursor: precursors)
                {
                    for(LibPrecursorRetentionTime precRetentionTime: precursor.getRetentionTimes())
                    {
                        precRetentionTime.setPrecursorId(precursor.getId());
                        precRetentionTimes.add(precRetentionTime);
                    }
                }
                _precRetentionTimeDao.saveAll(precRetentionTimes, connection);
            }
            if(_transitionDao != null)
            {
                for(LibPrecursor precursor: precursors)
                {
                    for(LibTransition transition: precursor.getTransitions())
                    {
                        transition.setPrecursorId(precursor.getId());
                        transitions.add(transition);
                    }
                }
                _transitionDao.saveAll(transitions, connection);
            }
        }
    }

    @Override
    protected List<LibPrecursor> parseQueryResult(ResultSet rs) throws SQLException
    {
        List<LibPrecursor> precursors = new ArrayList<>();
        while(rs.next())
        {
            LibPrecursor precursor = new LibPrecursor(rs);
            precursors.add(precursor);
        }
        return precursors;
    }

    public void loadTransitions(LibPrecursor precursor, Connection connection) throws SQLException
    {
        List<LibTransition> transitions = _transitionDao.queryForForeignKey(Constants.TransitionColumn.PrecursorId.baseColumn().name(),
                                                                            precursor.getId(),
                                                                            connection);
        for(LibTransition transition: transitions)
        {
            precursor.addTransition(transition);
        }
    }

    public void loadPrecursorIsotopeModifications(LibPrecursor precursor, Connection connection) throws SQLException
    {
        List<LibPrecursorIsotopeModification> precIsotopeMods = _precIsotopeModDao.queryForForeignKey(Constants.PrecursorIsotopeModificationColumn.PrecursorId.baseColumn().name(),
                                                                                                      precursor.getId(),
                                                                                                      connection);
        for(LibPrecursorIsotopeModification precIsoMod: precIsotopeMods)
        {
            precursor.addIsotopeModification(precIsoMod);
        }
    }

    public void loadPrecursorRetentionTimes(LibPrecursor precursor, Connection connection) throws SQLException
    {
        List<LibPrecursorRetentionTime> precRetTimes = _precRetentionTimeDao.queryForForeignKey(Constants.PrecursorRetentionTimeColumn.PrecursorId.baseColumn().name(),
                                                                                                precursor.getId(),
                                                                                                connection);
        for(LibPrecursorRetentionTime precRt: precRetTimes)
        {
            precursor.addRetentionTime(precRt);
        }
    }
}
