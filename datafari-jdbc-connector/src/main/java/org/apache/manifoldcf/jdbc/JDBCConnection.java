/* $Id: JDBCConnection.java 988245 2010-08-23 18:39:35Z kwright $ */

/**
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements. See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License. You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.apache.manifoldcf.jdbc;

import org.apache.manifoldcf.agents.interfaces.ServiceInterruption;
import org.apache.manifoldcf.core.database.RRow;
import org.apache.manifoldcf.core.database.RSet;
import org.apache.manifoldcf.core.interfaces.*;
import org.apache.manifoldcf.core.jdbcpool.WrappedConnection;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.ArrayList;
import java.util.Iterator;

/** This object describes a connection to a particular JDBC instance.
*/
public class JDBCConnection
{
  public static final String _rcsid = "@(#)$Id: JDBCConnection.java 988245 2010-08-23 18:39:35Z kwright $";

  protected String jdbcProvider = null;
  protected boolean useName;
  protected String driverString = null;
  protected String userName = null;
  protected String password = null;

  /** Constructor.
  */
  public JDBCConnection(String jdbcProvider, boolean useName, String host, String databaseName, String rawDriverString,
    String userName, String password, boolean sslTrustAll)
    throws ManifoldCFException
  {
    this.jdbcProvider = jdbcProvider;
    this.useName = useName;
    this.driverString = JDBCConnectionFactory.getJDBCDriverString(jdbcProvider, host, databaseName, rawDriverString, sslTrustAll);
    this.userName = userName;
    this.password = password;
  }

  protected static IDynamicResultRow readNextResultRowViaThread(ResultSet rs, ResultSetMetaData rsmd, String[] resultCols)
    throws ManifoldCFException, ServiceInterruption
  {
    NextResultRowThread t = new NextResultRowThread(rs,rsmd,resultCols);
    try
    {
      t.start();
      return t.finishUp();
    }
    catch (InterruptedException e)
    {
      t.interrupt();
      throw new ManifoldCFException("Interrupted: "+e.getMessage(),e,ManifoldCFException.INTERRUPTED);
    }
  }

  protected static class NextResultRowThread extends Thread
  {
    protected ResultSet rs;
    protected ResultSetMetaData rsmd;
    protected String[] resultCols;

    protected Throwable exception = null;
    protected IDynamicResultRow response = null;

    public NextResultRowThread(ResultSet rs, ResultSetMetaData rsmd, String[] resultCols)
    {
      super();
      setDaemon(true);
      this.rs = rs;
      this.rsmd = rsmd;
      this.resultCols = resultCols;
    }

    public void run()
    {
      try
      {
        response = readNextResultRow(rs,rsmd,resultCols);
      }
      catch (Throwable e)
      {
        this.exception = e;
      }
    }

    public IDynamicResultRow finishUp()
      throws ManifoldCFException, ServiceInterruption, InterruptedException
    {
      join();
      Throwable thr = exception;
      if (thr != null)
      {
        if (thr instanceof SQLException)
          throw new ManifoldCFException("Error fetching next JDBC result row: "+thr.getMessage(),thr);
        else if (thr instanceof ManifoldCFException)
          throw (ManifoldCFException)thr;
        else if (thr instanceof ServiceInterruption)
          throw (ServiceInterruption)thr;
        else if (thr instanceof RuntimeException)
          throw (RuntimeException)thr;
        else
          throw (Error)thr;
      }
      return response;
    }
    
  }

  protected static IDynamicResultRow readNextResultRow(ResultSet rs, ResultSetMetaData rsmd, String[] resultCols)
    throws ManifoldCFException, ServiceInterruption
  {
    try
    {
      if (rs.next())
      {
        return readResultRow(rs,rsmd,resultCols);
      }
      return null;
    }
    catch (SQLException e)
    {
      throw new ManifoldCFException("Result set error: "+e.getMessage(),e);
    }
  }

  protected static void closeResultset(ResultSet rs)
    throws ManifoldCFException, ServiceInterruption
  {
    try
    {
      rs.close();
    }
    catch (SQLException e)
    {
      throw new ManifoldCFException("Exception closing resultset: "+e.getMessage(),e);
    }
  }

  protected static void closeStmt(Statement stmt)
    throws ManifoldCFException, ServiceInterruption
  {
    try
    {
      stmt.close();
    }
    catch (SQLException e)
    {
      throw new ManifoldCFException("Exception closing statement: "+e.getMessage(),e);
    }
  }

  protected static void closePS(PreparedStatement ps)
    throws ManifoldCFException, ServiceInterruption
  {
    try
    {
      ps.close();
    }
    catch (SQLException e)
    {
      throw new ManifoldCFException("Exception closing statement: "+e.getMessage(),e);
    }
  }


  /** Test connection.
  */
  public void testConnection()
    throws ManifoldCFException, ServiceInterruption
  {
    TestConnectionThread t = new TestConnectionThread();
    try
    {
      t.start();
      t.finishUp();
    }
    catch (InterruptedException e)
    {
      t.interrupt();
      throw new ManifoldCFException("Interrupted: "+e.getMessage(),e,ManifoldCFException.INTERRUPTED);
    }
  }

  protected class TestConnectionThread extends Thread
  {
    protected Throwable exception = null;

    public TestConnectionThread()
    {
      super();
      setDaemon(true);
    }

    public void run()
    {
      try
      {
        WrappedConnection tempConnection = JDBCConnectionFactory.getConnection(jdbcProvider,driverString,userName,password);
        JDBCConnectionFactory.releaseConnection(tempConnection);
      }
      catch (Throwable e)
      {
        this.exception = e;
      }
    }

    public void finishUp()
      throws ManifoldCFException, ServiceInterruption, InterruptedException
    {
      join();
      Throwable thr = exception;
      if (thr != null)
      {
        if (thr instanceof SQLException)
          throw new ManifoldCFException("Error doing JDBC connection test: "+thr.getMessage(),thr);
        else if (thr instanceof ManifoldCFException)
          throw (ManifoldCFException)thr;
        else if (thr instanceof ServiceInterruption)
          throw (ServiceInterruption)thr;
        else if (thr instanceof RuntimeException)
          throw (RuntimeException)thr;
        else
          throw (Error)thr;
      }
    }
  }

  /** Execute query.
  */
  public IDynamicResultSet executeUncachedQuery(String query, ArrayList params, int maxResults)
    throws ManifoldCFException, ServiceInterruption
  {
    if (params == null)
      return new JDBCResultSet(query,maxResults);
    else
      return new JDBCPSResultSet(query,params,maxResults);
  }

  /** Execute operation.
  */
  public void executeOperation(String query, ArrayList params)
    throws ManifoldCFException, ServiceInterruption
  {
    ExecuteOperationThread t = new ExecuteOperationThread(query,params);
    try
    {
      t.start();
      t.finishUp();
    }
    catch (InterruptedException e)
    {
      t.interrupt();
      throw new ManifoldCFException("Interrupted: "+e.getMessage(),e,ManifoldCFException.INTERRUPTED);
    }
  }

  /** Read object as a string */
  public static String readAsString(Object o)
    throws ManifoldCFException
  {
    if (o instanceof BinaryInput)
    {
      // Convert this input to a string, since mssql can mess us up with the wrong column types here.
      BinaryInput bi = (BinaryInput)o;
      try
      {
        InputStream is = bi.getStream();
        try
        {
          InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
          StringBuilder sb = new StringBuilder();
          while (true)
          {
            int x = reader.read();
            if (x == -1)
              break;
            sb.append((char)x);
          }
          return sb.toString();
        }
        finally
        {
          is.close();
        }
      }
      catch (IOException e)
      {
        throw new ManifoldCFException(e.getMessage(),e);
      }
      finally
      {
        bi.doneWithStream();
      }
    }
    else if (o instanceof CharacterInput)
    {
      CharacterInput ci = (CharacterInput)o;
      try
      {
        Reader reader = ci.getStream();
        try
        {
          StringBuilder sb = new StringBuilder();
          while (true)
          {
            int x = reader.read();
            if (x == -1)
              break;
            sb.append((char)x);
          }
          return sb.toString();
        }
        finally
        {
          reader.close();
        }
      }
      catch (IOException e)
      {
        throw new ManifoldCFException(e.getMessage(),e);
      }
      finally
      {
        ci.doneWithStream();
      }
    }
    else
    {
      return o.toString();
    }
  }

  protected class ExecuteOperationThread extends Thread
  {
    protected String query;
    protected ArrayList params;

    protected Throwable exception = null;

    public ExecuteOperationThread(String query, ArrayList params)
    {
      super();
      setDaemon(true);
      this.query = query;
      this.params = params;
    }

    public void run()
    {
      try
      {
        WrappedConnection tempConnection = JDBCConnectionFactory.getConnection(jdbcProvider,driverString,userName,password);
        try
        {
          execute(tempConnection.getConnection(),query,params,false,0,useName);
        }
        finally
        {
          JDBCConnectionFactory.releaseConnection(tempConnection);
        }
      }
      catch (Throwable e)
      {
        this.exception = e;
      }
    }

    public void finishUp()
      throws ManifoldCFException, ServiceInterruption, InterruptedException
    {
      join();
      Throwable thr = exception;
      if (thr != null)
      {
        if (thr instanceof SQLException)
          throw new ManifoldCFException("Exception doing connector query '"+query+"': "+thr.getMessage(),thr);
        else if (thr instanceof ManifoldCFException)
          throw (ManifoldCFException)thr;
        else if (thr instanceof ServiceInterruption)
          throw (ServiceInterruption)thr;
        else if (thr instanceof RuntimeException)
          throw (RuntimeException)thr;
        else
          throw (Error)thr;
      }
    }
  }

  /** Run a query.  No caching is involved at all at this level.
  * @param query String the query string
  * @param maxResults is the maximum number of results to load: -1 if all
  * @param params ArrayList if params !=null, use preparedStatement
  */
  protected static IResultSet execute(Connection connection, String query, ArrayList params, boolean bResults, int maxResults, boolean useName)
    throws ManifoldCFException, ServiceInterruption
  {

    ResultSet rs;

    try
    {

      if (params==null)
      {
        // lightest statement type
        Statement stmt = connection.createStatement();
        try
        {
          stmt.execute(query);
          rs = stmt.getResultSet();
          try
          {
            // Suck data from resultset
            if (bResults)
              return getData(rs,maxResults,useName);
            return null;
          }
          finally
          {
            if (rs != null)
              rs.close();
          }
        }
        finally
        {
          stmt.close();
        }
      }
      else
      {
        PreparedStatement ps = connection.prepareStatement(query);
        try
        {
          loadPS(ps, params);

          if (bResults)
          {
            rs = ps.executeQuery();
            try
            {
              // Suck data from resultset
              return getData(rs,maxResults,useName);
            }
            finally
            {
              if (rs != null)
                rs.close();
            }
          }
          else
          {
            ps.executeUpdate();
            return null;
          }

        }
        finally
        {
          ps.close();
          cleanupParameters(params);
        }
      }

    }
    catch (ManifoldCFException e)
    {
      throw e;
    }
    catch (SQLException e)
    {
      throw new ManifoldCFException("Exception doing connector query '"+query+"': "+e.getMessage(),e);
    }
  }

  /** Read the current row from the resultset */
  protected static IDynamicResultRow readResultRow(ResultSet rs, ResultSetMetaData rsmd, String[] resultCols)
    throws ManifoldCFException, ServiceInterruption
  {
    try
    {
      Object value = null;
      RDynamicRow m = new RDynamicRow();

      // We have 'colcount' cols to look thru
      for (int i = 0; i < resultCols.length; i++)
      {
        String key = resultCols[i];
        // System.out.println("Key = "+key);
        int colnum = findColumn(rs,key);
        if (colnum > -1)
        {
          if (isBinaryData(rsmd,colnum))
          {
            InputStream bis = rs.getBinaryStream(colnum);
            if (bis != null)
            {
              try
              {
                value = new TempFileInput(bis);
              }
              catch (IOException e)
              {
                handleIOException(e,"reading binary data");
              }
            }
          }
          else if (isBLOB(rsmd,colnum))
          {
            // System.out.println("It's a blob!");
            Blob blob = getBLOB(rs,colnum);
            // Create a tempfileinput object!
            // Cleanup should happen by the user of the resultset.
            // System.out.println(" Blob length = "+Long.toString(blob.length()));
            if (blob != null)
            {
              try
              {
                value = new TempFileInput(blob.getBinaryStream(),blob.length());
              }
              catch (IOException e)
              {
                handleIOException(e,"reading blob");
              }
            }
          }
          else if (isCLOB(rsmd,colnum))
          {
            Clob clob = getCLOB(rs,colnum);
            if (clob != null)
            {
              try
              {
                value = new TempFileCharacterInput(clob.getCharacterStream(),clob.length());
              }
              catch (IOException e)
              {
                handleIOException(e,"reading clob");
              }
            }
          }
          else
          {
            // System.out.println("It's not a blob");
            value = getObject(rs,rsmd,colnum);
          }
        }
        if (value != null)
          m.put(key, value);
      }
      return m;

    }
    catch (SQLException e)
    {
      throw new ManifoldCFException("Resultset error: "+e.getMessage(),e);
    }
  }

  protected static void handleIOException(IOException e, String context)
    throws ManifoldCFException
  {
    if (e instanceof InterruptedIOException)
      throw new ManifoldCFException(e.getMessage(),e,ManifoldCFException.INTERRUPTED);
    throw new ManifoldCFException("IO exception while "+context+": "+e.getMessage(),e);
  }
  
  protected static String[] readColumnNames(ResultSetMetaData rsmd, boolean useName)
    throws ManifoldCFException, ServiceInterruption
  {
    try
    {
      String[] resultCols;
      if (rsmd != null)
      {
        int colcount = rsmd.getColumnCount();
        resultCols = new String[colcount];
        for (int i = 0; i < colcount; i++)
        {
          String name;
          if (useName)
            name = rsmd.getColumnName(i+1);
          else
            name = rsmd.getColumnLabel(i+1);
          resultCols[i] = name;
        }
      }
      else
        resultCols = new String[0];
      return resultCols;
    }
    catch (SQLException e)
    {
      throw new ManifoldCFException("Sql exception reading column names: "+e.getMessage(),e);
    }
  }

  // Read data from a resultset
  protected static IResultSet getData(ResultSet rs, int maxResults, boolean useName)
    throws ManifoldCFException, ServiceInterruption
  {
    try
    {
      RSet results = new RSet();  // might be empty but not an error

      if (rs != null)
      {
        // Optionally we're going to suck the data
        // out of the db and return it in a
        // readonly structure
        ResultSetMetaData rsmd = rs.getMetaData();
        String[] resultCols = readColumnNames(rsmd, useName);
        if (resultCols.length == 0)
        {
          // This is an error situation; if a result with no columns is
          // necessary, bResults must be false!!!
          throw new ManifoldCFException("Empty query, no columns returned",ManifoldCFException.GENERAL_ERROR);
        }

        while (rs.next() && (maxResults == -1 || maxResults > 0))
        {
          IResultRow m = readResultRow(rs,rsmd,resultCols);
          if (maxResults != -1)
            maxResults--;
          results.addRow(m);
        }
      }
      return results;
    }
    catch (SQLException e)
    {
      throw new ManifoldCFException("Resultset error: "+e.getMessage(),e);
    }
  }

  // pass params to preparedStatement
  protected static void loadPS(PreparedStatement ps, ArrayList data)
    throws SQLException, ManifoldCFException
  {
    if (data!=null)
    {
      for (int i = 0; i < data.size(); i++)
      {
        // If the input type is a string, then set it as such.
        // Otherwise, if it's an input stream, we make a blob out of it.
        Object x = data.get(i);
        if (x instanceof String)
        {
          String value = (String)x;
          // letting database do lame conversion!
          ps.setString(i+1, value);
        }
        else if (x instanceof BinaryInput)
        {
          BinaryInput value = (BinaryInput)x;
          ps.setBinaryStream(i+1,value.getStream(),value.getLength());
          // Hopefully with the introduction of CharacterInput below, this hackery is no longer needed.
          // System.out.println("Blob length on write = "+Long.toString(value.getLength()));
          // The oracle driver does a binary conversion to base 64 when writing data
          // into a clob column using a binary stream operator.  Since at this
          // point there is no way to distinguish the two, and since our tests use CLOB,
          // this code doesn't work for them.
          // So, for now, use the ascii stream method.
          //ps.setAsciiStream(i+1,value.getStream(),value.getLength());
        }
        else if (x instanceof CharacterInput)
        {
          CharacterInput value = (CharacterInput)x;
          ps.setCharacterStream(i+1,value.getStream(),value.getCharacterLength());
        }
        else if (x instanceof java.util.Date)
        {
          ps.setDate(i+1,new java.sql.Date(((java.util.Date)x).getTime()));
        }
        else if (x instanceof Long)
        {
          ps.setLong(i+1,((Long)x).longValue());
        }
        else if (x instanceof TimeMarker)
        {
          ps.setTimestamp(i+1,new Timestamp(((Long)x).longValue()));
        }
        else if (x instanceof Double)
        {
          ps.setDouble(i+1,((Double)x).doubleValue());
        }
        else if (x instanceof Integer)
        {
          ps.setInt(i+1,((Integer)x).intValue());
        }
        else if (x instanceof Float)
        {
          ps.setFloat(i+1,((Float)x).floatValue());
        }
        else
          throw new ManifoldCFException("Unknown data type: "+x.getClass().getName());
      }
    }
  }

  /** Permanently discard database object.
  */
  protected static void discardDatabaseObject(Object x)
    throws ManifoldCFException
  {
    if (x instanceof PersistentDatabaseObject)
    {
      PersistentDatabaseObject value = (PersistentDatabaseObject)x;
      value.discard();
    }
  }
  
  /** Call this method on every parameter or result object, when we're done with it, if it's possible that the object is a BLOB
  * or CLOB.
  */
  protected static void cleanupDatabaseObject(Object x)
    throws ManifoldCFException
  {
    if (x instanceof PersistentDatabaseObject)
    {
      PersistentDatabaseObject value = (PersistentDatabaseObject)x;
      value.doneWithStream();
    }
  }
  
  /** Clean up parameters after query has been triggered.
  */
  protected static void cleanupParameters(ArrayList data)
    throws ManifoldCFException
  {
    if (data != null)
    {
      for (Object x : data)
      {
        cleanupDatabaseObject(x);
      }
    }
  }

  protected static int findColumn(ResultSet rs, String name)
    throws ManifoldCFException, ServiceInterruption
  {
    try
    {
      return rs.findColumn(name);
    }
    catch (SQLException e)
    {
      return -1;
    }
  }

  protected static Blob getBLOB(ResultSet rs, int col)
    throws ManifoldCFException, ServiceInterruption
  {
    try
    {
      return rs.getBlob(col);
    }
    catch (SQLException sqle)
    {
      throw new ManifoldCFException("Error in getBlob("+col+"): "+sqle.getMessage(),sqle,ManifoldCFException.DATABASE_ERROR);
    }
  }

  protected static Clob getCLOB(ResultSet rs, int col)
    throws ManifoldCFException, ServiceInterruption
  {
    try
    {
      return rs.getClob(col);
    }
    catch (SQLException sqle)
    {
      throw new ManifoldCFException("Error in getClob("+col+"): "+sqle.getMessage(),sqle,ManifoldCFException.DATABASE_ERROR);
    }
  }

  protected static boolean isBLOB(ResultSetMetaData rsmd, int col)
    throws ManifoldCFException, ServiceInterruption
  {
    try
    {
      int type = rsmd.getColumnType(col);
      return (type == Types.BLOB);
    }
    catch (SQLException sqle)
    {
      throw new ManifoldCFException("Error in isBlob("+col+"): "+sqle.getMessage(),sqle,ManifoldCFException.DATABASE_ERROR);
    }
  }

  protected static boolean isBinaryData(ResultSetMetaData rsmd, int col)
    throws ManifoldCFException, ServiceInterruption
  {
    try
    {
      int type = rsmd.getColumnType(col);
      return (type == Types.VARBINARY ||
        type == Types.BINARY || type == Types.LONGVARBINARY);
    }
    catch (SQLException sqle)
    {
      throw new ManifoldCFException("Error in isBinaryData("+col+"): "+sqle.getMessage(),sqle,ManifoldCFException.DATABASE_ERROR);
    }
  }

  protected static boolean isCLOB(ResultSetMetaData rsmd, int col)
    throws ManifoldCFException, ServiceInterruption
  {
    try
    {
      int type = rsmd.getColumnType(col);
      return (type == Types.CLOB || type == Types.LONGVARCHAR);
    }
    catch (SQLException sqle)
    {
      throw new ManifoldCFException("Error in isClob("+col+"): "+sqle.getMessage(),sqle,ManifoldCFException.DATABASE_ERROR);
    }
  }

  protected static Object getObject(ResultSet rs, ResultSetMetaData rsmd, int col)
    throws ManifoldCFException, ServiceInterruption
  {
    Object result = null;

    try
    {
      Timestamp timestamp;
      java.sql.Date date;
      Clob clob;
      String resultString;

      switch (rsmd.getColumnType(col))
      {
      case Types.CHAR :
        if ((resultString = rs.getString(col)) != null)
        {
          if (rsmd.getColumnDisplaySize(col) < resultString.length())
          {
            result = resultString.substring(0,rsmd.getColumnDisplaySize(col));
          }
          else
            result = resultString;
        }
        break;
      case Types.CLOB :
        if ((clob = rs.getClob(col)) != null)
        {
          result = clob.getSubString(1, (int) clob.length());
        }
        break;

      case Types.BIGINT :
        long l = rs.getLong(col);
        if (!rs.wasNull())
          result = new Long(l);
        break;

      case Types.INTEGER :
        int i = rs.getInt(col);
        if (!rs.wasNull())
          result = new Integer(i);
        break;

      case Types.REAL :
      case Types.FLOAT :
        float f = rs.getFloat(col);
        if (!rs.wasNull())
          result = new Float(f);
        break;

      case Types.DOUBLE :
        double d = rs.getDouble(col);
        if (!rs.wasNull())
          result = new Double(d);
        break;

      case Types.DATE :
        if ((date = rs.getDate(col)) != null)
        {
          result = new java.util.Date(date.getTime());
        }
        break;

      case Types.TIMESTAMP :
        if ((timestamp = rs.getTimestamp(col)) != null)
        {
          result = new TimeMarker(timestamp.getTime());
        }
        break;

      case Types.BLOB:
      case Types.VARBINARY:
      case Types.BINARY:
      case Types.LONGVARBINARY:
        throw new ManifoldCFException("Binary type is not a string, column = " + col,ManifoldCFException.GENERAL_ERROR);
        //break

      default :
        result = rs.getString(col);
        break;
      }
      if (rs.wasNull())
      {
        result = null;
      }
    }
    catch (SQLException e)
    {
      throw new ManifoldCFException("Exception in getString(): "+e.getMessage(),e,ManifoldCFException.DATABASE_ERROR);
    }
    return result;
  }

  protected class JDBCResultSet implements IDynamicResultSet
  {
    protected WrappedConnection connection;
    protected Statement stmt;
    protected ResultSet rs;
    protected ResultSetMetaData rsmd;
    protected String[] resultCols;
    protected int maxResults;

    /** Constructor */
    public JDBCResultSet(String query, int maxResults)
      throws ManifoldCFException, ServiceInterruption
    {
      this.maxResults = maxResults;
      StatementQueryThread t = new StatementQueryThread(query);
      try
      {
        t.start();
        t.finishUp();
        connection = t.getConnection();
        stmt = t.getStatement();
        rs = t.getResultSet();
        rsmd = t.getResultSetMetaData();
        resultCols = t.getColumnNames();
      }
      catch (InterruptedException e)
      {
        t.interrupt();
        throw new ManifoldCFException("Interrupted: "+e.getMessage(),e,ManifoldCFException.INTERRUPTED);
      }
    }

    /** Get the next row from the resultset.
    *@return the immutable row description, or null if there is no such row.
    */
    public IDynamicResultRow getNextRow()
      throws ManifoldCFException, ServiceInterruption
    {
      if (maxResults == -1 || maxResults > 0)
      {
        IDynamicResultRow row = readNextResultRowViaThread(rs,rsmd,resultCols);
        if (row != null && maxResults != -1)
          maxResults--;
        return row;
      }
      return null;
    }

    /** Close this resultset.
    */
    public void close()
      throws ManifoldCFException, ServiceInterruption
    {
      ManifoldCFException rval = null;
      Error error = null;
      RuntimeException rtException = null;
      if (rs != null)
      {
        try
        {
          closeResultset(rs);
        }
        catch (ManifoldCFException e)
        {
          if (rval == null || e.getErrorCode() == ManifoldCFException.INTERRUPTED)
            rval = e;
        }
        catch (Error e)
        {
          error = e;
        }
        catch (RuntimeException e)
        {
          rtException = e;
        }
        finally
        {
          rs = null;
        }
      }
      if (stmt != null)
      {
        try
        {
          closeStmt(stmt);
        }
        catch (ManifoldCFException e)
        {
          if (rval == null || e.getErrorCode() == ManifoldCFException.INTERRUPTED)
            rval = e;
        }
        catch (Error e)
        {
          error = e;
        }
        catch (RuntimeException e)
        {
          rtException = e;
        }
        finally
        {
          stmt = null;
        }
      }
      if (connection != null)
      {
        try
        {
          JDBCConnectionFactory.releaseConnection(connection);
        }
        catch (Error e)
        {
          error = e;
        }
        catch (RuntimeException e)
        {
          rtException = e;
        }
        finally
        {
          connection = null;
        }
      }
      if (error != null)
        throw error;
      if (rtException != null)
        throw rtException;
      if (rval != null)
        throw rval;
    }

  }

  protected class StatementQueryThread extends Thread
  {
    protected String query;

    protected Throwable exception = null;
    protected WrappedConnection connection = null;
    protected Statement stmt = null;
    protected ResultSet rs = null;
    protected ResultSetMetaData rsmd = null;
    protected String[] resultCols = null;

    public StatementQueryThread(String query)
    {
      super();
      setDaemon(true);
      this.query = query;
    }

    public void run()
    {
      try
      {
        connection = JDBCConnectionFactory.getConnection(jdbcProvider,driverString,userName,password);
        // lightest statement type
        stmt = connection.getConnection().createStatement();
        stmt.execute(query);
        rs = stmt.getResultSet();
        rsmd = rs.getMetaData();
        resultCols = readColumnNames(rsmd,useName);
      }
      catch (Throwable e)
      {
        this.exception = e;
        if (rs != null)
        {
          try
          {
            closeResultset(rs);
          }
          catch (ManifoldCFException e2)
          {
            if (e2.getErrorCode() == ManifoldCFException.INTERRUPTED)
              this.exception = e2;
            // Ignore
          }
          catch (Throwable e2)
          {
            // We already have an exception to report.
            // Eat any other exceptions from closing
          }
          finally
          {
            rs = null;
          }
        }
        if (stmt != null)
        {
          try
          {
            closeStmt(stmt);
          }
          catch (ManifoldCFException e2)
          {
            if (e2.getErrorCode() == ManifoldCFException.INTERRUPTED)
              this.exception = e2;
            // Ignore
          }
          catch (Throwable e2)
          {
            // We already have an exception to report.
            // Eat any other exceptions from closing statements
          }
          finally
          {
            stmt = null;
          }
        }
        if (connection != null)
        {
          JDBCConnectionFactory.releaseConnection(connection);
          connection = null;
        }
      }
    }

    public void finishUp()
      throws ManifoldCFException, ServiceInterruption, InterruptedException
    {
      join();
      Throwable thr = exception;
      if (thr != null)
      {
        if (thr instanceof SQLException)
          throw new ManifoldCFException("Exception doing connector query '"+query+"': "+thr.getMessage(),thr);
        else if (thr instanceof ManifoldCFException)
          throw (ManifoldCFException)thr;
        else if (thr instanceof ServiceInterruption)
          throw (ServiceInterruption)thr;
        else if (thr instanceof RuntimeException)
          throw (RuntimeException)thr;
        else
          throw (Error)thr;
      }
    }

    public WrappedConnection getConnection()
    {
      return connection;
    }

    public Statement getStatement()
    {
      return stmt;
    }

    public ResultSet getResultSet()
    {
      return rs;
    }

    public ResultSetMetaData getResultSetMetaData()
    {
      return rsmd;
    }

    public String[] getColumnNames()
    {
      return resultCols;
    }
  }

  protected class JDBCPSResultSet implements IDynamicResultSet
  {
    protected WrappedConnection connection;
    protected PreparedStatement ps;
    protected ResultSet rs;
    protected ResultSetMetaData rsmd;
    protected String[] resultCols;
    protected int maxResults;
    protected ArrayList params;

    /** Constructor */
    public JDBCPSResultSet(String query, ArrayList params, int maxResults)
      throws ManifoldCFException, ServiceInterruption
    {
      this.maxResults = maxResults;
      this.params = params;
      PreparedStatementQueryThread t = new PreparedStatementQueryThread(query,params);
      try
      {
        t.start();
        t.finishUp();
        connection = t.getConnection();
        ps = t.getPreparedStatement();
        rs = t.getResultSet();
        rsmd = t.getResultSetMetaData();
        resultCols = t.getColumnNames();
      }
      catch (InterruptedException e)
      {
        cleanupParameters(params);
        t.interrupt();
        throw new ManifoldCFException("Interrupted: "+e.getMessage(),e,ManifoldCFException.INTERRUPTED);
      }
    }

    /** Get the next row from the resultset.
    *@return the immutable row description, or null if there is no such row.
    */
    public IDynamicResultRow getNextRow()
      throws ManifoldCFException, ServiceInterruption
    {
      if (maxResults == -1 || maxResults > 0)
      {
        IDynamicResultRow row = readNextResultRowViaThread(rs,rsmd,resultCols);
        if (row != null && maxResults != -1)
          maxResults--;
        return row;
      }
      return null;
    }

    /** Close this resultset.
    */
    public void close()
      throws ManifoldCFException, ServiceInterruption
    {
      ManifoldCFException rval = null;
      Error error = null;
      RuntimeException rtException = null;
      if (rs != null)
      {
        try
        {
          closeResultset(rs);
        }
        catch (ServiceInterruption e)
        {
        }
        catch (ManifoldCFException e)
        {
          if (rval == null || e.getErrorCode() == ManifoldCFException.INTERRUPTED)
            rval = e;
        }
        catch (Error e)
        {
          error = e;
        }
        catch (RuntimeException e)
        {
          rtException = e;
        }
        finally
        {
          rs = null;
        }
      }
      if (ps != null)
      {
        try
        {
          closePS(ps);
        }
        catch (ServiceInterruption e)
        {
        }
        catch (ManifoldCFException e)
        {
          if (rval == null || e.getErrorCode() == ManifoldCFException.INTERRUPTED)
            rval = e;
        }
        catch (Error e)
        {
          error = e;
        }
        catch (RuntimeException e)
        {
          rtException = e;
        }
        finally
        {
          ps = null;
        }
      }
      if (connection != null)
      {
        try
        {
          JDBCConnectionFactory.releaseConnection(connection);
        }
        catch (Error e)
        {
          error = e;
        }
        catch (RuntimeException e)
        {
          rtException = e;
        }
        finally
        {
          connection = null;
        }
      }
      if (params != null)
      {
        try
        {
          cleanupParameters(params);
        }
        catch (ManifoldCFException e)
        {
          if (rval == null || e.getErrorCode() == ManifoldCFException.INTERRUPTED)
            rval = e;
        }
        catch (Error e)
        {
          error = e;
        }
        catch (RuntimeException e)
        {
          rtException = e;
        }
        finally
        {
          params = null;
        }
      }
      if (error != null)
        throw error;
      if (rtException != null)
        throw rtException;
      if (rval != null)
        throw rval;

    }

  }

  protected class PreparedStatementQueryThread extends Thread
  {
    protected ArrayList params;
    protected String query;

    protected WrappedConnection connection = null;
    protected Throwable exception = null;
    protected PreparedStatement ps = null;
    protected ResultSet rs = null;
    protected ResultSetMetaData rsmd = null;
    protected String[] resultCols = null;

    public PreparedStatementQueryThread(String query, ArrayList params)
    {
      super();
      setDaemon(true);
      this.query = query;
      this.params = params;
    }

    public void run()
    {
      try
      {
        connection = JDBCConnectionFactory.getConnection(jdbcProvider,driverString,userName,password);
        ps = connection.getConnection().prepareStatement(query);
        loadPS(ps, params);
        rs = ps.executeQuery();
        rsmd = rs.getMetaData();
        resultCols = readColumnNames(rsmd,useName);
      }
      catch (Throwable e)
      {
        this.exception = e;
        if (rs != null)
        {
          try
          {
            closeResultset(rs);
          }
          catch (ManifoldCFException e2)
          {
            if (e2.getErrorCode() == ManifoldCFException.INTERRUPTED)
              this.exception = e2;
          }
          catch (Throwable e2)
          {
          }
          finally
          {
            rs = null;
          }
        }
        if (ps != null)
        {
          try
          {
            closePS(ps);
          }
          catch (ManifoldCFException e2)
          {
            if (e2.getErrorCode() == ManifoldCFException.INTERRUPTED)
              this.exception = e2;
          }
          catch (Throwable e2)
          {
          }
          finally
          {
            ps = null;
          }
        }
        if (connection != null)
        {
          JDBCConnectionFactory.releaseConnection(connection);
          connection = null;
        }
      }
    }

    public void finishUp()
      throws ManifoldCFException, ServiceInterruption, InterruptedException
    {
      join();
      Throwable thr = exception;
      if (thr != null)
      {
        // Cleanup of parameters happens even if exception doing query
        cleanupParameters(params);
        if (thr instanceof SQLException)
          throw new ManifoldCFException("Exception doing connector query '"+query+"': "+thr.getMessage(),thr);
        else if (thr instanceof ManifoldCFException)
          throw (ManifoldCFException)thr;
        else if (thr instanceof ServiceInterruption)
          throw (ServiceInterruption)thr;
        else if (thr instanceof RuntimeException)
          throw (RuntimeException)thr;
        else
          throw (Error)thr;
      }
    }

    public WrappedConnection getConnection()
    {
      return connection;
    }

    public PreparedStatement getPreparedStatement()
    {
      return ps;
    }

    public ResultSet getResultSet()
    {
      return rs;
    }

    public ResultSetMetaData getResultSetMetaData()
    {
      return rsmd;
    }

    public String[] getColumnNames()
    {
      return resultCols;
    }
  }

  /** Dynamic result row implementation */
  protected static class RDynamicRow extends RRow implements IDynamicResultRow
  {
    public RDynamicRow()
    {
      super();
    }
    
    /** Close this resultrow.
    */
    public void close()
      throws ManifoldCFException
    {
      // Discard everything permanently from the row
      Iterator<String> columns = getColumns();
      while (columns.hasNext())
      {
        String column = columns.next();
        Object o = getValue(column);
        discardDatabaseObject(o);
      }
    }

  }
  
}
