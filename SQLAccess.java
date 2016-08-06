package db.restlet;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class SQLAccess {
	protected Connection connect = null;
	private Statement statement = null;
	private PreparedStatement preparedStatement = null;
	private ResultSet resultSet = null;

	public void connection() throws Exception {
		try {
			Class.forName("com.mysql.jdbc.Driver");

			connect = DriverManager
					.getConnection("jdbc:mysql://localhost:3306/DBTESI?characterEncoding=UTF-8&useSSL=false",
							"root", "moonlight3");
			/*.getConnection("jdbc:mysql://localhost:3306/dbtesi?characterEncoding=UTF-8&useSSL=false",
												"root", "root"); */
		} catch (Exception e) {
			throw e;
		}
	}

	private String resultset_to_json(ResultSet rs) throws SQLException {
		JSONArray jarr = new JSONArray();
		while (rs.next()){
			HashMap<String, String> row = new HashMap<String, String>();
			for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) 
				row.put(rs.getMetaData().getColumnName(i), String.valueOf(rs.getObject(i)));
			jarr.put(new JSONObject(row));
		}
		return jarr.toString();
	}
	
	private String error_to_json(String error) throws SQLException, JSONException {	
		JSONObject jobj = new JSONObject();
		jobj.append("Error", error);
		return jobj.toString();
	}


	public String allUser ()  throws SQLException {
		statement = connect.createStatement();
		resultSet = statement.executeQuery("select * from USER");

		return resultset_to_json(resultSet);
	} 

	private boolean checkUser (String user)  throws SQLException {
		statement = connect.createStatement();
		resultSet = statement.executeQuery("select * from USER where ID = " + "'" + user + "'");

		if (!resultSet.next())
			return false; 	//user doesn't exist in DB

		return true;		//user exist in DB 
	} 

	private boolean checkOwner (String user, int COD_M) throws SQLException {
		statement = connect.createStatement();
		resultSet = statement.executeQuery("select * " +
				"from USER u, MONSTER m " +
				"where u.ID = " + "'" + user + "' " +
				"and m.COD_M = " + COD_M + " " +
				"and u.ID = m.ID_OWNER");

		if (!resultSet.next())
			return false; 	//user isn't the owner of monster COD_M

		return true;		//user is owner of COD_M 
	} 

	private boolean checkOwner (String user, String w_name) throws SQLException {
		statement = connect.createStatement();
		resultSet = statement.executeQuery("select * " +
				"from USER u, WEARABLE_OWNED w " +
				"where u.ID = " + "'" + user + "' " +
				"and w.W_NAME = " + "'" + w_name + "' " +
				"and u.ID = w.ID_OWNER");

		if (!resultSet.next())
			return false; 	//user isn't the owner of item w_name

		return true;		//user is owner of item w_name
	} 

	private String findOwner (int COD_M) throws SQLException {		//return ID of the owner
		statement = connect.createStatement();
		resultSet = statement.executeQuery("select ID_OWNER " +
				"from MONSTER_OWNED " +
				"where COD_M = " + COD_M );

		if (!resultSet.next())
			return null; 	//user isn't the owner of item w_name

		return resultSet.getString("ID_OWNER");		//user is owner of item w_name
	} 

	private boolean checkTeamSize(String user) throws SQLException{
		statement = connect.createStatement();

		//Ritorna il numero di mostri in team appartenenti a user
		resultSet = statement.executeQuery(""
				+ "select COUNT(*)"
				+ " from TEAM"
				+ " where ID_USER = '" + user + "'");

		//se nessun mostro in team
		if (!resultSet.next())
			return true; 	 
		//se mostri in team minore di 9
		else 
			if(resultSet.getInt("COUNT(*)") < 9)
				return true;
			else
				return false;  //mostri in team = 9
	}

	public String findFoe (String user)  throws SQLException, JSONException{
		statement = connect.createStatement();
		resultSet = statement.executeQuery("select * " +
				"from GAME " +
				"where ID1 = '" + user +"' " +
				"or ID2 = '" + user +"'" );

		if(!resultSet.next())
			return error_to_json("User not in game");	

		String ID1 = resultSet.getString("ID1");
		String ID2 = resultSet.getString("ID2");
		if (ID1.equals(user))
			return ID2;
		return ID1;
	}

	public String aAbility(int COD_M,  int COD_MA, String ability) throws SQLException, JSONException{
		statement = connect.createStatement();
		int pos = findPosition(COD_MA);

		if(checkLearnedAbility(ability, COD_M) == false)
			return "Ability: " + ability + " not learned by: " + COD_M;

		if(findRange(ability) < (pos / 3))				//controllo che l'attaccante riesca a raggiungere il bersaglio (ATTKRANGE)
			return error_to_json("Monster: "+ COD_M +" can't use the ability on monster: " + COD_MA + " not enought attack range");


		mAction(COD_M,  "ability");
		/*Inserisce nella tabella A_ABILITY l'abilità castata*/
		statement.executeUpdate(""
				+ "insert into A_ABILITY(COD_M,COD_MA, A_NAME)"
				+ " values (" + COD_M + "," + COD_MA + ", '" + ability + "')");

		String response = " Monster:" + COD_M + " attacked monster:" + COD_MA + " with ability:" + ability;
		return response;
	}

	private int findPosition(int COD_M) throws SQLException{
		statement = connect.createStatement();

		/*Estraggo dal database la pos del mostro cod_m*/
		resultSet = statement.executeQuery("select POS"
				+ " from MONSTER_FIGHTING"
				+ " where COD_M =" + COD_M);

		if (!resultSet.next())
			return -1; 	/*mostro non presente nel team*/

		int pos = resultSet.getInt("POS");
		return pos;
	}

	private int mSwitch2(int COD_M,int  pos) throws SQLException{			//switch tra mostro e posizione vuota
		statement = connect.createStatement();
		/*Mette il mostro COD_M nella posizione pos SENZA scambio con altri mostri*/
		statement.executeUpdate("UPDATE MONSTER_FIGHTING"
				+ " SET POS = " + pos
				+ " where COD_M = " + COD_M );
		return 0;
	}


	private int mSwitch(int COD_M, int COD_MA) throws SQLException{ 		//Switch di posizione fra due mostri
		statement = connect.createStatement();
		/*registro posizioni di entrambi i mostri*/
		int pos1 = findPosition(COD_M);
		int pos2 = findPosition(COD_MA);
		/*Switch delle posizioni nel database evitando di 2 posizioni uguali in monster fighting*/
		statement.executeUpdate("UPDATE MONSTER_FIGHTING" 
				+ " SET POS = -1"
				+ " where COD_M = " + COD_MA );
		statement.executeUpdate("UPDATE MONSTER_FIGHTING"
				+ " SET POS = " + pos2
				+ " where COD_M = " + COD_M );
		statement.executeUpdate("UPDATE MONSTER_FIGHTING"
				+ " SET POS = " + pos1
				+ " where COD_M = " + COD_MA );

		/*non so cosa metterici come return*/
		return 0;
	}


	private int findCod(int POS, String user) throws SQLException{
		statement = connect.createStatement();
		/*Estraggo dal database il cod_m del mostro nella posizione 'pos' appartenente all' utente 'user'*/
		/*migliorabile*/
		resultSet = statement.executeQuery(""
				+ "select mo.COD_M"
				+ " from MONSTER_FIGHTING mf, MONSTER_OWNED mo"
				+ " where POS = " + POS + " "
				+ "and mo.ID_OWNER = '" + user +"'"
				+ " and mf.COD_M = mo.COD_M");

		if (!resultSet.next())
			return -1; 			/*nessun mostro in quella posizione*/	

		int COD_MA = resultSet.getInt("COD_M");
		return COD_MA;
	}

	private boolean checkLearnedAbility (String A_NAME, int COD_M) throws SQLException { 	//controlla che l'abilità appartenga al mostro
		statement = connect.createStatement();		

		resultSet = statement.executeQuery("select * " +
				"from MONSTER_ABILITY " +
				"where A_NAME = " + "'" + A_NAME + "' " +
				"and COD_M = " + COD_M );

		if (!resultSet.next())
			return false; 	//monster COD_M hasn't learned the ability

		return true;		//monster COD_M knows the ability 
	}


	private String checkStatus (int COD_M) throws SQLException { 		//controlla lo status di un mostro in combattimento
		statement = connect.createStatement();

		resultSet = statement.executeQuery("select STATUS " +
				"from MONSTER_FIGHTING " +
				"where COD_M = " + COD_M );

		if (!resultSet.next())
			return "monster: " + COD_M + " not in monster_fighting"; 	//user isn't the owner of monster COD_M

		return resultSet.getString("STATUS");		//user is owner of COD_M 
	}


	private int findRange (int COD_M) throws SQLException {		//restituisce l'attack range del mostro COD_M
		statement = connect.createStatement();

		resultSet = statement.executeQuery("select ATTKRANGE " +
				"from CLASSES c, MONSTER m, MONSTER_OWNED mo " +
				"where mo.DENOMINATION = m.DENOMINATION " +
				"and c.CLASS = m.CLASS " +
				"and mo.COD_M = " + COD_M );

		if (!resultSet.next())
			return -1; 	//user isn't the owner of monster COD_M

		return resultSet.getInt("ATTKRANGE");		//user is owner of COD_M 
	}


	private int findRange (String a_name) throws SQLException { 		//restituisce l'attack range dell'abilita a_name
		statement = connect.createStatement();

		resultSet = statement.executeQuery(""
				+ "select A_ATTK_RANGE "
				+"from ABILITY "
				+"where A_NAME = '" + a_name + "'");

		if (!resultSet.next())
			return -1; 	//ability not found

		return resultSet.getInt("A_ATTK_RANGE");		//ability found 
	}

	private String findAbilityClass(String a_name)throws SQLException {			//ritorna la classe dell'abilità
		statement = connect.createStatement();

		resultSet = statement.executeQuery("select A_CLASS " +
				"from ABILITY " +
				"where A_NAME = " + "'" + a_name + "'");

		if (!resultSet.next())
			return "Ability not found"; 	//user isn't the owner of monster COD_M

		return resultSet.getString("A_CLASS");		//user is owner of COD_M 
	}

	private String findMonsterClass(int COD_M)throws SQLException {			//ritorna la classe del mostro
		statement = connect.createStatement();

		resultSet = statement.executeQuery(""
				+ "select CLASS"
				+ " from MONSTER m, MONSTER_OWNED mo"
				+ " where mo.COD_M = " + COD_M 
				+ " and m.DENOMINATION = mo.DENOMINATION ");

		if (!resultSet.next())
			return "Ability not found"; 	//user isn't the owner of monster COD_M

		return resultSet.getString("CLASS");		//user is owner of COD_M 
	}

	public int nEquipped (String user, String w_name) throws SQLException {		// ritorna la quantità di oggetti equipaggiati
		int nEquip;
		statement = connect.createStatement();
		resultSet = statement.executeQuery("select W_EQUIPPED " +
				"from USER u, WEARABLE_OWNED w " +
				"where u.ID = " + "'" + user + "' " +
				"and w.W_NAME = " + "'" + w_name + "' " +
				"and u.ID = w.ID_OWNER");

		if (!resultSet.next())
			return 0;

		nEquip= resultSet.getInt("W_EQUIPPED");
		return nEquip;
	} 

	public int nOwned (String user, String w_name) throws SQLException {		// ritorna la quantità di oggetti posseduti
		int nOwned;
		statement = connect.createStatement();
		resultSet = statement.executeQuery("select W_QUANTITY " +
				"from USER u, WEARABLE_OWNED w " +
				"where u.ID = " + "'" + user + "' " +
				"and w.W_NAME = " + "'" + w_name + "' " +
				"and u.ID = w.ID_OWNER");

		if (!resultSet.next())
			return 0;

		nOwned= resultSet.getInt("W_QUANTITY");
		return nOwned;
	} 

	public int nAvailable(String user, String w_name) throws SQLException {		// ritorna la quantità di oggetti posseduti non in uso
		return nOwned(user, w_name) - nEquipped(user, w_name);		//user is owner of item w_name
	} 

	public String insertUser (String user, String pw)  throws SQLException, JSONException {
		if (checkUser(user) == true){
			return error_to_json("Name already taken");		
		}
		statement = connect.createStatement();
		preparedStatement = connect
				.prepareStatement("insert into USER (ID, PW, MANA) values (?, ?, 10)");

		preparedStatement.setString(1, user);
		preparedStatement.setString(2, pw);
		preparedStatement.executeUpdate();
		return "Utente registrato";	    
	} 

	public String loginUser (String user, String pw) throws SQLException, JSONException { 

		statement = connect.createStatement();
		resultSet = statement.executeQuery("select * from USER where ID = " + "'" + user + "'" + " and PW = " + "'" + pw + "'");

		if (!resultSet.next())
			return error_to_json("Login error: username or password mismatched"); 

		String response = "You are now logged in as: " + user;
		return response; 
	}

	public String deleteUser (String user) throws SQLException { 

		statement = connect.createStatement();
		statement.executeUpdate("delete from USER where ID = " + "'" + user + "'");

		String response = "User " + user + " has been deleted";
		return response; 
	}

	public String mEquip (int COD_M, String w_name) throws SQLException, JSONException { 
		String user = findOwner(COD_M);
		if (checkOwner(user, w_name) == false){
			return error_to_json("Item " + w_name + " not found in " + user + " inventory");
		}

		if (nAvailable(user, w_name) < 1){
			return error_to_json("Not enought " + w_name + " available");
		}

		statement = connect.createStatement();	
		statement.executeUpdate("insert into EQUIPPED (W_NAME, COD_M) values " +
				"( " + "'" + w_name + "'" + ", " + COD_M + " )");

		int nEquipped = nEquipped(user, w_name) +1;
		statement.executeUpdate("UPDATE WEARABLE_OWNED " +
				"SET W_EQUIPPED = " + nEquipped + " " +
				"where W_NAME = '" + w_name + "' " +
				"and ID_OWNER = '" + user + "'");

		String response = "Item " + w_name + " has been equipped from monster " + COD_M;
		return response; 		
	}

	public String mUnequip (int COD_M, String w_name) throws SQLException, JSONException { 
		String user = findOwner(COD_M);
		if (checkOwner(user, w_name) == false){
			return error_to_json("Item " + w_name + " not found in " + user + " inventory");
		}

		int rs = statement.executeUpdate("delete from EQUIPPED " +
				"where COD_M = " + COD_M + " " +
				"and W_NAME = '" + w_name  + "'");
		if (rs == 0)
			return error_to_json("Item " + w_name + " is not equipped");

		int nEquipped = nEquipped(user, w_name) -1;
		statement.executeUpdate("UPDATE WEARABLE_OWNED " +
				"SET W_EQUIPPED = " + nEquipped + " " +
				"where W_NAME = '" + w_name + "' " +
				"and ID_OWNER = '" + user + "'");

		String response = "Item " + w_name + " has been unequipped from monster " + COD_M;
		return response; 
	}

	public String mInfo (String denomination) throws SQLException {
		statement = connect.createStatement();
		resultSet = statement.executeQuery(
				"select * " +
						"from MONSTER " +
						"where  DENOMINATION = " + "'" + denomination + "'");

		String response;
		response = resultset_to_json (resultSet);
		return response;
	}

	public String moInfo (int COD_M)	throws SQLException {
		statement = connect.createStatement();
		resultSet = statement.executeQuery(
				"select * " +
						"from MONSTER_OWNED " +
						"where COD_M = " + COD_M );

		String response;
		response = resultset_to_json (resultSet);
		return response;
	}

	public String mfInfo (int COD_M)	throws SQLException {
		statement = connect.createStatement();
		resultSet = statement.executeQuery(
				"select * " +
						"from MONSTER_FIGHTING " +
						"where COD_M = " + COD_M );
		String response;
		response = resultset_to_json (resultSet);
		return response;
	}

	public String wInfo (String w_name)	throws SQLException {
		statement = connect.createStatement();
		resultSet = statement.executeQuery(
				"select * " +
						"from WEARABLE " +
						"where W_NAME = " + w_name );
		String response;
		response = resultset_to_json (resultSet);
		return response;
	}

	public String mWInfo (int COD_M) throws SQLException {
		statement = connect.createStatement();
		resultSet = statement.executeQuery(
				"select * " +
						"from EQUIPPED e, WEARABLE w " +
						"where e.COD_M = " + COD_M + " " +
				"and w.W_NAME = e.W_NAME");

		String response;
		response = resultset_to_json (resultSet);
		return response;
	}

	public String mFighting (String user) throws SQLException, JSONException { 

		if (checkUser(user) == false) {
			return error_to_json("User not found");
		}

		statement = connect.createStatement();
		resultSet = statement.executeQuery(
				"select * " +
						"from MONSTER_FIGHTING mf, MONSTER_OWNED mo " +
						"where  mo.ID_OWNER = " + "'" + user + "'" + " " +
				"and mo.COD_M = mf.COD_M ");

		String response;
		response = resultset_to_json (resultSet);	

		return response; 
	}

	public String mAbility (int COD_M) throws SQLException { 

		statement = connect.createStatement();
		resultSet = statement.executeQuery(
				"select * " +
						"from ABILITY a, MONSTER_ABILITY ma " +
						"where  ma.COD_M = " + COD_M + " " +
				"and ma.A_NAME = a.A_NAME ");

		String response;
		response = resultset_to_json (resultSet);

		return response; 
	}

	public String mEquipped (int COD_M) throws SQLException { 

		statement = connect.createStatement();
		resultSet = statement.executeQuery(
				"select * " +
						"from EQUIPPED e, WEARABLE w " +
						"where  e.COD_M = " + COD_M + " " +
				"and e.W_NAME = w.W_NAME ");

		String response;
		response = resultset_to_json (resultSet);

		return response; 
	}

	public String mCollection (String user) throws SQLException { 
		statement = connect.createStatement();
		resultSet = statement.executeQuery(
				"select * " +
						"from MONSTER_OWNED mo " +
						"where  mo.ID_OWNER = '" + user + "'");

		String response;
		response = resultset_to_json (resultSet);

		return response; 
	}
	
	public String showInTeam (String user) throws SQLException { 
		statement = connect.createStatement();
		resultSet = statement.executeQuery(
				"select * " +
						"from TEAM t " +
						"where  t.ID_USER = '" + user + "'");

		String response;
		response = resultset_to_json (resultSet);

		return response; 
	}

	public String showNotInTeam (String user) throws SQLException { 
		statement = connect.createStatement();
		resultSet = statement.executeQuery(
				"select * " +
						"from MONSTER_OWNED mo " +
						"where  mo.ID_OWNER = '" + user + "'" +
						"and mo.COD_M not in " +
						"(select t.COD_M " +
						"from TEAM t " +
						"where t.ID_USER = '" + user + "')");

		String response;
		response = resultset_to_json (resultSet);

		return response; 
	}
	
	public String sCollection (String user) throws SQLException { 
		statement = connect.createStatement();
		resultSet = statement.executeQuery(
				"select * " +
						"from SUPPLIES_OWNED " +
						"where ID_OWNER = '" + user + "' ");

		String response;
		response = resultset_to_json (resultSet);

		return response; 
	}

	public String wCollection (String user) throws SQLException { 
		statement = connect.createStatement();
		resultSet = statement.executeQuery(
				"select * " +
						"from WEARABLE_OWNED " +
						"where ID_OWNER = '" + user + "'");

		String response;
		response = resultset_to_json (resultSet);

		return response; 
	}

	public String wCollectionType (String user, String type) throws SQLException { 
		statement = connect.createStatement();
		resultSet = statement.executeQuery(
				"select * " +
						"from WEARABLE_OWNED wo, WEARABLE w " +
						"where  wo.ID_OWNER = '" + user + "' " +
						"and wo.W_NAME = w.W_NAME " +
						"and w.W_TYPE = '" + type + "'");

		String response;
		response = resultset_to_json (resultSet);

		return response; 
	}	

	public String matchMaking () throws SQLException{
		statement = connect.createStatement();
		resultSet = statement.executeQuery(
				"select * " +
						"from MATCHMAKING " +
				"order by PRIORITY");

		String response;
		response = resultset_to_json (resultSet);

		return response; 
	}

	public String joinMatchMaking (String user) throws SQLException{
		statement = connect.createStatement();
		int lvl = lvlAvg(user);

		statement.executeUpdate("insert into MATCHMAKING " +
				"(ID, LVL) values " +
				"( '" + user + "', " + lvl + " )" );
		return "You are now in queue: waiting for a foe...";
	}

	public String exitMatchMaking (String user) throws SQLException{
		statement = connect.createStatement();		
		statement.executeUpdate("delete from MATCHMAKING " +
				"where ID = '" + user + "'" );
		return "You are no longer in queue";
	}


	public String learnAbility(String a_name, int COD_M) throws SQLException{
		statement = connect.createStatement();

		if(!(findMonsterClass(COD_M)).contentEquals(findAbilityClass(a_name)))
			return "Monster: " + COD_M + " can't learn ability: " + a_name + " different classes";

		statement.executeUpdate(""
				+ "insert into MONSTER_ABILITY (A_NAME, COD_M)"
				+ " values ('" + a_name + "', " + COD_M + ")");

		String response = "Monster:" + COD_M + " learned ability:" + a_name;
		return response;
	}


	public String mAddTeam (int COD_M) throws SQLException { 
		statement = connect.createStatement();
		String user = findOwner(COD_M);

		if(!checkTeamSize(findOwner(COD_M)))
			return "Nel team sono gia presenti 9 mostri";

		statement.executeUpdate(""
				+ "insert into TEAM (ID_USER, COD_M) values('" + user + "'," + COD_M + ")");

		String response = "Mostro COD_M:" + COD_M + " inserito nel team";
		return response; 
	}


	public String mRemoveTeam (int COD_M) throws SQLException { 
		statement = connect.createStatement();

		int up = statement.executeUpdate("delete from TEAM where COD_M = "  + COD_M );

		if(up == 0)
			return "Mostro non presente nel team";

		String response = "Mostro COD_M:" + COD_M + " tolto dal team";
		return response; 
	}

	public boolean checkExp (int COD_M) throws SQLException { 	// controlla se è il momento di fare il lvlUp
		statement = connect.createStatement();
		resultSet = statement.executeQuery(
				"select EXP, LVL " +
						"from MONSTER_OWNED " +
						"where COD_M = " + COD_M);
		resultSet.next();

		if (resultSet.getInt("EXP") >= resultSet.getInt("LVL")*100){
			return true;		// true -> se è il momento di fare il lvlUp
		}

		return false; 		// false -> se non è il momento di fare il lvlUp
	}

	public String lvlUp (int COD_M) throws SQLException { 
		statement = connect.createStatement();

		resultSet = statement.executeQuery(
				"select LVL, EXP " +
						"from MONSTER_OWNED " +
						"where COD_M = " + COD_M);
		resultSet.next();
		int lvl = resultSet.getInt("LVL") + 1;
		int exp = resultSet.getInt("EXP") - lvl*100;
		statement.executeUpdate("UPDATE MONSTER_OWNED " +
				"SET LVL = " + lvl + ", " + 
				"EXP = " + exp + " " +
				"where COD_M = " + COD_M);
		return "LvlUp!"; 
	}

	public int lvlAvg(String user) throws SQLException{
		statement = connect.createStatement();
		resultSet = statement.executeQuery("select AVG(LVL) from MONSTER_OWNED mo, TEAM t where t.COD_M = mo.COD_M  and mo.ID_OWNER = '" + user +"'" );
		if (!resultSet.next())
			return 0; 	//user hasn't monster in team

		return resultSet.getInt("AVG(LVL)");
	}

	private int mAction(int COD_M,  String action) throws SQLException{
		statement = connect.createStatement();

		/*Inserisce l'azione 'action' del mostro 'cod_m' nella tabella MONSTER_ACTION*/
		statement.executeUpdate(""
				+ "insert into MONSTER_ACTION (COD_M, ACTION)"
				+ " values (" + COD_M + ", '" + action + "');");

		return 0;
	}

	public String aAttack(int COD_M,  int COD_MA) throws SQLException, JSONException{
		statement = connect.createStatement();

		if(!(findFoe(findOwner(COD_M))).equalsIgnoreCase(findOwner(COD_MA)))
			return error_to_json("Attack target from another battle");

		if (checkStatus(COD_M)=="Dead")
			return error_to_json("Your monster is dead, can't attack");
		
		if (checkStatus(COD_MA)=="Dead")
			return error_to_json("There's no point attacking a dead target");
		
		if(findRange(COD_M) < (findPosition(COD_MA) / 3))				//controllo che l'attaccante riesca a raggiungere il bersaglio (ATTKRANGE)
			return error_to_json("Monster: "+ COD_M +" can't attack monster: " + COD_MA + " not enought attack range"); 

		mAction(COD_M,  "attack");
		/*inserisce nella tabella A_ATTACK l'attacco effettuato*/
		statement.executeUpdate(""
				+ "insert into A_ATTACK(COD_M,COD_MA)"
				+ " values (" + COD_M + "," + COD_MA + ")");
		/*Manca la parte di attuazione attacco con conseguente diminuzione di hp e morte*/
		attackEffect(COD_M, COD_MA);
		String response = " Monster:" + COD_M + " attacked monster:" + COD_MA;
		return response;
	}

	public String aMove(int COD_M,  int pos) throws SQLException{
		statement = connect.createStatement();
		String user = findOwner(COD_M);
		int COD_MA = findCod(pos, user);

		if(COD_MA == -1) /*significa che nella posizione 'pos' non c'è alcun mostro*/
			mSwitch2(COD_M, pos);
		else
			mSwitch(COD_M, COD_MA); /*scambia le posizione dei mostri*/

		mAction(COD_M,  "move");
		/*Inserisce nella tabella A_MOVE lo lo spostamento effettuato*/
		statement.executeUpdate(""
				+ "insert into A_MOVE(COD_M, POS)"
				+ " values (" + COD_M + "," + pos + ")");

		String response = " Monster:" + COD_M + " moved into position:" + pos;
		return response;
	}

	public boolean searchMatch (String user)  throws SQLException{
		statement = connect.createStatement();
		resultSet = statement.executeQuery("select * " +
				"from GAME " +
				"where ID1 = '" + user +"' " +
				"or ID2 = '" + user +"' ");
		if (!resultSet.next())
			return false; 	//user hasn't a match yet
		return true;
	}

	public String clearGames() throws SQLException{
		statement = connect.createStatement();

		statement.executeUpdate("truncate GAME");

		String response = "Games cleared";
		return response;
	}

	public String clearQueue() throws SQLException{
		statement = connect.createStatement();

		statement.executeUpdate("truncate MATCHMAKING");

		String response = "Queue cleared";
		return response;
	}

	public String createGame (String id1, String id2) throws SQLException { 
		statement = connect.createStatement();

		Date d = new Date();
		
		int up = statement.executeUpdate("insert into GAME (ID1, ID2, Tm) values ( '"  + id1 + "', '" + id2 + "', '" + d.getHours() + ":" + d.getMinutes() + ":" + d.getSeconds() + "')");

		if(up == 0)
			return "Inserimento non riuscito";

		String response = "Game started: " + id1 + " vs. " + id2;
		return response; 
	}

	public String clearActionQueue(String user) throws SQLException{
		statement = connect.createStatement();

		//Cancella tutte le monster_action riguardnti un certo user
		statement.executeUpdate("delete from MONSTER_ACTION " +
				"where COD_M in " +
				"( select COD_M from TEAM " +
				"where ID_USER = '" + user + "')");

		//Cancella tutte le a_attack riguardnti un certo user
		statement.executeUpdate("delete from A_ATTACK " +
				"where COD_M in " +
				"( select COD_M from TEAM " +
				"where ID_USER = '" + user + "')");

		//Cancella tutte le a_ability riguardnti un certo user
		statement.executeUpdate("delete from A_ABILITY " +
				"where COD_M in " +
				"( select COD_M from TEAM " +
				"where ID_USER = '" + user + "')");

		//Cancella tutte le a_move riguardnti un certo user
		statement.executeUpdate("delete from A_MOVE " +
				"where COD_M in " +
				"( select COD_M from TEAM " +
				"where ID_USER = '" + user + "')");

		String response = "ActionQueue cleared";
		return response;
	}


	public boolean addToFighting(int COD_M, int pos) throws SQLException, JSONException{
		//Controlla che la posizione non sia gia occupata
		if(findCod(pos, findOwner(COD_M)) != -1)
			return false;		

		statement = connect.createStatement();
		//Estraggo dall'array di JSON l'unico oggetto inserito
		JSONArray jarr = new JSONArray(showMonsterStatWithBonus(COD_M));
		JSONObject monsterStat = jarr.getJSONObject(0);
		//Inserisce in moster fighting il mostro COD_M nella posizione POS
		statement.executeUpdate(""
				+ "insert into MONSTER_FIGHTING (COD_M, POS, HP, AD, AP ,DEF ,MDEF)"
				+ " values (" + COD_M
				+ ", " + pos 
				+ ", " + monsterStat.getInt("HP") 
				+ ", " + monsterStat.getInt("AD") 
				+ ", " + monsterStat.getInt("AP") 
				+ ", " + monsterStat.getInt("DEF") 
				+ ", " + monsterStat.getInt("MDEF") + ")");

		return true; //inserimento riuscito
	}

	public String showMonsterStat(int COD_M) throws SQLException{		//Restituisce statistiche riguardanti un mostro in combattimento
		statement = connect.createStatement();
		//ritorna tabella statistiche posizione del mostro COD_M
		resultSet = statement.executeQuery(""
				+ "select m.HP+c.HP*mo.LVL as HP,"
				+ " m.AD+c.AD*mo.LVL as AD,"
				+ " m.AP+c.AP*mo.LVL as AP,"
				+ " m.DEF+c.DEF*mo.LVL as DEF,"
				+ " m.MDEF+c.MDEF*mo.LVL as MDEF"
				+ " from MONSTER m, CLASSES c, MONSTER_OWNED mo"
				+ " where m.class = c.class"
				+ " and mo.denomination = m.denomination"
				+ " and mo.COD_M = " + COD_M );
		//Trasforma in stringa JSON
		String response= resultset_to_json (resultSet);	
		return response;
	}

	public String showMonsterStatWithBonus(int COD_M) throws SQLException, JSONException{		//Restituisce statistiche riguardanti un mostro in combattimento
		JSONArray jstat = new JSONArray(showMonsterStat(COD_M));		
		JSONObject stat = jstat.getJSONObject(0);						//statistiche base


		JSONArray wj = new JSONArray(mWInfo(COD_M));		//info equipaggiamento mostro
		JSONObject wearable;

		for (int i=0; i<wj.length(); i++){		//damage dovuto a oggetti
			wearable = wj.getJSONObject(i);
			stat.put("HP", stat.getInt("HP") + wearable.getInt("W_HP"));
			stat.put("AD", stat.getInt("AD") + wearable.getInt("W_AD"));
			stat.put("AP", stat.getInt("AP") + wearable.getInt("W_AP"));
			stat.put("DEF", stat.getInt("DEF") + wearable.getInt("W_DEF"));
			stat.put("MDEF", stat.getInt("MDEF") + wearable.getInt("W_MDEF"));
		}		  	
		return stat.toString();
	}

	public String attackEffect(int COD_A, int COD_T) throws SQLException, JSONException{		//COD_A -> COD_M attaccante & COD_T -> COD_M difensore
		JSONArray aj = new JSONArray(mfInfo(COD_A));
		JSONObject aStat = aj.getJSONObject(0);		//statistiche attaccante

		JSONArray tj = new JSONArray(mfInfo(COD_T));
		JSONObject tStat = tj.getJSONObject(0);		//statistiche target

		int attkD = aStat.getInt("AD");			//ad damage, attacco fisico base

		int defD = tStat.getInt("DEF");			//difesa fisica

		int D = attkD - defD;
		if (D<0)	D=0;

		int totDamage = D;
		if (tStat.getInt("HP") < totDamage)
			totDamage = tStat.getInt("HP");

		statement = connect.createStatement();
		statement.executeUpdate(""
				+ "update MONSTER_FIGHTING "
				+ "set HP = HP - " + totDamage + " "
				+ "where COD_M = " + COD_T);
		return "Attack effect calculated";
	}

	public String abilityEffect(int COD_A, int COD_T, String a_name) throws SQLException{		//COD_A -> COD_M attaccante & COD_T -> COD_M difensore
		statement = connect.createStatement();

		resultSet = statement.executeQuery("");
		String response= resultset_to_json (resultSet);	
		return response;
	}

	public String buy(String denomination, String user, String name) throws SQLException, JSONException{		
		int monsterValue = findMonsterValue(denomination);
		if(findUserMana(user) - monsterValue < 0)			//Controllo che il mana sia sufficiente
			return "Not enought mana";

		changeMana(-monsterValue,user);							//Acquisto avvenuto sottraggo il mana all'user
		statement = connect.createStatement();
		int up =statement.executeUpdate(""						//Aggiungo il mostro tra i moster owned di user
				+ "insert into MONSTER_OWNED (DENOMINATION, NAME,  ID_OWNER)"
				+ " values ('"+denomination+"', '" + name + "', '" + user +"')");
		if(up != 1)
			return error_to_json("Error in buyMonster"); ;

		return "Monster in user: " + user + " inventory";
	}

	public String buy(String w_name, String user) throws SQLException, JSONException{		
		int wearableValue = findWearableValue(w_name);
		if(findUserMana(user) - wearableValue < 0)			//Controllo che il mana sia sufficiente
			return "Not enought mana";

		changeMana(-wearableValue,user);						//Acquisto avvenuto sottraggo il mana all'user
		statement = connect.createStatement();

		if(checkOwner(user, w_name) == false){				//Se l'user non possiede ancora l'oggetto a_name
			int up =statement.executeUpdate(""					//Inserisco l'oggetto nell'inventario
					+ "insert into WEARABLE_OWNED (W_NAME,  ID_OWNER)"
					+ " values ('"+ w_name +"', '" + user +"')");
			if(up != 1)
				return error_to_json ("Error in buyWearable");
			return "Wearable added in user: " + user + " inventory";
		}
		else{																			//Se l'user possiede l'oggetto a_name
			int up =statement.executeUpdate(""
					+ "update WEARABLE_OWNED"				//incremento il quantity in wearable owned di user
					+ " set W_QUANTITY = W_QUANTITY + 1"
					+ " where W_NAME = '" + w_name + "'");
			if(up != 1)
				return error_to_json("Error in buyWearable");		
			return "Wearable added in user: " + user + " inventory";
		}
	}

	public int findUserMana( String user) throws SQLException{		
		statement = connect.createStatement();
		resultSet = statement.executeQuery(""					
				+ "select MANA"
				+ " from USER"
				+ " where ID = '" + user + "'");
		
		if (!resultSet.next())
			return -1; 	//user hasn't a match yet

		return resultSet.getInt("MANA");		
	}

	public int findMonsterValue( String denomination) throws SQLException{		
		statement = connect.createStatement();
		resultSet = statement.executeQuery(""
				+ "select VALUE"
				+ " from MONSTER_STORE"
				+ " where DENOMINATION = '" + denomination + "'");
		
		if (!resultSet.next())
			return -1; 	//monster hasn't a match yet

		return resultSet.getInt("VALUE");		
	}

	public int findWearableValue( String w_name) throws SQLException{		
		statement = connect.createStatement();
		resultSet = statement.executeQuery(""
				+ "select VALUE"
				+ " from WEARABLE_STORE"
				+ " where W_NAME = '" + w_name + "'");
		
		if (!resultSet.next())
			return -1; 	//user wearable hasn't match yet

		return resultSet.getInt("VALUE");		
	}

	public int changeMana(int MANA, String user) throws SQLException{		
		statement = connect.createStatement();
		return statement.executeUpdate(""
				+ "update user"
				+ " set MANA = MANA + " + MANA  
				+ " where ID = '" + user + "'" );	
	}
	
	public String showMonsterStore() throws SQLException{
		statement = connect.createStatement();
		resultSet = statement.executeQuery(""
				+ "select *"
				+ " from MONSTER_STORE ms, MONSTER m"
				+ " where m.DENOMINATION = ms.DENOMINATION");
		return resultset_to_json(resultSet);
	}

	public String showWearableStore() throws SQLException{
		statement = connect.createStatement();
		resultSet = statement.executeQuery(""
				+ "select * "
				+ "from WEARABLE_STORE ws, WEARABLE w"
				+ " where w.W_NAME = ws.W_NAME");
		return resultset_to_json(resultSet);
	}
	
} 
