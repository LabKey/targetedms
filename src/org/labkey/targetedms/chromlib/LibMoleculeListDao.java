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

import org.labkey.targetedms.chromlib.Constants.ProteinColumn;
import org.labkey.targetedms.chromlib.Constants.Table;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * User: vsharma
 * Date: 1/2/13
 * Time: 10:15 PM
 */
public class LibMoleculeListDao extends BaseDaoImpl<LibMoleculeList>
{
    private final Dao<LibMolecule> _moleculeDao;

    public LibMoleculeListDao(Dao<LibMolecule> moleculeDao)
    {
        _moleculeDao = moleculeDao;
    }

    @Override
    public void save(LibMoleculeList moleculeList, Connection connection) throws SQLException
    {
        if(moleculeList != null)
        {
            super.save(moleculeList, connection);

            if(_moleculeDao != null)
            {
                for(LibMolecule molecule: moleculeList.getChildren())
                {
                    molecule.setMoleculeListId(moleculeList.getId());
                }
                _moleculeDao.saveAll(moleculeList.getChildren(), connection);
            }
        }
    }

    @Override
    protected void setValuesInStatement(LibMoleculeList moleculeList, PreparedStatement stmt) throws SQLException
    {
        int colIndex = 1;
        stmt.setString(colIndex++, moleculeList.getName());
        stmt.setString(colIndex++, moleculeList.getDescription());
    }

    @Override
    public String getTableName()
    {
        return Table.MoleculeList.name();
    }

    @Override
    protected Constants.ColumnDef[] getColumns()
    {
        return Constants.MoleculeListColumn.values();
    }

    @Override
    public void saveAll(List<LibMoleculeList> moleculeLists, Connection connection) throws SQLException
    {
        if(moleculeLists != null && moleculeLists.size() > 0)
        {
            super.saveAll(moleculeLists, connection);

            List<LibMolecule> molecules = new ArrayList<>();

            if(_moleculeDao != null)
            {
                for(LibMoleculeList moleculeList: moleculeLists)
                {
                    for(LibMolecule molecule: moleculeList.getChildren())
                    {
                        molecule.setMoleculeListId(moleculeList.getId());
                        moleculeList.addChild(molecule);
                    }
                }
                _moleculeDao.saveAll(molecules, connection);
            }
        }
    }

    @Override
    public List<LibMoleculeList> queryForForeignKey(String foreignKeyColumn, int foreignKeyValue, Connection connection)
    {
        throw new UnsupportedOperationException(getTableName()+" does not have a foreign key");
    }

    @Override
    protected List<LibMoleculeList> parseQueryResult(ResultSet rs) throws SQLException
    {
        List<LibMoleculeList> moleculeLists = new ArrayList<>();
        while(rs.next())
        {
            LibMoleculeList moleculeList = new LibMoleculeList();
            moleculeList.setId(rs.getInt(ProteinColumn.Id.baseColumn().name()));
            moleculeList.setName(rs.getString(ProteinColumn.Name.baseColumn().name()));
            moleculeList.setDescription(rs.getString(ProteinColumn.Description.baseColumn().name()));

            moleculeLists.add(moleculeList);
        }
        return moleculeLists;
    }

    public void loadMolecules(LibMoleculeList moleculeList, Connection connection) throws SQLException
    {
        List<LibMolecule> molecules = _moleculeDao.queryForForeignKey(Constants.MoleculeColumn.MoleculeListId.baseColumn().name(), moleculeList.getId(), connection);
        for(LibMolecule molecule: molecules)
        {
            moleculeList.addChild(molecule);
        }
    }
}
