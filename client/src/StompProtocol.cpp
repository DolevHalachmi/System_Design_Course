#include "../include/StompProtocol.h"
#include "../include/ClientState.h" 
#include "../include/event.h"
#include <iostream>
#include <sstream>
#include <vector>

StompProtocol::StompProtocol(ClientState& clientState) : state(clientState) {}

enum CommandType {
    CONNECTED,
    MESSAGE,
    RECEIPT,
    ERROR,
    UNKNOWN
};

CommandType stringToCommand(const std::string& command) {
    if (command == "CONNECTED") return CONNECTED;
    if (command == "MESSAGE") return MESSAGE;
    if (command == "RECEIPT") return RECEIPT;
    if (command == "ERROR") return ERROR;
    return UNKNOWN;
}

void StompProtocol::process(const std::string& message) {
    std::stringstream stream(message);
    std::string command;
    std::getline(stream, command);

    CommandType type = stringToCommand(command);
    switch (type) {
        case CONNECTED:
            handleConnected(message);
            break;
        case MESSAGE:
            handleMessage(message);
            break;
        case RECEIPT:
            handleReceipt(message);
            break;
        case ERROR:
            handleError(message);
            break;
        default:
            break;
    }
}


void StompProtocol::handleConnected(const std::string& frame) {

    std::cout << "CONNECTED" << std::endl;
    std::string version = extractHeader(frame, "version");
    std::cout << "Login successful, version:" << version << std::endl;
    
    state.setLoggedIn(true);
}

void StompProtocol::handleMessage(const std::string& frame) {
    try {
        std::string destination = extractHeader(frame, "destination");
        std::string subscription = extractHeader(frame, "subscription");
        std::string message_id = extractHeader(frame, "message-id");
        std::string user = extractHeader(frame, "user");
        
        if (!destination.empty() && destination[0] == '/') {
            destination = destination.substr(1);
        }

        std::cout << "MESSAGE" << std::endl;
        std::cout << "destination:" << destination << std::endl;
        std::cout << "subscription:" << subscription << std::endl;
        std::cout << "message-id:" << message_id << std::endl;


        std::string body = extractBody(frame);
        if (body.empty()) 
            return; 

        Event event = parseEventBody(body);
        if (!user.empty()) {
            state.addEvent(destination, user, event);
        }

        std::cout << "Time: " << event.get_time() << std::endl;
        std::cout << "Event Name: " << event.get_name() << std::endl;
        std::cout << "Description: " << event.get_description() << std::endl;

    } catch (const std::exception& e) {
        std::cerr << "Error: Failed to process MESSAGE frame: " << e.what() << std::endl;
    } catch (...) {
        std::cerr << "Error: Unknown error occurred while handling message" << std::endl;
    }
}

void StompProtocol::handleReceipt(const std::string& frame) {

    std::cout << "RECEIPT" << std::endl;
    std::string receiptId = extractHeader(frame, "receipt-id");
    if (receiptId != "") {
        std::cout << "Receipt " << receiptId << " received" << std::endl;
    }
}

void StompProtocol::handleError(const std::string& frame) {

    std::cout << "ERROR" << std::endl;

    std::string msg = extractHeader(frame, "message");
    std::string body = extractBody(frame);
    std::string receipt_id = extractHeader(frame, "receipt-id");

    if (receipt_id != "") {
            std::cout << "receipt-id:" << receipt_id << std::endl;
    }
    if(msg != ""){
        std::cout << msg << std::endl;
    }
    if (body != "") {
        std::cout << body << std::endl;
    }
    state.setLoggedIn(false);
    state.clearState();
    if (state.getHandler() != nullptr) {
        state.getHandler()->close();
    }
}


std::string StompProtocol::extractHeader(const std::string& frame, const std::string& headerName) {
    
    std::string key = headerName + ":";
    size_t keyPos = frame.find(key);
    
    if (keyPos == std::string::npos) {
        return "";
    }
    keyPos += key.length();
    size_t endOfLine = frame.find('\n', keyPos);
    
    if (endOfLine == std::string::npos) {
        return "";
    }
    return frame.substr(keyPos, endOfLine - keyPos);
}

std::string StompProtocol::extractBody(const std::string& frame) {
    std::string key = "user";
    size_t keyPos = frame.find(key);

    if (keyPos == std::string::npos) {
        key = "\n\n";
        keyPos = frame.find(key);
        if (keyPos == std::string::npos) return "";
        return frame.substr(keyPos + key.length());
    }
    return frame.substr(keyPos + key.length());
}

Event StompProtocol::parseEventBody(const std::string& body) {
    std::string team_a_name, team_b_name, event_name, description;
    int time = 0;
    std::map<std::string, std::string> general_updates, team_a_updates, team_b_updates;
    std::stringstream stream(body);
    std::string line;
    std::string current_section = "";

    while (std::getline(stream, line)) {
        if (!line.empty() && line.back() == '\r') line.pop_back();
        if (line.empty()) continue;
        if (line == "general game updates:") { current_section = "general"; continue; }
        if (line == "team a updates:") { current_section = "team_a"; continue; }
        if (line == "team b updates:") { current_section = "team_b"; continue; }
        if (line == "description:") { current_section = "description"; continue; }
        if (current_section == "description") {
            description += line + "\n";
            continue;
        }

        size_t splitPos = line.find(':');
        if (splitPos != std::string::npos) {
            std::string key = line.substr(0, splitPos);
            std::string value = line.substr(splitPos + 1);

            if (key == "event name") event_name = value;
            else if (key == "time") time = std::stoi(value);
            else if (key == "team a") team_a_name = value;
            else if (key == "team b") team_b_name = value;
            else {
                if (current_section == "general") general_updates[key] = value;
                else if (current_section == "team_a") team_a_updates[key] = value;
                else if (current_section == "team_b") team_b_updates[key] = value;
            }
        }
    }
    return Event(team_a_name, team_b_name, event_name, time, general_updates, team_a_updates, team_b_updates, description);
}

bool StompProtocol::shouldTerminate() {
    return !state.isLoggedIn(); 
}