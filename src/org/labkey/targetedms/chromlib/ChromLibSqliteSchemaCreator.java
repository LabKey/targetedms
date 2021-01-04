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

import org.labkey.targetedms.chromlib.Constants.Table;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * User: vsharma
 * Date: 12/26/12
 * Time: 3:26 PM
 */
public class ChromLibSqliteSchemaCreator
{
    public void createSchema(Connection connection) throws SQLException
    {
        try {
            createTables(connection);
        }
        finally
        {
            if(connection != null) try {connection.close();} catch(SQLException ignored){}
        }
    }

    private void createTables(Connection conn) throws SQLException
    {
        createLibInfoTable(conn);
        createSampleFileTable(conn);

        // Proteomics modifications
        createStructuralModificationTable(conn);
        createStructuralModLossTable(conn);
        createIsotopeModificationTable(conn);

        // Proteomics
        createProteinTable(conn);
        createPeptideTable(conn);
        createPeptideStructuralModificationTable(conn);
        createPrecursorTable(conn);
        createPrecursorIsotopeModificationTable(conn);
        createPrecursorRetentionTimeTable(conn);
        createTransitionTable(conn);
        createTransitionOptimizationTable(conn);

        // Small molecule
        createMoleculeListTable(conn);
        createMoleculeTable(conn);
        createMoleculePrecursorTable(conn);
        createMoleculePrecursorRetentionTimeTable(conn);
        createMoleculeTransitionTable(conn);
        createMoleculeTransitionOptimizationTable(conn);

        createIrtLibraryTable(conn);
    }

    private void createLibInfoTable(Connection conn) throws SQLException
    {
        createTable(conn, Table.LibInfo, Constants.LibInfoColumn.values());
    }

    private void createStructuralModificationTable(Connection conn) throws SQLException
    {
        createTable(conn, Table.StructuralModification, Constants.StructuralModificationColumn.values());
    }

    private void createStructuralModLossTable(Connection conn) throws SQLException
    {
        createTable(conn, Table.StructuralModLoss, Constants.StructuralModLossColumn.values());
    }

    private void createIsotopeModificationTable(Connection conn) throws SQLException
    {
        createTable(conn, Table.IsotopeModification, Constants.IsotopeModificationColumn.values());
    }

    private void createProteinTable(Connection conn) throws SQLException
    {
        createTable(conn, Table.Protein, Constants.ProteinColumn.values());
    }

    private void createMoleculeListTable(Connection conn) throws SQLException
    {
        createTable(conn, Table.MoleculeList, Constants.MoleculeListColumn.values());
    }

    private void createMoleculeTable(Connection conn) throws SQLException
    {
        createTable(conn, Table.Molecule, Constants.MoleculeColumn.values());
    }

    private void createMoleculePrecursorTable(Connection conn) throws SQLException
    {
        createTable(conn, Table.MoleculePrecursor, Constants.MoleculePrecursorColumn.values());
    }

    private void createMoleculePrecursorRetentionTimeTable(Connection conn) throws SQLException
    {
        createTable(conn, Table.MoleculePrecursorRetentionTime, Constants.MoleculePrecursorRetentionTimeColumn.values());
    }

    private void createMoleculeTransitionTable(Connection conn) throws SQLException
    {
        createTable(conn, Table.MoleculeTransition, Constants.MoleculeTransitionColumn.values());
    }

    private void createPeptideTable(Connection conn) throws SQLException
    {
        createTable(conn, Table.Peptide, Constants.PeptideColumn.values());
    }

    private void createPeptideStructuralModificationTable(Connection conn) throws SQLException
    {
        createTable(conn, Table.PeptideStructuralModification, Constants.PeptideStructuralModificationColumn.values());
    }

    private void createPrecursorTable(Connection conn) throws SQLException
    {
        createTable(conn, Table.Precursor, Constants.PrecursorColumn.values());
    }

    private void createPrecursorIsotopeModificationTable(Connection conn) throws SQLException
    {
        createTable(conn, Table.PrecursorIsotopeModification, Constants.PrecursorIsotopeModificationColumn.values());
    }

    private void createSampleFileTable(Connection conn) throws SQLException
    {
        createTable(conn, Table.SampleFile, Constants.SampleFileColumn.values());
    }

    private void createPrecursorRetentionTimeTable(Connection conn) throws SQLException
    {
       createTable(conn, Table.PrecursorRetentionTime, Constants.PrecursorRetentionTimeColumn.values());
    }

    private void createTransitionTable(Connection conn) throws SQLException
    {
        createTable(conn, Table.Transition, Constants.TransitionColumn.values());
    }

    private void createIrtLibraryTable(Connection conn) throws SQLException
    {
        createTable(conn, Table.IrtLibrary, Constants.IrtLibraryColumn.values());
    }

    private void createTransitionOptimizationTable(Connection conn) throws SQLException
    {
        createTable(conn, Table.TransitionOptimization, Constants.TransitionOptimizationColumn.values());
    }

    private void createMoleculeTransitionOptimizationTable(Connection conn) throws SQLException
    {
        createTable(conn, Table.MoleculeTransitionOptimization, Constants.MoleculeTransitionOptimization.values());
    }

    private String getColumnSql(Constants.ColumnDef[] columns)
    {
        StringBuilder columnSql = new StringBuilder();
        for(Constants.ColumnDef column: columns)
        {
            columnSql.append(", ").append(column.baseColumn().name()).append(" ").append(column.definition());

            // Append the foreign key if it has one
            if (column.baseColumn().getFkColumn() != null)
            {
                columnSql.append(" REFERENCES ").
                        append(column.baseColumn().getFkTable()).
                        append("(").
                        append(column.baseColumn().
                                getFkColumn()).append(")");
            }
        }
        columnSql.deleteCharAt(0); // delete first comma
        return columnSql.toString();
    }

    private void createTable(Connection conn, Table tableName, Constants.ColumnDef[] columns) throws SQLException
    {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE ");
        sql.append(tableName.name());
        sql.append(" (");
        sql.append(getColumnSql(columns));
        sql.append(" )");

        try (Statement stmt = conn.createStatement())
        {
            stmt.execute(sql.toString());
        }

        for (Constants.ColumnDef column : columns)
        {
            if (column.baseColumn().getFkColumn() != null)
            {
                try (Statement stmt = conn.createStatement())
                {
                    StringBuilder indexSQL = new StringBuilder("CREATE INDEX IDX_");
                    indexSQL.append(tableName);
                    indexSQL.append("_");
                    indexSQL.append(column.baseColumn().name());
                    indexSQL.append(" ON ");
                    indexSQL.append(tableName);
                    indexSQL.append("(");
                    indexSQL.append(column.baseColumn().name());
                    indexSQL.append(")");
                    stmt.execute(indexSQL.toString());
                }
            }
        }
    }
}
