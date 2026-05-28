#include <iostream>
#include <thread>
#include <vector>
#include <sstream>
#include <fstream>
#include "../include/ConnectionHandler.h"
#include "../include/ClientState.h"
#include "../include/StompProtocol.h"
#include "../include/event.h"

using namespace std;

//make StompWCIClient
// ./bin/StompWCIClient
//login 127.0.0.1:7777 userA passA
//join Germany_Japan
//report data/events1.json
//summary Germany_Japan userA output.json

bool shouldTerminate = false;

vector<string> split(const string& s, char delimiter) {
    vector<string> tokens;
    string token;
    istringstream tokenStream(s);
    while (getline(tokenStream, token, delimiter)) {
        tokens.push_back(token);
    }
    return tokens;
}

void socketThreadFunc(ConnectionHandler* handler, ClientState* state) {
    StompProtocol protocol(*state);
    while (!shouldTerminate) {
        string answer;
        bool result = handler->getFrameAscii(answer, '\0');
        if (!result) {
            cout << "Disconnected from server." << endl;
            state->setLoggedIn(false);
            shouldTerminate = true;
            break;
        }
        protocol.process(answer);
        if (!state->isLoggedIn()) 
            break;
    }
}


void sendFrame(ConnectionHandler* handler, string command, string headers, string body) {
    stringstream stream;
    stream << command << "\n";
    stream << headers; 
    stream << "\n"; 
    stream << body;
    stream << "\0"; 
    
    string frame = stream.str();
    handler->sendFrameAscii(frame, '\0');
}

int main(int argc, char *argv[]) {
    ClientState state;
    ConnectionHandler* handler = nullptr;
    thread* socketThread = nullptr;

    while (!shouldTerminate) {
    
        const short bufsize = 1024;
        char buf[bufsize];
        cin.getline(buf, bufsize);
        string line(buf);
        vector<string> args = split(line, ' ');

        if (args.empty()) continue;
        string command = args[0];

        //LOGIN
        if (command == "login") {
            if (state.isLoggedIn()) {
                cout << "User already logged in" << endl;
                continue;
            }
            if (socketThread != nullptr) {
                state.setLoggedIn(false);
                if (handler != nullptr)  handler->close();
                if (socketThread->joinable())  socketThread->join(); 
                delete socketThread;
                socketThread = nullptr;
            }
            if (handler != nullptr) {
                delete handler; 
                handler = nullptr;
            }
            if (args.size() < 4) {
                cout << "Usage: login {host:port} {username} {password}" << endl;
                continue;
            }
            string hostPort = args[1];
            vector<string> hp = split(hostPort, ':');
            if (hp.size() != 2) {
                cout << "Invalid host:port format" << endl;
                continue;
            }
            string host = hp[0];
            short port = (short)stoi(hp[1]);
            string username = args[2];
            string password = args[3];

            handler = new ConnectionHandler(host, port);
            if (!handler->connect()) {
                cerr << "Could not connect to server" << endl;
                delete handler;
                handler = nullptr;
                continue;
            }

            state.setConnectionHandler(handler);
            state.setUsername(username);
            socketThread = new thread(socketThreadFunc, handler, &state);

            string headers = "accept-version:1.2\n" "host:stomp.cs.bgu.ac.il\n"
                             "login:" + username + "\n" "passcode:" + password + "\n";
            
            sendFrame(handler, "CONNECT", headers, "");
        }

        //JOIN
        else if (command == "join") {
            if (!state.isLoggedIn()) {
                cout << "Login first" << endl;
                continue;
            }
            string gameName = args[1];
            int id = state.generateSubscriptionId();
            int receipt = state.generateReceiptId();
            state.addSubscription(gameName, id);

            string headers = "destination:" + gameName + "\n" +
                             "id:" + to_string(id) + "\n" +
                             "receipt:" + to_string(receipt) + "\n";
            sendFrame(handler, "SUBSCRIBE", headers, "");
            cout << "Joined channel " << gameName << endl;
        }

        //EXIT
        else if (command == "exit") {
            if (!state.isLoggedIn()) {
                cout << "Please login first" << endl;
                continue;
            }
            string gameName = args[1];
            int subId = state.getSubscriptionId(gameName);
            
            if (subId == -1) {
                cout << "Not subscribed to channel " << gameName << endl;
                continue;
            }
            int receipt = state.generateReceiptId();
            state.removeSubscription(gameName); 

            string headers = "id:" + to_string(subId) + "\n" +
                             "receipt:" + to_string(receipt) + "\n";

            sendFrame(handler, "UNSUBSCRIBE", headers, "");
        }

        //REPORT
        else if (command == "report") {
            if (!state.isLoggedIn()) {
                cout << "Please login first" << endl;
                continue;
            }
            string file = args[1];
            names_and_events data;
            try {
                data = parseEventsFile(file); 
            } catch (...) {
                cout << "Error parsing JSON file" << endl;
                continue;
            }
            for (const Event& event : data.events) {
                string channelName = data.team_a_name + "_" + data.team_b_name; // שם המשחק מהקובץ

                stringstream bodyStram;
                bodyStram << "user:" << state.getUsername() << "\n";
                bodyStram << "team a:" << event.get_team_a_name() << "\n";
                bodyStram << "team b:" << event.get_team_b_name() << "\n";
                bodyStram << "event name:" << event.get_name() << "\n";
                bodyStram << "time:" << event.get_time() << "\n";
                bodyStram << "general game updates:\n";
                for(auto const& [key, val] : event.get_game_updates()) {
                    bodyStram << key << ":" << val << "\n";
                }
                bodyStram << "team a updates:\n";
                for(auto const& [key, val] : event.get_team_a_updates()) {
                     bodyStram << key << ":" << val << "\n";
                }
                bodyStram << "team b updates:\n";
                for(auto const& [key, val] : event.get_team_b_updates()) {
                     bodyStram << key << ":" << val << "\n";
                }
                bodyStram << "description:\n" << event.get_description() << "\n";

                string headers = "destination:" + channelName + "\n";
                
                sendFrame(handler, "SEND", headers, bodyStram.str());
            }
        }

        //SUMMARY
        else if (command == "summary") {
            
            string gameName = args[1];
            string user = args[2];
            string file = args[3];
            string summary = state.getSummary(gameName, user);
            
            ofstream outFile(file);
            if (outFile.is_open()) {
                outFile << summary;
                outFile.close();
                cout << "Summary created in " << file << endl;
            } else {
                cout << "Error creating file" << endl;
            }
        }

        //LOGOUT
        else if (command == "logout") {
            if (!state.isLoggedIn()) {
                cout << "Not logged in" << endl;
                continue;
            }
            int receipt = state.generateReceiptId();
            string headers = "receipt:" + to_string(receipt) + "\n";
            sendFrame(handler, "DISCONNECT", headers, "");
            state.setLoggedIn(false);

            if (handler != nullptr)  handler->close();
            if (socketThread && socketThread->joinable()) {
                socketThread->join();
                delete socketThread;
                socketThread = nullptr;
            }
            delete handler;
            handler = nullptr;
            if (handler != nullptr){
                state.clearState();
                cout << "Logged out" << endl;
            }
        }
    }
    return 0;
}