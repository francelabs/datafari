<%@ include file="adminHeaders.jsp" %>

<%

/* $Id: execute.jsp 1620301 2014-08-25 12:13:35Z kwright $ */

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
%>

<%
	// The purpose of this jsp is to execute commands, and then dispatch to the right form or display page using
	// jsp:forward.
	// Note that, for errors, the logic must involve another step.  That step involves dispatching to the error
	// page, and passing in the error and the name of the page to go to when the error is accepted by the user.
	// This page must therefore NEVER cause text to be streamed, and must ALWAYS pass information to the page
	// it forwards to using arguments and thread context only.
	//
	// The parameters that the page expects depend on the operation.  The operation is passed as the parameter "op".
	// For many operations there is a secondary parameter determining the type of entity being operated on.
	// This is passed in the parameter "type".
	//
	// Dispatches are handled within the logic for each operation type.  This includes dispatches to the error page.
	// The error page receives the following argument values: "text" (the error text), and "target" (the page to go to
	// on confirmation).
	//
	// If no operation at all is present, or if no dispatch occurs, then this code simply dispatches to the home admin page.

	try
	{
		// Make a few things we will need
		// Get the job manager handle
		IJobManager manager = JobManagerFactory.make(threadContext);
		IAuthorityGroupManager authGroupManager = AuthorityGroupManagerFactory.make(threadContext);
		IRepositoryConnectionManager connManager = RepositoryConnectionManagerFactory.make(threadContext);
		IAuthorityConnectionManager authConnManager = AuthorityConnectionManagerFactory.make(threadContext);
		IMappingConnectionManager mappingConnManager = MappingConnectionManagerFactory.make(threadContext);
		IOutputConnectionManager outputManager = OutputConnectionManagerFactory.make(threadContext);
		ITransformationConnectionManager transformationManager = TransformationConnectionManagerFactory.make(threadContext);
		
		IOutputConnectorPool outputConnectorPool = OutputConnectorPoolFactory.make(threadContext);
		ITransformationConnectorPool transformationConnectorPool = TransformationConnectorPoolFactory.make(threadContext);
		IRepositoryConnectorPool repositoryConnectorPool = RepositoryConnectorPoolFactory.make(threadContext);
		
		String type = variableContext.getParameter("type");
		String op = variableContext.getParameter("op");
		if (type != null && op != null && type.equals("connection"))
		{
			// -- Connection editing operations --
			if (op.equals("Save") || op.equals("Continue"))
			{
				try
				{
					// Set up a connection object that is a merge of an existing connection object plus what was posted.
					IRepositoryConnection connection = null;
					boolean isNew = true;
					String x = variableContext.getParameter("isnewconnection");
					if (x != null)
						isNew = x.equals("true");

					String connectionName = variableContext.getParameter("connname");
					// If the connectionname is not null, load the connection description and prepopulate everything with what comes from it.
					if (connectionName != null && connectionName.length() > 0 && !isNew)
					{
						connection = connManager.load(connectionName);
					}
					
					if (connection == null)
					{
						connection = connManager.create();
						if (connectionName != null && connectionName.length() > 0)
							connection.setName(connectionName);
					}
					
					// Fill in connection object from posted data
					connection.setIsNew(isNew);
					x = variableContext.getParameter("description");
					if (x != null)
						connection.setDescription(x);
					x = variableContext.getParameter("classname");
					if (x != null)
						connection.setClassName(x);
					x = variableContext.getParameter("authorityname");
					if (x != null && x.length() > 0)
					{
						if (x.equals("_none_"))
							connection.setACLAuthority(null);
						else
							connection.setACLAuthority(x);
					}
					x = variableContext.getParameter("maxconnections");
					if (x != null && x.length() > 0)
						connection.setMaxConnections(Integer.parseInt(x));

					// Gather and save throttles
					x = variableContext.getParameter("throttlecount");
					if (x != null)
					{
						int throttleCount = Integer.parseInt(x);
						connection.clearThrottleValues();
						int j = 0;
						while (j < throttleCount)
						{
							String regexp = variableContext.getParameter("throttle_"+Integer.toString(j));
							String desc = variableContext.getParameter("throttledesc_"+Integer.toString(j));
							if (desc == null)
								desc = "";
							String value = variableContext.getParameter("throttlevalue_"+Integer.toString(j));
							connection.addThrottleValue(regexp,desc,(float)(((double)new Long(value).longValue())/(double)(60000.0)));
							j++;
						}
						x = variableContext.getParameter("throttleop");
						if (x != null && x.equals("Delete"))
						{
							// Delete an item from the throttles list
							x = variableContext.getParameter("throttlenumber");
							String regexp = variableContext.getParameter("throttle_"+x);
							connection.deleteThrottleValue(regexp);
						}
						else if (x != null && x.equals("Add"))
						{
							// Add an item to the throttles list
							String regexp = variableContext.getParameter("throttle");
							String desc = variableContext.getParameter("throttledesc");
							if (desc == null)
								desc = "";
							Long value = new Long(variableContext.getParameter("throttlevalue"));
							connection.addThrottleValue(regexp,desc,(float)(((double)value.longValue())/(double)(60000.0)));
						}
					}

					String error = RepositoryConnectorFactory.processConfigurationPost(threadContext,connection.getClassName(),variableContext,pageContext.getRequest().getLocale(),connection.getConfigParams());
						
					if (error != null)
					{
						variableContext.setParameter("text",error);
						variableContext.setParameter("target","listconnections.jsp");
%>
						<jsp:forward page="error.jsp"/>
<%
					}

					if (op.equals("Continue"))
					{
						threadContext.save("ConnectionObject",connection);
%>
						<jsp:forward page="editconnection.jsp"/>
<%
					}
					else if (op.equals("Save"))
					{
						connManager.save(connection);
						variableContext.setParameter("connname",connectionName);
%>
						<jsp:forward page="viewconnection.jsp"/>
<%
					}
				}
				catch (ManifoldCFException e)
				{
					e.printStackTrace();
					variableContext.setParameter("text",e.getMessage());
					variableContext.setParameter("target","listconnections.jsp");
%>
					<jsp:forward page="error.jsp"/>
<%
				}
			}
			else if (op.equals("Delete"))
			{
				try
				{
					String connectionName = variableContext.getParameter("connname");
					if (connectionName == null)
						throw new ManifoldCFException("Missing connection parameter");
					connManager.delete(connectionName);
%>
					<jsp:forward page="listconnections.jsp"/>
<%
				}
				catch (ManifoldCFException e)
				{
					e.printStackTrace();
					variableContext.setParameter("text",e.getMessage());
					variableContext.setParameter("target","listconnections.jsp");
%>
					<jsp:forward page="error.jsp"/>
<%
				}
			}
			else if (op.equals("Cancel"))
			{
%>
				<jsp:forward page="listconnections.jsp"/>
<%
			}
			else if (op.equals("ClearHistory"))
			{
				try
				{
					String connectionName = variableContext.getParameter("connname");
					if (connectionName == null)
						throw new ManifoldCFException("Missing connection parameter");
					connManager.cleanUpHistoryData(connectionName);
%>
					<jsp:forward page="listconnections.jsp"/>
<%
				}
				catch (ManifoldCFException e)
				{
					e.printStackTrace();
					variableContext.setParameter("text",e.getMessage());
					variableContext.setParameter("target","listconnections.jsp");
%>
					<jsp:forward page="error.jsp"/>
<%
				}
			}
			else
			{
				// Error
				variableContext.setParameter("text","Illegal parameter to connection execution page");
				variableContext.setParameter("target","listconnections.jsp");
%>
				<jsp:forward page="error.jsp"/>
<%
			}
		}
		else if (type != null && op != null && type.equals("group"))
		{
			// -- Group editing operations --
			if (op.equals("Save") || op.equals("Continue"))
			{
				try
				{
					// Set up a connection object that is a merge of an existing connection object plus what was posted.
					IAuthorityGroup group = null;
					boolean isNew = true;
					String x = variableContext.getParameter("isnewconnection");
					if (x != null)
						isNew = x.equals("true");

					String groupName = variableContext.getParameter("groupname");
					// If the groupname is not null, load the group and prepopulate everything with what comes from it.
					if (groupName != null && groupName.length() > 0 && !isNew)
					{
						group = authGroupManager.load(groupName);
					}
					
					if (group == null)
					{
						group = authGroupManager.create();
						if (groupName != null && groupName.length() > 0)
							group.setName(groupName);
					}

					// Gather all the data from the form.
					group.setIsNew(isNew);
					x = variableContext.getParameter("description");
					if (x != null)
						group.setDescription(x);

					if (op.equals("Continue"))
					{
						threadContext.save("GroupObject",group);
%>
						<jsp:forward page="editgroup.jsp"/>
<%
					}
					else if (op.equals("Save"))
					{
						authGroupManager.save(group);
						variableContext.setParameter("groupname",groupName);
%>
						<jsp:forward page="viewgroup.jsp"/>
<%
					}
				}
				catch (ManifoldCFException e)
				{
					e.printStackTrace();
					variableContext.setParameter("text",e.getMessage());
					variableContext.setParameter("target","listgroups.jsp");
%>
					<jsp:forward page="error.jsp"/>
<%
				}
			}
			else if (op.equals("Delete"))
			{
				try
				{
					String groupName = variableContext.getParameter("groupname");
					if (groupName == null)
						throw new ManifoldCFException("Missing group name parameter");
					authGroupManager.delete(groupName);
%>
					<jsp:forward page="listgroups.jsp"/>
<%
				}
				catch (ManifoldCFException e)
				{
					e.printStackTrace();
					variableContext.setParameter("text",e.getMessage());
					variableContext.setParameter("target","listgroups.jsp");
%>
					<jsp:forward page="error.jsp"/>
<%
				}
			}
			else if (op.equals("Cancel"))
			{
%>
				<jsp:forward page="listgroups.jsp"/>
<%
			}
			else
			{
				// Error
				variableContext.setParameter("text","Illegal parameter to authority group execution page");
				variableContext.setParameter("target","listgroups.jsp");
%>
				<jsp:forward page="error.jsp"/>
<%
			}

		}
		else if (type != null && op != null && type.equals("authority"))
		{
			// -- Authority editing operations --
			if (op.equals("Save") || op.equals("Continue"))
			{
				try
				{
					// Set up a connection object that is a merge of an existing connection object plus what was posted.
					IAuthorityConnection connection = null;
					boolean isNew = true;
					String x = variableContext.getParameter("isnewconnection");
					if (x != null)
						isNew = x.equals("true");

					String connectionName = variableContext.getParameter("connname");
					// If the connectionname is not null, load the connection description and prepopulate everything with what comes from it.
					if (connectionName != null && connectionName.length() > 0 && !isNew)
					{
						connection = authConnManager.load(connectionName);
					}
					
					if (connection == null)
					{
						connection = authConnManager.create();
						if (connectionName != null && connectionName.length() > 0)
							connection.setName(connectionName);
					}

					// Gather all the data from the form.
					connection.setIsNew(isNew);
					x = variableContext.getParameter("description");
					if (x != null)
						connection.setDescription(x);
					x = variableContext.getParameter("classname");
					if (x != null)
						connection.setClassName(x);
					x = variableContext.getParameter("maxconnections");
					if (x != null)
						connection.setMaxConnections(Integer.parseInt(x));
					x = variableContext.getParameter("prerequisites_present");
					if (x != null && x.equals("true"))
					{
						String y = variableContext.getParameter("prerequisites");
						if (y != null && y.length() == 0)
							y = null;
						connection.setPrerequisiteMapping(y);
					}
					x = variableContext.getParameter("authdomain");
					if (x != null)
						connection.setAuthDomain(x);
					x = variableContext.getParameter("authoritygroup");
					if (x != null)
						connection.setAuthGroup(x);

					String error = AuthorityConnectorFactory.processConfigurationPost(threadContext,connection.getClassName(),variableContext,pageContext.getRequest().getLocale(),connection.getConfigParams());
					
					if (error != null)
					{
						variableContext.setParameter("text",error);
						variableContext.setParameter("target","listauthorities.jsp");
%>
						<jsp:forward page="error.jsp"/>
<%
					}
					
					if (op.equals("Continue"))
					{
						threadContext.save("ConnectionObject",connection);
%>
						<jsp:forward page="editauthority.jsp"/>
<%
					}
					else if (op.equals("Save"))
					{
						authConnManager.save(connection);
						variableContext.setParameter("connname",connectionName);
%>
						<jsp:forward page="viewauthority.jsp"/>
<%
					}
				}
				catch (ManifoldCFException e)
				{
					e.printStackTrace();
					variableContext.setParameter("text",e.getMessage());
					variableContext.setParameter("target","listauthorities.jsp");
%>
					<jsp:forward page="error.jsp"/>
<%
				}
			}
			else if (op.equals("Delete"))
			{
				try
				{
					String connectionName = variableContext.getParameter("connname");
					if (connectionName == null)
						throw new ManifoldCFException("Missing connection parameter");
					authConnManager.delete(connectionName);
%>
					<jsp:forward page="listauthorities.jsp"/>
<%
				}
				catch (ManifoldCFException e)
				{
					e.printStackTrace();
					variableContext.setParameter("text",e.getMessage());
					variableContext.setParameter("target","listauthorities.jsp");
%>
					<jsp:forward page="error.jsp"/>
<%
				}
			}
			else if (op.equals("Cancel"))
			{
%>
				<jsp:forward page="listauthorities.jsp"/>
<%
			}
			else
			{
				// Error
				variableContext.setParameter("text","Illegal parameter to authority execution page");
				variableContext.setParameter("target","listauthorities.jsp");
%>
				<jsp:forward page="error.jsp"/>
<%
			}
		}
		else if (type != null && op != null && type.equals("mapper"))
		{
			// -- Mapping editing operations --
			if (op.equals("Save") || op.equals("Continue"))
			{
				try
				{
					// Set up a connection object that is a merge of an existing connection object plus what was posted.
					IMappingConnection connection = null;
					boolean isNew = true;
					String x = variableContext.getParameter("isnewconnection");
					if (x != null)
						isNew = x.equals("true");

					String connectionName = variableContext.getParameter("connname");
					// If the connectionname is not null, load the connection description and prepopulate everything with what comes from it.
					if (connectionName != null && connectionName.length() > 0 && !isNew)
					{
						connection = mappingConnManager.load(connectionName);
					}
					
					if (connection == null)
					{
						connection = mappingConnManager.create();
						if (connectionName != null && connectionName.length() > 0)
							connection.setName(connectionName);
					}

					// Gather all the data from the form.
					connection.setIsNew(isNew);
					x = variableContext.getParameter("description");
					if (x != null)
						connection.setDescription(x);
					x = variableContext.getParameter("classname");
					if (x != null)
						connection.setClassName(x);
					x = variableContext.getParameter("maxconnections");
					if (x != null && x.length() > 0)
						connection.setMaxConnections(Integer.parseInt(x));
					x = variableContext.getParameter("prerequisites_present");
					if (x != null && x.equals("true"))
					{
						String y = variableContext.getParameter("prerequisites");
						if (y != null && y.length() == 0)
							y = null;
						connection.setPrerequisiteMapping(y);
					}

					String error = MappingConnectorFactory.processConfigurationPost(threadContext,connection.getClassName(),variableContext,pageContext.getRequest().getLocale(),connection.getConfigParams());
					
					if (error != null)
					{
						variableContext.setParameter("text",error);
						variableContext.setParameter("target","listmappers.jsp");
%>
						<jsp:forward page="error.jsp"/>
<%
					}
					
					if (op.equals("Continue"))
					{
						threadContext.save("ConnectionObject",connection);
%>
						<jsp:forward page="editmapper.jsp"/>
<%
					}
					else if (op.equals("Save"))
					{
						mappingConnManager.save(connection);
						variableContext.setParameter("connname",connectionName);
%>
						<jsp:forward page="viewmapper.jsp"/>
<%
					}
				}
				catch (ManifoldCFException e)
				{
					e.printStackTrace();
					variableContext.setParameter("text",e.getMessage());
					variableContext.setParameter("target","listmappers.jsp");
%>
					<jsp:forward page="error.jsp"/>
<%
				}
			}
			else if (op.equals("Delete"))
			{
				try
				{
					String connectionName = variableContext.getParameter("connname");
					if (connectionName == null)
						throw new ManifoldCFException("Missing connection parameter");
					mappingConnManager.delete(connectionName);
%>
					<jsp:forward page="listmappers.jsp"/>
<%
				}
				catch (ManifoldCFException e)
				{
					e.printStackTrace();
					variableContext.setParameter("text",e.getMessage());
					variableContext.setParameter("target","listmappers.jsp");
%>
					<jsp:forward page="error.jsp"/>
<%
				}
			}
			else if (op.equals("Cancel"))
			{
%>
				<jsp:forward page="listmappers.jsp"/>
<%
			}
			else
			{
				// Error
				variableContext.setParameter("text","Illegal parameter to mapping execution page");
				variableContext.setParameter("target","listmappers.jsp");
%>
				<jsp:forward page="error.jsp"/>
<%
			}
		}
		else if (type != null && op != null && type.equals("output"))
		{
			// -- Output connection editing operations --
			if (op.equals("Save") || op.equals("Continue"))
			{
				try
				{
					// Set up a connection object that is a merge of an existing connection object plus what was posted.
					IOutputConnection connection = null;
					boolean isNew = true;
					String x = variableContext.getParameter("isnewconnection");
					if (x != null)
						isNew = x.equals("true");

					String connectionName = variableContext.getParameter("connname");
					// If the connectionname is not null, load the connection description and prepopulate everything with what comes from it.
					if (connectionName != null && connectionName.length() > 0 && !isNew)
					{
						connection = outputManager.load(connectionName);
					}
					
					if (connection == null)
					{
						connection = outputManager.create();
						if (connectionName != null && connectionName.length() > 0)
							connection.setName(connectionName);
					}

					// Gather all the data from the form.
					connection.setIsNew(isNew);
					x = variableContext.getParameter("description");
					if (x != null)
						connection.setDescription(x);
					x = variableContext.getParameter("classname");
					if (x != null)
						connection.setClassName(x);
					x = variableContext.getParameter("maxconnections");
					if (x != null && x.length() > 0)
						connection.setMaxConnections(Integer.parseInt(x));

					String error = OutputConnectorFactory.processConfigurationPost(threadContext,connection.getClassName(),variableContext,pageContext.getRequest().getLocale(),connection.getConfigParams());
					
					if (error != null)
					{
						variableContext.setParameter("text",error);
						variableContext.setParameter("target","listoutputs.jsp");
%>
						<jsp:forward page="error.jsp"/>
<%
					}
					
					if (op.equals("Continue"))
					{
						threadContext.save("ConnectionObject",connection);
%>
						<jsp:forward page="editoutput.jsp"/>
<%
					}
					else if (op.equals("Save"))
					{
						outputManager.save(connection);
						variableContext.setParameter("connname",connectionName);
%>
						<jsp:forward page="viewoutput.jsp"/>
<%
					}
				}
				catch (ManifoldCFException e)
				{
					e.printStackTrace();
					variableContext.setParameter("text",e.getMessage());
					variableContext.setParameter("target","listoutputs.jsp");
%>
					<jsp:forward page="error.jsp"/>
<%
				}
			}
			else if (op.equals("Delete"))
			{
				try
				{
					String connectionName = variableContext.getParameter("connname");
					if (connectionName == null)
						throw new ManifoldCFException("Missing connection parameter");
					outputManager.delete(connectionName);
%>
					<jsp:forward page="listoutputs.jsp"/>
<%
				}
				catch (ManifoldCFException e)
				{
					e.printStackTrace();
					variableContext.setParameter("text",e.getMessage());
					variableContext.setParameter("target","listoutputs.jsp");
%>
					<jsp:forward page="error.jsp"/>
<%
				}
			}
			else if (op.equals("Cancel"))
			{
%>
				<jsp:forward page="listoutputs.jsp"/>
<%
			}
			else if (op.equals("ReingestAll"))
			{
				try
				{
					String connectionName = variableContext.getParameter("connname");
					if (connectionName == null)
						throw new ManifoldCFException("Missing connection parameter");
					org.apache.manifoldcf.agents.system.ManifoldCF.signalOutputConnectionRedo(threadContext,connectionName);
%>
					<jsp:forward page="listoutputs.jsp"/>
<%
				}
				catch (ManifoldCFException e)
				{
					e.printStackTrace();
					variableContext.setParameter("text",e.getMessage());
					variableContext.setParameter("target","listoutputs.jsp");
%>
					<jsp:forward page="error.jsp"/>
<%
				}
			}
			else if (op.equals("RemoveAll"))
			{
				try
				{
					String connectionName = variableContext.getParameter("connname");
					if (connectionName == null)
						throw new ManifoldCFException("Missing connection parameter");
					org.apache.manifoldcf.agents.system.ManifoldCF.signalOutputConnectionRemoved(threadContext,connectionName);
%>
					<jsp:forward page="listoutputs.jsp"/>
<%
				}
				catch (ManifoldCFException e)
				{
					e.printStackTrace();
					variableContext.setParameter("text",e.getMessage());
					variableContext.setParameter("target","listoutputs.jsp");
%>
					<jsp:forward page="error.jsp"/>
<%
				}
			}
			else
			{
				// Error
				variableContext.setParameter("text","Illegal parameter to output connection execution page");
				variableContext.setParameter("target","listoutputs.jsp");
%>
				<jsp:forward page="error.jsp"/>
<%
			}
		}
		else if (type != null && op != null && type.equals("transformation"))
		{
			// -- Output connection editing operations --
			if (op.equals("Save") || op.equals("Continue"))
			{
				try
				{
					// Set up a connection object that is a merge of an existing connection object plus what was posted.
					ITransformationConnection connection = null;
					boolean isNew = true;
					String x = variableContext.getParameter("isnewconnection");
					if (x != null)
						isNew = x.equals("true");

					String connectionName = variableContext.getParameter("connname");
					// If the connectionname is not null, load the connection description and prepopulate everything with what comes from it.
					if (connectionName != null && connectionName.length() > 0 && !isNew)
					{
						connection = transformationManager.load(connectionName);
					}
					
					if (connection == null)
					{
						connection = transformationManager.create();
						if (connectionName != null && connectionName.length() > 0)
							connection.setName(connectionName);
					}

					// Gather all the data from the form.
					connection.setIsNew(isNew);
					x = variableContext.getParameter("description");
					if (x != null)
						connection.setDescription(x);
					x = variableContext.getParameter("classname");
					if (x != null)
						connection.setClassName(x);
					x = variableContext.getParameter("maxconnections");
					if (x != null && x.length() > 0)
						connection.setMaxConnections(Integer.parseInt(x));

					String error = TransformationConnectorFactory.processConfigurationPost(threadContext,connection.getClassName(),variableContext,pageContext.getRequest().getLocale(),connection.getConfigParams());
					
					if (error != null)
					{
						variableContext.setParameter("text",error);
						variableContext.setParameter("target","listtransformations.jsp");
%>
						<jsp:forward page="error.jsp"/>
<%
					}
					
					if (op.equals("Continue"))
					{
						threadContext.save("ConnectionObject",connection);
%>
						<jsp:forward page="edittransformation.jsp"/>
<%
					}
					else if (op.equals("Save"))
					{
						transformationManager.save(connection);
						variableContext.setParameter("connname",connectionName);
%>
						<jsp:forward page="viewtransformation.jsp"/>
<%
					}
				}
				catch (ManifoldCFException e)
				{
					e.printStackTrace();
					variableContext.setParameter("text",e.getMessage());
					variableContext.setParameter("target","listtransformations.jsp");
%>
					<jsp:forward page="error.jsp"/>
<%
				}
			}
			else if (op.equals("Delete"))
			{
				try
				{
					String connectionName = variableContext.getParameter("connname");
					if (connectionName == null)
						throw new ManifoldCFException("Missing connection parameter");
					transformationManager.delete(connectionName);
%>
					<jsp:forward page="listtransformations.jsp"/>
<%
				}
				catch (ManifoldCFException e)
				{
					e.printStackTrace();
					variableContext.setParameter("text",e.getMessage());
					variableContext.setParameter("target","listtransformations.jsp");
%>
					<jsp:forward page="error.jsp"/>
<%
				}
			}
			else if (op.equals("Cancel"))
			{
%>
				<jsp:forward page="listtransformations.jsp"/>
<%
			}
			else
			{
				// Error
				variableContext.setParameter("text","Illegal parameter to transformation connection execution page");
				variableContext.setParameter("target","listtransformations.jsp");
%>
				<jsp:forward page="error.jsp"/>
<%
			}
		}
		else if (type != null && op != null && type.equals("job"))
		{
			// -- Job editing operations --
			if (op.equals("Save") || op.equals("Continue"))
			{
				try
				{
					String jobID = variableContext.getParameter("jobid");
					IJobDescription job = null;
					if (jobID != null)
					{
						job = manager.load(new Long(jobID));
					}
					if (job == null)
						job = manager.createJob();

					// Figure out what got posted.
					String x = variableContext.getParameter("connectionpresent");
					boolean connectionPresent = (x != null && x.equals("true"));
					
					// Gather the rest of the data.
					x = variableContext.getParameter("description");
					if (x != null)
						job.setDescription(x);
					x = variableContext.getParameter("connectionname");
					if (x != null)
						job.setConnectionName(x);
					x = variableContext.getParameter("scheduletype");
					if (x != null)
						job.setType(Integer.parseInt(x));
					x = variableContext.getParameter("startmethod");
					if (x != null)
						job.setStartMethod(Integer.parseInt(x));
					x = variableContext.getParameter("hopcountmode");
					if (x != null)
						job.setHopcountMode(Integer.parseInt(x));
					x = variableContext.getParameter("pipeline_count");
					if (x != null)
					{
						// Do we need to keep the old specifications around, or can we destroy them?
						// Not clear that retention is required., so I'm not wasting time trying to implement that.
						int count = Integer.parseInt(x);
						job.clearPipeline();
						for (int j = 0; j < count; j++)
						{
							// Gather everything first; we'll look at edits later
							int precedent = Integer.parseInt(variableContext.getParameter("pipeline_"+j+"_precedent"));
							boolean isOutput = variableContext.getParameter("pipeline_"+j+"_isoutput").equals("true");
							String connectionName = variableContext.getParameter("pipeline_"+j+"_connectionname");
							String description = variableContext.getParameter("pipeline_"+j+"_description");
							job.addPipelineStage(precedent, isOutput, connectionName, description);
						}
					}

					x = variableContext.getParameter("schedulerecords");
					String[] y;

					if (x != null)
					{
						// Read records and put them into the job description
						job.clearScheduleRecords();
						int recordCount = Integer.parseInt(x);
						int j = 0;
						while (j < recordCount)
						{
							String indexValue = Integer.toString(j);
							EnumeratedValues srDayOfWeek = null;
							EnumeratedValues srDayOfMonth = null;
							EnumeratedValues srMonthOfYear = null;
							EnumeratedValues srYear = null;
							EnumeratedValues srHourOfDay = null;
							EnumeratedValues srMinutesOfHour = null;
							Long srDuration = null;
							boolean srRequestMinimum = false;

							y = variableContext.getParameterValues("dayofweek"+indexValue);
							if (y != null)
							{
								if (y.length >= 1 && y[0].equals("none"))
									srDayOfWeek = null;
								else
									srDayOfWeek = new EnumeratedValues(y);
							}
							y = variableContext.getParameterValues("dayofmonth"+indexValue);
							if (y != null)
							{
								if (y.length >= 1 && y[0].equals("none"))
									srDayOfMonth = null;
								else
									srDayOfMonth = new EnumeratedValues(y);
							}
							y = variableContext.getParameterValues("monthofyear"+indexValue);
							if (y != null)
							{
								if (y.length >= 1 && y[0].equals("none"))
									srMonthOfYear = null;
								else
									srMonthOfYear = new EnumeratedValues(y);
							}
							y = variableContext.getParameterValues("year"+indexValue);
							if (y != null)
							{
								if (y.length >= 1 && y[0].equals("none"))
									srYear = null;
								else
									srYear = new EnumeratedValues(y);
							}
							y = variableContext.getParameterValues("hourofday"+indexValue);
							if (y != null)
							{
								if (y.length >= 1 && y[0].equals("none"))
									srHourOfDay = null;
								else
									srHourOfDay = new EnumeratedValues(y);
							}
							y = variableContext.getParameterValues("minutesofhour"+indexValue);
							if (y != null)
							{
								if (y.length >= 1 && y[0].equals("none"))
									srMinutesOfHour = null;
								else
									srMinutesOfHour = new EnumeratedValues(y);
							}
							x = variableContext.getParameter("duration"+indexValue);
							if (x != null)
							{
								if (x.length() == 0)
									srDuration = null;
								else
									srDuration = new Long(new Long(x).longValue()*60000L);
							}
							x = variableContext.getParameter("invocation"+indexValue);
							if (x != null)
							{
								srRequestMinimum = x.equals("minimal");
							}
							
							x = variableContext.getParameter("recordop"+j);
							if (x == null || !x.equals("Remove Schedule"))
							{
								ScheduleRecord sr = new ScheduleRecord(srDayOfWeek,srMonthOfYear,srDayOfMonth,srYear,srHourOfDay,srMinutesOfHour,
									null,srDuration,srRequestMinimum);
								job.addScheduleRecord(sr);
							}
							j++;
						}
					}

					// Check for operation that adds to schedule
					x = variableContext.getParameter("recordop");
					if (x != null && x.equals("Add Scheduled Time"))
					{
						EnumeratedValues srDayOfWeek = null;
						EnumeratedValues srDayOfMonth = null;
						EnumeratedValues srMonthOfYear = null;
						EnumeratedValues srYear = null;
						EnumeratedValues srHourOfDay = null;
						EnumeratedValues srMinutesOfHour = null;
						Long srDuration = null;
						boolean srRequestMinimum = false;

						y = variableContext.getParameterValues("dayofweek");
						if (y != null)
						{
							if (y.length >= 1 && y[0].equals("none"))
								srDayOfWeek = null;
							else
								srDayOfWeek = new EnumeratedValues(y);
						}
						y = variableContext.getParameterValues("dayofmonth");
						if (y != null)
						{
							if (y.length >= 1 && y[0].equals("none"))
								srDayOfMonth = null;
							else
								srDayOfMonth = new EnumeratedValues(y);
						}
						y = variableContext.getParameterValues("monthofyear");
						if (y != null)
						{
							if (y.length >= 1 && y[0].equals("none"))
								srMonthOfYear = null;
							else
								srMonthOfYear = new EnumeratedValues(y);
						}
						y = variableContext.getParameterValues("year");
						if (y != null)
						{
							if (y.length >= 1 && y[0].equals("none"))
								srYear = null;
							else
								srYear = new EnumeratedValues(y);
						}
						y = variableContext.getParameterValues("hourofday");
						if (y != null)
						{
							if (y.length >= 1 && y[0].equals("none"))
								srHourOfDay = null;
							else
								srHourOfDay = new EnumeratedValues(y);
						}
						y = variableContext.getParameterValues("minutesofhour");
						if (y != null)
						{
							if (y.length >= 1 && y[0].equals("none"))
								srMinutesOfHour = null;
							else
								srMinutesOfHour = new EnumeratedValues(y);
						}
						x = variableContext.getParameter("duration");
						if (x != null)
						{
							if (x.length() == 0)
								srDuration = null;
							else
								srDuration = new Long(new Long(x).longValue() * 60000L);
						}
						x = variableContext.getParameter("invocation");
						if (x != null)
						{
							srRequestMinimum = x.equals("minimal");
						}

						ScheduleRecord sr = new ScheduleRecord(srDayOfWeek,srMonthOfYear,srDayOfMonth,srYear,srHourOfDay,srMinutesOfHour,
							null,srDuration,srRequestMinimum);
						job.addScheduleRecord(sr);
					}

					x = variableContext.getParameter("priority");
					if (x != null)
						job.setPriority(Integer.parseInt(x));
					x = variableContext.getParameter("recrawlinterval");
					if (x != null)
					{
						if (x.length() == 0)
							job.setInterval(null);
						else
							job.setInterval(new Long(new Long(x).longValue() * 60000L));
					}
					x = variableContext.getParameter("maxrecrawlinterval");
					if (x != null)
					{
						if (x.length() == 0)
							job.setMaxInterval(null);
						else
							job.setMaxInterval(new Long(new Long(x).longValue() * 60000L));
					}
					x = variableContext.getParameter("reseedinterval");
					if (x != null)
					{
						if (x.length() == 0)
							job.setReseedInterval(null);
						else
							job.setReseedInterval(new Long(new Long(x).longValue() * 60000L));
					}
					x = variableContext.getParameter("expirationinterval");
					if (x != null)
					{
						if (x.length() == 0)
							job.setExpiration(null);
						else
							job.setExpiration(new Long(new Long(x).longValue() * 60000L));
					}

					IRepositoryConnection connection = null;
					if (job.getConnectionName() != null && job.getConnectionName().length() > 0)
						connection = connManager.load(job.getConnectionName());
					
					if (connection != null)
					{
						String[] relationshipTypes = RepositoryConnectorFactory.getRelationshipTypes(threadContext,connection.getClassName());

						// Gather hopcount filters
						x = variableContext.getParameter("hopfilters");
						if (x != null && relationshipTypes != null)
						{
							job.clearHopCountFilters();
							int j = 0;
							while (j < relationshipTypes.length)
							{
								String relationshipType = relationshipTypes[j++];
								x = variableContext.getParameter("hopmax_"+relationshipType);
								if (x != null && x.length() > 0)
								{
									job.addHopCountFilter(relationshipType,new Long(x));
								}
							}
						}
					}
					
					if (connectionPresent && connection != null)
					{
						IRepositoryConnector repositoryConnector = repositoryConnectorPool.grab(connection);
						if (repositoryConnector != null)
						{
							try
							{
								String error = repositoryConnector.processSpecificationPost(variableContext,pageContext.getRequest().getLocale(),job.getSpecification(),0);
								if (error != null)
								{
									variableContext.setParameter("text",error);
									variableContext.setParameter("target","listjobs.jsp");
%>
									<jsp:forward page="error.jsp"/>
<%
								}
							}
							finally
							{
								repositoryConnectorPool.release(connection,repositoryConnector);
							}
						}
					}
					
					// Process all pipeline stages
					for (int j = 0; j < job.countPipelineStages(); j++)
					{
						if (job.getPipelineStageIsOutputConnection(j))
						{
							IOutputConnection outputConnection = outputManager.load(job.getPipelineStageConnectionName(j));
							if (outputConnection != null)
							{
								IOutputConnector outputConnector = outputConnectorPool.grab(outputConnection);
								if (outputConnector != null)
								{
									try
									{
										String error = outputConnector.processSpecificationPost(variableContext,pageContext.getRequest().getLocale(),job.getPipelineStageSpecification(j),1+j);
										if (error != null)
										{
											variableContext.setParameter("text",error);
											variableContext.setParameter("target","listjobs.jsp");
%>
									<jsp:forward page="error.jsp"/>
<%
										}
									}
									finally
									{
										outputConnectorPool.release(outputConnection,outputConnector);
									}
								}
							}
						}
						else
						{
							ITransformationConnection transformationConnection = transformationManager.load(job.getPipelineStageConnectionName(j));
							if (transformationConnection != null)
							{
								ITransformationConnector transformationConnector = transformationConnectorPool.grab(transformationConnection);
								if (transformationConnector != null)
								{
									try
									{
										String error = transformationConnector.processSpecificationPost(variableContext,pageContext.getRequest().getLocale(),job.getPipelineStageSpecification(j),1+j);
										if (error != null)
										{
											variableContext.setParameter("text",error);
											variableContext.setParameter("target","listjobs.jsp");
%>
									<jsp:forward page="error.jsp"/>
<%
										}
									}
									finally
									{
										transformationConnectorPool.release(transformationConnection,transformationConnector);
									}
								}
							}
						}
					}

					// Now, after gathering is complete, consider doing changes to the pipeline.
					int currentStage = 0;
					for (int j = 0; j < job.countPipelineStages(); j++)
					{
						// Look at the operation
						x = variableContext.getParameter("pipeline_"+j+"_op");
						if (x != null && x.equals("Delete"))
						{
							// Delete this pipeline stage (and rewire other stages according to rules)
							job.deletePipelineStage(currentStage);
						}
						else if (x != null && x.equals("InsertTransformation"))
						{
							// Insert a new stage before this one
							String connectionName = variableContext.getParameter("transformation_connectionname");
							String description = variableContext.getParameter("transformation_description");
							job.insertPipelineStage(currentStage,false,connectionName,description);
							currentStage++;
						}
						else if (x != null && x.equals("InsertOutput"))
						{
							// Insert a new stage before this one
							String connectionName = variableContext.getParameter("output_connectionname");
							String description = variableContext.getParameter("output_description");
							job.insertPipelineStage(currentStage,true,connectionName,description);
							currentStage++;
						}
						else
							currentStage++;
					}
					x = variableContext.getParameter("output_op");
					if (x != null && x.equals("Add"))
					{
						// Append a new stage at the end
						int precedent = Integer.parseInt(variableContext.getParameter("output_precedent"));
						String connectionName = variableContext.getParameter("output_connectionname");
						String description = variableContext.getParameter("output_description");
						job.addPipelineStage(precedent,true,connectionName,description);
					}
					
					if (op.equals("Continue"))
					{
						threadContext.save("JobObject",job);
%>
						<jsp:forward page="editjob.jsp"/>
<%
					}
					else if (op.equals("Save"))
					{
						manager.save(job);
						// Reset the job schedule. We may want to make this explicit at some point; having
						// this happen all the time seems wrong.
						manager.resetJobSchedule(job.getID());
						variableContext.setParameter("jobid",job.getID().toString());
%>
						<jsp:forward page="viewjob.jsp"/>
<%
					}
				}
				catch (ManifoldCFException e)
				{
					e.printStackTrace();
					variableContext.setParameter("text",e.getMessage());
					variableContext.setParameter("target","listjobs.jsp");
%>
					<jsp:forward page="error.jsp"/>
<%
				}
			}
			else if (op.equals("StartOver"))
			{
				try
				{
					String jobID = variableContext.getParameter("jobid");
					if (jobID == null)
						throw new ManifoldCFException("Missing job parameter");
					manager.clearJobSeedingState(new Long(jobID));
%>
					<jsp:forward page="listjobs.jsp"/>
<%
				}
				catch (ManifoldCFException e)
				{
					e.printStackTrace();
					variableContext.setParameter("text",e.getMessage());
					variableContext.setParameter("target","listjobs.jsp");
%>
					<jsp:forward page="error.jsp"/>
<%
				}
			}
			else if (op.equals("Delete"))
			{
				try
				{
					String jobID = variableContext.getParameter("jobid");
					if (jobID == null)
						throw new ManifoldCFException("Missing job parameter");
					manager.deleteJob(new Long(jobID));
%>
					<jsp:forward page="listjobs.jsp"/>
<%
				}
				catch (ManifoldCFException e)
				{
					e.printStackTrace();
					variableContext.setParameter("text",e.getMessage());
					variableContext.setParameter("target","listjobs.jsp");
%>
					<jsp:forward page="error.jsp"/>
<%
				}
			}
			else if (op.equals("Cancel"))
			{
				// Cancel operation
%>
				<jsp:forward page="listjobs.jsp"/>
<%
			}
			else
			{
				// Error
				variableContext.setParameter("text","Illegal parameter to job definition execution page: "+op);
				variableContext.setParameter("target","listjobs.jsp");
%>
				<jsp:forward page="error.jsp"/>
<%
			}
		}
		else if (type != null && op != null && (type.equals("simplereport") || type.equals("maxactivityreport") ||
				type.equals("maxbandwidthreport") || type.equals("resultreport") ||
				type.equals("documentstatus") || type.equals("queuestatus")))
		{
			// -- Report handling operations --
			if (op.equals("Continue") || op.equals("Report") || op.equals("Status"))
			{
%>
				<jsp:forward page='<%=type+".jsp"%>'/>
<%
			}
			else
			{
				// Error
				variableContext.setParameter("text","Illegal parameter to report/status execution page: "+op);
				variableContext.setParameter("target","index.jsp");
%>
				<jsp:forward page="error.jsp"/>
<%
			}
		}
		else if (op != null && type != null && type.equals("jobstatus"))
		{
			if (op.equals("Start") || op.equals("StartMinimal"))
			{
				// -- Start a job --
				String jobID = variableContext.getParameter("jobid");
				manager.manualStart(new Long(jobID),op.equals("StartMinimal"));
				// Forward to showjobstatus
%>
				<jsp:forward page="showjobstatus.jsp"/>
<%
			}
			else if (op.equals("Pause"))
			{
				// -- Pause a job --
				String jobID = variableContext.getParameter("jobid");
				manager.pauseJob(new Long(jobID));
				// Forward to showjobstatus
%>
				<jsp:forward page="showjobstatus.jsp"/>
<%
			}
			else if (op.equals("Abort"))
			{
				// -- Abort a job --
				String jobID = variableContext.getParameter("jobid");
				manager.manualAbort(new Long(jobID));
				// Forward to showjobstatus
%>
				<jsp:forward page="showjobstatus.jsp"/>
<%
			}
			else if (op.equals("Restart") || op.equals("RestartMinimal"))
			{
				// -- Restart a job --
				String jobID = variableContext.getParameter("jobid");
				manager.manualAbortRestart(new Long(jobID),op.equals("RestartMinimal"));
				// Forward to showjobstatus
%>
				<jsp:forward page="showjobstatus.jsp"/>
<%
			}
			else if (op.equals("Resume"))
			{
				// -- Resume a job --
				String jobID = variableContext.getParameter("jobid");
				manager.restartJob(new Long(jobID));
				// Forward to showjobstatus
%>
				<jsp:forward page="showjobstatus.jsp"/>
<%
			}
			else
			{
				// Error
				variableContext.setParameter("text","Illegal parameter to jobstatus execution page: "+op);
				variableContext.setParameter("target","index.jsp");
%>
				<jsp:forward page="error.jsp"/>
<%
			}
		}
		else
		{
			/*
			// If we didn't have an op, then we transfer control back to where the page said to.
			String target = variableContext.getParameter("target");
			if (target != null)
			{
				<jsp:forward page='<%=target%'/>
			}

			<jsp:forward page="index.jsp"/>
			*/

			// Error
			variableContext.setParameter("text","Illegal parameter to page");
			variableContext.setParameter("target","index.jsp");
%>
			<jsp:forward page="error.jsp"/>
<%
		}
	}
	catch (ManifoldCFException e)
	{
		e.printStackTrace();
		variableContext.setParameter("text",e.getMessage());
		variableContext.setParameter("target","index.jsp");
%>
		<jsp:forward page="error.jsp"/>
<%
	}
%>
