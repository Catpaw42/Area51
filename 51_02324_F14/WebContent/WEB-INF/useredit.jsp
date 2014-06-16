<%@page import="java.util.ArrayList"%>
<%@page
	import="javaMeasure.control.interfaces.IDatabaseController.DataBaseException"%>
<%@page import="javaMeasure.control.MainController"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@page import="javaMeasure.control.DataBaseController"%>

<%@page import="javaMeasure.User"%>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html;  charset=UTF-8">
<%
	String username = (String) session.getAttribute("username");
	User u = (User) session.getAttribute("editing");
%>
<title>User Edit</title>
</head>

<title>Noliac : <%
	out.println(username);
%> | udtræk
</title>
					<%
						if (request.getAttribute("editfail") != null) {
							out.println(" <p>Fandt ikke brugernavn i database.</p>");
						}
						if (request.getAttribute("edited") !=null){
							out.println("<p>Opdateret bruger.</p>");
						}
					%>
<link rel="stylesheet" type="text/css" href="view.css" media="all" />
<script type="text/javascript" src="view.js"></script>
<script type="text/javascript" src="calendar.js"></script>
</head>
<body id="main-body">

	<jsp:useBean id="database" class="javaMeasure.control.DataBaseController" scope="session" />

	<div id="wrapper">

		<div id="header">
			<img src="noliac_logo.png" alt="Logo">
		</div>

		<div id="form_container">

			<form id="user_edit" class="appnitro" method="post" action=UserEditServlet>
				<div class="form_description">
					<h1>Noliac User Edit</h1>
					
				
				
				
	<table width="100%" border="0" cellpadding="2" cellspacing="0">
		<tr>
			<td>
				<ul>
				
					<li id="li_3"><label class="description" for="username" >
					 <% //Checks if User was found
											if (request.getParameter("fail") != null) out.print("Bruger ikke genkendt! -");
											%>User name</label>
						<div>
							<input id="username" name="username" class="element text medium"
								type="text" maxlength="255" 
								list="batches" autocomplete="on" value="<%=u.getUserName()%>" />
										
						</div>

						
					<li id="li_"><label class="description" for="element_4">Password</label>
						<div>
							<input id="element_4" name="password" class="element text medium"
								type="text" maxlength="255" value="<%=u.getPassWord()%>" />
						  <input type="hidden" name="form_id"
						value="812583" />
				  </div>                    
				</ul>
          </td>
		  <td width="50%">
	
         <input type="checkbox" name="active" value="active"<%if(u.isActive()){%>checked<%} %>> active<BR>
		  <input type="checkbox" name="admin" value="admin"<%if(u.isAdmin()){%>checked<%} %>> admin<BR></tr>
		  </table>
		  
		 
				  <span class="buttons">
				  
				  <input id="save" class="button_text"
						type="submit" name="Save" value="Save" />


                
				  <input id="Done" class="button_text"
						type="submit" name="Done" value="Cancel" />
				  </span>



				
				</div>
				</form>
			
			<div id="footer">
				By Area51
				<!--Generated by <a href="http://www.phpform.org">pForm</a>-->
			</div>

		</div>
	</div>
</body>
</html>