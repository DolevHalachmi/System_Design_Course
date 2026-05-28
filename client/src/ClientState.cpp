#include "../include/ClientState.h"
#include <iostream>
#include <sstream>

using namespace std;

ClientState::ClientState() 
    : username(""), isConnected(false), subscriptionIdCounter(0), receiptIdCounter(0), channelToSubId(), gameHistory(), stateMutex(), handler(nullptr) {}

ClientState::~ClientState() {}

void ClientState::setConnectionHandler(ConnectionHandler* ConnectionHandler) {
    handler = ConnectionHandler;
}

ConnectionHandler* ClientState::getHandler() {
    return handler;
}

bool ClientState::isLoggedIn() {
    return isConnected;
}

void ClientState::setLoggedIn(bool status) {
    isConnected = status;
}

void ClientState::setUsername(string name) {
    username = name;
}

string ClientState::getUsername() {
    return username;
}

int ClientState::generateSubscriptionId() {
    return ++subscriptionIdCounter; 
}

int ClientState::generateReceiptId() {
    return ++receiptIdCounter;
}

void ClientState::addSubscription(const string& channelName, int subId) {
    std::lock_guard<std::mutex> lock(stateMutex);
    channelToSubId[channelName] = subId;
}

void ClientState::removeSubscription(const string& channelName) {
    std::lock_guard<std::mutex> lock(stateMutex);
    if (channelToSubId.count(channelName)) {
        channelToSubId.erase(channelName);
    }
}

void ClientState::removeSubscriptionById(int subId) {
    std::lock_guard<std::mutex> lock(stateMutex);
    for (auto it = channelToSubId.begin(); it != channelToSubId.end(); ++it) {
        if (it->second == subId) {
            channelToSubId.erase(it);
            break; 
        }
    }
}

int ClientState::getSubscriptionId(const string& channelName) {
    std::lock_guard<std::mutex> lock(stateMutex);
    if (channelToSubId.count(channelName)) {
        return channelToSubId[channelName];
    }
    return -1; 
}

bool ClientState::isSubscribed(const string& channelName) {
    std::lock_guard<std::mutex> lock(stateMutex);
    return channelToSubId.count(channelName) > 0;
}

void ClientState::addEvent(const string& channelName, const string& username, const Event& event) {
    std::lock_guard<std::mutex> lock(stateMutex);
    gameHistory[channelName][username].push_back(event);
}

string ClientState::getSummary(const string& channelName, const string& username) {
    std::lock_guard<std::mutex> lock(stateMutex);

    if (gameHistory.find(channelName) == gameHistory.end()) {
        return "No events found for this game.";
    }
    if (gameHistory[channelName].find(username) == gameHistory[channelName].end()) {
        return "No events found for this user in this game.";
    }
    const vector<Event>& events = gameHistory[channelName][username];
    if (events.empty()) return "No events found.";

    string teamA = events[0].get_team_a_name();
    string teamB = events[0].get_team_b_name();

    map<string, string> generalStats;
    map<string, string> teamAStats;
    map<string, string> teamBStats;

    for (const auto& event : events) {
        for (const auto& pair : event.get_game_updates()) {
            generalStats[pair.first] = pair.second;
        }
        for (const auto& pair : event.get_team_a_updates()) {
            teamAStats[pair.first] = pair.second;
        }
        for (const auto& pair : event.get_team_b_updates()) {
            teamBStats[pair.first] = pair.second;
        }
    }

    stringstream ss;
    ss << teamA << " vs " << teamB << "\n";
    ss << "Game stats:\n";
    
    ss << "General stats:\n";
    for (const auto& pair : generalStats) {
        ss << pair.first << ": " << pair.second << "\n";
    }

    ss << teamA << " stats:\n";
    for (const auto& pair : teamAStats) {
        ss << pair.first << ": " << pair.second << "\n";
    }

    ss << teamB << " stats:\n";
    for (const auto& pair : teamBStats) {
        ss << pair.first << ": " << pair.second << "\n";
    }

    ss << "Game event reports:\n";
    for (const auto& event : events) {
        ss << event.get_time() << " - " << event.get_name() << ":\n";
        ss << event.get_description() << "\n\n";
    }

    return ss.str();
}

void ClientState::clearState() {
    std::lock_guard<std::mutex> lock(stateMutex);
    channelToSubId.clear();
    gameHistory.clear();
    isConnected = false;
}