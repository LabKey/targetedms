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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.labkey.targetedms.chromlib.Constants.ColumnDef;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.List;

/**
 * User: vsharma
 * Date: 1/3/13
 * Time: 11:22 AM
 */
public abstract class BaseDaoImpl<T extends AbstractLibEntity> implements Dao<T>
{
    private static final Logger _log = LogManager.getLogger(BaseDaoImpl.class);

    public PreparedStatement getPreparedStatement(Connection connection, String sql) throws SQLException
    {
        return connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
    }

    @Override
    public void save(T t, Connection connection) throws SQLException
    {
        if(t != null)
        {
            String sql = getInsertSql();

            try (PreparedStatement stmt = getPreparedStatement(connection, sql))
            {
                setValuesInStatement(t, stmt);

                int id = insertAndReturnId(stmt);
                t.setId(id);
            }
        }
    }

    @Override
    public void saveAll(Collection<T> list, Connection connection) throws SQLException
    {
        _log.debug("Batch insert of " + list.size() + " objects");
        if (!list.isEmpty())
        {
            String sql = getInsertSql();

            try (PreparedStatement stmt = getPreparedStatement(connection, sql))
            {
                connection.setAutoCommit(false);
                for(T t: list)
                {
                    setValuesInStatement(t, stmt);
                    stmt.addBatch();
                }

                int[] ids = insertAndReturnIds(stmt, list.size());
                int index = 0;
                for(T t: list)
                {
                    t.setId(ids[index++]);
                }
                connection.commit();
                connection.setAutoCommit(true);
            }
        }
    }

    private int insertAndReturnId(PreparedStatement statement) throws SQLException
    {
        try (ResultSet generatedKeys = statement.executeQuery())
        {
            if (generatedKeys.next())
            {
                return generatedKeys.getInt(1);
            }
            else
            {
                throw new SQLException("Inserting in " + getTableName() + " failed. No keys were generated.");
            }
        }
    }

    private int[] insertAndReturnIds(PreparedStatement statement, int numInserts) throws SQLException
    {
        int[] ids = new int[numInserts];
        do
        {
            try (ResultSet rs = statement.executeQuery())
            {
                // This will be the value auto-generated for the first row in the batch
                ids[0] = rs.getInt(1);
            }
        }
        while (statement.getMoreResults());

        for (int i = 1; i < ids.length; i++)
        {
            ids[i] = ids[i - 1] + 1;
        }

        return ids;
    }

    @Override
    public List<T> queryAll(Connection connection) throws SQLException
    {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM ");
        sql.append(getTableName());

        return query(connection, sql);
    }

    @Override
    public T queryForId(int id, Connection connection) throws SQLException
    {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM ");
        sql.append(getTableName());
        sql.append(" WHERE Id = ").append(id);

        List<T> results = query(connection, sql);
        if(results == null || results.isEmpty())
        {
            return null;
        }
        if(results.size() != 1)
        {
            throw new SQLException("More than one entries found in "+getTableName()+" for Id "+id);
        }
        return results.get(0);
    }

    @Override
    public List<T> queryForForeignKey(String foreignKeyColumn, int foreignKeyValue, Connection connection) throws SQLException
    {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM ");
        sql.append(getTableName());
        sql.append(" WHERE ").append(foreignKeyColumn).append(" = ").append(foreignKeyValue);
        sql.append(" ORDER BY Id");

         return query(connection, sql);
    }

    private List<T> query(Connection connection, StringBuilder sql) throws SQLException
    {
        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(sql.toString()))
        {
            return parseQueryResult(rs);
        }
    }

    public Integer readInteger(ResultSet rs, String columnName) throws SQLException
    {
        int value = rs.getInt(columnName);
        if(!rs.wasNull())
        {
            return value;
        }
        else
        {
            return null;
        }
    }

    public static Double readDouble(ResultSet rs, String columnName) throws SQLException
    {
        double value = rs.getDouble(columnName);
        if(!rs.wasNull())
        {
            return value;
        }
        else
        {
            return null;
        }
    }

    public Character readCharacter(ResultSet rs, String columnName) throws SQLException
    {
        String value = rs.getString(columnName);
        if(value != null && !value.isEmpty())
        {
            return value.charAt(0);
        }
        else
        {
            return null;
        }
    }

    private String getInsertSql()
    {
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ");
        sql.append(getTableName());
        sql.append(" (");
        int startIdx = hasAutoGeneratedIdColumn() ? 1 : 0;
        ColumnDef[] colnames = getColumns();
        for(int i = startIdx; i < colnames.length; i++)
        {
            if(i > startIdx)
                sql.append(", ");
            sql.append(colnames[i].baseColumn().name());
        }

        sql.append(")");
        sql.append(" VALUES (");
        for(int i = startIdx; i < colnames.length; i++)
        {
            if(i > startIdx) sql.append(", ");
            sql.append("?");
        }
        sql.append(")");
        if (hasAutoGeneratedIdColumn())
        {
            sql.append(" RETURNING ");
            sql.append(getColumns()[0].baseColumn().name());
        }
        return sql.toString();
    }

    protected boolean hasAutoGeneratedIdColumn()
    {
        return true;
    }

    protected abstract List<T> parseQueryResult(ResultSet rs) throws SQLException;

    protected abstract void setValuesInStatement(T t, PreparedStatement stmt) throws SQLException;

    protected abstract ColumnDef[] getColumns();
}
