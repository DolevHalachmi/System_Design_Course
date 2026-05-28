#!/usr/bin/env python3
"""
Basic Python Server for STOMP Assignment – Stage 3.3

IMPORTANT:
DO NOT CHANGE the server name or the basic protocol.
Students should EXTEND this server by implementing
the methods below.
"""

import socket
import sys
import threading
import sqlite3
import os


SERVER_NAME = "STOMP_PYTHON_SQL_SERVER"  # DO NOT CHANGE!
DB_FILE = "stomp_server.db"              # DO NOT CHANGE!

#python3 ./sql_server.py 7778

def recv_null_terminated(sock: socket.socket) -> str:
    data = b""
    while True:
        chunk = sock.recv(1024)
        if not chunk:
            return ""
        data += chunk
        if b"\0" in data:
            msg, _ = data.split(b"\0", 1)
            return msg.decode("utf-8", errors="replace")

def init_database():
    try:
        connection = sqlite3.connect(DB_FILE)
        cur = connection.cursor()
        
        cur.executescript("""
            CREATE TABLE IF NOT EXISTS users (
                username VARCHAR(50) PRIMARY KEY,
                password VARCHAR(50) NOT NULL,
                registration_date DATETIME DEFAULT CURRENT_TIMESTAMP
                
            );

            CREATE TABLE IF NOT EXISTS login_history (
                logID INTEGER PRIMARY KEY AUTOINCREMENT,
                username VARCHAR(50) NOT NULL,
                login_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                logout_time DATETIME,
                FOREIGN KEY(username) REFERENCES users(username)
            );

            CREATE TABLE IF NOT EXISTS file_tracking (
                filesID INTEGER PRIMARY KEY AUTOINCREMENT,
                username VARCHAR(50) NOT NULL,
                filename VARCHAR(50) NOT NULL,
                upload_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY(username) REFERENCES users(username)
            );
            CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
            CREATE INDEX IF NOT EXISTS idx_login_username ON login_history(username);
        """)

        connection.commit()
        print(f"[{SERVER_NAME}] Database initialized")
    finally:
            try:
                connection.close()
            except Exception as e:
                return f'Error: {str(e)}'
    

def execute_sql_command(sql_command: str) -> str:
    try:
        with sqlite3.connect(DB_FILE) as connection:
            cur = connection.cursor()

            cur.execute(sql_command)
            connection.commit()
            return "done"   
    except Exception as e:
        return f'Error: {str(e)}'


def execute_sql_query(sql_query: str) -> str:
    try:
        with sqlite3.connect(DB_FILE) as conn:
            cur = conn.cursor()
            cur.execute(sql_query)
            rows = cur.fetchall()
            return str(rows)
    except Exception as e:
        return f'Error: {str(e)}'

def handle_client(client_socket: socket.socket, addr):
    print(f"[{SERVER_NAME}] Client connected from {addr}")

    try:
        while True:
            message = recv_null_terminated(client_socket)
            if message == "":
                break

            print(f"[{SERVER_NAME}] Received:")
            print(message)

            if message.strip().upper().startswith("SELECT"):
                result = execute_sql_query(message)
            else:
                result = execute_sql_command(message)
            client_socket.sendall((result + "\0").encode())

    except Exception as e:
        print(f"[{SERVER_NAME}] Error handling client {addr}: {e}")
    finally:
        try:
            client_socket.close()
        except Exception:
            pass
        print(f"[{SERVER_NAME}] Client {addr} disconnected")


def start_server(host="127.0.0.1", port=7778):
    server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)

    try:
        server_socket.bind((host, port))
        server_socket.listen(5)
        print(f"[{SERVER_NAME}] Server started on {host}:{port}")
        print(f"[{SERVER_NAME}] Waiting for connections")

        while True:
            client_socket, addr = server_socket.accept()
            t = threading.Thread(
                target=handle_client,
                args=(client_socket, addr),
                daemon=True
            )
            t.start()

    except KeyboardInterrupt:
        print(f"\n[{SERVER_NAME}] Shutting down server")
    finally:
        try:
            server_socket.close()
        except Exception:
            pass


if __name__ == "__main__":
    port = 7778
    if len(sys.argv) > 1:
        raw_port = sys.argv[1].strip()
        try:
            port = int(raw_port)
        except ValueError:
            print(f"Invalid port '{raw_port}', falling back to default {port}")
    init_database()
    start_server(port=port)
    