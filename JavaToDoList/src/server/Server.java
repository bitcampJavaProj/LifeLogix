package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import DAO.TeamToDoListDAO;
import DTO.TeamToDoList;
import mysql.DBConnection;

public class Server {
	
	private static final int PORT = 9999;

    public static void main(String[] args) {
        // DB 연결 초기화
        DBConnection.openConnection();

        List<TeamToDoList> todoList = new LinkedList<>();

        todoList = TeamToDoListDAO.getTodoList("all");

        DBConnection.closeConnection();

        // 서버 소켓 생성 및 클라이언트 연결 대기
        
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server listening on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                new ClientHandler(clientSocket).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class ClientHandler extends Thread {
        private Socket clientSocket;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try (Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/userdb", "your_mysql_username", "your_mysql_password");
                    BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true)) {

                // 클라이언트로부터 요청 받음
                String request = reader.readLine();

                if (request.equals("회원 가입")) {
                    // 회원 가입 요청 처리
                    String userId = reader.readLine();
                    String userName = reader.readLine();
                    String password = reader.readLine();

                    if (insertUser(connection, userId, userName, password)) {
                        writer.println("가입 성공");
                    } else {
                        writer.println("가입 실패");
                    }
                } else if (request.equals("로그인")) {
                    // 로그인 요청 처리
                    String userId = reader.readLine();
                    String password = reader.readLine();

                    if (loginUser(connection, userId, password)) {
                        writer.println("로그인 성공");
                    } else {
                        writer.println("로그인 실패");
                    }
                }

            } catch (IOException | SQLException e) {
                e.printStackTrace();
            }
        }

        private boolean insertUser(Connection connection, String userId, String userName, String password) throws SQLException {
            String insertQuery = "INSERT INTO users (userid, username, password) VALUES (?, ?, ?)";
            try (PreparedStatement preparedStatement = connection.prepareStatement(insertQuery)) {
                preparedStatement.setString(1, userId);
                preparedStatement.setString(2, userName);
                preparedStatement.setString(3, password);

                int rowsAffected = preparedStatement.executeUpdate();
                return rowsAffected > 0;
            }
        }

        private boolean loginUser(Connection connection, String userId, String password) throws SQLException {
            String loginQuery = "SELECT * FROM users WHERE userid = ? AND password = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(loginQuery)) {
                preparedStatement.setString(1, userId);
                preparedStatement.setString(2, password);

                ResultSet resultSet = preparedStatement.executeQuery();
                return resultSet.next();
            }
        }
    }
}
