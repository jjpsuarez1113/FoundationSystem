package com.information.process;
import com.information.process.DBConnection;
import com.information.personal.*;

import java.sql.*;
import java.io.IOException;
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Servlet implementation class Confirm
 */
@WebServlet(description = "confirms entry from database", urlPatterns = { "/ConfirmInformation" })
public class Confirm extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Connection con;
	private String user;
	private String pass;
	private String roles; 
	private ResultSet rs;
	private PersonalBean p = new PersonalBean();
	private HttpSession session;
	private ADBean a = new ADBean();
	private ArrayList<UserDonationBean> u;
	private double total;
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		con = new DBConnection().connect();
		try {
			Cookie ck[] = request.getCookies();
			session = request.getSession(false);
			user = ck[1].getValue();
			pass = ck[2].getValue();
			getUserDonationLogs(request, session);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		response.sendRedirect("successfulDonation.jsp");
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		con = new DBConnection().connect();
		try {
				if(checkLogin(request, response))
				{
					Cookie username = new Cookie("username", user);
					Cookie password = new Cookie("password", pass);
					Cookie role = new Cookie("role", roles);

					username.setMaxAge(-1);
					password.setMaxAge(-1);
					role.setMaxAge(-1);
					
					session = request.getSession();
					getUserDonationLogs(request, session);
					response.addCookie(username);
					response.addCookie(password);
					response.addCookie(role);
						
					request.getRequestDispatcher("landingPage.jsp").forward(request, response);
				}
				else
				{
					response.sendRedirect("login.jsp");
				}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void getUserDonationLogs(HttpServletRequest request, HttpSession session) throws SQLException
	{
		getInfo(user);
		getUserDonations(user);
		
		session.setAttribute("pbean", p);
		session.setAttribute("adbean", a);
		session.setAttribute("udbean", u);
		session.setAttribute("total", total);
		session.setAttribute("status", "in");
	}
	
	public boolean checkLogin(HttpServletRequest request, HttpServletResponse response) throws SQLException
	{
		user = request.getParameter("username");
		pass = request.getParameter("password");
		Statement st = con.createStatement();
		rs = st.executeQuery("SELECT * FROM AccountDetails");
		boolean c = false;
		
		while (rs.next())
		{
			String dbUsername = rs.getString("Username"), dbPassword = rs.getString("Password");
			if (dbUsername.equals(user) && dbPassword.equals(pass))
			{
				roles = rs.getString("RoleID");
				c = true;
				if (rs.getString("Active").equals("NO"))
					c = false;
				break;
			}
			else
				c = false;
		}
		return c;
	}
	
	public void getInfo(String username) throws SQLException
	{
		con = new DBConnection().connect();
		Statement st = con.createStatement();
		rs = st.executeQuery("SELECT * FROM PersonalInformation WHERE Username = \""+username+"\"");
		rs.next();
		
		p.setLastName(rs.getString(2));
		p.setFirstName(rs.getString(3));
		p.setBirthdate(rs.getString(5));
		p.setEmail(rs.getString(7));
		p.setPhoneNumber(rs.getString(8));
		
		int code = rs.getInt("AddressID");
		rs = st.executeQuery("SELECT * FROM AddressInformation WHERE AddressID = " + code);
		rs.next();
		p.setAddress(rs.getString(2) + ", " + rs.getString(4) + ", " + rs.getString(5) + ", " + rs.getString(3));
		
		st = con.createStatement();
		rs = st.executeQuery("SELECT Username, COUNT(*) FROM DonationLog WHERE Username = \""+ username+"\"");
		rs.next(); a.setDonationsCount(rs.getInt("COUNT(*)"));
		rs = st.executeQuery("SELECT DateJoined FROM AccountDetails WHERE Username = \""+username+"\"");
		rs.next(); a.setDateJoined(rs.getDate("DateJoined"));
		rs = st.executeQuery("SELECT Donations FROM UserDonation WHERE Username = \""+username+"\"");
		rs.next(); a.setTotalDonations(rs.getDouble("Donations"));
		rs = st.executeQuery("SELECT SUM(Amount) FROM DonationLog");
		rs.next();
		total = rs.getDouble(1);
	}
	
	public void getUserDonations(String username) throws SQLException
	{
		Statement  st = con.createStatement();
		u = new ArrayList<UserDonationBean>();
		ResultSet rs2 = st.executeQuery("SELECT * From DonationLog WHERE Username = \""+username+"\"");
		while(rs2.next())
		{
			UserDonationBean ud = new UserDonationBean();
			ud.setDonationID(rs2.getInt("DonationID"));
			ud.setAmount(rs2.getDouble("Amount"));
			ud.setDateDonated(rs2.getDate("DateDonated"));
			u.add(ud);
		}
	}
}

