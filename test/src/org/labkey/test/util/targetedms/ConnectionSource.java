package org.labkey.test.util.targetedms;

import org.apache.commons.dbcp.ConnectionFactory;
import org.apache.commons.dbcp.DriverManagerConnectionFactory;
import org.apache.commons.dbcp.PoolableConnectionFactory;
import org.apache.commons.dbcp.PoolingDataSource;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.jetbrains.annotations.NotNull;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class ConnectionSource implements AutoCloseable
{
    private final DataSource _dataSource;
    private final @NotNull GenericObjectPool _connectionPool;

    static
    {
        try
        {
            Class.forName("org.sqlite.JDBC");
        }
        catch (ClassNotFoundException e)
        {
            throw new RuntimeException(e);
        }
    }

    public ConnectionSource(String libraryFilePath)
    {
        // Create an ObjectPool that serves as the pool of connections.
        _connectionPool = new GenericObjectPool();
        _connectionPool.setMaxActive(5);

        // Create a ConnectionFactory that the pool will use to create Connections.
        ConnectionFactory connectionFactory = new DriverManagerConnectionFactory("jdbc:sqlite:/" + libraryFilePath, null);

        // Create a PoolableConnectionFactory, which wraps the "real" Connections created by the
        // ConnectionFactory with the classes that implement the pooling functionality.
        new PoolableConnectionFactory(connectionFactory,
                _connectionPool,
                null,
                "SELECT 1",  // validationQuery
                false, // defaultReadOnly
                true); // defaultAutoCommit

        // Create the PoolingDataSource
        _dataSource = new PoolingDataSource(_connectionPool);
    }

    public Connection getConnection() throws SQLException
    {
        return _dataSource.getConnection();
    }

    @Override
    public void close() throws Exception
    {
        _connectionPool.clear();
        try {_connectionPool.close();} catch(Exception ignored) {}
    }
}
