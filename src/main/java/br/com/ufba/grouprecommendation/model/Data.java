package br.com.ufba.grouprecommendation.model;

import br.com.ufba.grouprecommendation.dao.MySQLObject;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.Period;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class Data {

    
    /* Carrega dados fictícios no Mysql */
    public void putMySQLSyntheticData() throws SQLException, ParseException {
        
        Statement s =  MySQLObject.getConexaoMySQL().createStatement(); 
        String sqlQuery =  " DELETE FROM mate84.users  " ;
        s.executeUpdate(sqlQuery);
        
        LocalDateTime timeStart = LocalDateTime.of(2017, Month.DECEMBER, 01, 9, 00, 00);
        LocalDateTime timeEnd = LocalDateTime.of(2017, Month.DECEMBER, 01, 22, 00, 00);
        Duration oneHours = Duration.between(timeStart, timeEnd);
        
        long qtHoras = oneHours.toHours();
        Integer qtVotesByHour = 10;
        Integer intervalo = (60/qtVotesByHour); /* Partition of Synthetic Load: Intervalo de tempo entre os votos (se a quantidade de votos definida é 10 60/10 dará uma média de um voto a cada 6min) */
        Integer qtUsers = 4;
        Integer rating=5;
              
        /* Fields */
        LocalDateTime datetime = timeStart ;
        double value;
        double scale;
        String sensor_type = "";
        String unit="";
        String userid;
         
        /* Support */
        Integer margemtime;
        Random rn = new Random();
        Integer count=0;
        
        /* Generate Synthetic Data to MySql */
        for (int hour=0;hour<=qtHoras;hour++) {
            
            for (int v=0;v<=qtVotesByHour;v++) {
                count+=1;
                
                margemtime= (rn.nextInt(intervalo - 1 + 1) + 1);
                datetime = LocalDateTime.of(datetime.toLocalDate(), datetime.toLocalTime()).plusMinutes(margemtime);
                value = (rn.nextInt(rating - 1 + 1) + 1);
                scale = rn.nextInt((28 - 22) + 1) + 22;;
                sensor_type = "temperature";
                unit="celsius";
                userid =  "User" + (rn.nextInt(qtUsers - 1 + 1) + 1);
                
                sqlQuery="INSERT INTO mate84.users VALUES(" + count + ",'" + Timestamp.valueOf(datetime) + "'," + scale + "," + value + ",'" + sensor_type + "','" + unit + "','"  + userid + "')";
                System.out.println(sqlQuery);
                s.executeUpdate(sqlQuery);
                
            }
        
        }
    }
    
    /* Obtem os dados do Mysql */
    public List<User> getMySQLSyntheticData(Integer ultimas_horas) throws SQLException {
        
        /* Recebe dados do Banco */
        Statement sgeral =  MySQLObject.getConexaoMySQL().createStatement(); 
        ResultSet rsTemp;
        
        Statement sUserSelc =  MySQLObject.getConexaoMySQL().createStatement(); 
        //String sqlQuery =  " SELECT CONVERT(time,DATETIME) AS time, scale, value, userid  FROM mate84.users WHERE CONVERT(time,DATETIME) BETWEEN DATE_ADD(CONVERT(NOW(),DATETIME), INTERVAL -1 HOUR) AND CONVERT(time,DATETIME)  ORDER BY  CONVERT(time,DATETIME) DESC " ;
        ResultSet rsUsersByLastHour; //= sUserSelc.executeQuery(sqlQuery);

        Statement stmscale =  MySQLObject.getConexaoMySQL().createStatement(); 
        String sqlQuery =  " SELECT DISTINCT scale FROM mate84.users ORDER BY scale  " ;
        ResultSet rsScale = stmscale.executeQuery(sqlQuery);

        Statement stmuser =  MySQLObject.getConexaoMySQL().createStatement(); 
        sqlQuery =  " SELECT DISTINCT userid FROM mate84.users " ;
        ResultSet rsUsers = stmuser.executeQuery(sqlQuery);

        boolean hasvoted = false;
        Vote vote;
        List<Vote> ListVote = new ArrayList<>();
        User u;
        List<User> ListUsers = new ArrayList<>();

        while (rsUsers.next()) {   /* Usuário X */
            
            ListVote = new ArrayList<>();
            
            while (rsScale.next()) { /* Em um dado item da escala Y */

                /* Consulta horas do usuário seleiconado */
                sqlQuery =    " SELECT CONVERT(time,DATETIME) AS time, scale, value, userid  FROM mate84.users  "
                            + " WHERE CONVERT(time,DATETIME) BETWEEN DATE_ADD(CONVERT(NOW(),DATETIME), INTERVAL " + (ultimas_horas * (-1)) + " HOUR) AND CONVERT(time,DATETIME) "
                            + " AND userid = '" + rsUsers.getString("userid") + "' "
                            + " AND scale = "  + rsScale.getString("scale") + " "
                            + " ORDER BY  CONVERT(time,DATETIME) DESC " ;
                System.out.println(sqlQuery);
                rsUsersByLastHour = sUserSelc.executeQuery(sqlQuery);
                
                while (rsUsersByLastHour.next()) { /* Nos registro de ultima hora  */  
                            //System.out.println(rsUsers.getString("userid"));
                            //System.out.println(rsScale.getString("scale"));
                            //System.out.println(rsUsersByLastHour.getString("value"));
                        
                            vote = new Vote();   
                            System.out.println(rsUsersByLastHour.getDouble("value"));
                            vote.setVote(rsUsersByLastHour.getDouble("value"));
                            vote.setScaleValue(rsUsersByLastHour.getDouble("scale"));
                            ListVote.add(vote);
                            hasvoted = true;
                            break;
                  
                }
                
                /*  Usuários que não votaram na ultioma hpra mas estão na lista aplicar o 
                    calculo da quantidade de vezes que ele votou em casa ítem e assumir a quantidade como o rating 
                    [min:1 Max: 5]*/
                if (!hasvoted) {
                    
                    //System.out.println(rsUsers.getString("userid"));
                    //System.out.println(rsScale.getString("scale"));
                    //System.out.println(rsUsersByLastHour.getString("value"));
                            
                    //Filtra pelo valor da escala, se for 0 o valor rating será 1, se for maior será a sumarização de no máximo 5 itens  
                    sqlQuery =  " SELECT count(*) total FROM mate84.users WHERE scale = " +  rsScale.getString("scale")  + " and userid='" + rsUsers.getString("userid") +"'";
                    System.out.println(sqlQuery);
                    rsTemp = sgeral.executeQuery(sqlQuery);
                    if (rsTemp.next()) {
                        //Preenche os votos vazios
                        vote = new Vote();       
                        if (rsTemp.getInt("total") > 0) {
                            vote.setVote((rsTemp.getInt("total")>5) ? 5 : rsTemp.getInt("total"));
                        } else if (rsTemp.getInt("total") == 0) {
                            vote.setVote(1);
                        }
                        vote.setScaleValue(rsScale.getDouble("scale"));
                        ListVote.add(vote);                        
                    }
                    
                }
                hasvoted = false;
                rsUsersByLastHour.first();
            }
            
            /* Adiciona Usuáiro */
            u = new User();
            u.setName(rsUsers.getString("userid"));
            u.setVote(ListVote);
            
            ListUsers.add(u);
            rsScale.beforeFirst();
        } 
        return ListUsers;
        
        
        
       
            
                
          
            
            
     
        
//        List<Vote> v = new ArrayList<Vote>();
//        
//        Vote vote;
//        vote = new Vote();
//        vote.setScaleValue(22);
//        vote.setScaleValue(5);
//        v.add(vote);
//              
//        vote = new Vote();
//        vote.setScaleValue(23);
//        vote.setScaleValue(1);
//        v.add(vote);
//
//        vote = new Vote();
//        vote.setScaleValue(24);
//        vote.setScaleValue(2);        
//        v.add(vote);
//        
//        vote = new Vote();
//        vote.setScaleValue(26);
//        vote.setScaleValue(2);                
//        v.add(vote);
//
//        vote = new Vote();
//        vote.setScaleValue(27);
//        vote.setScaleValue(2);                
//        v.add(vote);
//        
//        vote = new Vote();
//        vote.setScaleValue(28);
//        vote.setScaleValue(2);                
//        v.add(vote);
//
//        /* Adiciona nome e votos */
//        User u = new User();
//        u.setName("Usuário1");
//        u.setVote(v);
        
        
        
      

        
        
        
//        Data2 oVote;
//        List<Data2> Votos = new ArrayList<>();
//        
//        Random rn = new Random();
//     
//        for (int row = 1;row<=rates;row++) {
//            oVote= new Data2();
//            oVote.setName("User" +  row);
//        }

     }

    
}